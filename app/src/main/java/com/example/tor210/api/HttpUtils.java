package com.example.tor210.api;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP请求工具类，封装Volley网络请求
 */
public class HttpUtils {
    private static final String TAG = "HttpUtils";
    private static RequestQueue requestQueue;

    /**
     * 初始化请求队列
     *
     * @param context 应用上下文
     */
    public static void init(Context context) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
    }

    /**
     * 发送GET请求
     *
     * @param url             请求URL
     * @param token           认证令牌（可选）
     * @param successListener 成功回调
     * @param errorListener   错误回调
     */
    public static void get(String url, final String token,
                          Response.Listener<JSONObject> successListener,
                          Response.ErrorListener errorListener) {
        if (requestQueue == null) {
            Log.e(TAG, "RequestQueue尚未初始化，请先调用init方法");
            if (errorListener != null) {
                errorListener.onErrorResponse(new VolleyError("RequestQueue尚未初始化"));
            }
            return;
        }

        Log.d(TAG, "发送GET请求: " + url);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null, successListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put(ApiConfig.HEADER_CONTENT_TYPE, ApiConfig.CONTENT_TYPE_JSON);
                if (token != null && !token.isEmpty()) {
                    headers.put(ApiConfig.HEADER_AUTHORIZATION, ApiConfig.getAuthorizationHeader(token));
                }
                return headers;
            }
        };

        // 设置超时和重试策略
        request.setRetryPolicy(new DefaultRetryPolicy(
                ApiConfig.TIMEOUT_MS,
                3,  // 增加重试次数
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    /**
     * 发送POST请求
     *
     * @param url             请求URL
     * @param jsonBody        请求体JSON
     * @param token           认证令牌（可选）
     * @param successListener 成功回调
     * @param errorListener   错误回调
     */
    public static void post(String url, JSONObject jsonBody, final String token,
                           Response.Listener<JSONObject> successListener,
                           Response.ErrorListener errorListener) {
        if (requestQueue == null) {
            Log.e(TAG, "RequestQueue尚未初始化，请先调用init方法");
            if (errorListener != null) {
                errorListener.onErrorResponse(new VolleyError("RequestQueue尚未初始化"));
            }
            return;
        }

        Log.d(TAG, "发送POST请求: " + url + ", 数据: " + (jsonBody != null ? jsonBody.toString() : "null"));
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, url, jsonBody, successListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put(ApiConfig.HEADER_CONTENT_TYPE, ApiConfig.CONTENT_TYPE_JSON);
                if (token != null && !token.isEmpty()) {
                    headers.put(ApiConfig.HEADER_AUTHORIZATION, ApiConfig.getAuthorizationHeader(token));
                }
                return headers;
            }
        };

        // 设置超时和重试策略
        request.setRetryPolicy(new DefaultRetryPolicy(
                ApiConfig.TIMEOUT_MS,
                3,  // 增加重试次数
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    /**
     * 发送DELETE请求
     *
     * @param url             请求URL
     * @param token           认证令牌（必需）
     * @param successListener 成功回调
     * @param errorListener   错误回调
     */
    public static void delete(String url, final String token,
                             Response.Listener<JSONObject> successListener,
                             Response.ErrorListener errorListener) {
        if (requestQueue == null) {
            Log.e(TAG, "RequestQueue尚未初始化，请先调用init方法");
            if (errorListener != null) {
                errorListener.onErrorResponse(new VolleyError("RequestQueue尚未初始化"));
            }
            return;
        }

        Log.d(TAG, "发送DELETE请求: " + url);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.DELETE, url, null, successListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put(ApiConfig.HEADER_CONTENT_TYPE, ApiConfig.CONTENT_TYPE_JSON);
                if (token != null && !token.isEmpty()) {
                    headers.put(ApiConfig.HEADER_AUTHORIZATION, ApiConfig.getAuthorizationHeader(token));
                }
                return headers;
            }
        };

        // 设置超时和重试策略
        request.setRetryPolicy(new DefaultRetryPolicy(
                ApiConfig.TIMEOUT_MS,
                3,  // 增加重试次数
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    /**
     * 获取错误描述
     *
     * @param error Volley错误对象
     * @return 用户友好的错误描述
     */
    public static String getErrorMessage(VolleyError error) {
        Log.e(TAG, "网络请求错误: " + error.toString());
        
        if (error instanceof TimeoutError) {
            return "请求超时，请检查网络连接";
        } else if (error instanceof NoConnectionError) {
            return "无法连接到服务器，请检查网络连接";
        } else if (error instanceof NetworkError) {
            return "网络错误，请稍后重试";
        } else if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;
            Log.e(TAG, "HTTP状态码: " + statusCode);
            
            switch (statusCode) {
                case 400:
                    return "请求参数错误(400)";
                case 401:
                    return "身份验证失败，请重新登录(401)";
                case 403:
                    return "没有权限执行此操作(403)";
                case 404:
                    return "请求的资源不存在(404)";
                case 500:
                    return "服务器内部错误(500)";
                default:
                    return "请求失败，错误码: " + statusCode;
            }
        }
        return "未知错误: " + error.getMessage();
    }
} 