package com.example.tor210.utils;

import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

/**
 * Glide模块配置类
 * 用于解决"Failed to find GeneratedAppGlideModule"警告
 */
@GlideModule
public final class MyAppGlideModule extends AppGlideModule {
    // 可以为空，或添加自定义配置
} 