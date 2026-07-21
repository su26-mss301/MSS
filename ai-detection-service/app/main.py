from fastapi import FastAPI, UploadFile, File, Depends, HTTPException, Query, Form
from dotenv import load_dotenv
import os
import json
import uuid
import math
import time
from datetime import datetime, timedelta, timezone
from typing import Optional
from pydantic import BaseModel
from sqlalchemy.orm import Session
from sqlalchemy import func, text

from app.detector import predict_image
from app.image_utils import decode_upload_bytes
from app.auth_context import CurrentUser, get_current_user, require_roles
from app.database import (
    get_db,
    engine,
    migrate_detection_logs,
    migrate_outbox_events,
)
from app.models.detection_log import DetectionLog, Base
from app.models.outbox_event import OutboxEvent

from app.kafka.clothing_created_consumer import (
    start_clothing_created_consumer,
)

from app.kafka.outbox_publisher import (
    start_outbox_publisher,
)

load_dotenv()
Base.metadata.create_all(bind=engine)
migrate_detection_logs()
migrate_outbox_events()

APP_NAME = os.getenv("APP_NAME", "ai-detection-service")
PORT = int(os.getenv("PORT", 8084))
INACTIVE_RETENTION_DAYS = 7

CLOTHING_REQUEST_TOPIC = os.getenv(
    "KAFKA_CLOTHING_REQUEST_TOPIC",
    "clothing-creation-requested",
)


def _purge_expired_inactive_logs(db: Session) -> int:
    """Xóa vĩnh viễn bản ghi đã thêm tủ ở trạng thái INACTIVE quá 7 ngày."""
    cutoff = datetime.utcnow() - timedelta(days=INACTIVE_RETENTION_DAYS)
    expired = db.query(DetectionLog).filter(
        DetectionLog.record_status == "INACTIVE",
        DetectionLog.wardrobe_status == "ADDED",
        DetectionLog.deactivated_at.isnot(None),
        DetectionLog.deactivated_at <= cutoff,
    ).all()

    for log in expired:
        db.delete(log)

    if expired:
        db.commit()

    return len(expired)


app = FastAPI(
    title="AI Detection Service",
    version="1.0.0",
)


@app.on_event("startup")
def startup_event():
    start_clothing_created_consumer()
    start_outbox_publisher()


class MarkAddedRequest(BaseModel):
    clothing_item_id: str
    item_name: str
    image_id: Optional[str] = None


class RequestAddClothingBody(BaseModel):
    item_name: str
    category_id: Optional[str] = None
    zone_id: str
    dominant_color: Optional[str] = None
    style: Optional[str] = None
    confidence_score: Optional[float] = None
    image_id: Optional[str] = None


def _serialize_color(color) -> Optional[str]:
    if color is None:
        return None
    if isinstance(color, str):
        return color
    return json.dumps(color, ensure_ascii=False)


def _serialize_list(value) -> Optional[str]:
    if value is None:
        return None
    if isinstance(value, str):
        return value
    return json.dumps(value, ensure_ascii=False)


def _parse_json_field(value: Optional[str]):
    if not value:
        return None
    try:
        return json.loads(value)
    except (json.JSONDecodeError, TypeError):
        return value


def _log_to_summary(log: DetectionLog) -> dict:
    days_until_deletion = None
    if log.record_status == "INACTIVE" and log.deactivated_at:
        elapsed_days = (datetime.utcnow() - log.deactivated_at).days
        days_until_deletion = max(0, INACTIVE_RETENTION_DAYS - elapsed_days)

    return {
        "id": log.id,
        "userId": log.user_id,
        "className": log.class_name or log.category,
        "category": log.class_name or log.category,
        "confidence": round(log.confidence, 1) if log.confidence is not None else 0,
        "dominantColor": _parse_json_field(log.dominant_color),
        "style": _parse_json_field(log.style),
        "gender": log.gender,
        "occasion": _parse_json_field(log.occasion),
        "imageId": log.image_id,
        "itemName": log.item_name,
        "wardrobeStatus": log.wardrobe_status or "NOT_ADDED",
        "clothingItemId": log.clothing_item_id,
        "isPinned": bool(log.is_pinned),
        "recordStatus": log.record_status or "ACTIVE",
        "createdAt": log.created_at.isoformat() if log.created_at else None,
        "addedAt": log.added_at.isoformat() if log.added_at else None,
        "deactivatedAt": log.deactivated_at.isoformat() if log.deactivated_at else None,
        "daysUntilDeletion": days_until_deletion,
    }


