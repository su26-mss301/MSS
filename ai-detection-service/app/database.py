from sqlalchemy import create_engine, text
from sqlalchemy.orm import declarative_base, sessionmaker

SQLALCHEMY_DATABASE_URL = "postgresql://admin:admin123@localhost:5439/ai_detection_db"

engine = create_engine(
    SQLALCHEMY_DATABASE_URL
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()


def migrate_detection_logs():
    """Add new columns to existing detection_logs table (idempotent)."""
    migrations = [
        "ALTER TABLE detection_logs ADD COLUMN IF NOT EXISTS session_id VARCHAR",
        "ALTER TABLE detection_logs ADD COLUMN IF NOT EXISTS detection_index INTEGER DEFAULT 0",
        "ALTER TABLE detection_logs ADD COLUMN IF NOT EXISTS class_name VARCHAR",
        "ALTER TABLE detection_logs ADD COLUMN IF NOT EXISTS dominant_color TEXT",
        "ALTER TABLE detection_logs ADD COLUMN IF NOT EXISTS style TEXT",
        "ALTER TABLE detection_logs ADD COLUMN IF NOT EXISTS gender VARCHAR",
        "ALTER TABLE detection_logs ADD COLUMN IF NOT EXISTS occasion TEXT",
        "ALTER TABLE detection_logs ADD COLUMN IF NOT EXISTS image_id VARCHAR",
        "ALTER TABLE detection_logs ADD COLUMN IF NOT EXISTS item_name VARCHAR",
        "ALTER TABLE detection_logs ADD COLUMN IF NOT EXISTS wardrobe_status VARCHAR DEFAULT 'NOT_ADDED'",
        "ALTER TABLE detection_logs ADD COLUMN IF NOT EXISTS clothing_item_id VARCHAR",
        "ALTER TABLE detection_logs ADD COLUMN IF NOT EXISTS added_at TIMESTAMP",
        "ALTER TABLE detection_logs ADD COLUMN IF NOT EXISTS record_status VARCHAR DEFAULT 'ACTIVE'",
        "ALTER TABLE detection_logs ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN DEFAULT FALSE",
        "ALTER TABLE detection_logs ADD COLUMN IF NOT EXISTS pinned_at TIMESTAMP",
        "ALTER TABLE detection_logs ADD COLUMN IF NOT EXISTS deactivated_at TIMESTAMP",
        "ALTER TABLE detection_logs ADD COLUMN IF NOT EXISTS processing_time_ms INTEGER",
    ]

    with engine.begin() as conn:
        for statement in migrations:
            conn.execute(text(statement))

def migrate_outbox_events():
    """Tạo và cập nhật bảng outbox_events theo cách idempotent."""

    statements = [
        """
        CREATE TABLE IF NOT EXISTS outbox_events (
                                                     id SERIAL PRIMARY KEY,
                                                     event_id VARCHAR(100) NOT NULL UNIQUE,
            aggregate_type VARCHAR(100) NOT NULL,
            aggregate_id VARCHAR(100) NOT NULL,
            event_type VARCHAR(100) NOT NULL,
            topic VARCHAR(255) NOT NULL,
            event_key VARCHAR(255) NOT NULL,
            payload TEXT NOT NULL,
            status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
            retry_count INTEGER NOT NULL DEFAULT 0,
            last_error TEXT,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            published_at TIMESTAMP NULL,
            next_retry_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            processing_started_at TIMESTAMP NULL,
            processing_owner VARCHAR(255) NULL
            )
        """,
        """
        CREATE INDEX IF NOT EXISTS idx_ai_outbox_status_next_retry
            ON outbox_events(status, next_retry_at)
        """,
        """
        CREATE INDEX IF NOT EXISTS idx_ai_outbox_created_at
            ON outbox_events(created_at)
        """,
        """
        CREATE INDEX IF NOT EXISTS idx_ai_outbox_aggregate
            ON outbox_events(aggregate_type, aggregate_id)
        """,
    ]

    with engine.begin() as conn:
        for statement in statements:
            conn.execute(text(statement))


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
