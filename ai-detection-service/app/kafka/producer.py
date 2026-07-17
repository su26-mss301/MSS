import json
import os
from typing import Any

from confluent_kafka import Producer


KAFKA_BOOTSTRAP_SERVERS = os.getenv(
    "KAFKA_BOOTSTRAP_SERVERS",
    "localhost:9092",
)

producer = Producer({
    "bootstrap.servers": KAFKA_BOOTSTRAP_SERVERS,
    "client.id": "ai-detection-service",
    "acks": "all",
})


def _delivery_callback(error, message) -> None:
    if error is not None:
        print(
            f"[KAFKA] Gửi event thất bại: {error}"
        )
        return

    print(
        "[KAFKA] Event đã gửi thành công "
        f"topic={message.topic()} "
        f"partition={message.partition()} "
        f"offset={message.offset()}"
    )


def publish_event(
        topic: str,
        key: str,
        payload: dict[str, Any],
) -> None:
    producer.produce(
        topic=topic,
        key=key.encode("utf-8"),
        value=json.dumps(
            payload,
            ensure_ascii=False,
            default=str,
        ).encode("utf-8"),
        callback=_delivery_callback,
    )

    remaining_messages = producer.flush(10)

    if remaining_messages > 0:
        raise RuntimeError(
            f"Còn {remaining_messages} event chưa gửi được tới Kafka"
        )