def _log_to_detail(log: DetectionLog) -> dict:
    detail = _log_to_summary(log)
    detail["status"] = log.status
    detail["pinnedAt"] = log.pinned_at.isoformat() if log.pinned_at else None
    return detail


def _active_logs_filter():
    return DetectionLog.record_status == "ACTIVE"


def _user_active_logs_filter(user_id: str):
    return (
        DetectionLog.record_status == "ACTIVE",
        DetectionLog.user_id == user_id,
    )


def _normalize_granularity(granularity: str | None) -> str:
    period = (granularity or "week").lower()
    return period if period in {"day", "week", "month"} else "week"


def _user_period_bounds(granularity: str | None):
    now = datetime.utcnow()
    period = _normalize_granularity(granularity)
    if period == "day":
        start = now.replace(hour=0, minute=0, second=0, microsecond=0)
    elif period == "month":
        start = now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
    else:
        start = (now - timedelta(days=6)).replace(hour=0, minute=0, second=0, microsecond=0)
    return start, now, period


def _user_period_filters(user_id: str, granularity: str | None):
    start, end, _ = _user_period_bounds(granularity)
    return (
        *_user_active_logs_filter(user_id),
        DetectionLog.created_at >= start,
        DetectionLog.created_at <= end,
    )


def _format_week_label(day_key: str) -> str:
    date = datetime.strptime(day_key, "%Y-%m-%d")
    labels = ["T2", "T3", "T4", "T5", "T6", "T7", "CN"]
    return labels[date.weekday()]


def _build_user_activity_series(db: Session, user_id: str, granularity: str | None) -> list[dict]:
    start, end, period = _user_period_bounds(granularity)
    filters = _user_period_filters(user_id, granularity)

    if period == "day":
        rows = db.query(
            func.to_char(DetectionLog.created_at, 'HH24').label('bucket'),
            func.count(DetectionLog.id).label('detections'),
            func.avg(DetectionLog.confidence).label('accuracy'),
        ).filter(*filters).group_by('bucket').order_by('bucket').all()
        bucket_map = {
            str(row.bucket).zfill(2): {
                "detections": row.detections,
                "accuracy": round(row.accuracy or 0, 1),
            }
            for row in rows
        }
        series = []
        for hour in range(24):
            key = f"{hour:02d}"
            values = bucket_map.get(key, {"detections": 0, "accuracy": 0})
            series.append({
                "label": f"{hour}h",
                "detections": values["detections"],
                "accuracy": values["accuracy"],
            })
        return series

    if period == "month":
        rows = db.query(
            func.to_char(DetectionLog.created_at, 'YYYY-MM-DD').label('bucket'),
            func.count(DetectionLog.id).label('detections'),
            func.avg(DetectionLog.confidence).label('accuracy'),
        ).filter(*filters).group_by('bucket').order_by('bucket').all()
        bucket_map = {
            row.bucket: {
                "detections": row.detections,
                "accuracy": round(row.accuracy or 0, 1),
            }
            for row in rows
        }
        series = []
        cursor = start
        while cursor.date() <= end.date():
            key = cursor.strftime("%Y-%m-%d")
            values = bucket_map.get(key, {"detections": 0, "accuracy": 0})
            series.append({
                "label": str(cursor.day),
                "detections": values["detections"],
                "accuracy": values["accuracy"],
            })
            cursor += timedelta(days=1)
        return series

    rows = db.query(
        func.to_char(DetectionLog.created_at, 'YYYY-MM-DD').label('bucket'),
        func.count(DetectionLog.id).label('detections'),
        func.avg(DetectionLog.confidence).label('accuracy'),
    ).filter(*filters).group_by('bucket').order_by('bucket').all()
    bucket_map = {
        row.bucket: {
            "detections": row.detections,
            "accuracy": round(row.accuracy or 0, 1),
        }
        for row in rows
    }
    series = []
    for offset in range(6, -1, -1):
        day = (end - timedelta(days=offset)).strftime("%Y-%m-%d")
        values = bucket_map.get(day, {"detections": 0, "accuracy": 0})
        series.append({
            "label": _format_week_label(day),
            "detections": values["detections"],
            "accuracy": values["accuracy"],
        })
    return series


