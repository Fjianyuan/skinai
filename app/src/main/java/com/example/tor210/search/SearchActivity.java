package com.example.tor210.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.tor210.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private EditText editSearch;
    private ImageView btnBack;
    private ImageView btnClear;
    private TextView btnSearch;
    private TextView btnClearHistory;
    private ChipGroup chipGroupHot;
    private RecyclerView recyclerHistory;
    private RecyclerView recyclerResults;

    private SearchHistoryAdapter historyAdapter;
    private SearchResultAdapter resultAdapter;
    private List<String> historyList = new ArrayList<>();
    private List<SearchResult> resultList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // 初始化控件
        initViews();
        // 设置监听器
        setupListeners();
        // 初始化适配器
        initAdapters();
        // 加载测试数据
        loadMockData();
    }

    private void initViews() {
        editSearch = findViewById(R.id.edit_search);
        btnBack = findViewById(R.id.btn_back);
        btnClear = findViewById(R.id.btn_clear);
        btnSearch = findViewById(R.id.btn_search);
        btnClearHistory = findViewById(R.id.btn_clear_history);
        chipGroupHot = findViewById(R.id.chip_group_hot);
        recyclerHistory = findViewById(R.id.recycler_history);
        recyclerResults = findViewById(R.id.recycler_results);
    }

    private void setupListeners() {
        // 返回按钮点击事件
        btnBack.setOnClickListener(v -> finish());

        // 搜索框输入监听
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    btnClear.setVisibility(View.VISIBLE);
                } else {
                    btnClear.setVisibility(View.GONE);
                    showInitialState();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // 清除按钮点击事件
        btnClear.setOnClickListener(v -> editSearch.setText(""));

        // 键盘搜索按钮点击事件
        editSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        // 搜索按钮点击事件
        btnSearch.setOnClickListener(v -> performSearch());

        // 清空历史按钮点击事件
        btnClearHistory.setOnClickListener(v -> {
            historyList.clear();
            historyAdapter.notifyDataSetChanged();
            Toast.makeText(this, "搜索历史已清空", Toast.LENGTH_SHORT).show();
        });

        // 热门搜索标签点击事件
        for (int i = 0; i < chipGroupHot.getChildCount(); i++) {
            View child = chipGroupHot.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setOnClickListener(v -> {
                    editSearch.setText(chip.getText());
                    performSearch();
                });
            }
        }
    }

    private void initAdapters() {
        // 搜索历史适配器
        historyAdapter = new SearchHistoryAdapter(this, historyList);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerHistory.setAdapter(historyAdapter);

        // 设置历史项点击事件
        historyAdapter.setOnItemClickListener((keyword, position) -> {
            editSearch.setText(keyword);
            performSearch();
        });

        // 搜索结果适配器
        resultAdapter = new SearchResultAdapter(this, resultList);
        
        // 设置布局管理器 - 两列瀑布流，与首页保持一致
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerResults.setLayoutManager(layoutManager);
        recyclerResults.setAdapter(resultAdapter);
        
        // 设置内边距
        int padding = getResources().getDimensionPixelSize(R.dimen.recycler_padding);
        recyclerResults.setPadding(padding, padding, padding, padding);
        recyclerResults.setClipToPadding(false);

        // 设置结果项点击事件
        resultAdapter.setOnItemClickListener((result, position) -> {
            Toast.makeText(this, "点击了: " + result.getTitle(), Toast.LENGTH_SHORT).show();
            // 查看详情逻辑
        });
    }

    private void loadMockData() {
        // 模拟搜索历史数据
        historyList.addAll(Arrays.asList("祛痘精华", "敏感肌面霜推荐", "去痘印最有效方法", "如何判断肌肤类型"));
        historyAdapter.notifyDataSetChanged();
    }

    private void performSearch() {
        String keyword = editSearch.getText().toString().trim();
        if (keyword.isEmpty()) {
            Toast.makeText(this, "请输入搜索内容", Toast.LENGTH_SHORT).show();
            return;
        }

        // 添加到搜索历史（避免重复）
        if (!historyList.contains(keyword)) {
            historyList.add(0, keyword);
            historyAdapter.notifyDataSetChanged();
        }

        // 模拟搜索结果
        showSearchResults(keyword);
    }

    private void showSearchResults(String keyword) {
        // 隐藏初始页面，显示结果页面
        findViewById(R.id.tv_hot_searches_title).setVisibility(View.GONE);
        findViewById(R.id.chip_group_hot).setVisibility(View.GONE);
        findViewById(R.id.layout_history_title).setVisibility(View.GONE);
        findViewById(R.id.recycler_history).setVisibility(View.GONE);
        findViewById(R.id.recycler_results).setVisibility(View.VISIBLE);

        // 清除旧结果
        resultList.clear();

        // 添加模拟搜索结果，格式与首页保持一致
        resultList.add(new SearchResult(
                1,
                "https://example.com/img1.jpg",
                keyword + " - 如何有效解决痘痘问题",
                "皮肤科专家",
                "https://example.com/avatar1.jpg",
                1200,
                88
        ));

        resultList.add(new SearchResult(
                2,
                "https://example.com/img2.jpg",
                "我用这款产品解决了" + keyword + "困扰",
                "护肤达人",
                "https://example.com/avatar2.jpg",
                3400,
                102
        ));

        resultList.add(new SearchResult(
                3,
                "https://example.com/img3.jpg",
                keyword + "问题的科学解答",
                "医学博士",
                "https://example.com/avatar3.jpg",
                2800,
                156
        ));

        resultList.add(new SearchResult(
                4,
                "https://example.com/img4.jpg",
                "关于" + keyword + "的常见误区",
                "美容顾问",
                "https://example.com/avatar4.jpg",
                1700,
                67
        ));
        
        resultList.add(new SearchResult(
                5,
                "https://example.com/img5.jpg",
                keyword + "相关的护肤品推荐",
                "美妆博主小红",
                "https://example.com/avatar5.jpg",
                4200,
                215
        ));
        
        resultList.add(new SearchResult(
                6,
                "https://example.com/img6.jpg",
                "抗痘修复 | " + keyword + "专题",
                "皮肤医生",
                "https://example.com/avatar6.jpg",
                2500,
                120
        ));
        
        resultList.add(new SearchResult(
                7,
                "https://example.com/img7.jpg",
                "「干货分享」我是如何解决" + keyword + "问题的",
                "护肤达人",
                "https://example.com/avatar7.jpg",
                3800,
                180
        ));
        
        resultList.add(new SearchResult(
                8,
                "https://example.com/img8.jpg",
                keyword + " | 从问题到解决的全过程",
                "医学博士",
                "https://example.com/avatar8.jpg",
                5200,
                260
        ));

        resultAdapter.notifyDataSetChanged();
        
        Toast.makeText(this, "搜索: " + keyword, Toast.LENGTH_SHORT).show();
    }

    private void showInitialState() {
        // 恢复初始页面
        findViewById(R.id.tv_hot_searches_title).setVisibility(View.VISIBLE);
        findViewById(R.id.chip_group_hot).setVisibility(View.VISIBLE);
        findViewById(R.id.layout_history_title).setVisibility(View.VISIBLE);
        findViewById(R.id.recycler_history).setVisibility(View.VISIBLE);
        findViewById(R.id.recycler_results).setVisibility(View.GONE);
    }
} 