package com.example.tor210.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tor210.MainActivity;
import com.example.tor210.R;
import com.example.tor210.api.UserManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilUsername, tilPassword;
    private TextInputEditText etUsername, etPassword;
    private MaterialButton btnLogin;
    private TextView tvForgotPassword, tvRegister;
    private CircularProgressIndicator progressIndicator;
    
    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 获取用户管理器实例
        userManager = UserManager.getInstance(this);
        
        initViews();
        setupListeners();
    }

    private void initViews() {
        tilUsername = findViewById(R.id.til_username);
        tilPassword = findViewById(R.id.til_password);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvRegister = findViewById(R.id.tv_register);
        
        // 添加进度指示器
        progressIndicator = findViewById(R.id.progress_indicator);
        if (progressIndicator == null) {
            // 如果布局中没有进度指示器，不进行相关操作
        }
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        tvForgotPassword.setOnClickListener(v -> {
            // 实现忘记密码功能
            Toast.makeText(LoginActivity.this, "忘记密码功能将在后续版本中提供", Toast.LENGTH_SHORT).show();
        });

        tvRegister.setOnClickListener(v -> {
            // 跳转到注册页面
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
        // 重置错误提示
        tilUsername.setError(null);
        tilPassword.setError(null);

        // 获取输入值
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // 检查密码
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("请输入密码");
            focusView = etPassword;
            cancel = true;
        } else if (password.length() < 6) {
            tilPassword.setError("密码长度至少为6位");
            focusView = etPassword;
            cancel = true;
        }

        // 检查用户名
        if (TextUtils.isEmpty(username)) {
            tilUsername.setError("请输入用户名或手机号");
            focusView = etUsername;
            cancel = true;
        }

        if (cancel) {
            // 存在错误，聚焦到第一个错误的输入框
            if (focusView != null) {
                focusView.requestFocus();
            }
        } else {
            // 执行登录操作
            showLoading(true);
            
            userManager.login(username, password, new UserManager.AuthListener() {
                @Override
                public void onSuccess() {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                    navigateToMainActivity();
                }

                @Override
                public void onError(String errorMessage) {
                    showLoading(false);
                    
                    // 判断是否是连接错误
                    if (errorMessage.contains("请求超时") || errorMessage.contains("无法连接到服务器") || errorMessage.contains("网络错误")) {
                        showConnectionErrorDialog(username, password);
                    } else {
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void showLoading(boolean isLoading) {
        if (progressIndicator != null) {
            if (isLoading) {
                progressIndicator.setVisibility(View.VISIBLE);
                btnLogin.setEnabled(false);
            } else {
                progressIndicator.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
            }
        }
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // 结束当前活动，防止用户按返回键回到登录页面
    }

    /**
     * 显示连接错误对话框
     */
    private void showConnectionErrorDialog(final String username, final String password) {
        new AlertDialog.Builder(this)
            .setTitle("连接失败")
            .setMessage("无法连接到服务器。\n\n可能原因:\n" +
                    "1. 服务器未启动\n" +
                    "2. 手机与服务器不在同一网络\n" +
                    "3. 服务器IP地址或端口配置错误\n\n" +
                    "您可以检查ApiConfig.java中的IP地址配置。")
            .setPositiveButton("重试", (dialog, which) -> {
                // 重试登录
                showLoading(true);
                userManager.login(username, password, new UserManager.AuthListener() {
                    @Override
                    public void onSuccess() {
                        showLoading(false);
                        Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        showLoading(false);
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
            .setCancelable(false)
            .show();
    }
}