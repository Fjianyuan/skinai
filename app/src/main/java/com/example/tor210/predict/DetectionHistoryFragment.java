package com.example.tor210.predict;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tor210.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 检测历史记录Fragment
 */
public class DetectionHistoryFragment extends Fragment {

    private static final String TAG = "DetectionHistoryFragment";
    
    private RecyclerView recyclerView;
    private DetectionRecordAdapter adapter;
    private LinearLayout emptyStateContainer;
    private ProgressBar progressBar;
    private ImageButton btnBack;
    private ImageButton btnDeleteAll;
    
    private List<DetectionRecord> recordList;
    private DetectionDatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detection_history, container, false);
        
        // 初始化控件
        recyclerView = view.findViewById(R.id.recycler_detection_records);
        emptyStateContainer = view.findViewById(R.id.empty_state_container);
        progressBar = view.findViewById(R.id.progress_bar);
        btnBack = view.findViewById(R.id.btn_back);
        btnDeleteAll = view.findViewById(R.id.btn_delete_all);
        
        // 设置返回按钮点击事件
        btnBack.setOnClickListener(v -> {
            getActivity().getSupportFragmentManager().popBackStack();
        });
        
        // 设置长按返回按钮显示清空所有记录的选项
        btnBack.setOnLongClickListener(v -> {
            showDeleteAllDialog();
            return true;
        });
        
        // 设置删除所有按钮点击事件
        btnDeleteAll.setOnClickListener(v -> {
            showDeleteAllDialog();
        });
        
        // 初始化数据库帮助类
        dbHelper = DetectionDatabaseHelper.getInstance(getContext());
        
        // 初始化RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recordList = new ArrayList<>();
        adapter = new DetectionRecordAdapter(getContext(), recordList);
        recyclerView.setAdapter(adapter);
        
        // 设置记录项点击事件
        adapter.setOnItemClickListener((record, position) -> {
            openDetectionResult(record);
        });
        
        // 设置删除按钮点击事件
        adapter.setOnDeleteClickListener((record, position) -> {
            deleteRecord(record, position);
        });
        
        // 加载数据
        loadRecords();
        
        return view;
    }
    
    /**
     * 加载检测记录数据
     */
    private void loadRecords() {
        showLoading();
        
        // 使用新线程加载数据，避免主线程卡顿
        new Thread(() -> {
            final List<DetectionRecord> records = dbHelper.getAllDetectionRecords();
            
            // 在UI线程更新界面
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    recordList.clear();
                    recordList.addAll(records);
                    adapter.notifyDataSetChanged();
                    
                    // 显示空状态或记录列表
                    if (records.isEmpty()) {
                        showEmptyState();
                    } else {
                        showRecordList();
                    }
                });
            }
        }).start();
    }
    
    /**
     * 显示加载中状态
     */
    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyStateContainer.setVisibility(View.GONE);
    }
    
    /**
     * 显示空状态
     */
    private void showEmptyState() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyStateContainer.setVisibility(View.VISIBLE);
    }
    
    /**
     * 显示记录列表
     */
    private void showRecordList() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateContainer.setVisibility(View.GONE);
    }
    
    /**
     * 打开检测结果详情
     */
    private void openDetectionResult(DetectionRecord record) {
        try {
            // 显示检测结果详情
            OverallPredictResultFragment resultFragment = new OverallPredictResultFragment();
            Bundle args = new Bundle();
            args.putString("imageUrl", record.getImageUrl());
            
            // 创建JSON数据传递给结果Fragment
            StringBuilder jsonBuilder = new StringBuilder("{");
            jsonBuilder.append("\"acne_count\":").append(record.getAcneCount()).append(",");
            jsonBuilder.append("\"severity\":\"").append(record.getSeverity()).append("\",");
            jsonBuilder.append("\"acne_types\":").append(record.getAcneTypes());
            
            // 添加AI诊断数据（如果有）
            String recommendation = record.getRecommendation();
            if (recommendation != null && !recommendation.isEmpty()) {
                Log.d(TAG, "从历史记录中加载AI诊断数据: " + recommendation);
                // 检查是否是JSON格式（AI诊断数据）
                if (recommendation.trim().startsWith("{")) {
                    // 是JSON格式，作为ai_diagnosis添加
                    jsonBuilder.append(",\"ai_diagnosis\":").append(recommendation);
                } else {
                    // 不是JSON格式，可能是简单文本建议
                    jsonBuilder.append(",\"recommendation\":\"").append(recommendation.replace("\"", "\\\"")).append("\"");
                }
            }
            
            jsonBuilder.append("}");
            
            String jsonResult = jsonBuilder.toString();
            Log.d(TAG, "传递给结果Fragment的JSON: " + jsonResult);
            
            args.putString("predictionResult", jsonResult);
            args.putBoolean("fromHistory", true); // 标记为来自历史记录
            resultFragment.setArguments(args);
            
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, resultFragment);
            transaction.addToBackStack(null);
            transaction.commit();
            
        } catch (Exception e) {
            Log.e(TAG, "打开检测结果详情时出错: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除单条记录
     */
    private void deleteRecord(DetectionRecord record, int position) {
        try {
            // 从数据库中删除
            dbHelper.deleteDetectionRecord(record.getId());
            
            // 从列表中移除
            adapter.removeItem(position);
            
            // 如果列表为空，显示空状态
            if (adapter.getItemCount() == 0) {
                showEmptyState();
            }
            
            Toast.makeText(getContext(), "记录已删除", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "删除记录时出错: " + e.getMessage(), e);
            Toast.makeText(getContext(), "删除记录失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 显示删除所有记录的确认对话框
     */
    private void showDeleteAllDialog() {
        if (recordList == null || recordList.isEmpty()) {
            Toast.makeText(getContext(), "没有记录可删除", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(getContext())
                .setTitle("删除所有记录")
                .setMessage("确定要删除所有检测记录吗？此操作无法撤销。")
                .setPositiveButton("删除", (dialog, which) -> {
                    deleteAllRecords();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 删除所有记录
     */
    private void deleteAllRecords() {
        try {
            // 从数据库中删除所有记录
            dbHelper.deleteAllDetectionRecords();
            
            // 清空列表
            recordList.clear();
            adapter.notifyDataSetChanged();
            
            // 显示空状态
            showEmptyState();
            
            Toast.makeText(getContext(), "所有记录已删除", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "删除所有记录时出错: " + e.getMessage(), e);
            Toast.makeText(getContext(), "删除记录失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
} 