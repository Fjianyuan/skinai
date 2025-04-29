from flask import Blueprint, request, jsonify, g
from services.database import (
    add_clock_in_record, 
    get_user_clock_in_records, 
    get_user_streak_days, 
    has_user_clocked_in_today, 
    get_monthly_clock_in_dates
)
import os
import json
from datetime import datetime
import uuid
from werkzeug.utils import secure_filename
from middleware.auth_middleware import login_required
from config import UPLOAD_FOLDER

# 创建蓝图
clock_in_api = Blueprint('clock_in_api', __name__)

# 图片上传目录
UPLOAD_PATH = os.path.join(UPLOAD_FOLDER, 'clock_in')
if not os.path.exists(UPLOAD_PATH):
    os.makedirs(UPLOAD_PATH)

# 允许的图片格式
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@clock_in_api.route('/api/clock-in/check', methods=['GET'])
@login_required
def check_clock_in_status():
    """检查用户今天是否已经打卡"""
    try:
        current_user = g.user
        user_id = current_user['id']
        has_clocked_in = has_user_clocked_in_today(user_id)
        streak_days = get_user_streak_days(user_id)
        
        return jsonify({
            'success': True,
            'data': {
                'hasClockedIn': has_clocked_in,
                'streakDays': streak_days
            }
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'检查打卡状态时出错: {str(e)}'
        }), 500

@clock_in_api.route('/api/clock-in/upload', methods=['POST'])
@login_required
def upload_images():
    """上传打卡图片"""
    try:
        current_user = g.user
        user_id = current_user['id']
        
        if 'images' not in request.files:
            return jsonify({
                'success': False,
                'message': '没有上传图片'
            }), 400
            
        files = request.files.getlist('images')
        if not files or len(files) == 0:
            return jsonify({
                'success': False,
                'message': '没有选择图片'
            }), 400
            
        # 限制最多上传3张图片
        if len(files) > 3:
            return jsonify({
                'success': False,
                'message': '最多只能上传3张图片'
            }), 400
            
        image_urls = []
        for file in files:
            if file and allowed_file(file.filename):
                # 生成安全的文件名
                filename = secure_filename(file.filename)
                timestamp = datetime.now().strftime('%Y%m%d%H%M%S')
                unique_id = str(uuid.uuid4())[:8]
                new_filename = f"{timestamp}_{unique_id}_{filename}"
                
                # 保存文件
                file_path = os.path.join(UPLOAD_PATH, new_filename)
                file.save(file_path)
                
                # 返回相对URL路径
                relative_path = f"/uploads/clock_in/{new_filename}"
                image_urls.append(relative_path)
            else:
                return jsonify({
                    'success': False,
                    'message': '不支持的文件格式'
                }), 400
                
        return jsonify({
            'success': True,
            'data': {
                'imageUrls': image_urls
            }
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'上传图片时出错: {str(e)}'
        }), 500

@clock_in_api.route('/api/clock-in/record', methods=['POST'])
@login_required
def create_clock_in_record():
    """创建打卡记录"""
    try:
        current_user = g.user
        user_id = current_user['id']
        
        # 检查今日是否已打卡
        if has_user_clocked_in_today(user_id):
            return jsonify({
                'success': False,
                'message': '今日已经打卡'
            }), 400
            
        data = request.json
        if not data:
            return jsonify({
                'success': False,
                'message': '请求数据不能为空'
            }), 400
            
        image_urls = data.get('imageUrls')
        skin_status = data.get('skinStatus')
        notes = data.get('notes')
        
        # 图片必须提交
        if not image_urls or len(image_urls) == 0:
            return jsonify({
                'success': False,
                'message': '请至少上传一张图片'
            }), 400
            
        # 添加打卡记录
        record_id = add_clock_in_record(user_id, image_urls, skin_status, notes)
        
        if record_id is None:
            return jsonify({
                'success': False,
                'message': '保存打卡记录失败'
            }), 500
            
        # 获取更新后的连续打卡天数
        streak_days = get_user_streak_days(user_id)
        
        return jsonify({
            'success': True,
            'data': {
                'recordId': record_id,
                'streakDays': streak_days
            },
            'message': '打卡成功'
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'创建打卡记录时出错: {str(e)}'
        }), 500

@clock_in_api.route('/api/clock-in/records', methods=['GET'])
@login_required
def get_records():
    """获取用户的打卡记录列表"""
    try:
        current_user = g.user
        user_id = current_user['id']
        records = get_user_clock_in_records(user_id)
        
        return jsonify({
            'success': True,
            'data': records
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'获取打卡记录时出错: {str(e)}'
        }), 500

@clock_in_api.route('/api/clock-in/calendar', methods=['GET'])
@login_required
def get_monthly_calendar():
    """获取用户某月的打卡日历数据"""
    try:
        current_user = g.user
        user_id = current_user['id']
        year = int(request.args.get('year', datetime.now().year))
        month = int(request.args.get('month', datetime.now().month))
        
        dates = get_monthly_clock_in_dates(user_id, year, month)
        streak_days = get_user_streak_days(user_id)
        
        return jsonify({
            'success': True,
            'data': {
                'dates': dates,
                'streakDays': streak_days
            }
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'获取打卡日历数据时出错: {str(e)}'
        }), 500 