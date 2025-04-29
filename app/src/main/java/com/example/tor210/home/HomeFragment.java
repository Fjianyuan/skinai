package com.example.tor210.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.tor210.R;
// import com.example.tor210.model.Post; - 此包不存在，改为使用home包中的Post类

import java.util.ArrayList;
import java.util.List;

/**
 * 首页Fragment，展示小红书风格的内容流
 */
public class HomeFragment extends Fragment {

    private RecyclerView recyclerPosts;
    private List<Post> posts = new ArrayList<>(); // 使用同包下的Post类
    private PostAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // 初始化控件
        recyclerPosts = view.findViewById(R.id.recycler_posts);
        LinearLayout searchBar = view.findViewById(R.id.search_bar);
        View btnCreate = view.findViewById(R.id.btn_create);
        
        // 设置点击事件
        searchBar.setOnClickListener(v -> {
            Toast.makeText(getContext(), "搜索功能即将上线", Toast.LENGTH_SHORT).show();
        });
        
        btnCreate.setOnClickListener(v -> {
            navigateToFragment(new CreatePostFragment());
        });
        
        // 初始化RecyclerView
        initPostsRecyclerView();
        
        return view;
    }
    
    /**
     * 初始化瀑布流列表
     */
    private void initPostsRecyclerView() {
        // 创建模拟数据
        createMockData();
        
        // 设置布局管理器为交错网格布局，类似于瀑布流
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerPosts.setLayoutManager(layoutManager);
        
        // 设置适配器
        adapter = new PostAdapter(getContext(), posts);
        adapter.setOnItemClickListener((post, position) -> {
            // 点击帖子，导航到帖子详情页
            PostDetailFragment detailFragment = new PostDetailFragment();
            Bundle args = new Bundle();
            args.putInt("postId", (int)post.getId());
            detailFragment.setArguments(args);
            navigateToFragment(detailFragment);
        });
        recyclerPosts.setAdapter(adapter);
    }
    
    private void navigateToFragment(Fragment fragment) {
        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
    
    /**
     * 创建模拟数据
     */
    private void createMockData() {
        posts.add(new Post(1, "https://img.xiaohongshu.com/1111", "好用的祛痘产品推荐，这款祛痘膏真的非常有效，坚持使用一周就能看到明显效果，适合轻度痘痘肌肤。", "肌肤护理达人", "https://img.xiaohongshu.com/avatar1", 128, 36));
        posts.add(new Post(2, "https://img.xiaohongshu.com/2222", "医生告诉你痘痘的真相，不要再用错误的方法对待痘痘了，会让情况更加严重。", "王医生皮肤科", "https://img.xiaohongshu.com/avatar2", 257, 98));
        posts.add(new Post(3, "https://img.xiaohongshu.com/3333", "痘痘肌肤的饮食注意事项，这些食物会加重痘痘，一定要少吃或者不吃，保持清淡饮食对改善痘痘有很大帮助。", "营养师小李", "https://img.xiaohongshu.com/avatar3", 182, 45));
        posts.add(new Post(4, "https://img.xiaohongshu.com/4444", "敏感肌护肤品推荐，这些产品都是经过敏感肌测试的，不含刺激性成分，可以放心使用。", "敏感肌博主", "https://img.xiaohongshu.com/avatar4", 142, 38));
        posts.add(new Post(5, "https://img.xiaohongshu.com/5555", "护肤的正确步骤，很多人的护肤顺序都是错的，导致护肤效果大打折扣，按照这个顺序来，效果翻倍。", "美妆达人小张", "https://img.xiaohongshu.com/avatar5", 301, 128));
        posts.add(new Post(6, "https://img.xiaohongshu.com/6666", "医生解答：为什么会长痘痘？该如何科学护理？痘痘背后的皮肤科学知识大解析。", "李医生皮肤科诊所", "https://img.xiaohongshu.com/avatar6", 421, 187));
        posts.add(new Post(7, "https://img.xiaohongshu.com/7777", "夏季控油小技巧，这几个方法帮你告别油光满面，做清爽女孩。", "美妆博主小红", "https://img.xiaohongshu.com/avatar7", 156, 42));
        posts.add(new Post(8, "https://img.xiaohongshu.com/8888", "如何正确使用水杨酸去痘？这款成分很多人都用错了，按照这个方法使用效果最好。", "成分党小王", "https://img.xiaohongshu.com/avatar8", 268, 93));
    }
} 