from flask import Blueprint, request, jsonify, g
from services.database import get_user_detection_records, save_detection_record, delete_detection_record
from middleware.auth_middleware import login_required
from flask import current_app
import os
import json
from datetime import datetime

# 创建蓝图
records_api = Blueprint('records', __name__, url_prefix='/api/records')

# 获取用户的检测记录
@records_api.route('/', methods=['GET'])
@login_required
def get_records():
    """获取用户的所有检测记录"""
    # 从g对象获取当前登录的用户
    current_user = g.user
    user_id = current_user['id']
    
    # 获取记录
    records = get_user_detection_records(user_id)
    if records is None:
        return jsonify({'success': False, 'message': '获取记录失败'}), 500
    
    return jsonify({
        'success': True,
        'records': records
    })

# 保存检测记录
@records_api.route('/', methods=['POST'])
@login_required
def save_record():
    """保存新的检测记录"""
    # 获取当前登录的用户
    current_user = g.user
    user_id = current_user['id']
    
    # 获取请求数据
    data = request.json
    if not data:
        return jsonify({'success': False, 'message': '无效的请求数据'}), 400
    
    # 验证必填字段
    required_fields = ['imageUrl', 'resultData']
    for field in required_fields:
        if field not in data:
            return jsonify({'success': False, 'message': f'缺少{field}字段'}), 400
    
    image_url = data.get('imageUrl')
    result_data = data.get('resultData')
    
    # 保存记录
    record_id = save_detection_record(user_id, image_url, result_data)
    if not record_id:
        return jsonify({'success': False, 'message': '保存记录失败'}), 500
    
    return jsonify({
        'success': True,
        'message': '记录保存成功',
        'recordId': record_id
    })

# 删除检测记录
@records_api.route('/<int:record_id>', methods=['DELETE'])
@login_required
def delete_record(record_id):
    """删除指定的检测记录"""
    # 获取当前登录的用户
    current_user = g.user
    user_id = current_user['id']
    
    # 删除记录
    success = delete_detection_record(record_id, user_id)
    if not success:
        return jsonify({'success': False, 'message': '删除记录失败，可能记录不存在或不属于当前用户'}), 400
    
    return jsonify({
        'success': True,
        'message': '记录删除成功'
    })

# 批量删除检测记录
@records_api.route('/batch-delete', methods=['POST'])
@login_required
def batch_delete_records():
    """批量删除检测记录"""
    # 获取当前登录的用户
    current_user = g.user
    user_id = current_user['id']
    
    # 获取请求数据
    data = request.json
    if not data or 'recordIds' not in data:
        return jsonify({'success': False, 'message': '无效的请求数据'}), 400
    
    record_ids = data.get('recordIds')
    
    if not isinstance(record_ids, list) or len(record_ids) == 0:
        return jsonify({'success': False, 'message': '记录ID列表无效'}), 400
    
    # 批量删除
    success_count = 0
    failed_count = 0
    
    for record_id in record_ids:
        if delete_detection_record(record_id, user_id):
            success_count += 1
        else:
            failed_count += 1
    
    return jsonify({
        'success': True,
        'message': f'批量删除完成: {success_count}个成功, {failed_count}个失败',
        'successCount': success_count,
        'failedCount': failed_count
    }) 