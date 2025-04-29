package com.example.tor210.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 用户管理类，处理用户登录状态、token和会话管理
 */
public class UserManager {
    private static final String TAG = "UserManager";
    
    // SharedPreferences存储键
    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_AVATAR_URL = "avatar_url";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    
    private static UserManager instance;
    private final SharedPreferences preferences;
    private final Context context;
    
    // 用户信息
    private String userId;
    private String username;
    private String avatarUrl;
    private String token;
    private boolean isLoggedIn;
    
    // 监听器接口
    public interface AuthListener {
        void onSuccess();
        void onError(String errorMessage);
    }
    
    // 单例模式获取实例
    public static synchronized UserManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserManager(context.getApplicationContext());
        }
        return instance;
    }
    
    // 私有构造函数
    private UserManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadUserData();
    }
    
    // 从SharedPreferences加载用户数据
    private void loadUserData() {
        this.token = preferences.getString(KEY_TOKEN, null);
        this.userId = preferences.getString(KEY_USER_ID, null);
        this.username = preferences.getString(KEY_USERNAME, null);
        this.avatarUrl = preferences.getString(KEY_AVATAR_URL, null);
        this.isLoggedIn = preferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    // 保存用户数据到SharedPreferences
    private void saveUserData() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_AVATAR_URL, avatarUrl);
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);
        editor.apply();
    }
    
    // 清除用户数据
    private void clearUserData() {
        this.token = null;
        this.userId = null;
        this.username = null;
        this.avatarUrl = null;
        this.isLoggedIn = false;
        
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }
    
    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @param listener 回调监听器
     */
    public void login(String username, String password, final AuthListener listener) {
        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("username", username);
            requestBody.put("password", password);
            
            // 发送登录请求
            HttpUtils.post(ApiConfig.LOGIN_URL, requestBody, null, 
                response -> {
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            // 提取用户信息和令牌
                            JSONObject userObj = response.getJSONObject("user");
                            this.userId = userObj.getString("id");
                            this.username = userObj.getString("username");
                            this.avatarUrl = userObj.optString("avatarUrl", null);
                            this.token = response.getString("token");
                            this.isLoggedIn = true;
                            
                            // 保存用户数据
                            saveUserData();
                            
                            if (listener != null) {
                                listener.onSuccess();
                            }
                        } else {
                            String message = response.optString("message", "登录失败");
                            if (listener != null) {
                                listener.onError(message);
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "解析登录响应时出错: " + e.getMessage());
                        if (listener != null) {
                            listener.onError("登录数据解析错误");
                        }
                    }
                },
                error -> {
                    String errorMsg = HttpUtils.getErrorMessage(error);
                    Log.e(TAG, "登录请求失败: " + errorMsg);
                    if (listener != null) {
                        listener.onError(errorMsg);
                    }
                });
        } catch (JSONException e) {
            Log.e(TAG, "构建登录请求时出错: " + e.getMessage());
            if (listener != null) {
                listener.onError("构建登录请求失败");
            }
        }
    }
    
    /**
     * 用户注册
     *
     * @param username 用户名
     * @param phone    手机号
     * @param password 密码
     * @param listener 回调监听器
     */
    public void register(String username, String phone, String password, final AuthListener listener) {
        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("username", username);
            requestBody.put("phone", phone);
            requestBody.put("password", password);
            
            // 发送注册请求
            HttpUtils.post(ApiConfig.REGISTER_URL, requestBody, null,
                response -> {
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            // 注册成功
                            if (listener != null) {
                                listener.onSuccess();
                            }
                        } else {
                            String message = response.optString("message", "注册失败");
                            if (listener != null) {
                                listener.onError(message);
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "解析注册响应时出错: " + e.getMessage());
                        if (listener != null) {
                            listener.onError("注册数据解析错误");
                        }
                    }
                },
                error -> {
                    String errorMsg = HttpUtils.getErrorMessage(error);
                    Log.e(TAG, "注册请求失败: " + errorMsg);
                    if (listener != null) {
                        listener.onError(errorMsg);
                    }
                });
        } catch (JSONException e) {
            Log.e(TAG, "构建注册请求时出错: " + e.getMessage());
            if (listener != null) {
                listener.onError("构建注册请求失败");
            }
        }
    }
    
    /**
     * 退出登录
     *
     * @param listener 回调监听器
     */
    public void logout(final AuthListener listener) {
        if (token == null || !isLoggedIn) {
            // 已经是登出状态
            clearUserData();
            if (listener != null) {
                listener.onSuccess();
            }
            return;
        }
        
        // 发送退出登录请求
        HttpUtils.post(ApiConfig.LOGOUT_URL, null, token,
            response -> {
                // 无论服务器响应如何，都清除本地用户数据
                clearUserData();
                if (listener != null) {
                    listener.onSuccess();
                }
            },
            error -> {
                // 即使请求失败，也清除本地用户数据
                clearUserData();
                if (listener != null) {
                    listener.onSuccess();
                }
            });
    }
    
    /**
     * 验证当前令牌是否有效
     *
     * @param listener 回调监听器
     */
    public void verifyToken(final AuthListener listener) {
        if (token == null || !isLoggedIn) {
            if (listener != null) {
                listener.onError("未登录");
            }
            return;
        }
        
        // 发送令牌验证请求
        HttpUtils.get(ApiConfig.VERIFY_TOKEN_URL, token,
            response -> {
                try {
                    boolean valid = response.getBoolean("valid");
                    if (valid) {
                        // 令牌有效，可以选择更新用户信息
                        if (response.has("user")) {
                            JSONObject userObj = response.getJSONObject("user");
                            this.userId = userObj.getString("id");
                            this.username = userObj.getString("username");
                            this.avatarUrl = userObj.optString("avatarUrl", null);
                            saveUserData();
                        }
                        
                        if (listener != null) {
                            listener.onSuccess();
                        }
                    } else {
                        // 令牌无效，清除用户数据
                        clearUserData();
                        if (listener != null) {
                            String message = response.optString("message", "令牌无效");
                            listener.onError(message);
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "解析令牌验证响应时出错: " + e.getMessage());
                    if (listener != null) {
                        listener.onError("验证数据解析错误");
                    }
                }
            },
            error -> {
                // 如果是401错误，则令牌可能已过期
                if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                    clearUserData();
                }
                
                String errorMsg = HttpUtils.getErrorMessage(error);
                Log.e(TAG, "令牌验证请求失败: " + errorMsg);
                if (listener != null) {
                    listener.onError(errorMsg);
                }
            });
    }
    
    // Getters
    public String getUserId() {
        return userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public String getToken() {
        return token;
    }
    
    public boolean isLoggedIn() {
        return isLoggedIn;
    }
} 