import pyodbc
from datetime import datetime
import sys
import os

# 尝试导入配置，如果失败则使用硬编码的配置
try:
    # 方式1：尝试相对导入
    from ..config import DB_CONFIG
except (ImportError, ValueError):
    try:
        # 方式2：添加父目录到sys.path
        sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
        from config import DB_CONFIG
    except ImportError:
        # 方式3：使用硬编码配置作为临时解决方案
        print("警告：使用硬编码的数据库配置")
        DB_CONFIG = {
            'host': 'localhost',
            'port': 1433,
            'user': 'sa',
            'password': '682528',
            'database': 'app'
        }

# 创建数据库连接
def get_db_connection():
    try:
        conn_str = f"DRIVER={{ODBC Driver 17 for SQL Server}};SERVER={DB_CONFIG['host']},{DB_CONFIG['port']};DATABASE={DB_CONFIG['database']};UID={DB_CONFIG['user']};PWD={DB_CONFIG['password']}"
        return pyodbc.connect(conn_str)
    except Exception as e:
        print(f"数据库连接错误: {str(e)}")
        return None

# 初始化数据库表
def init_db():
    try:
        conn = get_db_connection()
        if conn is None:
            print("无法连接到数据库，表初始化失败")
            return False
            
        cursor = conn.cursor()
        
        # 创建用户表
        cursor.execute('''
        IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'users')
        CREATE TABLE users (
            id INT IDENTITY(1,1) PRIMARY KEY,
            username NVARCHAR(50) NOT NULL,
            password NVARCHAR(255) NOT NULL,
            phone NVARCHAR(20) NOT NULL,
            avatar_url NVARCHAR(255) NULL,
            created_at DATETIME NOT NULL,
            updated_at DATETIME NOT NULL
        )
        ''')
        
        # 为用户名和手机号创建唯一索引
        cursor.execute('''
        IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_users_username' AND object_id = OBJECT_ID('users'))
        CREATE UNIQUE INDEX IX_users_username ON users(username)
        ''')
        
        cursor.execute('''
        IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_users_phone' AND object_id = OBJECT_ID('users'))
        CREATE UNIQUE INDEX IX_users_phone ON users(phone)
        ''')
        
        # 创建用户令牌表
        cursor.execute('''
        IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'user_tokens')
        CREATE TABLE user_tokens (
            id INT IDENTITY(1,1) PRIMARY KEY,
            user_id INT NOT NULL,
            token NVARCHAR(255) NOT NULL,
            expiry_date DATETIME NOT NULL,
            created_at DATETIME DEFAULT GETDATE(),
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        )
        ''')
        
        # 为令牌创建唯一索引
        cursor.execute('''
        IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_user_tokens_token' AND object_id = OBJECT_ID('user_tokens'))
        CREATE UNIQUE INDEX IX_user_tokens_token ON user_tokens(token)
        ''')
        
        # 创建聊天历史表
        cursor.execute('''
        IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'chat_histories')
        CREATE TABLE chat_histories (
            id INT IDENTITY(1,1) PRIMARY KEY,
            title NVARCHAR(255) NOT NULL,
            user_id NVARCHAR(50) NOT NULL,
            created_at DATETIME NOT NULL
        )
        ''')
        
        # 创建聊天消息表
        cursor.execute('''
        IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'chat_messages')
        CREATE TABLE chat_messages (
            id INT IDENTITY(1,1) PRIMARY KEY,
            history_id INT NOT NULL,
            message NVARCHAR(MAX) NOT NULL,
            is_user_message BIT NOT NULL,
            user_id NVARCHAR(50) NOT NULL,
            timestamp DATETIME NOT NULL,
            FOREIGN KEY (history_id) REFERENCES chat_histories(id) ON DELETE CASCADE
        )
        ''')
        
        # 创建皮肤检测记录表
        cursor.execute('''
        IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'detection_records')
        CREATE TABLE detection_records (
            id INT IDENTITY(1,1) PRIMARY KEY,
            user_id INT NOT NULL,
            image_url NVARCHAR(255) NOT NULL,
            record_time DATETIME DEFAULT GETDATE(),
            acne_count INT NULL,
            severity NVARCHAR(50) NULL,
            acne_types NVARCHAR(MAX) NULL,
            recommendation NVARCHAR(MAX) NULL,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        )
        ''')
        
        # 创建打卡记录表
        cursor.execute('''
        IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'clock_in_records')
        CREATE TABLE clock_in_records (
            id INT IDENTITY(1,1) PRIMARY KEY,
            user_id INT NOT NULL,
            clock_in_date DATETIME NOT NULL DEFAULT GETDATE(),
            image_urls NVARCHAR(MAX) NULL,
            skin_status NVARCHAR(50) NULL,
            notes NVARCHAR(MAX) NULL,
            streak_days INT NOT NULL DEFAULT 1,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        )
        ''')
        
        # 创建护肤小贴士表
        cursor.execute('''
        IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'tips')
        CREATE TABLE tips (
            id INT IDENTITY(1,1) PRIMARY KEY,
            title NVARCHAR(100) NOT NULL,
            content NVARCHAR(MAX) NOT NULL,
            image_url NVARCHAR(255) NULL,
            create_time DATETIME DEFAULT GETDATE(),
            update_time DATETIME DEFAULT GETDATE()
        )
        ''')
        
        conn.commit()
        conn.close()
        print("数据库表初始化成功")
        return True
    except Exception as e:
        print(f"初始化数据库表时出错: {str(e)}")
        return False

