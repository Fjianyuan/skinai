package com.example.tor210.database.api;

import android.content.Context;
import android.util.Log;

import com.example.tor210.api.ApiConfig;
import com.example.tor210.database.DatabaseConfig;
import com.example.tor210.database.models.ChatHistory;
import com.example.tor210.database.models.ChatMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
    
    // 接口回调
    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }
    
    // 获取用户的所有聊天历史
    public void getChatHistories(String token, ApiCallback<List<ChatHistory>> callback) {
        if (token == null || token.isEmpty()) {
            callback.onError("请先登录");
            return;
        }
        Request request = new Request.Builder()
                .url(ApiConfig.CHAT_HISTORY_ENDPOINT)
                .header(ApiConfig.HEADER_AUTHORIZATION, ApiConfig.getAuthorizationHeader(token))
                .get()
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to get chat histories", e);
                callback.onError("网络请求失败: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("服务器错误: " + response.code());
                    return;
                }
                
                try {
                    String responseData = response.body().string();
                    JSONArray jsonArray = new JSONArray(responseData);
                    List<ChatHistory> histories = new ArrayList<>();
                    
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        ChatHistory history = parseChatHistory(jsonObject);
                        histories.add(history);
                    }
                    
                    callback.onSuccess(histories);
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to parse chat histories", e);
                    callback.onError("数据解析错误: " + e.getMessage());
                }
            }
        });
    }
    
    // 根据ID获取聊天历史
    public void getChatHistory(int historyId, ApiCallback<ChatHistory> callback) {
        String url = DatabaseConfig.BASE_API_URL + "chatHistory/" + historyId;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to get chat history", e);
                callback.onError("网络请求失败: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("服务器错误: " + response.code());
                    return;
                }
                
                try {
                    String responseData = response.body().string();
                    JSONObject jsonObject = new JSONObject(responseData);
                    ChatHistory history = parseChatHistory(jsonObject);
                    callback.onSuccess(history);
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to parse chat history", e);
                    callback.onError("数据解析错误: " + e.getMessage());
                }
            }
        });
    }
    
    // 创建新的聊天历史
    public void createChatHistory(ChatHistory history, ApiCallback<ChatHistory> callback) {
        try {
            String url = DatabaseConfig.BASE_API_URL + "chatHistory";
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("title", history.getTitle());
            jsonObject.put("userId", history.getUserId());
            jsonObject.put("createdAt", dateFormat.format(history.getCreatedAt()));
            
            RequestBody body = RequestBody.create(JSON, jsonObject.toString());
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Failed to create chat history", e);
                    callback.onError("网络请求失败: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onError("服务器错误: " + response.code());
                        return;
                    }
                    
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseData);
                        ChatHistory createdHistory = parseChatHistory(jsonResponse);
                        callback.onSuccess(createdHistory);
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to parse created chat history", e);
                        callback.onError("数据解析错误: " + e.getMessage());
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create JSON for chat history", e);
            callback.onError("数据创建错误: " + e.getMessage());
        }
    }
    
    // 添加聊天消息
    public void addChatMessage(int historyId, ChatMessage message, ApiCallback<ChatMessage> callback) {
        try {
            String url = DatabaseConfig.BASE_API_URL + "chatHistory/" + historyId + "/message";
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", message.getMessage());
            jsonObject.put("isUserMessage", message.isUserMessage());
            jsonObject.put("userId", message.getUserId());
            jsonObject.put("timestamp", dateFormat.format(message.getTimestamp()));
            
            RequestBody body = RequestBody.create(JSON, jsonObject.toString());
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Failed to add chat message", e);
                    callback.onError("网络请求失败: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onError("服务器错误: " + response.code());
                        return;
                    }
                    
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseData);
                        ChatMessage addedMessage = parseChatMessage(jsonResponse);
                        callback.onSuccess(addedMessage);
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to parse added chat message", e);
                        callback.onError("数据解析错误: " + e.getMessage());
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create JSON for chat message", e);
            callback.onError("数据创建错误: " + e.getMessage());
        }
    }
    
    // 删除聊天历史
    public void deleteChatHistory(int historyId, ApiCallback<Boolean> callback) {
        String url = DatabaseConfig.BASE_API_URL + "chatHistory/" + historyId;
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to delete chat history", e);
                callback.onError("网络请求失败: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("服务器错误: " + response.code());
                    return;
                }
                
                callback.onSuccess(true);
            }
        });
    }
    
    // 解析ChatHistory对象
    private ChatHistory parseChatHistory(JSONObject jsonObject) throws JSONException {
        ChatHistory history = new ChatHistory();
        history.setId(jsonObject.getInt("id"));
        history.setTitle(jsonObject.getString("title"));
        history.setUserId(jsonObject.getString("userId"));
        
        try {
            String dateStr = jsonObject.getString("createdAt");
            history.setCreatedAt(dateFormat.parse(dateStr));
        } catch (Exception e) {
            history.setCreatedAt(new Date());
        }
        
        if (jsonObject.has("messages")) {
            JSONArray messagesArray = jsonObject.getJSONArray("messages");
            List<ChatMessage> messages = new ArrayList<>();
            
            for (int i = 0; i < messagesArray.length(); i++) {
                JSONObject messageJson = messagesArray.getJSONObject(i);
                ChatMessage message = parseChatMessage(messageJson);
                messages.add(message);
            }
            
            history.setMessages(messages);
        }
        
        return history;
    }
    
    // 解析ChatMessage对象
    private ChatMessage parseChatMessage(JSONObject jsonObject) throws JSONException {
        ChatMessage message = new ChatMessage();
        message.setId(jsonObject.getInt("id"));
        message.setMessage(jsonObject.getString("message"));
        message.setUserMessage(jsonObject.getBoolean("isUserMessage"));
        message.setUserId(jsonObject.getString("userId"));
        
        try {
            String dateStr = jsonObject.getString("timestamp");
            message.setTimestamp(dateFormat.parse(dateStr));
        } catch (Exception e) {
            message.setTimestamp(new Date());
        }
        
        return message;
    }
} 
