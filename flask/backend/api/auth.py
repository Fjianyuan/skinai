import pyodbc
import secrets
import hashlib
import time
from datetime import datetime, timedelta
from flask import Blueprint, request, jsonify, current_app
from services.database import get_db_connection

# 创建蓝图
auth_api = Blueprint('auth', __name__, url_prefix='/api/auth')

# 生成安全的密码哈希
def hash_password(password):
    """使用SHA256加盐生成密码哈希"""
    salt = secrets.token_hex(8)
    hash_obj = hashlib.sha256((password + salt).encode())
    hashed = hash_obj.hexdigest()
    return f"{salt}${hashed}"

# 验证密码
def verify_password(stored_hash, provided_password):
    """验证提供的密码是否匹配存储的哈希值"""
    if not stored_hash or "$" not in stored_hash:
        return False
    salt, hash_value = stored_hash.split("$", 1)
    hash_obj = hashlib.sha256((provided_password + salt).encode())
    return hash_obj.hexdigest() == hash_value

# 生成令牌
def generate_token():
    """生成唯一的访问令牌"""
    return secrets.token_hex(16)

# 登录API
@auth_api.route('/login', methods=['POST'])
def login():
    """用户登录API"""
    # 获取请求数据
    data = request.json
    if not data or 'username' not in data or 'password' not in data:
        return jsonify({'success': False, 'message': '请提供用户名和密码'}), 400
    
    username = data.get('username')
    password = data.get('password')
    
    try:
        # 连接数据库
        conn = get_db_connection()
        if conn is None:
            return jsonify({'success': False, 'message': '数据库连接失败'}), 500
            
        cursor = conn.cursor()
        
        # 查询用户
        cursor.execute('''
        SELECT id, username, password, avatar_url
        FROM users 
        WHERE username = ? OR phone = ?
        ''', (username, username))
        
        user = cursor.fetchone()
        
        # 验证用户和密码
        if not user or not verify_password(user.password, password):
            return jsonify({'success': False, 'message': '用户名或密码不正确'}), 401
        
        # 生成新令牌
        token = generate_token()
        token_expiry = datetime.now() + timedelta(days=7)  # 令牌7天有效
        
        # 保存令牌
        cursor.execute('''
        INSERT INTO user_tokens (user_id, token, expiry_date)
        VALUES (?, ?, ?)
        ''', (user.id, token, token_expiry))
        
        conn.commit()
        conn.close()
        
        # 返回用户信息和令牌
        return jsonify({
            'success': True,
            'message': '登录成功',
            'user': {
                'id': user.id,
                'username': user.username,
                'avatarUrl': user.avatar_url or ''
            },
            'token': token,
            'expiresAt': token_expiry.isoformat()
        })
    
    except Exception as e:
        print(f"登录过程中出错: {str(e)}")
        return jsonify({'success': False, 'message': f'登录过程中发生错误: {str(e)}'}), 500

# 注册API
@auth_api.route('/register', methods=['POST'])
def register():
    """用户注册API"""
    # 获取请求数据
    data = request.json
    if not data:
        return jsonify({'success': False, 'message': '无效的请求数据'}), 400
    
    # 验证必填字段
    required_fields = ['username', 'password', 'phone']
    for field in required_fields:
        if field not in data or not data[field]:
            return jsonify({'success': False, 'message': f'请提供{field}'}), 400
    
    username = data.get('username')
    password = data.get('password')
    phone = data.get('phone')
    
    # 验证密码长度
    if len(password) < 6:
        return jsonify({'success': False, 'message': '密码长度必须至少为6位'}), 400
    
    try:
        # 连接数据库
        conn = get_db_connection()
        if conn is None:
            return jsonify({'success': False, 'message': '数据库连接失败'}), 500
            
        cursor = conn.cursor()
        
        # 检查用户名是否已被使用
        cursor.execute('SELECT id FROM users WHERE username = ?', (username,))
        if cursor.fetchone():
            return jsonify({'success': False, 'message': '用户名已存在'}), 409
        
        # 检查手机号是否已被注册
        cursor.execute('SELECT id FROM users WHERE phone = ?', (phone,))
        if cursor.fetchone():
            return jsonify({'success': False, 'message': '手机号已被注册'}), 409
        
        # 哈希密码并创建用户
        hashed_password = hash_password(password)
        now = datetime.now()
        
        # 插入新用户
        cursor.execute('''
        INSERT INTO users (username, password, phone, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?)
        ''', (username, hashed_password, phone, now, now))
        
        conn.commit()
        
        # 获取新用户ID
        cursor.execute('SELECT @@IDENTITY')
        user_id = cursor.fetchone()[0]
        
        conn.close()
        
        return jsonify({
            'success': True,
            'message': '注册成功',
            'userId': user_id
        })
    
    except Exception as e:
        print(f"注册过程中出错: {str(e)}")
        return jsonify({'success': False, 'message': f'注册过程中发生错误: {str(e)}'}), 500

# 检查令牌有效性
@auth_api.route('/verify', methods=['GET'])
def verify_token():
    """验证用户令牌有效性"""
    auth_header = request.headers.get('Authorization')
    
    if not auth_header or not auth_header.startswith('Bearer '):
        return jsonify({'valid': False, 'message': '未提供令牌'}), 401
    
    token = auth_header.split(' ')[1]
    
    try:
        conn = get_db_connection()
        if conn is None:
            return jsonify({'valid': False, 'message': '数据库连接失败'}), 500
            
        cursor = conn.cursor()
        
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
            return jsonify({
                'valid': True,
                'user': {
                    'id': result.user_id,
                    'username': result.username,
                    'avatarUrl': result.avatar_url or ''
                }
            })
        else:
            return jsonify({'valid': False, 'message': '令牌无效或已过期'}), 401
    
    except Exception as e:
        print(f"验证令牌时出错: {str(e)}")
        return jsonify({'valid': False, 'message': f'验证过程中发生错误: {str(e)}'}), 500

# 退出登录
@auth_api.route('/logout', methods=['POST'])
def logout():
    """用户退出登录，使令牌失效"""
    auth_header = request.headers.get('Authorization')
    
    if not auth_header or not auth_header.startswith('Bearer '):
        return jsonify({'success': False, 'message': '未提供令牌'}), 401
    
    token = auth_header.split(' ')[1]
    
    try:
        conn = get_db_connection()
        if conn is None:
            return jsonify({'success': False, 'message': '数据库连接失败'}), 500
            
        cursor = conn.cursor()
        
        # 删除令牌
        cursor.execute('DELETE FROM user_tokens WHERE token = ?', (token,))
        conn.commit()
        conn.close()
        
        return jsonify({'success': True, 'message': '已成功退出登录'})
    
    except Exception as e:
        print(f"退出登录时出错: {str(e)}")
        return jsonify({'success': False, 'message': f'退出过程中发生错误: {str(e)}'}), 500 