# 获取用户的所有聊天历史
def get_user_chat_histories(user_id):
    conn = get_db_connection()
    if conn is None:
        return None
        
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
    return histories

# 保存预测结果到数据库
def save_detection_record(user_id, image_url, result_data):
    """保存检测记录到数据库"""
    conn = get_db_connection()
    if conn is None:
        return None
        
    cursor = conn.cursor()
    
    try:
        # 提取结果数据
        acne_count = result_data.get('acneCount')
        severity = result_data.get('severity')
        acne_types = result_data.get('acneTypes')
        recommendation = result_data.get('recommendation')
        
        # 将acne_types列表转换为字符串
        if isinstance(acne_types, list):
            acne_types = ','.join(acne_types)
        
        # 插入记录
        cursor.execute('''
        INSERT INTO detection_records (user_id, image_url, acne_count, severity, acne_types, recommendation)
        VALUES (?, ?, ?, ?, ?, ?)
        ''', (user_id, image_url, acne_count, severity, acne_types, recommendation))
        
        # 获取新记录ID
        cursor.execute('SELECT @@IDENTITY')
        record_id = cursor.fetchone()[0]
        
        conn.commit()
        conn.close()
        
        return record_id
    except Exception as e:
        print(f"保存检测记录时出错: {str(e)}")
        if conn:
            conn.close()
        return None

# 获取用户的检测历史记录
def get_user_detection_records(user_id):
    """获取用户的所有检测记录"""
    conn = get_db_connection()
    if conn is None:
        return None
        
    cursor = conn.cursor()
    
    try:
        cursor.execute('''
        SELECT id, image_url, record_time, acne_count, severity, acne_types, recommendation
        FROM detection_records
        WHERE user_id = ?
        ORDER BY record_time DESC
        ''', (user_id,))
        
        records = []
        for row in cursor.fetchall():
            # 将acne_types字符串转换回列表
            acne_types = row[5].split(',') if row[5] else []
            
            record = {
                'id': row[0],
                'imageUrl': row[1],
                'recordTime': row[2].isoformat(),
                'acneCount': row[3],
                'severity': row[4],
                'acneTypes': acne_types,
                'recommendation': row[6]
            }
            records.append(record)
        
        conn.close()
        return records
    except Exception as e:
        print(f"获取检测记录时出错: {str(e)}")
        if conn:
            conn.close()
        return None

# 删除检测记录
def delete_detection_record(record_id, user_id):
    """删除指定的检测记录"""
    conn = get_db_connection()
    if conn is None:
        return False
        
    cursor = conn.cursor()
    
    try:
        # 首先验证记录是否属于该用户
        cursor.execute('''
        SELECT COUNT(*)
        FROM detection_records
        WHERE id = ? AND user_id = ?
        ''', (record_id, user_id))
        
        if cursor.fetchone()[0] == 0:
            conn.close()
            return False
        
        # 删除记录
        cursor.execute('''
        DELETE FROM detection_records
        WHERE id = ? AND user_id = ?
        ''', (record_id, user_id))
        
        conn.commit()
        conn.close()
        return True
    except Exception as e:
        print(f"删除检测记录时出错: {str(e)}")
        if conn:
            conn.close()
        return False

# 打卡相关的数据库操作函数

