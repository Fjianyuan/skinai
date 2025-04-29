from flask import Blueprint, request, jsonify, current_app, send_from_directory
import os
import uuid
import cv2
import numpy as np
from werkzeug.utils import secure_filename
from ultralytics import YOLO
from PIL import Image
from io import BytesIO
import base64
from datetime import datetime
import requests
import json

# 创建蓝图
predict_api = Blueprint('predict_api', __name__, url_prefix='/api/predict')

# 允许的文件扩展名
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg'}

# 痘痘类型映射
ACNE_TYPES = ['白头粉刺', '黑头粉刺', '丘疹', '脓包', '结节', '囊肿']

# 加载预训练的 YOLOv8 模型
yolo_model_path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "models", "acne615.pt")
yolo_model = YOLO(yolo_model_path)

# DeepSeek API配置
DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions"
DEEPSEEK_API_KEY = "sk-70b83cb1c9a64f6e863788595d038ee5"  # 直接配置API密钥

def allowed_file(filename):
    """检查文件是否有允许的扩展名"""
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def get_deepseek_diagnosis(acne_count, severity, acne_types, user_description=""):
    """
    调用DeepSeek API获取诊断报告
    
    参数:
    - acne_count: 痘痘数量
    - severity: 严重程度 (轻度、中度、重度)
    - acne_types: 痘痘类型列表
    - user_description: 用户对自己皮肤状况的描述
    
    返回:
    - 诊断报告字典
    """
    print(f"开始调用DeepSeek API, 参数: acne_count={acne_count}, severity={severity}, types={acne_types}")
    
    if not DEEPSEEK_API_KEY:
        print("错误: DeepSeek API密钥未配置")
        current_app.logger.error("未配置DeepSeek API密钥")
        return {
            "diagnosis": "无法生成AI诊断报告（API密钥未配置）",
            "skin_care_tips": ["请配置DeepSeek API密钥以启用AI诊断功能"],
            "lifestyle_suggestions": [],
            "medical_advice": []
        }
    
    # 准备发送给DeepSeek的提示
    prompt = f"""
    你是一位专业的皮肤科医生，请根据以下皮肤检测结果给出专业的诊断和建议：
    
    检测结果:
    - 痘痘数量: {acne_count}个
    - 严重程度: {severity}
    - 痘痘类型: {', '.join(acne_types)}
    
    用户描述:
    {user_description if user_description else "用户未提供描述"}
    
    请提供以下内容：
    1. 基于这些数据的专业诊断分析
    2. 皮肤护理建议和注意事项
    3. 生活习惯改善建议
    4. 可能需要的医疗干预建议
    
    请以JSON格式输出，包含以下字段：
    - diagnosis: 诊断分析（字符串）
    - skin_care_tips: 皮肤护理建议（字符串数组）
    - lifestyle_suggestions: 生活习惯改善建议（字符串数组）
    - medical_advice: 医疗干预建议（字符串数组）
    """
    
    try:
        print(f"使用API密钥: {DEEPSEEK_API_KEY[:5]}...{DEEPSEEK_API_KEY[-5:]}")
        # 调用DeepSeek API
        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {DEEPSEEK_API_KEY}"
        }
        
        data = {
            "model": "deepseek-chat",  # 使用DeepSeek聊天模型
            "messages": [
                {"role": "system", "content": "你是一位专业的皮肤科医生，根据皮肤检测数据提供专业诊断和建议。请以JSON格式输出结果。"},
                {"role": "user", "content": prompt}
            ],
            "temperature": 0.3,  # 低温度以获得更一致的响应
            "response_format": {"type": "json_object"}  # 指定JSON输出
        }
        
        print("发送请求到DeepSeek API...")
        response = requests.post(DEEPSEEK_API_URL, headers=headers, json=data)
        print(f"DeepSeek API返回状态码: {response.status_code}")
        
        if response.status_code != 200:
            print(f"DeepSeek API错误: {response.text}")
            return {
                "diagnosis": f"调用AI服务出错(HTTP {response.status_code})",
                "skin_care_tips": ["无法连接到AI诊断服务"],
                "lifestyle_suggestions": [],
                "medical_advice": ["请咨询专业医生获取更准确的建议"]
            }
        
        response.raise_for_status()  # 如果请求失败，抛出异常
        
        result = response.json()
        print(f"DeepSeek API返回结果: {result}")
        
        # 解析DeepSeek的响应
        if 'choices' in result and len(result['choices']) > 0:
            content = result['choices'][0]['message']['content']
            print(f"DeepSeek返回内容: {content[:100]}...")
            diagnosis_data = json.loads(content)
            return diagnosis_data
        else:
            print(f"DeepSeek API返回了不符合预期的响应: {result}")
            current_app.logger.error(f"DeepSeek API返回了不符合预期的响应: {result}")
            return {
                "diagnosis": "无法解析AI诊断报告",
                "skin_care_tips": ["获取诊断报告时发生错误"],
                "lifestyle_suggestions": [],
                "medical_advice": ["请咨询专业医生获取更准确的建议"]
            }
            
    except Exception as e:
        print(f"调用DeepSeek API出错: {str(e)}")
        current_app.logger.error(f"调用DeepSeek API出错: {str(e)}")
        return {
            "diagnosis": "生成AI诊断报告时发生错误",
            "skin_care_tips": ["无法连接到AI诊断服务"],
            "lifestyle_suggestions": [],
            "medical_advice": ["请咨询专业医生获取更准确的建议"]
        }

