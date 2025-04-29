package com.example.tor210.home;

/**
 * 帖子数据模型
 */
public class Post {
    private long id;
    private String imageUrl;
    private String title;
    private String authorName;
    private String authorAvatarUrl;
    private int likeCount;
    private int commentCount;
    private String content;         // 帖子正文内容
    private long createTime;        // 创建时间
    private String[] imageUrls;     // 多图片URL数组
    private boolean isLiked;        // 是否已点赞

    public Post(long id, String imageUrl, String title, String authorName, String authorAvatarUrl, int likeCount, int commentCount) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.title = title;
        this.authorName = authorName;
        this.authorAvatarUrl = authorAvatarUrl;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.createTime = System.currentTimeMillis();
        this.isLiked = false;
    }

    public Post(long id, String imageUrl, String title, String authorName, String authorAvatarUrl, 
               int likeCount, int commentCount, String content, long createTime, String[] imageUrls, boolean isLiked) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.title = title;
        this.authorName = authorName;
        this.authorAvatarUrl = authorAvatarUrl;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.content = content;
        this.createTime = createTime;
        this.imageUrls = imageUrls;
        this.isLiked = isLiked;
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

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public void setAuthorAvatarUrl(String authorAvatarUrl) {
        this.authorAvatarUrl = authorAvatarUrl;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String[] getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(String[] imageUrls) {
        this.imageUrls = imageUrls;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    /**
     * 格式化点赞数，超过1000显示为k
     */
    public String getFormattedLikeCount() {
        if (likeCount < 1000) {
            return String.valueOf(likeCount);
        } else {
            return String.format("%.1fk", likeCount / 1000f);
        }
    }

    /**
     * 获取格式化的时间
     */
    public String getFormattedTime() {
        long now = System.currentTimeMillis();
        long diff = now - createTime;
        
        // 小于1分钟
        if (diff < 60 * 1000) {
            return "刚刚";
        }
        // 小于1小时
        else if (diff < 60 * 60 * 1000) {
            return diff / (60 * 1000) + "分钟前";
        }
        // 小于24小时
        else if (diff < 24 * 60 * 60 * 1000) {
            return diff / (60 * 60 * 1000) + "小时前";
        }
        // 小于30天
        else if (diff < 30 * 24 * 60 * 60 * 1000L) {
            return diff / (24 * 60 * 60 * 1000) + "天前";
        } else {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            return sdf.format(new java.util.Date(createTime));
        }
    }
} 