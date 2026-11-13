# SkinAI

SkinAI is a student team project for exploring acne detection on facial images. The Android client sends an image to a Flask API, which runs a custom YOLOv8 detector and returns an annotated image and a detection count. The app also supports detection history and daily check-ins.

## Research and implementation

In the research prototype, we evaluated a staged approach that used U-Net to segment the face, YOLOv8 to detect acne within that region, and U-Net to delineate lesion contours. Jianyuan Fu led the student team and proposed the staged approach. The team deployed an Android client and a Flask backend.

The prediction endpoint in this repository runs YOLOv8 directly on the uploaded image. It uses OpenCV to render detections. If configured, it can send a detection summary and an optional user description to DeepSeek to generate a text response.

## Repository layout

| Path | Contents |
| --- | --- |
| [`app/`](app/) | Android client written in Java |
| [`flask/backend/api/predict.py`](flask/backend/api/predict.py) | Image upload, YOLOv8 inference, and result rendering |
| [`flask/backend/services/database.py`](flask/backend/services/database.py) | SQL Server schema initialization and persistence |
| [`flask/backend/.env.example`](flask/backend/.env.example) | Backend configuration template |
| [`PROJECT_RESUME_DOC.md`](PROJECT_RESUME_DOC.md) | Project overview in Chinese |

## Local setup

You will need Android Studio, a Python environment, SQL Server with ODBC Driver 17, and compatible YOLOv8 weights. The repository does not include the trained weights, training images, or the U-Net training pipeline. The Flask app loads its detector during startup, so even the health check requires a usable model file.

From the repository root, set up the backend in PowerShell:

```powershell
cd flask/backend
python -m venv .venv
Copy-Item .env.example .env
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

Set the SQL Server connection details in `.env`, using an existing database. Add compatible weights at `flask/backend/models/acne615.pt`. Set `DEEPSEEK_API_KEY` only if you want to enable the text response. Run the following from `flask/backend`:

```powershell
.\.venv\Scripts\python.exe app.py
```

Check `http://127.0.0.1:5000/health`. Then open the repository in Android Studio and set `CURRENT_IP` in [`ApiConfig.java`](app/src/main/java/com/example/tor210/api/ApiConfig.java). Use `LOCAL_IP` (`10.0.2.2`) for the Android emulator, or set `SERVER_IP` to the backend computer's LAN address for a physical device.

Use test images you have permission to process. The backend saves uploads locally, and the current Android client uses HTTP for development. SkinAI is an educational prototype for image analysis and has no clinical validation.

The code is available under the [MIT License](LICENSE).