def _calculate_trend_percent(this_week: int, last_week: int) -> float:
    if last_week == 0:
        return 100.0 if this_week > 0 else 0.0
    return round((this_week - last_week) * 1000 / last_week) / 10.0


def _subtract_months(dt: datetime, months: int) -> datetime:
    year = dt.year
    month = dt.month - months
    while month <= 0:
        month += 12
        year -= 1
    return dt.replace(year=year, month=month, day=1, hour=0, minute=0, second=0, microsecond=0)


def _build_analytics_summary(db: Session, granularity: str = "week") -> dict:
    period = (granularity or "week").lower()
    if period not in {"day", "week", "month"}:
        period = "week"

    now = datetime.utcnow()

    if period == "day":
        today_start = now.replace(hour=0, minute=0, second=0, microsecond=0)
        yesterday_start = today_start - timedelta(days=1)
        current_start, current_end = today_start, now
        previous_start, previous_end = yesterday_start, today_start
    elif period == "month":
        month_start = now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
        prev_month_start = _subtract_months(month_start, 1)
        current_start, current_end = month_start, now
        previous_start, previous_end = prev_month_start, month_start
    else:
        week_ago = now - timedelta(days=7)
        two_weeks_ago = now - timedelta(days=14)
        current_start, current_end = week_ago, now
        previous_start, previous_end = two_weeks_ago, week_ago

    base_query = db.query(DetectionLog).filter(_active_logs_filter())
    total = base_query.count()
    this_period = base_query.filter(
        DetectionLog.created_at >= current_start,
        DetectionLog.created_at < current_end,
    ).count()
    last_period = base_query.filter(
        DetectionLog.created_at >= previous_start,
        DetectionLog.created_at < previous_end,
    ).count()

    if period == "day":
        today_start = now.replace(hour=0, minute=0, second=0, microsecond=0)
        hourly_rows = db.query(
            func.to_char(DetectionLog.created_at, 'HH24').label('hour'),
            func.count(DetectionLog.id).label('count'),
        ).filter(
            _active_logs_filter(),
            DetectionLog.created_at >= today_start,
        ).group_by('hour').order_by('hour').all()
        hourly_map = {str(row.hour).zfill(2): row.count for row in hourly_rows}
        daily = [
            {"date": f"{hour:02d}", "count": hourly_map.get(f"{hour:02d}", 0)}
            for hour in range(24)
        ]
    elif period == "month":
        daily_from = now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
        daily_offsets = range((now - daily_from).days, -1, -1)
        daily_rows = db.query(
            func.to_char(DetectionLog.created_at, 'YYYY-MM-DD').label('day'),
            func.count(DetectionLog.id).label('count'),
        ).filter(
            _active_logs_filter(),
            DetectionLog.created_at >= daily_from,
        ).group_by('day').order_by('day').all()
        daily_map = {row.day: row.count for row in daily_rows}
        daily = []
        for offset in daily_offsets:
            day = (now - timedelta(days=offset)).strftime('%Y-%m-%d')
            daily.append({"date": day, "count": daily_map.get(day, 0)})
    else:
        daily_from = (now - timedelta(days=6)).replace(hour=0, minute=0, second=0, microsecond=0)
        daily_rows = db.query(
            func.to_char(DetectionLog.created_at, 'YYYY-MM-DD').label('day'),
            func.count(DetectionLog.id).label('count'),
        ).filter(
            _active_logs_filter(),
            DetectionLog.created_at >= daily_from,
        ).group_by('day').order_by('day').all()
        daily_map = {row.day: row.count for row in daily_rows}
        daily = []
        for offset in range(6, -1, -1):
            day = (now - timedelta(days=offset)).strftime('%Y-%m-%d')
            daily.append({"date": day, "count": daily_map.get(day, 0)})

    month_start = now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
    monthly_from = _subtract_months(month_start, 5)
    monthly_rows = db.query(
        func.to_char(DetectionLog.created_at, 'YYYY-MM').label('month'),
        func.count(DetectionLog.id).label('count'),
    ).filter(
        _active_logs_filter(),
        DetectionLog.created_at >= monthly_from,
    ).group_by('month').order_by('month').all()
    monthly_map = {row.month: row.count for row in monthly_rows}

    monthly = []
    for offset in range(5, -1, -1):
        month_dt = _subtract_months(month_start, offset)
        month_key = month_dt.strftime('%Y-%m')
        monthly.append({"month": month_key, "count": monthly_map.get(month_key, 0)})

    return {
        "total": total,
        "thisWeek": this_period,
        "lastWeek": last_period,
        "weekTrendPercent": _calculate_trend_percent(this_period, last_period),
        "daily": daily,
        "monthly": monthly,
    }


