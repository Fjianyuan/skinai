import sys
import os

# 添加父目录到sys.path以解决导入问题
current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.append(current_dir)

from services.database import get_db_connection

def clear_tips_table():
    """清空tips表中的所有记录"""
    conn = get_db_connection()
    if conn is None:
        print("无法连接到数据库")
        return False
        
    cursor = conn.cursor()
    
    try:
        cursor.execute('DELETE FROM tips')
        conn.commit()
        conn.close()
        print("成功清空tips表")
        return True
    except Exception as e:
        print(f"清空tips表时出错: {str(e)}")
        if conn:
            conn.rollback()
            conn.close()
        return False

if __name__ == "__main__":
    clear_tips_table() 