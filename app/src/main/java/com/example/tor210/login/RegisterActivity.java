package com.example.tor210.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tor210.R;
import com.example.tor210.api.UserManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etPhone, etPassword, etConfirmPassword;
    private TextInputLayout tilUsername, tilPhone, tilPassword, tilConfirmPassword;
    private MaterialButton btnRegister;
    private MaterialCheckBox cbAgree;
    private ImageView ivBack;
    private CircularProgressIndicator progressIndicator;
    
    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 获取用户管理器实例
        userManager = UserManager.getInstance(this);
        
        initViews();
        setupListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        tilUsername = findViewById(R.id.til_username);
        tilPhone = findViewById(R.id.til_phone);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        cbAgree = findViewById(R.id.cb_agree);
        ivBack = findViewById(R.id.iv_back);
        
        // 添加进度指示器
        progressIndicator = findViewById(R.id.progress_indicator);
        if (progressIndicator == null) {
            // 如果布局中没有进度指示器，不进行相关操作
        }
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> attemptRegister());
        
        ivBack.setOnClickListener(v -> onBackPressed());
    }

    private void attemptRegister() {
        // 重置错误提示
        tilUsername.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        // 获取输入值
        String username = etUsername.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // 简单验证
        boolean cancel = false;
        View focusView = null;

        if (!cbAgree.isChecked()) {
            Toast.makeText(this, "请阅读并同意用户协议和隐私政策", Toast.LENGTH_SHORT).show();
            cbAgree.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("请确认密码");
            focusView = etConfirmPassword;
            cancel = true;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("两次输入的密码不一致");
            focusView = etConfirmPassword;
            cancel = true;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("请输入密码");
            focusView = etPassword;
            cancel = true;
        } else if (password.length() < 6) {
            tilPassword.setError("密码长度至少为6位");
            focusView = etPassword;
            cancel = true;
        }

        if (TextUtils.isEmpty(phone)) {
            tilPhone.setError("请输入手机号");
            focusView = etPhone;
            cancel = true;
        } else if (phone.length() != 11) {
            tilPhone.setError("请输入有效的手机号");
            focusView = etPhone;
            cancel = true;
        }

        if (TextUtils.isEmpty(username)) {
            tilUsername.setError("请输入用户名");
            focusView = etUsername;
            cancel = true;
        }

        if (cancel) {
            // 聚焦到第一个错误的输入框
            focusView.requestFocus();
        } else {
            // 执行注册操作
            showLoading(true);
            
            userManager.register(username, phone, password, new UserManager.AuthListener() {
                @Override
                public void onSuccess() {
                    showLoading(false);
                    Toast.makeText(RegisterActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
                    // 跳转到登录页面
                    navigateToLogin();
                }

                @Override
                public void onError(String errorMessage) {
                    showLoading(false);
                    
                    // 判断是否是连接错误
                    if (errorMessage.contains("请求超时") || errorMessage.contains("无法连接到服务器") || errorMessage.contains("网络错误")) {
                        showConnectionErrorDialog(username, phone, password);
                    } else {
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
    
    private void showLoading(boolean isLoading) {
        if (progressIndicator != null) {
            if (isLoading) {
                progressIndicator.setVisibility(View.VISIBLE);
                btnRegister.setEnabled(false);
            } else {
                progressIndicator.setVisibility(View.GONE);
                btnRegister.setEnabled(true);
            }
        }
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * 显示连接错误对话框
     */
    private void showConnectionErrorDialog(final String username, final String phone, final String password) {
        new AlertDialog.Builder(this)
            .setTitle("连接失败")
            .setMessage("无法连接到服务器。\n\n可能原因:\n" +
                    "1. 服务器未启动\n" +
                    "2. 手机与服务器不在同一网络\n" +
                    "3. 服务器IP地址或端口配置错误\n\n" +
                    "您可以检查ApiConfig.java中的IP地址配置。")
            .setPositiveButton("重试", (dialog, which) -> {
                // 重试注册
                showLoading(true);
                userManager.register(username, phone, password, new UserManager.AuthListener() {
                    @Override
                    public void onSuccess() {
                        showLoading(false);
                        Toast.makeText(RegisterActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
                        navigateToLogin();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        showLoading(false);
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
            .setCancelable(false)
            .show();
    }
} 