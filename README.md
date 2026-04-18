<h1 align="center">🩺 SkinAI — Intelligent Skin Diagnostic System</h1>

<p align="center">
  <em>An AI-powered mobile application that detects facial acne using YOLOv8, generates professional diagnostic reports via DeepSeek LLM, and tracks skin health over time.</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-Java%2017-3DDC84?logo=android&logoColor=white" alt="Android"/>
  <img src="https://img.shields.io/badge/Backend-Flask%203.0-000000?logo=flask&logoColor=white" alt="Flask"/>
  <img src="https://img.shields.io/badge/AI-YOLOv8-FF6F00?logo=pytorch&logoColor=white" alt="YOLOv8"/>
  <img src="https://img.shields.io/badge/LLM-DeepSeek-4A90D9?logo=openai&logoColor=white" alt="DeepSeek"/>
  <img src="https://img.shields.io/badge/Database-SQL%20Server-CC2927?logo=microsoftsqlserver&logoColor=white" alt="SQL Server"/>
  <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License"/>
</p>

---

## ✨ Highlights

- 🔬 **Real-time Acne Detection** — Custom-trained YOLOv8 model (`acne615.pt`) identifies and localizes acne types (whitehead, blackhead, papule, pustule, nodule, cyst) on facial photos.
- 🎨 **Enhanced Medical Visualization** — Uses OpenCV to render detection results with Gaussian-blur halos and confidence-mapped color gradients (red-yellow), providing a friendlier visual experience than traditional bounding boxes.
- 🤖 **AI-Powered Diagnostic Reports** — Structures detection data into prompts for DeepSeek LLM, generating multi-dimensional reports covering pathology analysis, skincare tips, lifestyle advice, and medical intervention.
- 📅 **Skin Health Tracker** — Calendar-based daily check-in system with streak tracking and trend visualization.
- ⚡ **Optimized Image Pipeline** — Smart compression (LANCZOS resampling + 85% JPEG quality) reduces storage by ~60% without affecting detection accuracy.

---

## 🏗️ Architecture

```
┌─────────────────┐         HTTP/JSON          ┌──────────────────────┐
│                 │  ◄───────────────────────►  │                      │
│  Android Client │                             │   Flask Backend      │
│  (Java / SDK34) │    Upload Photo             │   (Python 3.8+)      │
│                 │  ────────────────────────►  │                      │
│  • Home         │                             │  ┌────────────────┐  │
│  • Detection    │    Detection Result +       │  │  YOLOv8 Model  │  │
│  • Check-in     │    AI Diagnosis Report      │  │  (acne615.pt)  │  │
│  • Profile      │  ◄────────────────────────  │  └────────┬───────┘  │
│                 │                             │           │          │
└─────────────────┘                             │  ┌────────▼───────┐  │
                                                │  │  DeepSeek API  │  │
                                                │  │  (LLM Report)  │  │
                                                │  └────────────────┘  │
                                                │           │          │
                                                │  ┌────────▼───────┐  │
                                                │  │  SQL Server    │  │
                                                │  │  (Data Store)  │  │
                                                │  └────────────────┘  │
                                                └──────────────────────┘
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Mobile Client** | Java, Android SDK 34, Material Design, Glide, MPAndroidChart |
| **Backend Server** | Python 3.8+, Flask 3.0, RESTful API, Gunicorn/Waitress |
| **Computer Vision** | Ultralytics YOLOv8, OpenCV, NumPy, Pillow |
| **Large Language Model** | DeepSeek Chat API (Prompt Engineering) |
| **Database** | Microsoft SQL Server, PyODBC |
| **DevOps / Tools** | Git, Android Studio, PyCharm, Postman |

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** (Hedgehog or later)
- **Python 3.8+**
- **Microsoft SQL Server** (with ODBC Driver 17)
- **DeepSeek API Key** ([Get one here](https://platform.deepseek.com/))

### 1. Clone the Repository

```bash
git clone https://github.com/Fjianyuan/tor210.git
cd tor210
```

### 2. Backend Setup

```bash
cd flask/backend

# Create and activate virtual environment
python -m venv venv
source venv/bin/activate        # Linux/macOS
# venv\Scripts\activate         # Windows

# Install dependencies
pip install -r requirements.txt

# Configure environment variables
cp .env.example .env
# Edit .env with your database password and DeepSeek API key

# Place your trained model files in the models/ directory:
#   - models/acne615.pt    (YOLOv8 acne detection model)
#   - models/model_2.1.0.pkl (optional FastAI model)

# Run the server
python app.py
```

The backend will start at `http://localhost:5000`. Verify with:
```bash
curl http://localhost:5000/health
```

### 3. Android Client Setup

1. Open the project root in **Android Studio**.
2. Edit `app/src/main/java/com/example/tor210/api/ApiConfig.java`:
   - Set `SERVER_IP` to your backend server's IP address.
   - For emulator testing, use `LOCAL_IP` (`10.0.2.2`).
3. Build and run on your device or emulator.

---

## 📁 Project Structure

```
tor210/
├── app/                          # Android client
│   └── src/main/java/com/example/tor210/
│       ├── api/                  # API config & HTTP utilities
│       ├── check/                # Detection feature
│       ├── clockin/              # Check-in feature
│       ├── home/                 # Home page
│       ├── login/                # Authentication
│       ├── predict/              # AI prediction UI
│       ├── porfile/              # User profile
│       └── utils/                # Utility classes
├── flask/backend/                # Python Flask backend
│   ├── api/                      # API blueprints (auth, predict, clock_in, etc.)
│   ├── services/                 # Database & prediction services
│   ├── models/                   # AI model files (*.pt, *.pkl)
│   ├── config.py                 # Server configuration
│   ├── app.py                    # Flask application entry
│   └── .env.example              # Environment variable template
└── README.md
```

---

## 📊 Key Achievements

| Metric | Result |
|--------|--------|
| **Detection Accuracy** | mAP@0.5 > 85% on custom test set |
| **Inference Latency** | < 1.5s per image (including network transfer) |
| **Storage Reduction** | ~60% via smart compression without accuracy loss |
| **Image Load Speed** | 3x faster with Glide disk caching strategy |

---

## 🔑 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/login` | User login |
| `POST` | `/api/auth/register` | User registration |
| `GET` | `/api/auth/verify` | Token verification |
| `POST` | `/api/predict/` | Upload image for acne detection |
| `GET` | `/api/records?user_id=` | Get detection history |
| `POST` | `/api/clock-in/` | Submit daily check-in |
| `GET` | `/api/tips/` | Get skincare tips |
| `GET` | `/health` | Health check |

---

## ⚠️ Important Notes

- **Model files** (`acne615.pt`, `model_2.1.0.pkl`) are not included in this repository due to their large size. Please train your own model or contact the author.
- **Environment variables** must be configured before running the backend. See `.env.example` for details.
- This project is for **educational and research purposes only**. It is not intended to replace professional medical advice.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

## 👤 Author

**Fjianyuan** — Full-stack Developer

- Independently designed and implemented the complete architecture from Android client to Flask backend.
- Led AI model data preprocessing and backend inference API development.
- Innovatively combined Computer Vision (YOLOv8) with LLM (DeepSeek) for an end-to-end skin diagnostic solution.