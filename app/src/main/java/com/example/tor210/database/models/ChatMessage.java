package com.example.tor210.database.models;

import java.util.Date;

public class ChatMessage {
    private int id;
    private String message;
    private boolean isUserMessage;
    private Date timestamp;
    private String userId;
    
    public ChatMessage() {
        this.timestamp = new Date();
    }
    
    public ChatMessage(String message, boolean isUserMessage, String userId) {
        this.message = message;
        this.isUserMessage = isUserMessage;
        this.userId = userId;
        this.timestamp = new Date();
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public boolean isUserMessage() {
        return isUserMessage;
    }
    
    public void setUserMessage(boolean userMessage) {
        isUserMessage = userMessage;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
} 