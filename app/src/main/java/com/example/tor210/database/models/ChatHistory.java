package com.example.tor210.database.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatHistory {
    private int id;
    private String title;
    private Date createdAt;
    private String userId;
    private List<ChatMessage> messages;
    
    public ChatHistory() {
        this.createdAt = new Date();
        this.messages = new ArrayList<>();
    }
    
    public ChatHistory(String title, String userId) {
        this.title = title;
        this.userId = userId;
        this.createdAt = new Date();
        this.messages = new ArrayList<>();
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public List<ChatMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }
    
    public void addMessage(ChatMessage message) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        this.messages.add(message);
    }
} 