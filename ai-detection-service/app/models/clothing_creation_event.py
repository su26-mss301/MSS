from typing import Optional

from pydantic import BaseModel


class ClothingCreationRequestedEvent(BaseModel):
    event_id: str
    detection_log_id: int
    user_id: str

    item_name: str
    category_id: Optional[str] = None
    zone_id: str

    dominant_color: Optional[str] = None
    style: Optional[str] = None
    confidence_score: Optional[float] = None
    image_id: Optional[str] = None

    created_at: str