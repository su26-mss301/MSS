from fastapi import FastAPI, UploadFile, File, Depends
from dotenv import load_dotenv
import os

from app.detector import predict_image
from app.auth_context import (
    CurrentUser,
    require_roles,
)

load_dotenv()

APP_NAME = os.getenv("APP_NAME", "ai-detection-service")
PORT = int(os.getenv("PORT", 8084))

UPLOAD_DIR = "uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

app = FastAPI(
    title="AI Detection Service",
    version="1.0.0",
)


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
def health():
    return {
        "service": APP_NAME,
        "status": "UP"
    }


# ──────────────────────────────────────────────
# Protected endpoints (yêu cầu đăng nhập + role)
# Gateway (KeycloakAuthGlobalFilter) đã xác thực JWT
# và inject X-Auth-* headers trước khi request tới đây.
# ──────────────────────────────────────────────

@app.post("/detect")
async def detect(
        file: UploadFile = File(...),
        current_user: CurrentUser = Depends(require_roles("ROLE_USER")),
):
    """
    Nhận diện trang phục từ ảnh upload.

    Yêu cầu: ROLE_USER hoặc ROLE_ADMIN (ADMIN tự động được phép
    do thứ bậc role trong require_roles).
    """
    file_path = os.path.join(UPLOAD_DIR, file.filename)

    content = await file.read()

    with open(file_path, "wb") as buffer:
        buffer.write(content)

    detections = predict_image(file_path)

    return {
        "success": True,
        "userId": current_user.user_id,
        "email": current_user.email,
        "role": current_user.role,
        "detections": detections
    }
