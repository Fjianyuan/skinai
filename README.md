# AI 皮肤诊断应用

一个基于Android的皮肤问题AI诊断应用，使用SQL Server数据库存储聊天记录。

## 系统组成

1. **Android客户端**: 提供用户界面，允许用户与AI进行皮肤问题咨询
2. **Flask后端**: 处理数据存储和检索，连接到SQL Server数据库
3. **SQL Server数据库**: 存储聊天历史和消息

## 数据库设置

数据库配置:
- 主机: localhost
- 端口: 1433
- 用户名: sa
- 密码: 682528
- 数据库名: app

需要确保SQL Server已安装并运行，并且已创建名为"app"的数据库。

## 后端设置

1. 安装Python 3.7+
2. 进入flask目录
3. 安装依赖:
   ```
   pip install -r requirements.txt
   ```
4. 安装SQL Server ODBC驱动
5. 运行后端:
   ```
   python app.py
   ```

后端将在`http://localhost:5000`上运行，并提供API端点供Android应用使用。

## Android应用设置

1. 在Android Studio中打开项目
2. 如果在模拟器上运行，后端API地址已配置为`http://10.0.2.2:5000`(指向开发计算机上的localhost)
3. 如果在真机上运行，需要修改`DatabaseConfig.java`中的BASE_API_URL为后端服务器的实际IP地址
4. 构建并运行应用

## 功能特点

- AI皮肤诊断聊天界面
- 聊天历史记录保存和查看
- 简单的AI响应模拟
- 用户友好的界面

## 技术栈

- Android (Java)
- Flask (Python)
- SQL Server
- OkHttp (网络请求)
- Picasso (图片加载) 