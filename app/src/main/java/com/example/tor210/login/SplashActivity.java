package com.example.tor210.login;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tor210.MainActivity;
import com.example.tor210.R;
import com.example.tor210.api.UserManager;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 2000; // 2秒延迟
    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 获取用户管理器实例
        userManager = UserManager.getInstance(this);

        // 设置版本号（使用硬编码版本号代替BuildConfig）
        TextView tvVersion = findViewById(R.id.tv_version);
        tvVersion.setText("v1.0.0");  // 使用硬编码版本号

        // 延迟2秒后跳转
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateToNextScreen, SPLASH_DELAY);
    }

    private void navigateToNextScreen() {
        // 如果已登录，则验证token有效性
        if (userManager.isLoggedIn()) {
            userManager.verifyToken(new UserManager.AuthListener() {
                @Override
                public void onSuccess() {
                    // token有效，直接进入主页
                    navigateToMainActivity();
                }

                @Override
                public void onError(String errorMessage) {
                    // token无效，跳转到登录页面
                    navigateToLoginActivity();
                }
            });
        } else {
            // 未登录，跳转到登录页面
            navigateToLoginActivity();
        }
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // 结束当前活动
    }

    private void navigateToLoginActivity() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // 结束当前活动
    }
} 