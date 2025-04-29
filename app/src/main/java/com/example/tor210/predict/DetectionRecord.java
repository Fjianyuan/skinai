package com.example.tor210.predict;

import java.util.Date;

/**
 * 检测记录数据模型
 */
public class DetectionRecord {
    private long id;            // 记录ID
    private String imageUrl;    // 结果图片URL
    private Date recordTime;    // 记录时间
    private int acneCount;      // 痘痘数量
    private String severity;    // 严重程度
    private String acneTypes;   // 痘痘类型 (JSON格式的字符串)
    private String recommendation; // 护理建议

    public DetectionRecord() {
        this.recordTime = new Date(); // 默认为当前时间
    }

    public DetectionRecord(String imageUrl, int acneCount, String severity, String acneTypes) {
        this.imageUrl = imageUrl;
        this.acneCount = acneCount;
        this.severity = severity;
        this.acneTypes = acneTypes;
        this.recordTime = new Date();
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Date getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(Date recordTime) {
        this.recordTime = recordTime;
    }

    public int getAcneCount() {
        return acneCount;
    }

    public void setAcneCount(int acneCount) {
        this.acneCount = acneCount;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getAcneTypes() {
        return acneTypes;
    }

    public void setAcneTypes(String acneTypes) {
        this.acneTypes = acneTypes;
    }
    
    public String getRecommendation() {
        return recommendation;
    }
    
    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
} 