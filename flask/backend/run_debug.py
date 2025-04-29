"""
调试脚本，用于启动Flask应用并捕获可能的错误
"""
import sys
import os
import traceback

# 将当前目录添加到模块搜索路径
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

try:
    print("开始导入模块...")
    from flask import Flask
    print("Flask导入成功")
    
    try:
        print("尝试导入api.chat...")
        from api.chat import chat_api
        print("导入chat_api成功")
    except Exception as e:
        print(f"导入api.chat时出错: {str(e)}")
        traceback.print_exc()
    
    try:
        print("尝试导入api.predict...")
        from api.predict import predict_api
        print("导入predict_api成功")
    except Exception as e:
        print(f"导入api.predict时出错: {str(e)}")
        traceback.print_exc()
    
    try:
        print("尝试导入services.database...")
        from services.database import init_db
        print("导入init_db成功")
    except Exception as e:
        print(f"导入services.database时出错: {str(e)}")
        traceback.print_exc()
    
    try:
        print("尝试导入config...")
        from config import DEBUG, HOST, PORT, UPLOAD_FOLDER, RESULT_FOLDER
        print(f"导入config成功: DEBUG={DEBUG}, HOST={HOST}, PORT={PORT}")
    except Exception as e:
        print(f"导入config时出错: {str(e)}")
        traceback.print_exc()
    
    print("开始创建应用...")
    app = Flask(__name__)
    
    # 配置应用
    app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
    app.config['RESULT_FOLDER'] = RESULT_FOLDER
    app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024
    
    print("注册蓝图...")
    # 注册蓝图
    try:
        app.register_blueprint(chat_api)
        print("chat_api蓝图注册成功")
    except Exception as e:
        print(f"注册chat_api蓝图时出错: {str(e)}")
        traceback.print_exc()
    
    try:
        app.register_blueprint(predict_api)
        print("predict_api蓝图注册成功")
    except Exception as e:
        print(f"注册predict_api蓝图时出错: {str(e)}")
        traceback.print_exc()
    
    # 健康检查
    @app.route('/health', methods=['GET'])
    def health_check():
        """健康检查端点"""
        return {'status': 'ok', 'version': '1.0.0'}
    
    # 启动应用
    if __name__ == '__main__':
        print("===== AI 皮肤诊断应用后端服务(调试模式) =====")
        print(f"服务器启动在 {HOST}:{PORT}")
        print(f"调试模式: {'开启' if DEBUG else '关闭'}")
        
        app.run(host=HOST, port=PORT, debug=DEBUG)

except Exception as e:
    print(f"启动应用时出错: {str(e)}")
    traceback.print_exc() 