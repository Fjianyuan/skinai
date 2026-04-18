package com.example.tor210.api;

/**
 * API配置类，包含所有API端点和服务器配置
 */
public class ApiConfig {
    // 服务器地址配置
    // 使用10.0.2.2可以让Android模拟器访问主机上的服务
    public static final String LOCAL_IP = "10.0.2.2"; 
    
    // 使用127.0.0.1可以让应用在真机上访问同一台设备上的服务器
    public static final String LOCALHOST_IP = "127.0.0.1";
    
    // 局域网IP，需要根据你的实际网络环境替换
    public static final String SERVER_IP = "YOUR_LAN_IP"; // 局域网IP地址

    // 公网IP地址，用于远程访问服务器
    // 注意：请替换为您的实际公网IP地址或域名
    public static final String PUBLIC_IP = "YOUR_PUBLIC_IP";
    
    /**
     * 当前使用的服务器地址，根据需要修改:
     * 1. 使用模拟器测试时，使用 LOCAL_IP (10.0.2.2)
     * 2. 在真机上访问同一设备上的服务器，使用 LOCALHOST_IP (127.0.0.1)
     * 3. 连接局域网内的服务器，使用 SERVER_IP (192.168.84.249)
     * 4. 连接公网服务器，使用 PUBLIC_IP
     */
    // 根据您的测试日志，服务器运行在局域网IP地址上，因此应该使用SERVER_IP
    public static final String CURRENT_IP = SERVER_IP; // 使用局域网IP进行测试
    
    // 服务器端口
    public static final int PORT = 5000;
    
    // API基础URL
    public static final String BASE_URL = "http://" + CURRENT_IP + ":" + PORT;
    
    // 健康检查API
    public static final String HEALTH_CHECK_URL = BASE_URL + "/health";
    
    // 用户认证API
    public static final String LOGIN_URL = BASE_URL + "/api/auth/login";
    public static final String REGISTER_URL = BASE_URL + "/api/auth/register";
    public static final String VERIFY_TOKEN_URL = BASE_URL + "/api/auth/verify";
    public static final String LOGOUT_URL = BASE_URL + "/api/auth/logout";
    
    // 检测记录API
    public static final String DETECTION_RECORDS_URL = BASE_URL + "/api/records";
    public static final String BATCH_DELETE_RECORDS_URL = BASE_URL + "/api/records/batch-delete";
    
    // 皮肤预测API
    public static final String PREDICT_URL = BASE_URL + "/api/predict";
    
    // 聊天历史API
    public static final String CHAT_HISTORY_ENDPOINT = BASE_URL + "/api/chat/histories";
    
    // 资源路径
    public static final String UPLOADS_PATH = BASE_URL + "/uploads/";
    
    // 请求超时时间 (毫秒)
    public static final int TIMEOUT_MS = 30000;  // 增加超时时间到30秒
    
    // HTTP头部键值
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    
    // 预定义内容类型
    public static final String CONTENT_TYPE_JSON = "application/json";
    
    // 构造授权头部值
    public static String getAuthorizationHeader(String token) {
        return "Bearer " + token;
    }
}