@predict_api.route('/', methods=['POST'])
def predict():
    """
    皮肤图像痘痘检测API
    ---
    接收图像文件，进行痘痘检测并返回结果
    """
    # 检查是否有文件在请求中
    if 'file' not in request.files:
        return jsonify({'error': '没有上传文件'}), 400
    
    file = request.files['file']
    
    # 检查文件名是否为空
    if file.filename == '':
        return jsonify({'error': '未选择文件'}), 400
    
    # 检查文件类型是否允许
    if not allowed_file(file.filename):
        return jsonify({'error': '不支持的文件类型，请上传jpg、jpeg或png格式图片'}), 400
    
    # 获取用户描述（如果提供）
    user_description = request.form.get('description', '')
    
    try:
        # 生成唯一文件名并保存
        filename = secure_filename(file.filename)
        ext = filename.rsplit('.', 1)[1].lower()
        unique_filename = f"{uuid.uuid4().hex}.{ext}"
        file_path = os.path.join(current_app.config['UPLOAD_FOLDER'], unique_filename)
        file.save(file_path)
        
        # 使用 YOLOv8 进行目标检测
        results = yolo_model(file_path, conf=0.5)
        
        # 读取原始图像
        original_img = cv2.imread(file_path)
        
        # 获取检测框
        if len(results) > 0 and hasattr(results[0], 'boxes') and results[0].boxes is not None:
            boxes = results[0].boxes
            
            # 绘制自定义圆形标记（而不是方框）
            for box in boxes:
                # 获取边界框坐标和置信度
                x1, y1, x2, y2 = map(int, box.xyxy[0])
                conf = float(box.conf[0])
                
                # 计算圆心和半径
                center_x = (x1 + x2) // 2
                center_y = (y1 + y2) // 2
                radius = max(int(min(x2 - x1, y2 - y1) * 0.5), 5)  # 确保圆形不会太小
                
                # 根据置信度调整颜色 (从黄色到红色)
                # 高置信度为红色，低置信度为黄色
                red = int(255)
                green = int(255 * (1 - conf))
                color = (0, green, red)  # OpenCV使用BGR格式
                
                # 绘制美观的圆形标记
                # 1. 先绘制一个较大的模糊圆形作为光晕效果
                blur_radius = radius + 5
                overlay = original_img.copy()
                cv2.circle(overlay, (center_x, center_y), blur_radius, color, -1)
                cv2.addWeighted(overlay, 0.2, original_img, 0.8, 0, original_img)
                
                # 2. 绘制主要的半透明圆形
                overlay = original_img.copy()
                cv2.circle(overlay, (center_x, center_y), radius, color, -1)
                cv2.addWeighted(overlay, 0.4, original_img, 0.6, 0, original_img)
                
                # 3. 绘制圆形边缘
                cv2.circle(original_img, (center_x, center_y), radius, color, 2)
        
        # 保存处理后的图像
        result_image_path = os.path.join(current_app.config['RESULT_FOLDER'], unique_filename)
        cv2.imwrite(result_image_path, original_img)

        # 计算痘痘数量并确定严重程度
        acne_count = 0
        if len(results) > 0 and hasattr(results[0], 'boxes') and results[0].boxes is not None:
            acne_count = len(results[0].boxes)
        
        # 根据痘痘数量确定严重程度
        severity = "无痘痘"
        if acne_count > 0:
            if acne_count <= 3:
                severity = "轻度"
            elif acne_count <= 7:
                severity = "中度"
            else:
                severity = "重度"
        
        # 生成结果图片URL
        server_address = request.host
        result_image_url = f"http://{server_address}/api/predict/results/{unique_filename}"
        
        # 添加图片的Base64编码
        image_base64 = ""
        try:
            with open(result_image_path, "rb") as img_file:
                image_bytes = img_file.read()
                image_size_kb = len(image_bytes) / 1024
                
                # 如果图片太大，尝试压缩（超过1MB）
                if image_size_kb > 1000:
                    img = Image.open(BytesIO(image_bytes))
                    # 计算新尺寸，保持宽高比
                    width, height = img.size
                    if width > 1280 or height > 1280:
                        # 等比例缩小
                        factor = min(1280/width, 1280/height)
                        new_width = int(width * factor)
                        new_height = int(height * factor)
                        img = img.resize((new_width, new_height), Image.LANCZOS)
                    
                    # 保存为JPEG，使用较高质量但不是最高
                    output = BytesIO()
                    img.save(output, format='JPEG', quality=85)
                    image_bytes = output.getvalue()
                
                # Base64编码
                image_base64 = base64.b64encode(image_bytes).decode('utf-8')
        except Exception as e:
            return jsonify({'error': f'图片编码过程中出错: {str(e)}'}), 500
        
        # 根据检测到的痘痘数量确定痘痘类型
        acne_types = []
        if acne_count > 0:
            if acne_count <= 3:
                acne_types = [ACNE_TYPES[0], ACNE_TYPES[2]]  # 白头粉刺、丘疹
            elif acne_count <= 7:
                acne_types = [ACNE_TYPES[0], ACNE_TYPES[2], ACNE_TYPES[3]]  # 白头粉刺、丘疹、脓包
            else:
                acne_types = [ACNE_TYPES[0], ACNE_TYPES[2], ACNE_TYPES[3], ACNE_TYPES[4]]  # 白头粉刺、丘疹、脓包、结节
        
        # 调用DeepSeek API获取AI诊断报告
        ai_diagnosis = get_deepseek_diagnosis(
            acne_count=acne_count, 
            severity=severity, 
            acne_types=acne_types, 
            user_description=user_description
        )
        
        # 返回结果
        return jsonify({
            'status': 'success',
            'data': {
                'result_image_url': result_image_url,
                'image_base64': f"data:image/jpeg;base64,{image_base64}",
                'acne_count': acne_count,
                'severity': severity,
                'acne_types': acne_types,
                'ai_diagnosis': ai_diagnosis  # 添加AI诊断报告
            }
        })
    
    except Exception as e:
        current_app.logger.error(f"处理过程中出错: {str(e)}")
        return jsonify({'error': f'处理过程中出错: {str(e)}'}), 500

@predict_api.route('/results/<filename>')
def get_result_image(filename):
    """获取检测结果图像"""
    return send_from_directory(current_app.config['RESULT_FOLDER'], filename)

@predict_api.route('/history', methods=['GET'])
def get_prediction_history():
    """获取用户的预测历史记录"""
    user_id = request.args.get('user_id')
    
    if not user_id:
        return jsonify({'error': '缺少用户ID'}), 400
    
    # 这里应该从数据库获取预测历史
    # 目前返回模拟数据
    history = [
        {
            "id": "pred_001",
            "timestamp": "2023-11-10T08:30:45",
            "image_path": "abc123.jpg",
            "diagnosis": "痤疮",
            "acne_count": 5,
            "severity": "中度",
            "acne_types": ["白头粉刺", "丘疹", "脓包"]
        },
        {
            "id": "pred_002",
            "timestamp": "2023-11-05T14:22:10",
            "image_path": "def456.jpg",
            "diagnosis": "痤疮",
            "acne_count": 2,
            "severity": "轻度",
            "acne_types": ["白头粉刺", "丘疹"]
        }
    ]
    
    return jsonify({
        'status': 'success',
        'history': history
    })