import sys
import os

# 添加父目录到sys.path以解决导入问题
current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.append(current_dir)

from api.tips import init_sample_tips
from flask import Flask

app = Flask(__name__)

with app.app_context():
    print("开始初始化示例小贴士数据...")
    response = init_sample_tips()
    print(response.json) 