import os

# 基础目录
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# 应用配置
DEBUG = True
HOST = '0.0.0.0'
PORT = 5000

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

# 数据库配置
DB_CONFIG = {
    'host': 'localhost',
    'port': 1433,
    'user': 'sa',
    'password': '682528',
    'database': 'app'
}

# 上传文件配置
MAX_CONTENT_LENGTH = 16 * 1024 * 1024  # 限制上传大小为16MB