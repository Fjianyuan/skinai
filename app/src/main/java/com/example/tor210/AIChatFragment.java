package com.example.tor210;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tor210.database.api.ApiClient;
import com.example.tor210.database.models.ChatHistory;
import com.example.tor210.database.models.ChatMessage;

import java.util.List;
import java.util.UUID;

public class AIChatFragment extends Fragment {

    private ScrollView chatScrollView;
    private LinearLayout chatContainer;
    private EditText messageInput;
    private ImageButton sendButton;
    private Button newChatButton;
    private Button historyButton;
    private ApiClient apiClient;
    private ChatHistory currentChatHistory;
    private String userId;
    private static final String PREF_USER_ID = "user_id";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ai_chat, container, false);
        
        // 初始化API客户端
        apiClient = new ApiClient();
        
        // 获取或生成用户ID
        getUserId();
        
        // 初始化控件
        chatScrollView = view.findViewById(R.id.chat_scroll_view);
        chatContainer = view.findViewById(R.id.chat_container);
        messageInput = view.findViewById(R.id.message_input);
        sendButton = view.findViewById(R.id.send_button);
        newChatButton = view.findViewById(R.id.new_chat_button);
        historyButton = view.findViewById(R.id.history_button);
        
        // 设置发送按钮点击事件
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
            }
        });
        
        // 设置新对话按钮点击事件
        newChatButton.setOnClickListener(v -> {
            startNewChat();
        });
        
        // 设置历史记录按钮点击事件
        historyButton.setOnClickListener(v -> {
            showChatHistories();
        });
        
        // 创建新的聊天
        startNewChat();
        
        return view;
    }
    
    private void getUserId() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("chat_preferences", Context.MODE_PRIVATE);
        userId = sharedPreferences.getString(PREF_USER_ID, null);
        
        if (userId == null) {
            // 生成新的用户ID
            userId = UUID.randomUUID().toString();
            sharedPreferences.edit().putString(PREF_USER_ID, userId).apply();
        }
    }
    
    private void startNewChat() {
        // 清空聊天记录
        chatContainer.removeAllViews();
        
        // 创建新的聊天历史
        currentChatHistory = new ChatHistory("新对话", userId);
        apiClient.createChatHistory(currentChatHistory, new ApiClient.ApiCallback<ChatHistory>() {
            @Override
            public void onSuccess(ChatHistory result) {
                currentChatHistory = result;
                getActivity().runOnUiThread(() -> {
                    // 添加欢迎消息
                    addAIMessage("您好，我是皮肤AI助手。请描述您的皮肤问题，我会尽力为您提供专业建议。");
                    // 添加提示标签
                    addAIMessage("您可以这样提问：\n1. 我的脸上有红疹，很痒，可能是什么问题？\n2. 如何改善皮肤干燥？\n3. 青春痘如何护理？");
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "创建聊天失败: " + errorMessage, Toast.LENGTH_SHORT).show();
                    // 离线模式下也显示欢迎消息
                    addAIMessage("您好，我是皮肤AI助手。请描述您的皮肤问题，我会尽力为您提供专业建议。");
                    addAIMessage("您可以这样提问：\n1. 我的脸上有红疹，很痒，可能是什么问题？\n2. 如何改善皮肤干燥？\n3. 青春痘如何护理？");
                });
            }
        });
    }
    
    private void showChatHistories() {
        apiClient.getChatHistories(userId, new ApiClient.ApiCallback<List<ChatHistory>>() {
            @Override
            public void onSuccess(List<ChatHistory> result) {
                if (result.isEmpty()) {
                    getActivity().runOnUiThread(() -> 
                        Toast.makeText(getContext(), "暂无聊天记录", Toast.LENGTH_SHORT).show());
                    return;
                }
                
                // 显示历史记录界面，这里简化为加载最近的一次聊天
                ChatHistory latestHistory = result.get(0);
                loadChatHistory(latestHistory.getId());
            }
            
            @Override
            public void onError(String errorMessage) {
                getActivity().runOnUiThread(() -> 
                    Toast.makeText(getContext(), "获取历史记录失败: " + errorMessage, Toast.LENGTH_SHORT).show());
            }
        });
    }
    
    private void loadChatHistory(int historyId) {
        apiClient.getChatHistory(historyId, new ApiClient.ApiCallback<ChatHistory>() {
            @Override
            public void onSuccess(ChatHistory result) {
                currentChatHistory = result;
                getActivity().runOnUiThread(() -> {
                    // 清空聊天容器
                    chatContainer.removeAllViews();
                    
                    // 显示历史消息
                    for (ChatMessage message : result.getMessages()) {
                        if (message.isUserMessage()) {
                            addUserMessage(message.getMessage());
                        } else {
                            addAIMessage(message.getMessage());
                        }
                    }
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                getActivity().runOnUiThread(() -> 
                    Toast.makeText(getContext(), "加载聊天历史失败: " + errorMessage, Toast.LENGTH_SHORT).show());
            }
        });
    }
    
    private void sendMessage(String message) {
        // 显示用户消息
        addUserMessage(message);
        messageInput.setText("");
        
        // 保存到数据库
        if (currentChatHistory != null && currentChatHistory.getId() > 0) {
            ChatMessage chatMessage = new ChatMessage(message, true, userId);
            apiClient.addChatMessage(currentChatHistory.getId(), chatMessage, new ApiClient.ApiCallback<ChatMessage>() {
                @Override
                public void onSuccess(ChatMessage result) {
                    // 消息添加成功
                }
                
                @Override
                public void onError(String errorMessage) {
                    getActivity().runOnUiThread(() -> 
                        Toast.makeText(getContext(), "保存消息失败: " + errorMessage, Toast.LENGTH_SHORT).show());
                }
            });
        }
        
        // 模拟AI回复
        simulateAIResponse(message);
    }
    
    private void addUserMessage(String message) {
        View messageView = LayoutInflater.from(getContext()).inflate(R.layout.item_user_message, chatContainer, false);
        TextView messageText = messageView.findViewById(R.id.message_text);
        messageText.setText(message);
        chatContainer.addView(messageView);
        
        // 滚动到底部
        chatScrollView.post(() -> chatScrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }
    
    private void addAIMessage(String message) {
        View messageView = LayoutInflater.from(getContext()).inflate(R.layout.item_ai_message, chatContainer, false);
        TextView messageText = messageView.findViewById(R.id.message_text);
        messageText.setText(message);
        chatContainer.addView(messageView);
        
        // 滚动到底部
        chatScrollView.post(() -> chatScrollView.fullScroll(ScrollView.FOCUS_DOWN));
        
        // 如果当前有聊天历史，保存AI回复到数据库
        if (currentChatHistory != null && currentChatHistory.getId() > 0) {
            ChatMessage chatMessage = new ChatMessage(message, false, userId);
            apiClient.addChatMessage(currentChatHistory.getId(), chatMessage, new ApiClient.ApiCallback<ChatMessage>() {
                @Override
                public void onSuccess(ChatMessage result) {
                    // 消息添加成功
                }
                
                @Override
                public void onError(String errorMessage) {
                    // 失败时不提示用户，避免干扰体验
                    Log.e("AIChatFragment", "保存AI消息失败: " + errorMessage);
                }
            });
        }
    }
    
    private void simulateAIResponse(String userMessage) {
        // 模拟网络延迟
        chatScrollView.postDelayed(() -> {
            String response;
            
            // 简单的关键词回复逻辑
            if (userMessage.contains("痘") || userMessage.contains("青春痘") || userMessage.contains("痤疮")) {
                response = "青春痘(痤疮)是一种常见的皮肤问题。建议：\n\n1. 保持面部清洁，每天用温和洁面产品清洗两次\n2. 使用含有水杨酸或过氧化苯甲酰的产品\n3. 避免挤压痘痘\n4. 保持规律作息和健康饮食\n5. 严重情况建议咨询皮肤科医生";
            } else if (userMessage.contains("干燥") || userMessage.contains("干")) {
                response = "皮肤干燥的改善方法：\n\n1. 使用温和无皂基的洁面产品\n2. 选择含有保湿成分的护肤品(如透明质酸、甘油、神经酰胺等)\n3. 每天补充足够的水分\n4. 使用加湿器增加室内湿度\n5. 避免过热水洗脸和洗澡";
            } else if (userMessage.contains("红疹") || userMessage.contains("痒") || userMessage.contains("湿疹")) {
                response = "皮肤出现红疹并伴有瘙痒可能是湿疹、过敏或其他皮肤炎症。建议：\n\n1. 避免用热水清洗患处\n2. 使用无香料的温和护肤品\n3. 避免抓挠\n4. 可以尝试使用含有1%氢化可的松的非处方药膏\n5. 如症状持续或加重，请尽快就医";
            } else if (userMessage.contains("防晒") || userMessage.contains("晒黑")) {
                response = "防晒建议：\n\n1. 每天使用SPF30以上的防晒产品，并每2-3小时补涂一次\n2. 寻找包含物理和化学防晒成分的产品\n3. 在户外活动时佩戴宽边帽和防晒衣\n4. 尽量避开上午10点至下午4点的强烈阳光\n5. 即使阴天也需要防晒";
            } else {
                response = "感谢您的咨询。根据您描述的情况，我建议您提供更多具体症状，如发病部位、持续时间、是否有疼痛或瘙痒感等，这样我能给您更准确的建议。\n\n请注意，AI诊断仅供参考，严重或持续的皮肤问题请及时就医。";
            }
            
            addAIMessage(response);
        }, 1000);
    }
} 