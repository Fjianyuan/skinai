package com.example.tor210.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.tor210.R;

/**
 * 图片加载帮助类
 */
public class ImageLoadHelper {

    /**
     * 加载图片
     *
     * @param context  上下文
     * @param imageUrl 图片URL
     * @param imageView 图片视图
     * @param callback 回调
     */
    public static void loadImage(Context context, String imageUrl, ImageView imageView, ImageLoadCallback callback) {
        // 处理URL格式
        String fullImageUrl = getFullImageUrl(imageUrl);
        
        // 记录日志
        android.util.Log.d("ImageLoadHelper", "开始加载图片: " + fullImageUrl);
        
        // 如果URL为空，直接显示占位图并返回
        if (fullImageUrl == null || fullImageUrl.isEmpty()) {
            android.util.Log.w("ImageLoadHelper", "图片URL为空，显示占位图");
            imageView.setImageResource(R.drawable.placeholder_image);
            if (callback != null) {
                callback.onError(new GlideException("图片URL为空"));
            }
            return;
        }
        
        try {
            // 检查是否是drawable资源
            if (fullImageUrl.startsWith("drawable://")) {
                try {
                    // 从drawable://scheme中提取资源ID
                    int resourceId = Integer.parseInt(fullImageUrl.substring("drawable://".length()));
                    android.util.Log.d("ImageLoadHelper", "加载本地drawable资源: " + resourceId);
                    imageView.setImageResource(resourceId);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                    return;
                } catch (NumberFormatException e) {
                    android.util.Log.e("ImageLoadHelper", "解析drawable资源ID失败: " + e.getMessage());
                    imageView.setImageResource(R.drawable.placeholder_image);
                    if (callback != null) {
                        callback.onError(new GlideException("解析drawable资源ID失败", e));
                    }
                    return;
                }
            }
            
            // 加载网络图片
            Glide.with(context)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            android.util.Log.e("ImageLoadHelper", "图片加载失败: " + fullImageUrl);
                            if (e != null) {
                                android.util.Log.e("ImageLoadHelper", "错误信息: " + e.getMessage());
                                for (Throwable t : e.getRootCauses()) {
                                    android.util.Log.e("ImageLoadHelper", "根本原因: " + t.getMessage());
                                }
                            }
                            if (callback != null) {
                                callback.onError(e);
                            }
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            android.util.Log.d("ImageLoadHelper", "图片加载成功: " + fullImageUrl + " (来源: " + dataSource + ")");
                            if (callback != null) {
                                callback.onSuccess();
                            }
                            return false;
                        }
                    })
                    .into(imageView);
        } catch (Exception e) {
            android.util.Log.e("ImageLoadHelper", "Glide加载异常: " + e.getMessage());
            imageView.setImageResource(R.drawable.placeholder_image);
            if (callback != null) {
                callback.onError(new GlideException("Glide加载异常", e));
            }
        }
    }
    
    /**
     * 获取完整的图片URL
     *
     * @param imageUrl 图片URL
     * @return 处理后的完整URL
     */
    public static String getFullImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return "";
        }
        
        // 服务器基础URL，需要与HomeFragment中的API_BASE_URL保持一致
        String baseUrl = "http://192.168.100.249:5000"; // 修改为服务器实际IP地址
        
        // 检查URL格式
        if (imageUrl.startsWith("/")) {
            // 相对路径，需要添加基础URL
            return baseUrl + imageUrl;
        } else if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            // 完整URL，直接返回
            return imageUrl;
        } else {
            // 其他情况，可能是资源ID或其他格式
            return imageUrl;
        }
    }
} 