from enum import Enum


class WardrobeStatus(str, Enum):
    NOT_ADDED = "NOT_ADDED"
    WAITING_WARDROBE = "WAITING_WARDROBE"
    ADDED = "ADDED"
    FAILED = "FAILED"