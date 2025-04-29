package com.example.tor210.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.GlideException;
import com.example.tor210.R;
import com.example.tor210.utils.ImageLoadCallback;
import com.example.tor210.utils.ImageLoadHelper;

import java.util.List;

/**
 * 护肤小贴士适配器，用于瀑布流展示
 */
public class TipAdapter extends RecyclerView.Adapter<TipAdapter.TipViewHolder> {

    private Context context;
    private List<Tip> tipList;
    private OnItemClickListener itemClickListener;

    public TipAdapter(Context context, List<Tip> tipList) {
        this.context = context;
        this.tipList = tipList;
    }

    @NonNull
    @Override
    public TipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tip_card, parent, false);
        return new TipViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TipViewHolder holder, int position) {
        Tip tip = tipList.get(position);
        
        // 设置标题
        holder.tvTitle.setText(tip.getTitle());
        
        // 设置封面图片，使用ImageLoadHelper加载
        if (tip.getImageUrl() != null && !tip.getImageUrl().isEmpty()) {
            // 移除随机高度设置，使用固定宽高比
            ViewGroup.LayoutParams layoutParams = holder.imageCover.getLayoutParams();
            // 使用原始布局参数中的高度，依赖于XML中的设置
            holder.imageCover.setLayoutParams(layoutParams);
            
            // 使用ImageLoadHelper加载图片
            ImageLoadHelper.loadImage(context, tip.getImageUrl(), holder.imageCover, new ImageLoadCallback() {
                @Override
                public void onSuccess() {
                    android.util.Log.d("TipAdapter", "图片加载成功: " + tip.getImageUrl());
                }

                @Override
                public void onError(GlideException e) {
                    android.util.Log.e("TipAdapter", "图片加载失败: " + tip.getImageUrl() + ", 错误: " + (e != null ? e.getMessage() : "未知错误"));
                }
            });
        } else {
            // 没有URL，显示占位图
            holder.imageCover.setImageResource(R.drawable.placeholder_image);
        }
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(tipList.get(position), position);
            }
        });
    }

    @Override
    public int getItemCount() {
        int count = tipList != null ? tipList.size() : 0;
        android.util.Log.d("TipAdapter", "当前列表中的小贴士数量: " + count);
        if (count == 0) {
            android.util.Log.w("TipAdapter", "小贴士列表为空，请检查数据源");
        }
        return count;
    }
    
    /**
     * 设置项目点击监听器
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }
    
    /**
     * 项目点击监听器接口
     */
    public interface OnItemClickListener {
        void onItemClick(Tip tip, int position);
    }
    
    /**
     * 将dp转换为像素
     */
    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * 护肤小贴士视图持有者
     */
    static class TipViewHolder extends RecyclerView.ViewHolder {
        ImageView imageCover;
        TextView tvTitle;

        public TipViewHolder(@NonNull View itemView) {
            super(itemView);
            imageCover = itemView.findViewById(R.id.iv_tip_image);
            tvTitle = itemView.findViewById(R.id.tv_tip_title);
        }
    }
} 