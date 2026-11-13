import base64
import json
import os
import uuid
from functools import lru_cache
from io import BytesIO
from pathlib import Path

import cv2
import requests
from flask import Blueprint, current_app, jsonify, request, send_from_directory
from PIL import Image
from werkzeug.utils import secure_filename

from config import (
    DEEPSEEK_API_KEY, DEEPSEEK_API_URL, FACE_UNET_INPUT_SIZE, FACE_UNET_PATH,
    LESION_UNET_INPUT_SIZE, LESION_UNET_PATH, PREDICTION_MODE, YOLO_MODEL_PATH,
)
from services.unet_segmentation import BinaryUNet, segment_lesions, select_prediction_mode

# 创建蓝图
predict_api = Blueprint('predict_api', __name__, url_prefix='/api/predict')

# 允许的文件扩展名
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg'}

# 痘痘类型映射
ACNE_TYPES = ['白头粉刺', '黑头粉刺', '丘疹', '脓包', '结节', '囊肿']

@lru_cache(maxsize=1)
def load_prediction_models():
    face_path = Path(FACE_UNET_PATH)
    lesion_path = Path(LESION_UNET_PATH)
    mode = select_prediction_mode(PREDICTION_MODE, face_path, lesion_path)
    if mode == 'unet':
        return mode, (
            BinaryUNet(face_path, FACE_UNET_INPUT_SIZE),
            BinaryUNet(lesion_path, LESION_UNET_INPUT_SIZE),
        )

    from ultralytics import YOLO
    return mode, YOLO(YOLO_MODEL_PATH)


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
    
    if not DEEPSEEK_API_KEY:
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
        
        response = requests.post(DEEPSEEK_API_URL, headers=headers, json=data, timeout=15)
        
        if response.status_code != 200:
            return {
                "diagnosis": f"调用AI服务出错(HTTP {response.status_code})",
                "skin_care_tips": ["无法连接到AI诊断服务"],
                "lifestyle_suggestions": [],
                "medical_advice": ["请咨询专业医生获取更准确的建议"]
            }
        
        response.raise_for_status()  # 如果请求失败，抛出异常
        
        result = response.json()
        
        # 解析DeepSeek的响应
        if 'choices' in result and len(result['choices']) > 0:
            content = result['choices'][0]['message']['content']
            diagnosis_data = json.loads(content)
            return diagnosis_data
        else:
            return {
                "diagnosis": "无法解析AI诊断报告",
                "skin_care_tips": ["获取诊断报告时发生错误"],
                "lifestyle_suggestions": [],
                "medical_advice": ["请咨询专业医生获取更准确的建议"]
            }
            
    except (requests.RequestException, ValueError, KeyError, IndexError, TypeError):
        current_app.logger.warning("DeepSeek request or response handling failed")
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
        mode, models = load_prediction_models()
    except (ImportError, OSError, ValueError, cv2.error):
        current_app.logger.exception('Prediction models are unavailable')
        return jsonify({'error': '图像推理模型不可用'}), 503

    try:
        # 生成唯一文件名并保存
        filename = secure_filename(file.filename)
        ext = filename.rsplit('.', 1)[1].lower()
        unique_filename = f"{uuid.uuid4().hex}.{ext}"
        file_path = os.path.join(current_app.config['UPLOAD_FOLDER'], unique_filename)
        file.save(file_path)
        
        original_img = cv2.imread(file_path)
        if original_img is None:
            return jsonify({'error': '无法读取上传的图像'}), 400

        contours = None
        if mode == 'unet':
            contours = segment_lesions(original_img, *models)
            if contours is None:
                return jsonify({'error': '未检测到可分割的面部区域'}), 422
            acne_count = len(contours)
            cv2.drawContours(original_img, contours, -1, (0, 160, 255), 2)
        else:
            results = models(file_path, conf=0.5)
            acne_count = 0
            # 获取检测框
            if len(results) > 0 and hasattr(results[0], 'boxes') and results[0].boxes is not None:
                boxes = results[0].boxes
                acne_count = len(boxes)

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
        if not cv2.imwrite(result_image_path, original_img):
            return jsonify({'error': '无法保存标注图像'}), 500
        
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
        try:
            with Image.open(result_image_path) as annotated:
                if os.path.getsize(result_image_path) > 1000 * 1024:
                    annotated.thumbnail((1280, 1280), Image.Resampling.LANCZOS)
                output = BytesIO()
                annotated.convert('RGB').save(output, format='JPEG', quality=85)
                image_base64 = base64.b64encode(output.getvalue()).decode('utf-8')
        except OSError:
            current_app.logger.exception('Could not encode prediction image')
            return jsonify({'error': '图片编码失败'}), 500
        
        # 根据检测到的痘痘数量确定痘痘类型
        acne_types = []
        if mode == 'yolo' and acne_count > 0:
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
        
        data = {
            'result_image_url': result_image_url,
            'image_base64': f"data:image/jpeg;base64,{image_base64}",
            'acne_count': acne_count,
            'severity': severity,
            'acne_types': acne_types,
            'ai_diagnosis': ai_diagnosis,
        }
        if contours is not None:
            data['lesion_contours'] = [contour.reshape(-1, 2).tolist() for contour in contours]
        return jsonify({'status': 'success', 'data': data})
    
    except Exception as e:
        current_app.logger.error(f"处理过程中出错: {str(e)}")
        return jsonify({'error': '图片处理失败'}), 500

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

