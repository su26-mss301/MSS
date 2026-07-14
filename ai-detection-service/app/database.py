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
    ]

    with engine.begin() as conn:
        for statement in migrations:
            conn.execute(text(statement))


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
