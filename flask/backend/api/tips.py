#!/usr/bin/env python
# -*- coding: utf-8 -*-

from flask import Blueprint, jsonify, request
import sys
import os

# 添加父目录到sys.path以解决导入问题
current_dir = os.path.dirname(os.path.abspath(__file__))
parent_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if parent_dir not in sys.path:
    sys.path.append(parent_dir)

from services.database import get_all_tips, get_tip_by_id, add_tip, update_tip, delete_tip
from werkzeug.utils import secure_filename
from config import UPLOAD_FOLDER

# 创建蓝图
tips_api = Blueprint('tips_api', __name__)

# 配置图片上传
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}
TIPS_UPLOAD_FOLDER = os.path.join(UPLOAD_FOLDER, 'tips')

# 确保上传文件夹存在
if not os.path.exists(TIPS_UPLOAD_FOLDER):
    os.makedirs(TIPS_UPLOAD_FOLDER)

def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

# 获取所有护肤小贴士
@tips_api.route('/tips', methods=['GET'])
def get_tips():
    """获取所有护肤小贴士"""
    tips = get_all_tips()
    if tips is None:
        return jsonify({'error': '获取小贴士列表失败'}), 500
    return jsonify(tips)

# 获取特定护肤小贴士详情
@tips_api.route('/tips/<int:tip_id>', methods=['GET'])
def get_tip_detail(tip_id):
    """获取特定护肤小贴士详情"""
    tip = get_tip_by_id(tip_id)
    if tip is None:
        return jsonify({'error': '未找到该小贴士'}), 404
    # 将单个对象包装为数组返回，以匹配客户端预期
    return jsonify([tip])

# 添加新的护肤小贴士（仅限管理员使用）
@tips_api.route('/admin/tips', methods=['POST'])
def add_new_tip():
    """添加新的护肤小贴士"""
    # 这里可以添加管理员权限验证
    
    # 获取表单数据
    title = request.form.get('title')
    content = request.form.get('content')
    
    if not title or not content:
        return jsonify({'error': '标题和内容不能为空'}), 400
    
    # 处理图片上传
    image_url = None
    if 'image' in request.files:
        file = request.files['image']
        if file and allowed_file(file.filename):
            filename = secure_filename(file.filename)
            # 保存文件
            save_path = os.path.join(TIPS_UPLOAD_FOLDER, filename)
            file.save(save_path)
            # 设置图片URL
            image_url = f'/uploads/tips/{filename}'
    
    # 添加小贴士
    success = add_tip(title, content, image_url)
    if not success:
        return jsonify({'error': '添加小贴士失败'}), 500
    
    return jsonify({'message': '小贴士添加成功'})

# 更新护肤小贴士（仅限管理员使用）
@tips_api.route('/admin/tips/<int:tip_id>', methods=['PUT'])
def update_existing_tip(tip_id):
    """更新护肤小贴士"""
    # 这里可以添加管理员权限验证
    
    # 获取表单数据
    title = request.form.get('title')
    content = request.form.get('content')
    
    if not title and not content:
        return jsonify({'error': '标题和内容不能同时为空'}), 400
    
    # 处理图片上传
    image_url = None
    if 'image' in request.files:
        file = request.files['image']
        if file and allowed_file(file.filename):
            filename = secure_filename(file.filename)
            # 保存文件
            save_path = os.path.join(TIPS_UPLOAD_FOLDER, filename)
            file.save(save_path)
            # 设置图片URL
            image_url = f'/uploads/tips/{filename}'
    
    # 更新小贴士
    success = update_tip(tip_id, title, content, image_url)
    if not success:
        return jsonify({'error': '更新小贴士失败'}), 500
    
    return jsonify({'message': '小贴士更新成功'})

# 删除护肤小贴士（仅限管理员使用）
@tips_api.route('/admin/tips/<int:tip_id>', methods=['DELETE'])
def delete_existing_tip(tip_id):
    """删除护肤小贴士"""
    # 这里可以添加管理员权限验证
    
    # 删除小贴士
    success = delete_tip(tip_id)
    if not success:
        return jsonify({'error': '删除小贴士失败'}), 500
    
    return jsonify({'message': '小贴士删除成功'})

