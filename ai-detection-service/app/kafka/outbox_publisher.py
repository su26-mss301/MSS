import json
import os
import socket
import threading
import time
from datetime import datetime, timedelta

from sqlalchemy import or_
from sqlalchemy.orm import Session

from app.database import SessionLocal
from app.enums.outbox_status import OutboxStatus
from app.kafka.producer import publish_event
from app.models.outbox_event import OutboxEvent


OUTBOX_BATCH_SIZE = int(
    os.getenv("OUTBOX_BATCH_SIZE", "20")
)

OUTBOX_POLL_INTERVAL_SECONDS = float(
    os.getenv("OUTBOX_POLL_INTERVAL_SECONDS", "3")
)

OUTBOX_PROCESSING_TIMEOUT_SECONDS = int(
    os.getenv("OUTBOX_PROCESSING_TIMEOUT_SECONDS", "300")
)

OUTBOX_MAX_BACKOFF_SECONDS = int(
    os.getenv("OUTBOX_MAX_BACKOFF_SECONDS", "300")
)

PROCESSING_OWNER = (
    f"{socket.gethostname()}-"
    f"{os.getpid()}-"
    f"{id(threading.current_thread())}"
)

OUTBOX_RETENTION_DAYS = int(
    os.getenv("OUTBOX_RETENTION_DAYS", "30")
)

OUTBOX_CLEANUP_INTERVAL_SECONDS = int(
    os.getenv("OUTBOX_CLEANUP_INTERVAL_SECONDS", "86400")
)


def _calculate_retry_delay(retry_count: int) -> int:
    """
    Exponential backoff:

    retry 1 -> 2 giây
    retry 2 -> 4 giây
    retry 3 -> 8 giây
    ...
    tối đa 300 giây.
    """
    safe_retry_count = min(max(retry_count, 1), 20)
    delay = 2 ** safe_retry_count

    return min(delay, OUTBOX_MAX_BACKOFF_SECONDS)


def _truncate_error(error: Exception, max_length: int = 4000) -> str:
    message = f"{type(error).__name__}: {error}"

    if len(message) <= max_length:
        return message

    return message[:max_length]


def _recover_stuck_events(db: Session) -> int:
    """
    Phục hồi event bị kẹt ở PROCESSING khi AI Service bị crash
    sau lúc claim nhưng trước lúc cập nhật kết quả.
    """
    expired_before = (
            datetime.utcnow()
            - timedelta(seconds=OUTBOX_PROCESSING_TIMEOUT_SECONDS)
    )

    stuck_events = (
        db.query(OutboxEvent)
        .filter(
            OutboxEvent.status == OutboxStatus.PROCESSING.value,
            OutboxEvent.processing_started_at.isnot(None),
            OutboxEvent.processing_started_at <= expired_before,
            )
        .with_for_update(skip_locked=True)
        .limit(OUTBOX_BATCH_SIZE)
        .all()
    )

    now = datetime.utcnow()

    for event in stuck_events:
        event.status = OutboxStatus.FAILED.value
        event.last_error = (
            "Event được phục hồi vì bị kẹt ở PROCESSING"
        )
        event.next_retry_at = now
        event.processing_started_at = None
        event.processing_owner = None

    if stuck_events:
        db.commit()

        print(
            f"[AI-OUTBOX] Đã phục hồi "
            f"{len(stuck_events)} event bị kẹt"
        )

    return len(stuck_events)


def _claim_events() -> list[dict]:
    """
    Claim một batch event bằng FOR UPDATE SKIP LOCKED.

    Transaction này chỉ:
    - lấy event;
    - chuyển PROCESSING;
    - commit;
    - giải phóng row lock.

    Không gửi Kafka trong transaction này.
    """
    db: Session = SessionLocal()

    try:
        now = datetime.utcnow()

        events = (
            db.query(OutboxEvent)
            .filter(
                OutboxEvent.status.in_([
                    OutboxStatus.PENDING.value,
                    OutboxStatus.FAILED.value,
                ]),
                or_(
                    OutboxEvent.next_retry_at.is_(None),
                    OutboxEvent.next_retry_at <= now,
                    ),
            )
            .order_by(
                OutboxEvent.created_at.asc(),
                OutboxEvent.id.asc(),
            )
            .with_for_update(skip_locked=True)
            .limit(OUTBOX_BATCH_SIZE)
            .all()
        )

        claimed_events: list[dict] = []

        for event in events:
            event.status = OutboxStatus.PROCESSING.value
            event.processing_started_at = now
            event.processing_owner = PROCESSING_OWNER

            claimed_events.append({
                "id": event.id,
                "event_id": event.event_id,
                "topic": event.topic,
                "event_key": event.event_key,
                "payload": event.payload,
                "retry_count": event.retry_count or 0,
            })

        db.commit()

        return claimed_events

    except Exception:
        db.rollback()
        raise

    finally:
        db.close()


