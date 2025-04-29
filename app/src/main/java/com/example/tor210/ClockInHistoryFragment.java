package com.example.tor210;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.tor210.api.UserManager;
import com.example.tor210.database.ClockInDatabaseHelper;
import com.example.tor210.database.models.ClockInRecord;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 打卡历史记录Fragment
 */
public class ClockInHistoryFragment extends Fragment {
    private static final String TAG = "ClockInHistoryFragment";
    
    private RecyclerView rvHistory;
    private TextView tvNoData;
    private ClockInHistoryAdapter historyAdapter;
    
    private ClockInDatabaseHelper dbHelper;
    private UserManager userManager;
    private List<ClockInRecord> historyList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clock_in_history, container, false);
        
        // 初始化数据库助手和用户管理器
        dbHelper = ClockInDatabaseHelper.getInstance(getContext());
        userManager = UserManager.getInstance(getContext());
        
        // 初始化控件
        rvHistory = view.findViewById(R.id.rv_history);
        tvNoData = view.findViewById(R.id.tv_no_data);
        
        // 返回按钮
        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
        
        // 初始化历史记录列表
        setupHistoryRecyclerView();
        
        // 加载历史记录
        loadClockInHistory();
        
        return view;
    }
    
    private void setupHistoryRecyclerView() {
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        historyAdapter = new ClockInHistoryAdapter(getContext(), historyList);
        rvHistory.setAdapter(historyAdapter);
    }
    
    private void loadClockInHistory() {
        if (userManager.isLoggedIn()) {
            try {
                // 在UI线程直接加载数据，避免线程安全问题
                List<ClockInRecord> records = dbHelper.getAllClockInRecords(userManager.getUserId());
                historyList.clear();
                historyList.addAll(records);
                
                // 检查Fragment是否仍然附加到Activity
                if (getActivity() == null || !isAdded()) {
                    Log.d(TAG, "Fragment已分离，跳过UI更新");
                    return;
                }
                
                // 更新适配器
                if (historyAdapter != null) {
                    historyAdapter.notifyDataSetChanged();
                }
                
                // 显示暂无数据提示或隐藏
                if (historyList.isEmpty()) {
                    tvNoData.setVisibility(View.VISIBLE);
                } else {
                    tvNoData.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Log.e(TAG, "加载打卡历史记录失败: " + e.getMessage(), e);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "加载历史记录失败", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(getContext(), "请先登录再查看历史记录", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 打卡历史记录适配器
     */
    private class ClockInHistoryAdapter extends RecyclerView.Adapter<ClockInHistoryAdapter.ViewHolder> {
        
        private Context context;
        private List<ClockInRecord> recordList;
        private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        public ClockInHistoryAdapter(Context context, List<ClockInRecord> recordList) {
            this.context = context;
            this.recordList = recordList;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_clock_in_history, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ClockInRecord record = recordList.get(position);
            
            // 设置日期
            holder.tvDate.setText(dateFormat.format(record.getClockInDate()));
            
            // 设置状态
            holder.tvStatus.setText("皮肤状态: " + record.getSkinStatus());
            
            // 尝试从备注中解析照片JSON
            String notes = record.getNotes();
            String actualNotes = notes;
            List<Uri> recordPhotos = new ArrayList<>();
            
            if (notes != null && notes.contains("|||")) {
                String[] parts = notes.split("\\|\\|\\|", 2);
                actualNotes = parts[0];
                
                if (parts.length > 1) {
                    Log.d(TAG, "原始照片JSON字符串: " + parts[1]);
                    try {
                        Gson gson = new GsonBuilder()
                                .registerTypeAdapter(Uri.class, new UriSerializer())
                                .registerTypeAdapter(Uri.class, new UriDeserializer())
                                .create();
                        Type type = new TypeToken<List<Uri>>(){}.getType();
                        recordPhotos = gson.fromJson(parts[1], type);
                        Log.d(TAG, "成功解析照片JSON，数量: " + recordPhotos.size());
                    } catch (Exception e) {
                        Log.e(TAG, "解析照片JSON失败: " + e.getMessage(), e);
                        // 尝试解析为字符串列表，然后转换为Uri列表
                        try {
                            Gson simpleGson = new Gson();
                            Type stringListType = new TypeToken<List<String>>(){}.getType();
                            List<String> stringList = simpleGson.fromJson(parts[1], stringListType);
                            if (stringList != null) {
                                for (String uriString : stringList) {
                                    Uri uri = safeParseUri(uriString);
                                    if (uri != null) {
                                        recordPhotos.add(uri);
                                    }
                                }
                                Log.d(TAG, "使用备用方法成功解析照片JSON，数量: " + recordPhotos.size());
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "尝试第二种方法解析照片JSON失败: " + ex.getMessage(), ex);
                        }
                    }
                }
            } else {
                Log.d(TAG, "没有找到照片分隔符，notes: " + (notes != null ? notes : "null"));
            }
            
            // 设置图片（使用第一张照片作为主图）
            if (!recordPhotos.isEmpty()) {
                Uri imageUri = recordPhotos.get(0);
                Log.d(TAG, "使用recordPhotos中的第一张照片: " + imageUri);
                
                // 使用Glide加载图片并添加错误处理
                Glide.with(context)
                    .load(imageUri)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.ivPhoto);
            } else {
                // 使用原始图片路径
                String imagePath = record.getImagePath();
                Log.d(TAG, "使用原始图片路径: " + imagePath);
                
                // 使用辅助方法加载图片
                loadImageWithFallback(imagePath, holder.ivPhoto);
            }
            
            // 设置备注
            if (actualNotes != null && !actualNotes.isEmpty()) {
                holder.tvNotes.setVisibility(View.VISIBLE);
                holder.tvNotes.setText(actualNotes);
            } else {
                holder.tvNotes.setVisibility(View.GONE);
            }
            
            // 设置照片数量提示
            if (recordPhotos.size() > 1) {
                holder.tvPhotoCount.setVisibility(View.VISIBLE);
                holder.tvPhotoCount.setText("+" + (recordPhotos.size() - 1) + "张");
            } else {
                holder.tvPhotoCount.setVisibility(View.GONE);
            }
            
            // 设置删除按钮点击事件
            holder.btnDelete.setOnClickListener(v -> {
                showDeleteConfirmDialog(record.getId(), position);
            });
        }
        
        /**
         * 显示删除确认对话框
         */
        private void showDeleteConfirmDialog(final long recordId, final int position) {
            new AlertDialog.Builder(context)
                    .setTitle("删除记录")
                    .setMessage("确定要删除这条打卡记录吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        deleteRecord(recordId, position);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
        
        /**
         * 删除打卡记录
         */
        private void deleteRecord(long recordId, int position) {
            boolean success = dbHelper.deleteClockInRecord(recordId);
            if (success) {
                // 从列表中移除并通知适配器
                recordList.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, recordList.size() - position);
                
                // 如果列表为空，显示暂无数据提示
                if (recordList.isEmpty()) {
                    tvNoData.setVisibility(View.VISIBLE);
                }
                
                Toast.makeText(context, "记录已删除", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "删除记录失败", Toast.LENGTH_SHORT).show();
            }
        }
        
        @Override
        public int getItemCount() {
            return recordList.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivPhoto;
            TextView tvDate, tvStatus, tvNotes, tvPhotoCount;
            CardView cardView;
            ImageButton btnDelete;
            
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivPhoto = itemView.findViewById(R.id.iv_photo);
                tvDate = itemView.findViewById(R.id.tv_date);
                tvStatus = itemView.findViewById(R.id.tv_status);
                tvNotes = itemView.findViewById(R.id.tv_notes);
                tvPhotoCount = itemView.findViewById(R.id.tv_photo_count);
                cardView = itemView.findViewById(R.id.card_view);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
        }
    }
    
    private static class UriSerializer implements JsonSerializer<Uri> {
        @Override
        public JsonElement serialize(Uri src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return null;
            }
            String uriString = src.toString();
            Log.d("UriSerializer", "序列化URI: " + uriString);
            return new JsonPrimitive(uriString);
        }
    }
    
    private static class UriDeserializer implements JsonDeserializer<Uri> {
        @Override
        public Uri deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return null;
            }
            
            try {
                String uriString = json.getAsString();
                if (uriString == null || uriString.isEmpty()) {
                    Log.e("UriDeserializer", "URI字符串为空");
                    return null;
                }
                
                Uri uri = Uri.parse(uriString);
                Log.d("UriDeserializer", "解析URI成功: " + uri);
                return uri;
            } catch (Exception e) {
                Log.e("UriDeserializer", "解析URI失败: " + e.getMessage(), e);
                throw new JsonParseException("无法解析URI: " + e.getMessage());
            }
        }
    }
    
    // 添加辅助方法，尝试从多种途径加载图片
    private void loadImageWithFallback(String imagePath, ImageView imageView) {
        Log.d(TAG, "尝试从多种途径加载图片: " + imagePath);
        
        try {
            // 尝试方法1：直接使用URI
            Uri uri = Uri.parse(imagePath);
            Glide.with(getContext())
                .load(uri)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(imageView);
            Log.d(TAG, "使用URI方式加载图片: " + uri);
            
            // 尝试方法2：检查是否为file://开头的路径
            if (imagePath.startsWith("file://")) {
                String filePath = imagePath.substring(7); // 去掉file://前缀
                File imageFile = new File(filePath);
                if (imageFile.exists()) {
                    Log.d(TAG, "找到物理文件: " + filePath);
                }
            }
            
            // 尝试方法3：检查是否为content://开头的路径
            if (imagePath.startsWith("content://")) {
                Log.d(TAG, "检测到content scheme URI");
                try {
                    getContext().getContentResolver().openInputStream(uri).close();
                    Log.d(TAG, "Content URI有效，可以打开流");
                } catch (Exception e) {
                    Log.e(TAG, "Content URI无效或无法访问: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "所有图片加载方法均失败: " + e.getMessage(), e);
            imageView.setImageResource(R.drawable.error_image);
        }
    }
    
    // 添加一个方法来处理URI解析失败的情况
    private Uri safeParseUri(String uriString) {
        try {
            if (uriString == null || uriString.isEmpty()) {
                Log.e(TAG, "URI字符串为空");
                return null;
            }
            
            Uri uri = Uri.parse(uriString);
            return uri;
        } catch (Exception e) {
            Log.e(TAG, "解析URI失败: " + uriString, e);
            return null;
        }
    }
} 