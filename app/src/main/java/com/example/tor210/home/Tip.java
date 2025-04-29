package com.example.tor210.home;

/**
 * 护肤小贴士数据模型
 */
public class Tip {
    private long id;
    private String imageUrl;     // 封面图片URL
    private String title;        // 标题
    private String content;      // 详细内容
    
    public Tip(long id, String imageUrl, String title, String content) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.title = title;
        this.content = content;
    }

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
} 