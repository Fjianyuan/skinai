import os
from dotenv import load_dotenv

# 加载 .env 文件中的环境变量
load_dotenv()

# 基础目录
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# 应用配置
DEBUG = os.getenv('FLASK_DEBUG', 'False').lower() in ('true', '1', 'yes')
HOST = os.getenv('FLASK_HOST', '127.0.0.1')
PORT = int(os.getenv('FLASK_PORT', 5000))

# 文件路径
MODEL_DIR = os.path.join(BASE_DIR, 'models')
UPLOAD_FOLDER = os.path.join(BASE_DIR, 'uploads')
RESULT_FOLDER = os.path.join(BASE_DIR, 'results')

# 确保目录存在
for directory in [MODEL_DIR, UPLOAD_FOLDER, RESULT_FOLDER]:
    if not os.path.exists(directory):
        os.makedirs(directory)

# 模型文件
YOLO_MODEL_PATH = os.path.join(MODEL_DIR, 'acne615.pt')
FASTAI_MODEL_PATH = os.path.join(MODEL_DIR, 'model_2.1.0.pkl')

# 数据库配置 (通过环境变量配置，确保敏感信息不暴露在代码中)
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'port': int(os.getenv('DB_PORT', 1433)),
    'user': os.getenv('DB_USER', 'sa'),
    'password': os.getenv('DB_PASSWORD', ''),
    'database': os.getenv('DB_NAME', 'app')
}

# DeepSeek API 配置
DEEPSEEK_API_KEY = os.getenv('DEEPSEEK_API_KEY', '')
DEEPSEEK_API_URL = os.getenv('DEEPSEEK_API_URL', 'https://api.deepseek.com/v1/chat/completions')

# 上传文件配置
MAX_CONTENT_LENGTH = 16 * 1024 * 1024  # 限制上传大小为16MB