def _added_visible_logs_filter():
    return DetectionLog.record_status.in_(["ACTIVE", "INACTIVE"])


def _build_page_response(items: list, page: int, size: int, total_items: int) -> dict:
    total_pages = max(1, math.ceil(total_items / size)) if size > 0 else 1
    return {
        "items": items,
        "page": page,
        "size": size,
        "totalItems": total_items,
        "totalPages": total_pages,
        "first": page == 0,
        "last": page >= total_pages - 1,
        "hasNext": page < total_pages - 1,
        "hasPrevious": page > 0,
    }


def _create_detection_logs(
        db: Session,
        current_user: CurrentUser,
        detections: list,
        image_id: Optional[str],
        processing_time_ms: Optional[int] = None,
) -> list[DetectionLog]:
    session_id = str(uuid.uuid4())
    logs: list[DetectionLog] = []

    for index, detection in enumerate(detections):
        log = DetectionLog(
            user_id=current_user.user_id,
            session_id=session_id,
            detection_index=index,
            category=detection["class_name"],
            class_name=detection["class_name"],
            confidence=round(detection["confidence"] * 100, 2),
            dominant_color=_serialize_color(detection.get("dominant_color")),
            style=_serialize_list(detection.get("style")),
            gender=detection.get("gender"),
            occasion=_serialize_list(detection.get("occasion")),
            image_id=image_id,
            processing_time_ms=processing_time_ms if index == 0 else None,
            wardrobe_status="NOT_ADDED",
            record_status="ACTIVE",
            is_pinned=False,
            status="success",
        )
        db.add(log)
        logs.append(log)

    db.flush()
    return logs


# ──────────────────────────────────────────────
# Public endpoints (không cần đăng nhập)
# ──────────────────────────────────────────────

@app.get("/")
def root():
    return {
        "service": APP_NAME,
        "status": "UP"
    }


