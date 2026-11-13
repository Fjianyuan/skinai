from flask import Blueprint, g, jsonify

from middleware.auth_middleware import login_required
from services.database import get_db_connection

# 修改蓝图定义，与app.py的导入保持一致
chat_api = Blueprint('chat_api', __name__, url_prefix='/api/chat')

# 修改路由定义，去掉前缀（因为已经在url_prefix中定义）
@chat_api.route('/histories', methods=['GET'])
@login_required
def get_chat_histories():
    user_id = g.user['id']
    
    conn = get_db_connection()
    if conn is None:
        return jsonify({'error': '数据库连接失败'}), 500
        
    cursor = conn.cursor()
    
    # 获取聊天历史
    cursor.execute('''
    SELECT id, title, user_id, created_at
    FROM chat_histories
    WHERE user_id = ?
    ORDER BY created_at DESC
    ''', (user_id,))
    
    histories = []
    for row in cursor.fetchall():
        history = {
            'id': row[0],
            'title': row[1],
            'userId': row[2],
            'createdAt': row[3].isoformat()
        }
        histories.append(history)
    
    conn.close()
    return jsonify(histories)

# 其他聊天API路由...
