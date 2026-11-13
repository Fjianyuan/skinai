# SkinAI

SkinAI is a student team project for exploring acne detection on facial images. The Android client sends an image to a Flask API and receives an annotated image and a count. The app also supports detection history and daily check-ins.

## Research and implementation

Jianyuan Fu led the student team and proposed using facial segmentation to restrict acne detection. The team deployed an Android client and a Flask backend.

The backend supports two-stage segmentation. The first U-Net segments the face. The second U-Net takes the masked face crop and segments acne lesions. The backend clips the lesion mask to the face region, extracts contours, and draws them on the original image. In this mode the API returns `lesion_contours` as polygons in original-image pixel coordinates and reports the number of segmented regions. It leaves `acne_types` empty because the model predicts a binary mask. The original YOLOv8 detector is also available.

If configured, the backend can send a detection summary and an optional user description to DeepSeek to generate a text response.

## Repository layout

| Path | Contents |
| --- | --- |
| [`app/`](app/) | Android client written in Java |
| [`flask/backend/api/predict.py`](flask/backend/api/predict.py) | Image upload, YOLOv8 inference, and result rendering |
| [`flask/backend/services/unet_segmentation.py`](flask/backend/services/unet_segmentation.py) | Face segmentation, lesion segmentation, and contour extraction |
| [`flask/backend/services/database.py`](flask/backend/services/database.py) | SQL Server schema initialization and persistence |
| [`flask/backend/.env.example`](flask/backend/.env.example) | Backend configuration template |
| [`PROJECT_RESUME_DOC.md`](PROJECT_RESUME_DOC.md) | Project overview in Chinese |

## Local setup

You will need Android Studio, a Python environment, SQL Server with ODBC Driver 17, and compatible trained model files. Model files and training images are kept outside the repository. Flask loads the models on the first prediction request.

From the repository root, set up the backend in PowerShell:

```powershell
cd flask/backend
python -m venv .venv
Copy-Item .env.example .env
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

Set the SQL Server connection details in `.env`, using an existing database. Select an inference mode and put its files in `flask/backend/models/`.

- `PREDICTION_MODE=unet` requires `face_unet.onnx` and `lesion_unet.onnx`.
- `PREDICTION_MODE=yolo` uses `acne615.pt`.
- The default, `PREDICTION_MODE=auto`, selects U-Net when both ONNX files exist and YOLOv8 when neither exists. A single ONNX file leaves the prediction endpoint unavailable until the pair is complete or the mode is set explicitly to `yolo`.

For U-Net, export each trained model as ONNX with one float32 RGB input in NCHW format, normalized to `[0, 1]`, and one output shaped `[1, 1, H, W]` containing foreground logits before sigmoid. Set `FACE_UNET_INPUT_SIZE` and `LESION_UNET_INPUT_SIZE` to the respective square input sizes used when exporting the models. Both default to 256. The lesion model should be trained on face crops with pixels outside the face masked out.

Set `DEEPSEEK_API_KEY` only if you want to enable the text response. Run the following from `flask/backend`:

```powershell
.\.venv\Scripts\python.exe app.py
```

Check `http://127.0.0.1:5000/health`. Then open the repository in Android Studio and set `CURRENT_IP` in [`ApiConfig.java`](app/src/main/java/com/example/tor210/api/ApiConfig.java). Use `LOCAL_IP` (`10.0.2.2`) for the Android emulator. For a physical device on a trusted LAN, set `FLASK_HOST` in `.env` to the backend computer's LAN address and use that same address for `SERVER_IP`. Keep `FLASK_DEBUG=False`, including in any existing `.env` created from an earlier template.

Use test images you have permission to process. The backend saves uploads locally, and the current Android client uses HTTP for development. SkinAI is an educational prototype for image analysis and has no clinical validation.

The code is available under the [MIT License](LICENSE).

