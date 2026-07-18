import json
import os
import threading
from datetime import datetime

from confluent_kafka import Consumer, KafkaError
from sqlalchemy.orm import Session

from app.database import SessionLocal
from app.models.detection_log import DetectionLog


KAFKA_BOOTSTRAP_SERVERS = os.getenv(
    "KAFKA_BOOTSTRAP_SERVERS",
    "localhost:9092",
)

KAFKA_CLOTHING_CREATED_TOPIC = os.getenv(
    "KAFKA_CLOTHING_CREATED_TOPIC",
    "clothing-created",
)

KAFKA_CLOTHING_CREATED_GROUP = os.getenv(
    "KAFKA_CLOTHING_CREATED_GROUP",
    "ai-detection-clothing-created-group",
)


consumer = Consumer({
    "bootstrap.servers": KAFKA_BOOTSTRAP_SERVERS,
    "group.id": KAFKA_CLOTHING_CREATED_GROUP,
    "auto.offset.reset": "earliest",
    "enable.auto.commit": False,
})


def process_clothing_created_event(payload: dict) -> None:
    detection_log_id = payload.get("detectionLogId")
    clothing_item_id = payload.get("clothingItemId")
    image_id = payload.get("imageId")
    item_name = payload.get("itemName")

    if detection_log_id is None:
        raise ValueError("Event không có detectionLogId")

    if not clothing_item_id:
        raise ValueError("Event không có clothingItemId")

    db: Session = SessionLocal()

    try:
        log = (
            db.query(DetectionLog)
            .filter(DetectionLog.id == detection_log_id)
            .first()
        )

        if not log:
            print(
                f"[KAFKA] Không tìm thấy DetectionLog id={detection_log_id}"
            )
            return

        # Idempotent: event gửi lại cũng không gây lỗi.
        if (
                log.wardrobe_status == "ADDED"
                and log.clothing_item_id == clothing_item_id
        ):
            print(
                f"[KAFKA] DetectionLog {detection_log_id} "
                "đã được cập nhật trước đó"
            )
            return

        log.wardrobe_status = "ADDED"
        log.clothing_item_id = clothing_item_id
        log.added_at = datetime.utcnow()

        if item_name:
            log.item_name = item_name

        if image_id:
            log.image_id = image_id

        db.commit()

        print(
            f"[KAFKA] DetectionLog {detection_log_id} "
            f"đã chuyển thành ADDED, "
            f"clothingItemId={clothing_item_id}"
        )

    except Exception:
        db.rollback()
        raise

    finally:
        db.close()


def consume_clothing_created_events() -> None:
    consumer.subscribe([KAFKA_CLOTHING_CREATED_TOPIC])

    print(
        "[KAFKA] AI consumer đang lắng nghe topic "
        f"{KAFKA_CLOTHING_CREATED_TOPIC}"
    )

    try:
        while True:
            message = consumer.poll(1.0)

            if message is None:
                continue

            if message.error():
                if message.error().code() == KafkaError._PARTITION_EOF:
                    continue

                print(
                    f"[KAFKA] Consumer error: {message.error()}"
                )
                continue

            try:
                payload = json.loads(
                    message.value().decode("utf-8")
                )

                process_clothing_created_event(payload)

                # Chỉ commit offset sau khi cập nhật DB thành công.
                consumer.commit(
                    message=message,
                    asynchronous=False,
                )

            except Exception as exception:
                print(
                    "[KAFKA] Xử lý ClothingCreatedEvent thất bại: "
                    f"{exception}"
                )

                # Không commit offset để có thể xử lý lại.
                # Tránh vòng lặp quá nhanh nếu event lỗi dữ liệu.
                continue

    finally:
        consumer.close()


def start_clothing_created_consumer() -> threading.Thread:
    thread = threading.Thread(
        target=consume_clothing_created_events,
        name="clothing-created-consumer",
        daemon=True,
    )

    thread.start()
    return thread