from datetime import datetime

from sqlalchemy import (
    Column,
    Integer,
    String,
    DateTime,
    Text,
    Index,
)

from app.database import Base
from app.enums.outbox_status import OutboxStatus


class OutboxEvent(Base):
    __tablename__ = "outbox_events"

    id = Column(
        Integer,
        primary_key=True,
        autoincrement=True,
    )

    event_id = Column(
        String(100),
        nullable=False,
        unique=True,
        index=True,
    )

    aggregate_type = Column(
        String(100),
        nullable=False,
    )

    aggregate_id = Column(
        String(100),
        nullable=False,
    )

    event_type = Column(
        String(100),
        nullable=False,
    )

    topic = Column(
        String(255),
        nullable=False,
    )

    event_key = Column(
        String(255),
        nullable=False,
    )

    # Lưu JSON dưới dạng TEXT giống Wardrobe Service,
    # không dùng JSONB.
    payload = Column(
        Text,
        nullable=False,
    )

    status = Column(
        String(30),
        nullable=False,
        default=OutboxStatus.PENDING.value,
        index=True,
    )

    retry_count = Column(
        Integer,
        nullable=False,
        default=0,
    )

    last_error = Column(
        Text,
        nullable=True,
    )

    created_at = Column(
        DateTime,
        nullable=False,
        default=datetime.utcnow,
        index=True,
    )

    published_at = Column(
        DateTime,
        nullable=True,
    )

    next_retry_at = Column(
        DateTime,
        nullable=True,
        default=datetime.utcnow,
    )

    processing_started_at = Column(
        DateTime,
        nullable=True,
    )

    processing_owner = Column(
        String(255),
        nullable=True,
    )


Index(
    "idx_ai_outbox_publishable",
    OutboxEvent.status,
    OutboxEvent.next_retry_at,
    OutboxEvent.created_at,
)

Index(
    "idx_ai_outbox_aggregate",
    OutboxEvent.aggregate_type,
    OutboxEvent.aggregate_id,
)