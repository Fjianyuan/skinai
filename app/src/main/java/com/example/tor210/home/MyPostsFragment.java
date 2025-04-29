package com.example.tor210.home;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.tor210.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 我的帖子Fragment
 * 用于展示用户发布的帖子
 */
public class MyPostsFragment extends Fragment {

    private static final String PREF_MY_POSTS = "my_posts";
    
    private RecyclerView recyclerPosts;
    private PostAdapter adapter;
    private List<Post> myPosts = new ArrayList<>();
    private TextView tvEmpty;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_posts, container, false);
        
        // 初始化控件
        recyclerPosts = view.findViewById(R.id.recycler_posts);
        tvEmpty = view.findViewById(R.id.tv_empty);
        
        // 设置返回按钮
        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });
        
        // 初始化RecyclerView
        setupRecyclerView();
        
        // 加载数据
        loadMyPosts();
        
        return view;
    }
    
    /**
     * 设置RecyclerView
     */
    private void setupRecyclerView() {
        // 设置布局管理器 - 两列瀑布流
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerPosts.setLayoutManager(layoutManager);
        
        // 创建适配器
        adapter = new PostAdapter(getContext(), myPosts);
        recyclerPosts.setAdapter(adapter);
        
        // 设置点击事件
        adapter.setOnItemClickListener((post, position) -> {
            // 点击进入帖子详情
            PostDetailFragment detailFragment = PostDetailFragment.newInstance(post.getId());
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }
    
    /**
     * 从本地存储加载我的帖子
     */
    private void loadMyPosts() {
        if (getContext() == null) return;
        
        SharedPreferences preferences = getContext().getSharedPreferences("app_preferences", Activity.MODE_PRIVATE);
        String postsJson = preferences.getString(PREF_MY_POSTS, "");
        
        if (!postsJson.isEmpty()) {
            // 解析JSON
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Post>>() {}.getType();
            myPosts.clear();
            myPosts.addAll(gson.fromJson(postsJson, type));
            
            // 更新UI
            adapter.notifyDataSetChanged();
            
            // 显示/隐藏空视图
            updateEmptyView();
        } else {
            // 没有帖子
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerPosts.setVisibility(View.GONE);
        }
    }
    
    /**
     * 更新空视图显示状态
     */
    private void updateEmptyView() {
        if (myPosts.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerPosts.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerPosts.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 每次显示时重新加载数据，确保数据最新
        loadMyPosts();
    }
} 