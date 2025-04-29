package com.example.tor210.home;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tor210.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 帖子详情Fragment
 * 用于显示帖子详情信息
 */
public class PostDetailFragment extends Fragment {

    private static final String ARG_POST_ID = "post_id";
    
    private long postId;
    private Post post;
    
    // UI组件
    private CircleImageView ivAuthorAvatar;
    private TextView tvAuthorName;
    private TextView tvPostTime;
    private MaterialButton btnFollow;
    private TextView tvPostTitle;
    private TextView tvPostContent;
    private RecyclerView recyclerImages;
    private TextView tvLikeCount;
    private TextView tvCommentCount;
    private ImageView ivLike;
    private ImageView btnFavorite;
    private RecyclerView recyclerComments;
    private EditText editComment;
    private View btnSendComment;
    
    // 评论列表数据
    private List<Comment> commentList = new ArrayList<>();
    
    /**
     * 创建PostDetailFragment实例
     * @param postId 帖子ID
     * @return Fragment实例
     */
    public static PostDetailFragment newInstance(long postId) {
        PostDetailFragment fragment = new PostDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_POST_ID, postId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            postId = getArguments().getLong(ARG_POST_ID, 0);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_post_detail, container, false);
        initViews(view);
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 加载帖子数据
        loadPostData();
        
