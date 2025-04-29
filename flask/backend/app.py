from flask import Flask, jsonify
from api.chat import chat_api
from api.predict import predict_api
from api.auth import auth_api  # 导入认证API蓝图
from api.records import records_api  # 导入检测记录API蓝图
from api.clock_in import clock_in_api  # 导入打卡API蓝图
from api.tips import tips_api  # 导入护肤小贴士API蓝图
from services.database import init_db, get_db_connection  # 修正导入路径
from config import DEBUG, HOST, PORT, UPLOAD_FOLDER, RESULT_FOLDER

app = Flask(__name__)

# 配置应用
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['RESULT_FOLDER'] = RESULT_FOLDER
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024

# 注册蓝图
app.register_blueprint(chat_api)
app.register_blueprint(predict_api)
app.register_blueprint(auth_api)  # 注册认证API蓝图
app.register_blueprint(records_api)  # 注册检测记录API蓝图
app.register_blueprint(clock_in_api)  # 注册打卡API蓝图
app.register_blueprint(tips_api)  # 注册护肤小贴士API蓝图

# 健康检查
@app.route('/health', methods=['GET'])
def health_check():
    """健康检查端点"""
    return jsonify({
        'status': 'ok',
        'version': '1.0.0'
    })

# 错误处理
@app.errorhandler(404)
def not_found(error):
    return jsonify({'error': '找不到请求的资源'}), 404

@app.errorhandler(500)
def server_error(error):
    return jsonify({'error': '服务器内部错误'}), 500

# 启动应用
if __name__ == '__main__':
    print("===== AI 皮肤诊断应用后端服务 =====")
    print(f"服务器启动在 {HOST}:{PORT}")
    print(f"调试模式: {'开启' if DEBUG else '关闭'}")
    
    # 在应用启动前初始化数据库（替代before_first_request）
    with app.app_context():
        init_db()
    
    app.run(host=HOST, port=PORT, debug=DEBUG)