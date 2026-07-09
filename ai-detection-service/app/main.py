from fastapi import FastAPI, UploadFile, File, Depends, HTTPException
from dotenv import load_dotenv
import os
from sqlalchemy.orm import Session
from sqlalchemy import func

from app.detector import predict_image
from app.image_utils import decode_upload_bytes
from app.auth_context import CurrentUser, require_roles
from app.database import get_db, engine
from app.models.detection_log import DetectionLog, Base

load_dotenv()
Base.metadata.create_all(bind=engine)

APP_NAME = os.getenv("APP_NAME", "ai-detection-service")
PORT = int(os.getenv("PORT", 8084))

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
        db: Session = Depends(get_db)
):
    """
    Nhận diện trang phục từ ảnh upload (xử lý in-memory, không lưu disk).

    Yêu cầu: ROLE_USER hoặc ROLE_ADMIN (ADMIN tự động được phép
    do thứ bậc role trong require_roles).
    """
    content = await file.read()
    image = decode_upload_bytes(content)

    if image is None:
        raise HTTPException(status_code=400, detail="Không thể đọc ảnh upload")

    detections = predict_image(image)

    if detections:
        primary = detections[0]
        log = DetectionLog(
            user_id=current_user.user_id,
            category=primary["class_name"],
            confidence=primary["confidence"] * 100,
            status="success"
        )
        db.add(log)
        db.commit()

    return {
        "success": True,
        "userId": current_user.user_id,
        "email": current_user.email,
        "role": current_user.role,
        "detections": detections
    }

@app.get("/analytics/stats")
def get_stats(db: Session = Depends(get_db)):
    total = db.query(DetectionLog).count()
    avg_conf = db.query(func.avg(DetectionLog.confidence)).scalar() or 0
    high_acc = db.query(DetectionLog).filter(DetectionLog.confidence >= 90).count()
    return {
        "total": total,
        "avg_confidence": round(avg_conf, 1),
        "high_accuracy": high_acc
    }

@app.get("/analytics/daily")
def get_daily(db: Session = Depends(get_db)):
    results = db.query(
        func.to_char(DetectionLog.created_at, 'YYYY-MM-DD').label('day'),
        func.count(DetectionLog.id).label('detections'),
        func.avg(DetectionLog.confidence).label('accuracy')
    ).group_by('day').order_by('day').limit(7).all()
    return [{"day": r.day, "detections": r.detections, "accuracy": round(r.accuracy or 0, 1)} for r in results]

@app.get("/analytics/monthly")
def get_monthly(db: Session = Depends(get_db)):
    results = db.query(
        func.to_char(DetectionLog.created_at, 'YYYY-MM').label('month'),
        func.avg(DetectionLog.confidence).label('confidence')
    ).group_by('month').order_by('month').limit(6).all()
    return [{"month": r.month, "confidence": round(r.confidence or 0, 1)} for r in results]

@app.get("/analytics/categories")
def get_categories(db: Session = Depends(get_db)):
    results = db.query(
        DetectionLog.category,
        func.count(DetectionLog.id).label('detections'),
        func.avg(DetectionLog.confidence).label('accuracy')
    ).group_by(DetectionLog.category).all()
    return [{"category": r.category, "detections": r.detections, "accuracy": round(r.accuracy or 0, 1)} for r in results]

@app.get("/analytics/recent")
def get_recent(db: Session = Depends(get_db)):
    results = db.query(DetectionLog).order_by(DetectionLog.created_at.desc()).limit(10).all()
    return [{
        "id": r.id,
        "item": r.category,
        "category": r.category,
        "confidence": round(r.confidence, 1),
        "time": r.created_at.isoformat(),
        "status": r.status,
        "img": r.image_url or "https://images.unsplash.com/photo-1556905055-8f358a7a47b2?w=60&h=60&fit=crop"
    } for r in results]