        // 设置点击事件
        setupClickListeners();
    }
    
    /**
     * 初始化视图组件
     */
    private void initViews(View view) {
        // 顶部工具栏
        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });
        
        // 作者信息
        ivAuthorAvatar = view.findViewById(R.id.iv_author_avatar);
        tvAuthorName = view.findViewById(R.id.tv_author_name);
        tvPostTime = view.findViewById(R.id.tv_post_time);
        btnFollow = view.findViewById(R.id.btn_follow);
        
        // 帖子内容
        tvPostTitle = view.findViewById(R.id.tv_post_title);
        tvPostContent = view.findViewById(R.id.tv_post_content);
        recyclerImages = view.findViewById(R.id.recycler_images);
        
        // 互动区域
        tvLikeCount = view.findViewById(R.id.tv_like_count);
        tvCommentCount = view.findViewById(R.id.tv_comment_count);
        ivLike = view.findViewById(R.id.iv_like);
        btnFavorite = view.findViewById(R.id.btn_favorite);
        
        // 评论区
        recyclerComments = view.findViewById(R.id.recycler_comments);
        recyclerComments.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // 评论输入区
        editComment = view.findViewById(R.id.edit_comment);
        btnSendComment = view.findViewById(R.id.btn_send_comment);
    }
    
    /**
     * 设置点击事件
     */
    private void setupClickListeners() {
        // 关注按钮
        btnFollow.setOnClickListener(v -> {
            Toast.makeText(getContext(), "关注成功", Toast.LENGTH_SHORT).show();
            btnFollow.setText("已关注");
            btnFollow.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        });
        
        // 点赞按钮
        View layoutLike = getView().findViewById(R.id.layout_like);
        layoutLike.setOnClickListener(v -> {
            if (post != null) {
                boolean isLiked = post.isLiked();
                post.setLiked(!isLiked);
                
                if (!isLiked) {
                    // 点赞
                    post.setLikeCount(post.getLikeCount() + 1);
                    ivLike.setColorFilter(getResources().getColor(android.R.color.holo_red_light));
                } else {
                    // 取消点赞
                    post.setLikeCount(post.getLikeCount() - 1);
                    ivLike.setColorFilter(getResources().getColor(android.R.color.darker_gray));
                }
                
                tvLikeCount.setText(post.getFormattedLikeCount());
            }
        });
        
        // 收藏按钮
        btnFavorite.setOnClickListener(v -> {
            btnFavorite.setColorFilter(getResources().getColor(android.R.color.holo_orange_light));
            Toast.makeText(getContext(), "收藏成功", Toast.LENGTH_SHORT).show();
        });
        
        // 发送评论
        btnSendComment.setOnClickListener(v -> {
            String commentText = editComment.getText().toString().trim();
            if (commentText.isEmpty()) {
                Toast.makeText(getContext(), "评论内容不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // TODO: 提交评论到服务器
            
            // 添加到本地评论列表
            Comment newComment = new Comment(
                    System.currentTimeMillis(),
                    "当前用户",
                    null,
                    commentText,
                    System.currentTimeMillis()
            );
            commentList.add(0, newComment);
            
            // 更新UI
            recyclerComments.getAdapter().notifyItemInserted(0);
            
            // 清空输入框
            editComment.setText("");
            
            // 更新评论数
            if (post != null) {
                post.setCommentCount(post.getCommentCount() + 1);
                tvCommentCount.setText(String.valueOf(post.getCommentCount()));
            }
        });
    }
    
    /**
     * 加载帖子数据
     */
    private void loadPostData() {
        // TODO: 从后端API加载数据
        // 这里使用模拟数据
        post = getMockPost(postId);
        
        if (post != null) {
            // 设置作者信息
            tvAuthorName.setText(post.getAuthorName());
            tvPostTime.setText(post.getFormattedTime());
            Glide.with(this)
                    .load(R.drawable.placeholder_image)
                    .into(ivAuthorAvatar);
            
            // 设置帖子内容
            tvPostTitle.setText(post.getTitle());
            tvPostContent.setText(post.getContent());
            
            // 设置点赞评论数
            tvLikeCount.setText(post.getFormattedLikeCount());
            tvCommentCount.setText(String.valueOf(post.getCommentCount()));
            
            // 设置点赞状态
            if (post.isLiked()) {
                ivLike.setColorFilter(getResources().getColor(android.R.color.holo_red_light));
            }
            
            // 加载评论
            loadComments();
        }
    }
    
    /**
     * 加载评论数据
     */
    private void loadComments() {
        // TODO: 从后端API加载评论数据
        // 这里使用模拟数据
        commentList = getMockComments();
        
        CommentAdapter adapter = new CommentAdapter(getContext(), commentList);
        recyclerComments.setAdapter(adapter);
    }
    
    /**
     * 获取模拟帖子数据
     */
    private Post getMockPost(long id) {
        // 模拟数据
        String content = "大家好，今天给大家分享一下我的护肤心得。\n\n" +
                "作为一个敏感肌+干皮，我试了很多产品，终于找到了一套适合我的护肤方案。首先推荐大家使用温和的氨基酸洁面，然后是无酒精的爽肤水，接着是含有神经酰胺的面霜。\n\n" +
                "最重要的是防晒！一定要选择物理防晒，化学防晒容易过敏。希望对大家有帮助～";
                
        String[] imageUrls = {
            "https://example.com/img1.jpg",
            "https://example.com/img2.jpg",
            "https://example.com/img3.jpg"
        };
        
        return new Post(
                id,
                "https://example.com/img1.jpg",
                "敏感肌护肤心得分享，从此告别过敏泛红",
                "护肤达人",
                "https://example.com/avatar1.jpg",
                1200,
                86,
                content,
                System.currentTimeMillis() - 24 * 60 * 60 * 1000, // 一天前
                imageUrls,
                false
        );
    }
    
    /**
     * 获取模拟评论数据
     */
    private List<Comment> getMockComments() {
        List<Comment> comments = new ArrayList<>();
        
        comments.add(new Comment(1, "用户A", "https://example.com/avatar1.jpg", "很实用的分享，我也是敏感肌，试了很多产品都不合适，谢谢博主！", System.currentTimeMillis() - 12 * 60 * 60 * 1000));
        comments.add(new Comment(2, "用户B", "https://example.com/avatar2.jpg", "请问博主用的是什么牌子的防晒啊？", System.currentTimeMillis() - 10 * 60 * 60 * 1000));
        comments.add(new Comment(3, "用户C", "https://example.com/avatar3.jpg", "收藏了，下次买护肤品就按这个清单来", System.currentTimeMillis() - 8 * 60 * 60 * 1000));
        comments.add(new Comment(4, "用户D", "https://example.com/avatar4.jpg", "我也是干敏皮，冬天特别容易泛红，希望博主多发一些护肤知识", System.currentTimeMillis() - 6 * 60 * 60 * 1000));
        comments.add(new Comment(5, "用户E", "https://example.com/avatar5.jpg", "最近换季，皮肤状态很差，正好需要这样的护肤建议", System.currentTimeMillis() - 4 * 60 * 60 * 1000));
        
        return comments;
    }
    
    /**
     * 评论数据模型
     */
    private static class Comment {
        private long id;
        private String userName;
        private String userAvatar;
        private String content;
        private long createTime;
        
        public Comment(long id, String userName, String userAvatar, String content, long createTime) {
            this.id = id;
            this.userName = userName;
            this.userAvatar = userAvatar;
            this.content = content;
            this.createTime = createTime;
        }
        
        public String getFormattedTime() {
            long now = System.currentTimeMillis();
            long diff = now - createTime;
            
            if (diff < 60 * 1000) {
                return "刚刚";
            } else if (diff < 60 * 60 * 1000) {
                return diff / (60 * 1000) + "分钟前";
            } else if (diff < 24 * 60 * 60 * 1000) {
                return diff / (60 * 60 * 1000) + "小时前";
            } else {
                return diff / (24 * 60 * 60 * 1000) + "天前";
            }
        }
    }
    
    /**
     * 评论适配器
     */
    private class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
        
        private Context context;
        private List<Comment> commentList;
        
        public CommentAdapter(Context context, List<Comment> commentList) {
            this.context = context;
            this.commentList = commentList;
        }
        
        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
            return new CommentViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
            Comment comment = commentList.get(position);
            
            holder.tvUserName.setText(comment.userName);
            holder.tvComment.setText(comment.content);
            holder.tvTime.setText(comment.getFormattedTime());
            
            Glide.with(context)
                    .load(R.drawable.placeholder_image)
                    .into(holder.ivUserAvatar);
        }
        
        @Override
        public int getItemCount() {
            return commentList.size();
        }
        
        class CommentViewHolder extends RecyclerView.ViewHolder {
            CircleImageView ivUserAvatar;
            TextView tvUserName;
            TextView tvComment;
            TextView tvTime;
            
            public CommentViewHolder(@NonNull View itemView) {
                super(itemView);
                ivUserAvatar = itemView.findViewById(R.id.iv_user_avatar);
                tvUserName = itemView.findViewById(R.id.tv_user_name);
                tvComment = itemView.findViewById(R.id.tv_comment);
                tvTime = itemView.findViewById(R.id.tv_time);
            }
        }
    }
} 