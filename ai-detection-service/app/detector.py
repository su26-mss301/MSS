from ultralytics import YOLO
from dotenv import load_dotenv
import os

load_dotenv()

MODEL_PATH = os.getenv("MODEL_PATH")

model = YOLO(MODEL_PATH)


def predict_image(image_path):
    results = model.predict(
        source=image_path,
        conf=0.25,
        verbose=False
    )

    detections = []

    for result in results:
        for box in result.boxes:

            cls = int(box.cls[0])
            conf = float(box.conf[0])

            detections.append({
                "class_id": cls,
                "class_name": model.names[cls],
                "confidence": round(conf, 4),
                "bbox": {
                    "x1": round(float(box.xyxy[0][0]), 2),
                    "y1": round(float(box.xyxy[0][1]), 2),
                    "x2": round(float(box.xyxy[0][2]), 2),
                    "y2": round(float(box.xyxy[0][3]), 2)
                }
            })

    return detections