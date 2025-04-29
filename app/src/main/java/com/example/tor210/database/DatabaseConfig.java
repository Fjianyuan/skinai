package com.example.tor210.database;

public class DatabaseConfig {
    // 数据库连接信息
    public static final String HOST = "localhost";
    public static final int PORT = 1433;
    public static final String USER = "sa";
    public static final String PASSWORD = "682528";
    public static final String DATABASE = "app";
    
    // API基础URL
    public static final String BASE_API_URL = "http://10.0.2.2:5000/api/"; // 使用10.0.2.2指向开发机的localhost
} 