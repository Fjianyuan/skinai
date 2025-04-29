package com.example.tor210.utils;

import com.bumptech.glide.load.engine.GlideException;

/**
 * 图片加载回调接口
 */
public interface ImageLoadCallback {
    /**
     * 加载成功回调
     */
    void onSuccess();

    /**
     * 加载失败回调
     *
     * @param e 异常
     */
    void onError(GlideException e);
} 