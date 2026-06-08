from contextlib import asynccontextmanager
from fastapi import FastAPI, UploadFile, File
from dotenv import load_dotenv
import py_eureka_client.eureka_client as eureka_client
import os

from app.detector import predict_image

load_dotenv()

APP_NAME = os.getenv("APP_NAME")
PORT = int(os.getenv("PORT"))
INSTANCE_HOST = os.getenv("INSTANCE_HOST")

EUREKA_HOST = os.getenv("EUREKA_HOST")
EUREKA_PORT = os.getenv("EUREKA_PORT")
EUREKA_SERVER = f"http://{EUREKA_HOST}:{EUREKA_PORT}/eureka"

UPLOAD_DIR = "uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)


@asynccontextmanager
async def lifespan(app: FastAPI):
    print(f"Registering {APP_NAME} to Eureka: {EUREKA_SERVER}")

    await eureka_client.init_async(
        eureka_server=EUREKA_SERVER,
        app_name=APP_NAME,
        instance_host=INSTANCE_HOST,
        instance_port=PORT,
        health_check_url=f"http://{INSTANCE_HOST}:{PORT}/health",
        status_page_url=f"http://{INSTANCE_HOST}:{PORT}/docs",
        home_page_url=f"http://{INSTANCE_HOST}:{PORT}/"
    )

    print(f"{APP_NAME} registered successfully!")

    yield

    try:
        eureka_client.stop()
        print(f"{APP_NAME} stopped and unregistered from Eureka")
    except Exception as e:
        print(f"Failed to stop Eureka client: {e}")


app = FastAPI(
    title="AI Detection Service",
    version="1.0.0",
    lifespan=lifespan
)


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


@app.post("/detect")
async def detect(file: UploadFile = File(...)):
    file_path = os.path.join(UPLOAD_DIR, file.filename)

    content = await file.read()

    with open(file_path, "wb") as buffer:
        buffer.write(content)

    detections = predict_image(file_path)

    return {
        "success": True,
        "detections": detections
    }