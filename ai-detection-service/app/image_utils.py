import cv2
import numpy as np


def decode_upload_bytes(content: bytes) -> np.ndarray | None:
    """Giải mã bytes upload thành ảnh BGR in-memory, không ghi disk."""
    if not content:
        return None

    buffer = np.frombuffer(content, dtype=np.uint8)
    image = cv2.imdecode(buffer, cv2.IMREAD_COLOR)
    return image