def add_clock_in_record(user_id, image_urls=None, skin_status=None, notes=None):
    """添加打卡记录"""
    conn = get_db_connection()
    if conn is None:
        return None
        
    cursor = conn.cursor()
    
    try:
        # 将image_urls列表转换为JSON字符串存储
        if isinstance(image_urls, list):
            import json
            image_urls = json.dumps(image_urls)
        
        # 计算连续打卡天数
        streak_days = calculate_streak_days(user_id)
        
        # 插入记录
        cursor.execute('''
        INSERT INTO clock_in_records (user_id, clock_in_date, image_urls, skin_status, notes, streak_days)
        VALUES (?, GETDATE(), ?, ?, ?, ?)
        ''', (user_id, image_urls, skin_status, notes, streak_days))
        
        # 获取新记录ID
        cursor.execute('SELECT @@IDENTITY')
        record_id = cursor.fetchone()[0]
        
        conn.commit()
        conn.close()
        
        return record_id
    except Exception as e:
        print(f"添加打卡记录时出错: {str(e)}")
        if conn:
            conn.close()
        return None

def calculate_streak_days(user_id):
    """计算用户的连续打卡天数"""
    conn = get_db_connection()
    if conn is None:
        return 1
        
    cursor = conn.cursor()
    
    try:
        # 检查是否有今天的打卡记录
        cursor.execute('''
        SELECT COUNT(*)
        FROM clock_in_records
        WHERE user_id = ? AND CONVERT(date, clock_in_date) = CONVERT(date, GETDATE())
        ''', (user_id,))
        
        if cursor.fetchone()[0] > 0:
            # 今天已经打卡，获取最近一条记录的连续天数
            cursor.execute('''
            SELECT TOP 1 streak_days
            FROM clock_in_records
            WHERE user_id = ?
            ORDER BY clock_in_date DESC
            ''', (user_id,))
            current_streak = cursor.fetchone()
            conn.close()
            return current_streak[0] if current_streak else 1
        
        # 检查是否有昨天的打卡记录
        cursor.execute('''
        SELECT TOP 1 streak_days
        FROM clock_in_records
        WHERE user_id = ? AND CONVERT(date, clock_in_date) = DATEADD(day, -1, CONVERT(date, GETDATE()))
        ORDER BY clock_in_date DESC
        ''', (user_id,))
        
        yesterday_streak = cursor.fetchone()
        conn.close()
        
        if yesterday_streak:
            # 昨天有打卡，连续天数+1
            return yesterday_streak[0] + 1
        else:
            # 昨天没打卡，从1开始
            return 1
    except Exception as e:
        print(f"计算连续打卡天数时出错: {str(e)}")
        if conn:
            conn.close()
        return 1

def get_user_clock_in_records(user_id):
    """获取用户的所有打卡记录"""
    conn = get_db_connection()
    if conn is None:
        return None
        
    cursor = conn.cursor()
    
    try:
        cursor.execute('''
        SELECT id, clock_in_date, image_urls, skin_status, notes, streak_days
        FROM clock_in_records
        WHERE user_id = ?
        ORDER BY clock_in_date DESC
        ''', (user_id,))
        
        records = []
        for row in cursor.fetchall():
            # 将image_urls字符串转换回列表
            import json
            image_urls = json.loads(row[2]) if row[2] else []
            
            record = {
                'id': row[0],
                'clockInDate': row[1].isoformat(),
                'imageUrls': image_urls,
                'skinStatus': row[3],
                'notes': row[4],
                'streakDays': row[5]
            }
            records.append(record)
        
        conn.close()
        return records
    except Exception as e:
        print(f"获取打卡记录时出错: {str(e)}")
        if conn:
            conn.close()
        return None

def get_user_streak_days(user_id):
    """获取用户的当前连续打卡天数"""
    conn = get_db_connection()
    if conn is None:
        return 0
        
    cursor = conn.cursor()
    
    try:
        cursor.execute('''
        SELECT TOP 1 streak_days
        FROM clock_in_records
        WHERE user_id = ?
        ORDER BY clock_in_date DESC
        ''', (user_id,))
        
        result = cursor.fetchone()
        conn.close()
        
        return result[0] if result else 0
    except Exception as e:
        print(f"获取连续打卡天数时出错: {str(e)}")
        if conn:
            conn.close()
        return 0

def has_user_clocked_in_today(user_id):
    """检查用户今天是否已经打卡"""
    conn = get_db_connection()
    if conn is None:
        return False
        
    cursor = conn.cursor()
    
    try:
        cursor.execute('''
        SELECT COUNT(*)
        FROM clock_in_records
        WHERE user_id = ? AND CONVERT(date, clock_in_date) = CONVERT(date, GETDATE())
        ''', (user_id,))
        
        count = cursor.fetchone()[0]
        conn.close()
        
        return count > 0
    except Exception as e:
        print(f"检查今日打卡状态时出错: {str(e)}")
        if conn:
            conn.close()
        return False

