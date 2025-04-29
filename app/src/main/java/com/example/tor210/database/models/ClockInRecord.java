package com.example.tor210.database.models;

import java.util.Date;

/**
 * 打卡记录数据模型
 */
public class ClockInRecord {
    private long id;           // 记录ID
    private String userId;     // 用户ID
    private Date clockInDate;  // 打卡日期
    private String imagePath;  // 照片路径
    private String skinStatus; // 皮肤状态：良好、一般、不佳
    private String notes;      // 备注信息
    private int streakDays;    // 连续打卡天数

    public ClockInRecord() {
        this.clockInDate = new Date(); // 默认为当前时间
    }

    public ClockInRecord(String userId, String skinStatus, String imagePath, String notes) {
        this.userId = userId;
        this.skinStatus = skinStatus;
        this.imagePath = imagePath;
        this.notes = notes;
        this.clockInDate = new Date();
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getClockInDate() {
        return clockInDate;
    }

    public void setClockInDate(Date clockInDate) {
        this.clockInDate = clockInDate;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getSkinStatus() {
        return skinStatus;
    }

    public void setSkinStatus(String skinStatus) {
        this.skinStatus = skinStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getStreakDays() {
        return streakDays;
    }

    public void setStreakDays(int streakDays) {
        this.streakDays = streakDays;
    }
} 