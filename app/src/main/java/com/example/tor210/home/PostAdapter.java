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
import com.example.tor210.R;

import java.util.List;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 帖子适配器，用于瀑布流展示
 */
public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private Context context;
    private List<Post> postList;
    private OnItemClickListener itemClickListener;
    private Random random = new Random();  // 用于生成随机高度

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_card, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        
        // 设置标题
        holder.tvTitle.setText(post.getTitle());
        
        // 设置作者信息
        holder.tvUsername.setText(post.getAuthorName());
        
        // 设置点赞数
        holder.tvLikeCount.setText(post.getFormattedLikeCount());
        
        // 设置评论数
        holder.tvCommentCount.setText(String.valueOf(post.getCommentCount()));
        
        // 设置封面图片，使用Glide加载
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            // 设置一个随机高度，模拟瀑布流效果
            int height = 300 + random.nextInt(300);  // 300-600dp之间的随机高度
            ViewGroup.LayoutParams layoutParams = holder.imageCover.getLayoutParams();
            layoutParams.height = dpToPx(height);
            holder.imageCover.setLayoutParams(layoutParams);
            
            Glide.with(context)
                    .load(R.drawable.placeholder_image)  // 实际项目中应该使用post.getImageUrl()
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .centerCrop()
                    .into(holder.imageCover);
        }
        
        // 设置作者头像
        Glide.with(context)
                .load(R.drawable.placeholder_image)  // 实际项目中应该使用post.getAuthorAvatarUrl()
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .centerCrop()
                .into(holder.imageAvatar);
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(postList.get(position), position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList != null ? postList.size() : 0;
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
        void onItemClick(Post post, int position);
    }
    
    /**
     * 将dp转换为像素
     */
    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * 帖子视图持有者
     */
    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView imageCover;
        CircleImageView imageAvatar;
        TextView tvTitle;
        TextView tvUsername;
        TextView tvLikeCount;
        TextView tvCommentCount;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            imageCover = itemView.findViewById(R.id.iv_post_image);
            imageAvatar = itemView.findViewById(R.id.iv_author_avatar);
            tvTitle = itemView.findViewById(R.id.tv_post_title);
            tvUsername = itemView.findViewById(R.id.tv_author_name);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count);
            tvCommentCount = itemView.findViewById(R.id.tv_comment_count);
        }
    }
} 