def get_monthly_clock_in_dates(user_id, year, month):
    """获取用户某月的打卡日期列表"""
    conn = get_db_connection()
    if conn is None:
        return []
        
    cursor = conn.cursor()
    
    try:
        # 构建月份开始和结束日期
        start_date = f"{year}-{month:02d}-01"
        end_date = f"{year}-{month:02d}-31" if month != 2 else f"{year}-{month:02d}-29"
        
        cursor.execute('''
        SELECT CONVERT(date, clock_in_date)
        FROM clock_in_records
        WHERE user_id = ? AND 
              clock_in_date >= ? AND 
              clock_in_date < DATEADD(month, 1, ?)
        GROUP BY CONVERT(date, clock_in_date)
        ''', (user_id, start_date, start_date))
        
        dates = []
        for row in cursor.fetchall():
            dates.append(row[0].isoformat())
        
        conn.close()
        return dates
    except Exception as e:
        print(f"获取月度打卡日期时出错: {str(e)}")
        if conn:
            conn.close()
        return []

# 获取所有护肤小贴士
def get_all_tips():
    """获取所有护肤小贴士"""
    conn = get_db_connection()
    if conn is None:
        return None
        
    cursor = conn.cursor()
    
    try:
        cursor.execute('''
        SELECT id, title, content, image_url FROM tips ORDER BY id
        ''')
        
        tips = []
        for row in cursor.fetchall():
            tip = {
                'id': row[0],
                'title': row[1],
                'content': row[2],
                'imageUrl': row[3]
            }
            tips.append(tip)
        
        conn.close()
        return tips
    except Exception as e:
        print(f"获取护肤小贴士时出错: {str(e)}")
        if conn:
            conn.close()
        return None

# 获取特定护肤小贴士详情
def get_tip_by_id(tip_id):
    """根据ID获取护肤小贴士详情"""
    conn = get_db_connection()
    if conn is None:
        return None
        
    cursor = conn.cursor()
    
    try:
        cursor.execute('''
        SELECT id, title, content, image_url FROM tips WHERE id = ?
        ''', (tip_id,))
        
        row = cursor.fetchone()
        if row:
            tip = {
                'id': row[0],
                'title': row[1],
                'content': row[2],
                'imageUrl': row[3]
            }
            conn.close()
            return tip
        else:
            conn.close()
            return None
    except Exception as e:
        print(f"获取护肤小贴士详情时出错: {str(e)}")
        if conn:
            conn.close()
        return None

# 添加新的护肤小贴士
def add_tip(title, content, image_url=None):
    """添加新的护肤小贴士"""
    conn = get_db_connection()
    if conn is None:
        return False
        
    cursor = conn.cursor()
    
    try:
        now = datetime.now()
        cursor.execute('''
        INSERT INTO tips (title, content, image_url, create_time, update_time)
        VALUES (?, ?, ?, ?, ?)
        ''', (title, content, image_url, now, now))
        
        conn.commit()
        conn.close()
        return True
    except Exception as e:
        print(f"添加护肤小贴士时出错: {str(e)}")
        if conn:
            conn.rollback()
            conn.close()
        return False

# 更新护肤小贴士
def update_tip(tip_id, title, content, image_url=None):
    """更新护肤小贴士"""
    conn = get_db_connection()
    if conn is None:
        return False
        
    cursor = conn.cursor()
    
    try:
        now = datetime.now()
        
        # 构建更新语句
        update_query = '''
        UPDATE tips SET update_time = ?
        '''
        params = [now]
        
        if title:
            update_query += ", title = ?"
            params.append(title)
        
        if content:
            update_query += ", content = ?"
            params.append(content)
        
        if image_url:
            update_query += ", image_url = ?"
            params.append(image_url)
        
        update_query += " WHERE id = ?"
        params.append(tip_id)
        
        cursor.execute(update_query, params)
        
        if cursor.rowcount == 0:
            conn.close()
            return False
            
        conn.commit()
        conn.close()
        return True
    except Exception as e:
        print(f"更新护肤小贴士时出错: {str(e)}")
        if conn:
            conn.rollback()
            conn.close()
        return False

# 删除护肤小贴士
def delete_tip(tip_id):
    """删除护肤小贴士"""
    conn = get_db_connection()
    if conn is None:
        return False
        
    cursor = conn.cursor()
    
    try:
        cursor.execute('''
        DELETE FROM tips WHERE id = ?
        ''', (tip_id,))
        
        if cursor.rowcount == 0:
            conn.close()
            return False
            
        conn.commit()
        conn.close()
        return True
    except Exception as e:
        print(f"删除护肤小贴士时出错: {str(e)}")
        if conn:
            conn.rollback()
            conn.close()
        return False

# 更多数据库操作函数...