@app.get("/health")
def health(db: Session = Depends(get_db)):
    database_status = "UP"
    database_message = "Database connection is healthy"

    try:
        db.execute(text("SELECT 1"))
    except Exception as exception:
        database_status = "DOWN"
        database_message = f"Database connection failed: {str(exception)}"

    overall_status = (
        "UP"
        if database_status == "UP"
        else "DEGRADED"
    )

    return {
        "service": APP_NAME,
        "status": overall_status,
        "components": {
            "database": {
                "status": database_status,
                "message": database_message,
            }
        },
        "checkedAt": datetime.utcnow().isoformat(),
    }


# ──────────────────────────────────────────────
# Protected endpoints (yêu cầu đăng nhập + role)
# Gateway (KeycloakAuthGlobalFilter) đã xác thực JWT
# và inject X-Auth-* headers trước khi request tới đây.
# ──────────────────────────────────────────────

@app.post("/detect")
async def detect(
        file: UploadFile = File(...),
        image_id: Optional[str] = Form(default=None),
        current_user: CurrentUser = Depends(require_roles("ROLE_USER")),
        db: Session = Depends(get_db)
):
    """
    Nhận diện trang phục từ ảnh upload (xử lý in-memory, không lưu disk).

    Yêu cầu: ROLE_USER hoặc ROLE_ADMIN (ADMIN tự động được phép
    do thứ bậc role trong require_roles).
    """
    content = await file.read()
    image = decode_upload_bytes(content)

    if image is None:
        raise HTTPException(status_code=400, detail="Không thể đọc ảnh upload")

    started_at = time.perf_counter()
    detections = predict_image(image)
    processing_time_ms = int((time.perf_counter() - started_at) * 1000)

    if detections:
        logs = _create_detection_logs(
            db,
            current_user,
            detections,
            image_id,
            processing_time_ms,
        )
        db.commit()

        for detection, log in zip(detections, logs):
            detection["logId"] = log.id

    return {
        "success": True,
        "userId": current_user.user_id,
        "email": current_user.email,
        "role": current_user.role,
        "detections": detections
    }


@app.post(
    "/detection-logs/{log_id}/request-add",
    status_code=202,
)
def request_add_clothing(
        log_id: int,
        body: RequestAddClothingBody,
        current_user: CurrentUser = Depends(require_roles("ROLE_USER")),
        db: Session = Depends(get_db),
):
    print(
        "[AI-OUTBOX-NEW] request_add_clothing đang chạy code mới, "
        f"log_id={log_id}"
    )
    log = (
        db.query(DetectionLog)
        .filter(DetectionLog.id == log_id)
        .first()
    )

    if not log:
        raise HTTPException(
            status_code=404,
            detail="Không tìm thấy bản ghi nhận diện",
        )

    if (
            log.user_id != current_user.user_id
            and current_user.role != "ROLE_ADMIN"
    ):
        raise HTTPException(
            status_code=403,
            detail="Bạn không có quyền cập nhật bản ghi này",
        )

    if log.wardrobe_status == "ADDED":
        return {
            "success": True,
            "status": "ADDED",
            "message": "Vật phẩm đã được thêm vào tủ đồ",
            "clothingItemId": log.clothing_item_id,
        }

    if log.wardrobe_status == "WAITING_WARDROBE":
        return {
            "success": True,
            "status": "WAITING_WARDROBE",
            "message": "Yêu cầu đang được xử lý",
        }

    event_id = str(uuid.uuid4())
    now = datetime.utcnow()

    event_payload = {
        "eventId": event_id,
        "eventType": "CLOTHING_CREATION_REQUESTED",
        "detectionLogId": log.id,
        "userId": current_user.user_id,
        "itemName": body.item_name,
        "categoryId": body.category_id,
        "zoneId": body.zone_id,
        "dominantColor": body.dominant_color,
        "style": body.style,
        "confidenceScore": body.confidence_score,
        "imageId": body.image_id or log.image_id,
        "createdAt": now.isoformat(),
    }

    try:
        log.wardrobe_status = "WAITING_WARDROBE"
        log.item_name = body.item_name

        if body.image_id:
            log.image_id = body.image_id

        outbox_event = OutboxEvent(
            event_id=event_id,
            aggregate_type="DETECTION_LOG",
            aggregate_id=str(log.id),
            event_type="CLOTHING_CREATION_REQUESTED",
            topic=CLOTHING_REQUEST_TOPIC,
            event_key=event_id,
            payload=json.dumps(
                event_payload,
                ensure_ascii=False,
                default=str,
            ),
            status="PENDING",
            retry_count=0,
            created_at=now,
            next_retry_at=now,
        )

        db.add(outbox_event)

        print(
            f"[AI-OUTBOX-NEW] chuẩn bị lưu outbox eventId={event_id}, "
            f"detectionLogId={log.id}"
        )

        db.flush()

        print(
            f"[AI-OUTBOX-NEW] đã flush outbox id={outbox_event.id}, "
            f"eventId={event_id}"
        )

        db.commit()

        print(
            f"[AI-OUTBOX-NEW] đã commit outbox eventId={event_id}"
        )

        db.refresh(log)

    except Exception as exception:
        db.rollback()

        print(
            f"[AI-OUTBOX-ERROR] Không thể lưu outbox: "
            f"{type(exception).__name__}: {exception}"
        )

        raise HTTPException(
            status_code=500,
            detail=f"Không thể lưu yêu cầu thêm tủ đồ: {str(exception)}",
        )

    return {
        "success": True,
        "requestId": event_id,
        "status": "WAITING_WARDROBE",
        "message": "Yêu cầu đã được tiếp nhận và đang xử lý",
        "data": _log_to_detail(log),
    }


