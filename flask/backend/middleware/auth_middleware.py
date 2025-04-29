from functools import wraps
from flask import request, jsonify, g
from services.database import get_db_connection
from datetime import datetime

def get_user_from_token(token):
    """根据令牌获取用户信息"""
    if not token:
        return None
        
    conn = get_db_connection()
    if conn is None:
        return None
        
    cursor = conn.cursor()
    
    try:
        # 查询有效的令牌
        cursor.execute('''
        SELECT ut.user_id, u.username, u.avatar_url 
        FROM user_tokens ut
        JOIN users u ON ut.user_id = u.id
        WHERE ut.token = ? AND ut.expiry_date > ?
        ''', (token, datetime.now()))
        
        result = cursor.fetchone()
        conn.close()
        
        if result:
            return {
                'id': result.user_id,
                'username': result.username,
                'avatarUrl': result.avatar_url or ''
            }
        else:
            return None
    except Exception as e:
        print(f"获取用户信息时出错: {str(e)}")
        if conn:
            conn.close()
        return None

def login_required(f):
    """需要登录的装饰器"""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        auth_header = request.headers.get('Authorization')
        
        if not auth_header or not auth_header.startswith('Bearer '):
            return jsonify({'success': False, 'message': '需要登录，未提供令牌'}), 401
            
        token = auth_header.split(' ')[1]
        user = get_user_from_token(token)
        
        if not user:
            return jsonify({'success': False, 'message': '无效或已过期的令牌，请重新登录'}), 401
            
        # 将用户信息保存到g对象，以便在视图函数中使用
        g.user = user
        return f(*args, **kwargs)
    return decorated_function

# 使用示例：
'''
from middleware.auth_middleware import login_required

@app.route('/api/protected-route', methods=['GET'])
@login_required
def protected_route():
    # 通过g.user访问当前登录的用户信息
    current_user = g.user
    return jsonify({'message': f'Hello, {current_user["username"]}!'})
''' 