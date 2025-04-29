package com.example.tor210;

import android.app.Application;
import android.util.Log;

import com.example.tor210.api.HttpUtils;
import com.example.tor210.api.UserManager;

/**
 * 应用程序类，用于全局初始化
 */
public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 初始化网络请求工具
        HttpUtils.init(this);
        
        // 初始化用户管理器
        UserManager.getInstance(this);
        
        Log.d(TAG, "应用初始化完成");
    }
} 