@app.patch("/detection-logs/{log_id}/mark-added")
def mark_detection_added(
        log_id: int,
        body: MarkAddedRequest,
        current_user: CurrentUser = Depends(require_roles("ROLE_USER")),
        db: Session = Depends(get_db),
):
    log = db.query(DetectionLog).filter(DetectionLog.id == log_id).first()
    if not log:
        raise HTTPException(status_code=404, detail="Không tìm thấy bản ghi nhận diện")

    if log.user_id != current_user.user_id and current_user.role != "ROLE_ADMIN":
        raise HTTPException(status_code=403, detail="Bạn không có quyền cập nhật bản ghi này")

    log.wardrobe_status = "ADDED"
    log.clothing_item_id = body.clothing_item_id
    log.item_name = body.item_name
    log.added_at = datetime.utcnow()
    if body.image_id:
        log.image_id = body.image_id
    db.commit()

    return {
        "success": True,
        "data": _log_to_detail(log),
    }


@app.get("/admin/detection-history")
def get_admin_detection_history(
        tab: str = Query(default="not_added", pattern="^(not_added|added)$"),
        page: int = Query(default=0, ge=0),
        size: int = Query(default=10, ge=1, le=100),
        sort: str = Query(default="newest", pattern="^(newest|oldest)$"),
        current_user: CurrentUser = Depends(require_roles("ROLE_ADMIN")),
        db: Session = Depends(get_db),
):
    _purge_expired_inactive_logs(db)

    if tab == "not_added":
        base_query = db.query(DetectionLog).filter(
            _active_logs_filter(),
            DetectionLog.wardrobe_status == "NOT_ADDED",
        )
    else:
        base_query = db.query(DetectionLog).filter(
            _added_visible_logs_filter(),
            DetectionLog.wardrobe_status == "ADDED",
        )

    total_items = base_query.count()

    order_clauses = [DetectionLog.is_pinned.desc()]
    if sort == "oldest":
        order_clauses.append(DetectionLog.created_at.asc())
    else:
        order_clauses.append(DetectionLog.created_at.desc())

    results = (
        base_query
        .order_by(*order_clauses)
        .offset(page * size)
        .limit(size)
        .all()
    )

    not_added_count = db.query(DetectionLog).filter(
        _active_logs_filter(),
        DetectionLog.wardrobe_status == "NOT_ADDED",
    ).count()

    added_count = db.query(DetectionLog).filter(
        _added_visible_logs_filter(),
        DetectionLog.wardrobe_status == "ADDED",
    ).count()

    response = _build_page_response(
        [_log_to_summary(log) for log in results],
        page,
        size,
        total_items,
    )
    response["notAddedCount"] = not_added_count
    response["addedCount"] = added_count
    return response