# 初始化示例数据（仅用于开发环境）
@tips_api.route('/admin/init-sample-tips', methods=['POST'])
def init_sample_tips():
    """初始化示例小贴士数据"""
    # 这里可以添加管理员权限验证
    
    # 示例数据
    sample_tips = [
        {
            "title": "痘痘肌肤早餐",
            "content": "正确的早餐选择可以帮助控制痘痘：\n\n1. 避免高糖高脂食物，如甜甜圈、油条等\n2. 选择全麦面包、燕麦等低GI食物\n3. 添加富含抗氧化物的水果，如蓝莓\n4. 可以适量摄入坚果，补充必要的脂肪酸\n5. 喝足够的水，保持肌肤水分\n\n建议早餐食物：全麦吐司+煮蛋+蓝莓+杏仁",
            "image_url": "/static/images/tips/6.jpg"
        },
        {
            "title": "反复\"爆痘\"？可能是皮肤有炎症了！",
            "content": "很多人认为痘痘只是皮肤问题，但实际上反复爆痘可能是因为皮肤存在炎症。\n\n炎症原因：\n1. 细菌感染：皮肤表面的痤疮丙酸杆菌大量繁殖\n2. 皮脂分泌过多：荷尔蒙变化导致\n3. 角质堆积：毛孔堵塞，无法正常排除废物\n4. 免疫反应：身体对细菌的防御反应\n\n如何缓解：\n- 使用含有水杨酸的洁面产品\n- 使用含有烟酰胺的修复产品\n- 保持面部清洁\n- 避免挤压痘痘\n- 保持规律生活作息",
            "image_url": "/static/images/tips/1.jpg"
        },
        {
            "title": "痘痘类型自测",
            "content": "了解自己的痘痘类型对症下药：\n\n1. 粉刺：毛囊口处于堵塞状态，形成黑头或白头\n   解决方法：使用水杨酸产品，定期去角质\n\n2. 丘疹：皮脂排出受阻，微生物大量繁殖\n   解决方法：控制油脂，使用抗菌成分\n\n3. 脓包：炎症加重，形成内含脓液的脓包\n   解决方法：看皮肤科医生，可能需要药物治疗\n\n4. 结节囊肿：最严重的痘痘类型\n   解决方法：必须就医，可能需要口服药物治疗",
            "image_url": "/static/images/tips/4.jpg"
        },
        {
            "title": "长痘痘的内外原因",
            "content": "外部刺激：\n- 熬夜导致荷尔蒙分泌紊乱\n- 过度清洁反而导致皮肤分泌更多油脂\n- 防晒不足导致紫外线伤害\n- 环境温度升高导致皮脂分泌增加\n\n内部因素：\n- 内分泌失调导致荷尔蒙变化\n- 情绪焦虑影响皮肤健康\n- 高糖高脂饮食增加痘痘风险\n- 遗传因素也会影响皮脂腺活跃程度\n\n建议：保持规律作息、均衡饮食、心情舒畅，痘痘会明显减少",
            "image_url": "/static/images/tips/8.png"
        },
        {
            "title": "身体缺啥脸长啥",
            "content": "面部长痘位置与身体健康的关系：\n\n1. 额头痘痘：可能是消化系统问题，注意饮食规律\n\n2. T区痘痘：与肝脏和胃部健康相关，减少辛辣油腻食物\n\n3. 下巴痘痘：可能是内分泌失调，注意作息规律\n\n4. 脸颊痘痘：可能与肺部健康有关，避免吸烟和污染环境\n\n改善方法：\n- 保持规律作息\n- 适量运动\n- 均衡饮食\n- 减少压力",
            "image_url": "/static/images/tips/2.jpg"
        },
        {
            "title": "痘痘肌9种必吃蔬菜",
            "content": "对抗痘痘的必吃蔬菜：\n\n1. 胡萝卜：富含维生素A，帮助皮肤修复\n2. 西兰花：含有抗氧化物质，减少炎症\n3. 菠菜：补充铁质和维生素，改善肤色\n4. 黄瓜：水分充足，有助于排毒\n5. 芹菜：利尿消肿，帮助排出毒素\n6. 番茄：含有番茄红素，减少皮肤炎症\n7. 甘蓝：抗氧化，改善皮肤质地\n8. 南瓜：富含锌元素，控制油脂分泌\n9. 紫甘蓝：含有花青素，提高皮肤抵抗力",
            "image_url": "/static/images/tips/5.png"
        },
        {
            "title": "万痘可消灭 痘痘肌护肤课堂",
            "content": "痘痘肌肤护理需要注意的要点：\n\n1. 清洁温和为先：使用弱酸性洁面产品，不要过度清洁\n\n2. 保湿很重要：控油≠不保湿，选择清爽质地的保湿产品\n\n3. 抗菌消炎：含有水杨酸、烟酰胺的产品有助于控制炎症\n\n4. 防晒必不可少：选择无油配方的防晒霜\n\n5. 少用粉底：减少彩妆使用频率，让皮肤休息\n\n6. 不要挤压痘痘：会导致二次感染和留疤\n\n7. 定期去角质：每周1-2次温和去角质\n\n8. 及时就医：严重的痘痘问题应该寻求医生帮助",
            "image_url": "/static/images/tips/3.jpg"
        },
        {
            "title": "养成\"不长痘\"体质",
            "content": "\"不长痘\"体质需要综合调理：\n\n1. 饮食调整：\n   - 控制高糖高脂食物摄入\n   - 多吃蔬果，补充维生素\n   - 适量饮水，每天2000ml左右\n   - 少吃乳制品和高升糖指数食物\n\n2. 生活习惯：\n   - 规律作息，早睡早起\n   - 适量运动，促进新陈代谢\n   - 减少压力，避免焦虑\n   - 保持心情愉悦\n   - 勤换枕套和毛巾\n\n3. 皮肤护理：\n   - 温和清洁\n   - 适当保湿\n   - 规律去角质\n   - 全年防晒",
            "image_url": "/static/images/tips/7.png"
        }
    ]
    
    # 添加示例数据
    success_count = 0
    for tip in sample_tips:
        if add_tip(tip["title"], tip["content"], tip["image_url"]):
            success_count += 1
    
    return jsonify({
        'message': f'成功添加 {success_count}/{len(sample_tips)} 条示例小贴士数据'
    }) 