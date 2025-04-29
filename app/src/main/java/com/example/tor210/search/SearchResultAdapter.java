package com.example.tor210.search;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tor210.R;

import java.util.List;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 搜索结果适配器
 */
public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ResultViewHolder> {

    private Context context;
    private List<SearchResult> resultList;
    private OnItemClickListener listener;
    private Random random = new Random();  // 用于生成随机高度，与首页保持一致

    public interface OnItemClickListener {
        void onItemClick(SearchResult result, int position);
    }

    public SearchResultAdapter(Context context, List<SearchResult> resultList) {
        this.context = context;
        this.resultList = resultList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_card, parent, false);
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        SearchResult result = resultList.get(position);
        
        // 设置标题
        holder.tvTitle.setText(result.getTitle());
        
        // 设置作者信息
        holder.tvAuthor.setText(result.getAuthor());
        
        // 设置点赞数（格式化处理）
        holder.tvLikeCount.setText(formatLikeCount(result.getLikeCount()));
        
        // 设置评论数
        holder.tvCommentCount.setText(String.valueOf(result.getCommentCount()));
        
        // 设置封面图片，使用Glide加载
        if (result.getImageUrl() != null && !result.getImageUrl().isEmpty()) {
            // 设置一个随机高度，模拟瀑布流效果
            int height = 300 + random.nextInt(300);  // 300-600dp之间的随机高度
            ViewGroup.LayoutParams layoutParams = holder.ivCover.getLayoutParams();
            layoutParams.height = dpToPx(height);
            holder.ivCover.setLayoutParams(layoutParams);
            
            Glide.with(context)
                    .load(R.drawable.placeholder_image)  // 使用占位图，实际开发中应使用result.getImageUrl()
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .centerCrop()
                    .into(holder.ivCover);
        }
        
        // 设置作者头像
        Glide.with(context)
                .load(R.drawable.placeholder_image)  // 使用占位图，实际开发中应使用result.getAuthorAvatar()
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .centerCrop()
                .into(holder.ivAuthorAvatar);
                
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(result, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return resultList.size();
    }
    
    /**
     * 格式化点赞数，超过1000显示为k，与首页保持一致
     */
    private String formatLikeCount(int likeCount) {
        if (likeCount < 1000) {
            return String.valueOf(likeCount);
        } else {
            return String.format("%.1fk", likeCount / 1000f);
        }
    }
    
    /**
     * 将dp转换为像素，与首页保持一致
     */
    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle;
        TextView tvAuthor;
        CircleImageView ivAuthorAvatar;
        TextView tvLikeCount;
        TextView tvCommentCount;

        public ResultViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_post_image);
            tvTitle = itemView.findViewById(R.id.tv_post_title);
            tvAuthor = itemView.findViewById(R.id.tv_author_name);
            ivAuthorAvatar = itemView.findViewById(R.id.iv_author_avatar);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count);
            tvCommentCount = itemView.findViewById(R.id.tv_comment_count);
        }
    }
} 