@app.get("/admin/detection-history/{log_id}")
def get_admin_detection_detail(
        log_id: int,
        current_user: CurrentUser = Depends(require_roles("ROLE_ADMIN")),
        db: Session = Depends(get_db),
):
    log = db.query(DetectionLog).filter(DetectionLog.id == log_id).first()
    if not log or log.record_status not in ("ACTIVE", "INACTIVE"):
        raise HTTPException(status_code=404, detail="Không tìm thấy bản ghi nhận diện")

    return _log_to_detail(log)


@app.patch("/admin/detection-history/{log_id}/pin")
def toggle_detection_pin(
        log_id: int,
        current_user: CurrentUser = Depends(require_roles("ROLE_ADMIN")),
        db: Session = Depends(get_db),
):
    log = db.query(DetectionLog).filter(DetectionLog.id == log_id).first()
    if not log or log.record_status not in ("ACTIVE", "INACTIVE"):
        raise HTTPException(status_code=404, detail="Không tìm thấy bản ghi nhận diện")

    log.is_pinned = not bool(log.is_pinned)
    log.pinned_at = datetime.utcnow() if log.is_pinned else None
    db.commit()

    return {
        "success": True,
        "data": _log_to_summary(log),
    }


@app.delete("/admin/detection-history/{log_id}")
def hard_delete_detection(
        log_id: int,
        current_user: CurrentUser = Depends(require_roles("ROLE_ADMIN")),
        db: Session = Depends(get_db),
):
    """Xóa trực tiếp — chỉ áp dụng cho bản ghi chưa thêm tủ đồ."""
    log = db.query(DetectionLog).filter(DetectionLog.id == log_id).first()
    if not log or log.record_status != "ACTIVE":
        raise HTTPException(status_code=404, detail="Không tìm thấy bản ghi nhận diện")

    if log.wardrobe_status != "NOT_ADDED":
        raise HTTPException(
            status_code=400,
            detail="Chỉ có thể xóa trực tiếp bản ghi chưa thêm vào tủ đồ",
        )

    db.delete(log)
    db.commit()

    return {
        "success": True,
        "message": "Đã xóa bản ghi nhận diện",
    }


@app.patch("/admin/detection-history/{log_id}/toggle-status")
def toggle_detection_status(
        log_id: int,
        current_user: CurrentUser = Depends(require_roles("ROLE_ADMIN")),
        db: Session = Depends(get_db),
):
    """Chuyển trạng thái hoạt động/ngừng hoạt động — chỉ áp dụng cho bản ghi đã thêm tủ."""
    log = db.query(DetectionLog).filter(DetectionLog.id == log_id).first()
    if not log:
        raise HTTPException(status_code=404, detail="Không tìm thấy bản ghi nhận diện")

    if log.wardrobe_status != "ADDED":
        raise HTTPException(
            status_code=400,
            detail="Chỉ có thể chuyển trạng thái với bản ghi đã thêm vào tủ đồ",
        )

    if log.record_status == "ACTIVE":
        log.record_status = "INACTIVE"
        log.deactivated_at = datetime.utcnow()
        log.is_pinned = False
        log.pinned_at = None
        message = "Đã chuyển sang ngừng hoạt động. Bản ghi sẽ tự xóa sau 7 ngày."
    elif log.record_status == "INACTIVE":
        log.record_status = "ACTIVE"
        log.deactivated_at = None
        message = "Đã kích hoạt lại bản ghi"
    else:
        raise HTTPException(status_code=400, detail="Bản ghi không thể chuyển trạng thái")

    db.commit()

    return {
        "success": True,
        "message": message,
        "data": _log_to_summary(log),
    }