def _mark_published(outbox_id: int) -> None:
    db: Session = SessionLocal()

    try:
        event = (
            db.query(OutboxEvent)
            .filter(OutboxEvent.id == outbox_id)
            .first()
        )

        if not event:
            print(
                f"[AI-OUTBOX] Không tìm thấy event id={outbox_id}"
            )
            return

        if event.status == OutboxStatus.PUBLISHED.value:
            return

        event.status = OutboxStatus.PUBLISHED.value
        event.published_at = datetime.utcnow()
        event.last_error = None
        event.next_retry_at = None
        event.processing_started_at = None
        event.processing_owner = None

        db.commit()

    except Exception:
        db.rollback()
        raise

    finally:
        db.close()


def _mark_failed(
        outbox_id: int,
        error: Exception,
) -> None:
    db: Session = SessionLocal()

    try:
        event = (
            db.query(OutboxEvent)
            .filter(OutboxEvent.id == outbox_id)
            .first()
        )

        if not event:
            print(
                f"[AI-OUTBOX] Không tìm thấy event id={outbox_id}"
            )
            return

        if event.status == OutboxStatus.PUBLISHED.value:
            return

        next_retry_count = (event.retry_count or 0) + 1
        delay_seconds = _calculate_retry_delay(
            next_retry_count
        )

        event.status = OutboxStatus.FAILED.value
        event.retry_count = next_retry_count
        event.last_error = _truncate_error(error)
        event.next_retry_at = (
                datetime.utcnow()
                + timedelta(seconds=delay_seconds)
        )
        event.processing_started_at = None
        event.processing_owner = None

        db.commit()

        print(
            f"[AI-OUTBOX] Publish thất bại "
            f"id={event.id}, "
            f"eventId={event.event_id}, "
            f"retryCount={next_retry_count}, "
            f"retrySau={delay_seconds}s, "
            f"error={event.last_error}"
        )

    except Exception:
        db.rollback()
        raise

    finally:
        db.close()


def _publish_single_event(event: dict) -> None:
    try:
        payload = json.loads(event["payload"])

        publish_event(
            topic=event["topic"],
            key=event["event_key"],
            payload=payload,
        )

        _mark_published(event["id"])

        print(
            f"[AI-OUTBOX] Publish thành công "
            f"id={event['id']}, "
            f"eventId={event['event_id']}, "
            f"topic={event['topic']}"
        )

    except Exception as exception:
        try:
            _mark_failed(
                event["id"],
                exception,
            )
        except Exception as update_exception:
            print(
                f"[AI-OUTBOX] Không thể cập nhật FAILED "
                f"id={event['id']}: {update_exception}"
            )


def run_outbox_publisher() -> None:
    print(
        "[AI-OUTBOX] Publisher đã khởi động, "
        f"owner={PROCESSING_OWNER}"
    )

    last_recovery_at = datetime.min
    last_cleanup_at = datetime.min

    while True:
        try:
            now = datetime.utcnow()

            # Recovery mỗi khoảng 60 giây.
            if (
                    now - last_recovery_at
            ).total_seconds() >= 60:
                recovery_db: Session = SessionLocal()

                try:
                    _recover_stuck_events(recovery_db)
                except Exception as exception:
                    recovery_db.rollback()

                    print(
                        "[AI-OUTBOX] Recovery thất bại: "
                        f"{exception}"
                    )
                finally:
                    recovery_db.close()

                last_recovery_at = now

            if (
                    now - last_cleanup_at
            ).total_seconds() >= OUTBOX_CLEANUP_INTERVAL_SECONDS:
                try:
                    _cleanup_published_events()
                except Exception as exception:
                     print(
                        "[AI-OUTBOX] Cleanup thất bại: "
                        f"{type(exception).__name__}: {exception}"
                    )

                last_cleanup_at = now

            events = _claim_events()

            if events:
                print(
                    f"[AI-OUTBOX] Đã claim "
                    f"{len(events)} event"
                )

            for event in events:
                _publish_single_event(event)

        except Exception as exception:
            print(
                "[AI-OUTBOX] Publisher loop gặp lỗi: "
                f"{type(exception).__name__}: {exception}"
            )

        time.sleep(OUTBOX_POLL_INTERVAL_SECONDS)


def start_outbox_publisher() -> threading.Thread:
    thread = threading.Thread(
        target=run_outbox_publisher,
        name="ai-outbox-publisher",
        daemon=True,
    )

    thread.start()

    return thread

def _cleanup_published_events() -> int:
    db: Session = SessionLocal()

    try:
        expired_before = (
                datetime.utcnow()
                - timedelta(days=OUTBOX_RETENTION_DAYS)
        )

        deleted_count = (
            db.query(OutboxEvent)
            .filter(
                OutboxEvent.status
                == OutboxStatus.PUBLISHED.value,
                OutboxEvent.published_at.isnot(None),
                OutboxEvent.published_at < expired_before,
                )
            .delete(synchronize_session=False)
        )

        db.commit()

        if deleted_count > 0:
            print(
                f"[AI-OUTBOX] Đã cleanup {deleted_count} "
                f"event PUBLISHED quá {OUTBOX_RETENTION_DAYS} ngày"
            )

        return deleted_count

    except Exception:
        db.rollback()
        raise

    finally:
        db.close()