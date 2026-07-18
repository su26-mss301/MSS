from sqlalchemy import Boolean, Column, Integer, String, Float, DateTime, Text
from datetime import datetime
from app.database import Base
from app.enums.wardrobe_status import WardrobeStatus



class DetectionLog(Base):
    __tablename__ = "detection_logs"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(String, index=True)
    session_id = Column(String, nullable=True, index=True)
    detection_index = Column(Integer, default=0)

    category = Column(String, index=True)
    class_name = Column(String, nullable=True)
    confidence = Column(Float)
    dominant_color = Column(Text, nullable=True)
    style = Column(Text, nullable=True)
    gender = Column(String, nullable=True)
    occasion = Column(Text, nullable=True)

    image_url = Column(String, nullable=True)
    image_id = Column(String, nullable=True)

    item_name = Column(String, nullable=True)
    wardrobe_status = Column(
        String,
        default=WardrobeStatus.NOT_ADDED.value,
        nullable=False,
        index=True
    )
    clothing_item_id = Column(String, nullable=True)
    added_at = Column(DateTime, nullable=True)

    status = Column(String, default="success")
    record_status = Column(String, default="ACTIVE", index=True)
    is_pinned = Column(Boolean, default=False)
    pinned_at = Column(DateTime, nullable=True)
    deactivated_at = Column(DateTime, nullable=True)

    created_at = Column(DateTime, default=datetime.utcnow, index=True)