@app.get("/admin/analytics/summary")
def get_admin_analytics_summary(
    granularity: str = Query(default="week"),
    db: Session = Depends(get_db),
    _: CurrentUser = Depends(require_roles("ROLE_ADMIN")),
):
    return _build_analytics_summary(db, granularity)


@app.get("/analytics/stats")
def get_stats(
    granularity: str = Query(default="week"),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
):
    period_filters = _user_period_filters(current_user.user_id, granularity)
    total = db.query(DetectionLog).filter(*period_filters).count()
    avg_conf = db.query(func.avg(DetectionLog.confidence)).filter(*period_filters).scalar() or 0
    high_acc = db.query(DetectionLog).filter(
        *period_filters,
        DetectionLog.confidence >= 90,
    ).count()
    session_rows = db.query(
        func.max(DetectionLog.processing_time_ms).label("processing_time_ms")
    ).filter(
        *period_filters,
        DetectionLog.processing_time_ms.isnot(None),
    ).group_by(DetectionLog.session_id).all()
    avg_processing_ms = (
        sum(row.processing_time_ms for row in session_rows) / len(session_rows)
        if session_rows
        else None
    )
    avg_processing_time_sec = (
        round(float(avg_processing_ms) / 1000, 1)
        if avg_processing_ms is not None
        else None
    )
    return {
        "total": total,
        "avg_confidence": round(avg_conf, 1),
        "high_accuracy": high_acc,
        "avg_processing_time_sec": avg_processing_time_sec,
        "granularity": _normalize_granularity(granularity),
    }


@app.get("/analytics/daily")
def get_daily(
    granularity: str = Query(default="week"),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
):
    return _build_user_activity_series(db, current_user.user_id, granularity)


@app.get("/analytics/monthly")
def get_monthly(
    granularity: str = Query(default="week"),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
):
    series = _build_user_activity_series(db, current_user.user_id, granularity)
    return [{"month": point["label"], "confidence": point["accuracy"]} for point in series]


@app.get("/analytics/categories")
def get_categories(
    granularity: str = Query(default="week"),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
):
    results = db.query(
        DetectionLog.category,
        func.count(DetectionLog.id).label('detections'),
        func.avg(DetectionLog.confidence).label('accuracy')
    ).filter(*_user_period_filters(current_user.user_id, granularity)).group_by(DetectionLog.category).all()
    return [{"category": r.category, "detections": r.detections, "accuracy": round(r.accuracy or 0, 1)} for r in
            results]


def _format_utc_iso(value: datetime | None) -> str | None:
    if not value:
        return None
    if value.tzinfo is None:
        value = value.replace(tzinfo=timezone.utc)
    return value.isoformat().replace("+00:00", "Z")


@app.get("/analytics/recent")
def get_recent(
    granularity: str = Query(default="week"),
    page: int = Query(default=0, ge=0),
    size: int = Query(default=10, ge=1, le=50),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
):
    filters = _user_period_filters(current_user.user_id, granularity)
    total_items = db.query(DetectionLog).filter(*filters).count()
    results = (
        db.query(DetectionLog)
        .filter(*filters)
        .order_by(DetectionLog.created_at.desc())
        .offset(page * size)
        .limit(size)
        .all()
    )
    items = [{
        "id": r.id,
        "item": r.class_name or r.category,
        "category": r.class_name or r.category,
        "confidence": round(r.confidence, 1),
        "time": _format_utc_iso(r.created_at),
        "status": r.status,
        "imageId": r.image_id,
    } for r in results]
    return _build_page_response(items, page, size, total_items)
