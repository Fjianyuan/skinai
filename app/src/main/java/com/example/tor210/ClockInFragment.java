package com.example.tor210;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tor210.api.UserManager;
import com.example.tor210.clock.PhotoAdapter;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClockInFragment extends Fragment {

    private static final String TAG = "ClockInFragment";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_GALLERY_IMAGE = 2;
    private static final int MAX_PHOTOS = 3; // 最多允许上传的照片数
    
    private Button clockInButton;
    private Button saveRecordButton;
    private Button btnViewHistory;
    private RadioButton statusGood, statusNormal, statusBad;
    private EditText notesEditText;
    private Uri currentPhotoUri;
    private TextView tvStreakDays;
    private LinearLayout calendarGrid;
    private RecyclerView rvPhotos;
    private Button btnAddPhoto;
    private PhotoAdapter photoAdapter;
    
    private ClockInDatabaseHelper dbHelper;
    private UserManager userManager;
    private List<Uri> photoUris = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clock_in, container, false);
        
        // 初始化数据库助手和用户管理器
        dbHelper = ClockInDatabaseHelper.getInstance(getContext());
        userManager = UserManager.getInstance(getContext());
        
        // 初始化控件
        clockInButton = view.findViewById(R.id.clock_in_button);
        saveRecordButton = view.findViewById(R.id.save_record_button);
        statusGood = view.findViewById(R.id.status_good);
        statusNormal = view.findViewById(R.id.status_normal);
        statusBad = view.findViewById(R.id.status_bad);
        notesEditText = view.findViewById(R.id.notes_edit_text);
        tvStreakDays = view.findViewById(R.id.tv_streak_days);
        calendarGrid = view.findViewById(R.id.calendar_grid);
        rvPhotos = view.findViewById(R.id.rv_photos);
        btnAddPhoto = view.findViewById(R.id.btn_add_photo);
        btnViewHistory = view.findViewById(R.id.btn_view_history);
        
        // 设置当前日期显示
        TextView tvTodayDate = view.findViewById(R.id.tv_today_date);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault());
        tvTodayDate.setText(dateFormat.format(new Date()));
        
        // 初始化照片列表
        setupPhotosRecyclerView();
        
        // 设置默认选择
        statusNormal.setChecked(true);
        
        // 检查用户是否已登录
        if (!userManager.isLoggedIn()) {
            Toast.makeText(getContext(), "请先登录再进行打卡", Toast.LENGTH_SHORT).show();
            return view;
        }
        
        // 更新连续打卡天数显示
        updateStreakDays();
        
        // 更新日历显示
        updateCalendar();
        
        // 检查今日是否已打卡
        checkTodayClockInStatus();
        
        // 设置点击事件
        btnAddPhoto.setOnClickListener(v -> {
            if (photoUris.size() >= MAX_PHOTOS) {
                Toast.makeText(getContext(), "最多只能上传" + MAX_PHOTOS + "张照片", Toast.LENGTH_SHORT).show();
                return;
            }
            showImagePickOptions();
        });
        
        clockInButton.setOnClickListener(v -> {
            performClockIn();
        });
        
        saveRecordButton.setOnClickListener(v -> {
            saveClockInRecord();
        });
        
        btnViewHistory.setOnClickListener(v -> {
            navigateToHistoryPage();
        });
        
        return view;
    }
    
    private void setupPhotosRecyclerView() {
        rvPhotos.setLayoutManager(new GridLayoutManager(getContext(), 3));
        photoAdapter = new PhotoAdapter(photoUris, position -> {
            // 删除照片
            photoUris.remove(position);
            photoAdapter.notifyItemRemoved(position);
        });
        rvPhotos.setAdapter(photoAdapter);
    }
    
    private void updateStreakDays() {
        if (userManager.isLoggedIn()) {
            int streakDays = dbHelper.getUserStreakDays(userManager.getUserId());
            if (tvStreakDays != null) {
                tvStreakDays.setText(streakDays + "天");
            }
        }
    }
    
    private void updateCalendar() {
        if (calendarGrid != null) {
            calendarGrid.removeAllViews();
            
            // 获取当前月份
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            
            // 获取当月有打卡记录的日期
            List<Date> clockInDates = dbHelper.getMonthlyClockInDates(userManager.getUserId(), year, month);
            
            // 获取当月第一天是星期几
            calendar.set(year, month, 1);
            int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 让周日为0
            
            // 获取当月总天数
            int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            
            // 添加星期标题行
            LinearLayout weekHeaderRow = new LinearLayout(getContext());
            weekHeaderRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            weekHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
            weekHeaderRow.setWeightSum(7);
            
            String[] weekDays = {"日", "一", "二", "三", "四", "五", "六"};
            for (int i = 0; i < 7; i++) {
                TextView dayHeader = new TextView(getContext());
                dayHeader.setText(weekDays[i]);
                dayHeader.setGravity(Gravity.CENTER);
                dayHeader.setTextSize(12);
                
                // 设置颜色
                if (i == 0) { // 周日
                    dayHeader.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
                } else if (i == 6) { // 周六
                    dayHeader.setTextColor(ContextCompat.getColor(getContext(), R.color.green));
                } else { // 工作日
                    dayHeader.setTextColor(ContextCompat.getColor(getContext(), R.color.gray));
                }
                
                LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                headerParams.setMargins(4, 4, 4, 8);
                dayHeader.setLayoutParams(headerParams);
                
                weekHeaderRow.addView(dayHeader);
            }
            calendarGrid.addView(weekHeaderRow);
            
            // 计算需要几行来显示所有日期
            int totalDays = firstDayOfWeek + daysInMonth;
            int totalRows = (int) Math.ceil(totalDays / 7.0);
            
            // 从第1天开始填充日历
            int dayCounter = 1;
            
            // 为每一行创建一个LinearLayout
            for (int row = 0; row < totalRows; row++) {
                LinearLayout weekRow = new LinearLayout(getContext());
                weekRow.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                weekRow.setOrientation(LinearLayout.HORIZONTAL);
                weekRow.setWeightSum(7);
                
                // 添加此行的7个单元格
                for (int col = 0; col < 7; col++) {
                    TextView dayView = new TextView(getContext());
                    
                    // 第一行需要跳过前几个位置
                    if (row == 0 && col < firstDayOfWeek) {
                        // 空白单元格
                        dayView.setText("");
                    } else if (dayCounter <= daysInMonth) {
                        // 有效日期单元格
                        dayView.setText(String.valueOf(dayCounter));
                        
                        // 检查该日期是否有打卡记录
                        boolean hasClockIn = false;
                        Calendar checkDate = Calendar.getInstance();
                        checkDate.set(year, month, dayCounter);
                        
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        String checkDateStr = sdf.format(checkDate.getTime());
                        
                        for (Date date : clockInDates) {
                            String dateStr = sdf.format(date);
                            if (dateStr.equals(checkDateStr)) {
                                hasClockIn = true;
                                break;
                            }
                        }
                        
                        // 为有打卡记录的日期设置特殊样式
                        if (hasClockIn) {
                            dayView.setBackgroundResource(R.drawable.circle_green);
                            dayView.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
                        }
                        
                        // 当天日期高亮显示
                        else if (dayCounter == calendar.get(Calendar.DAY_OF_MONTH) &&
                               month == calendar.get(Calendar.MONTH) &&
                               year == calendar.get(Calendar.YEAR)) {
                            dayView.setBackgroundResource(R.drawable.circle_today);
                        }
                        
                        dayCounter++;
                    } else {
                        // 超出当月天数的空白单元格
                        dayView.setText("");
                    }
                    
                    // 统一设置样式
                    dayView.setGravity(Gravity.CENTER);
                    dayView.setTextSize(14);
                    
                    // 创建等宽的布局参数
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                    
                    // 设置内边距和外边距
                    int padding = getResources().getDimensionPixelSize(R.dimen.calendar_day_padding);
                    dayView.setPadding(padding, padding, padding, padding);
                    params.setMargins(4, 4, 4, 4);
                    
                    dayView.setLayoutParams(params);
                    weekRow.addView(dayView);
                }
                
                // 在行之间添加一些间距
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.setMargins(0, 0, 0, 8);
                weekRow.setLayoutParams(rowParams);
                
                // 添加行到日历网格
                calendarGrid.addView(weekRow);
            }
        }
    }
    
    private void checkTodayClockInStatus() {
        if (userManager.isLoggedIn()) {
            boolean hasClockIn = dbHelper.hasUserClockedInToday(userManager.getUserId());
            if (hasClockIn) {
                clockInButton.setText("今日已打卡");
                clockInButton.setEnabled(false);
            } else {
                clockInButton.setText("今日打卡");
                clockInButton.setEnabled(true);
            }
        }
    }
    
    private void performClockIn() {
        if (userManager.isLoggedIn()) {
            // 显示加载动画
            clockInButton.setEnabled(false);
            clockInButton.setText("打卡中...");
            
            // 延迟一点时间模拟网络请求
            new Handler().postDelayed(() -> {
                boolean success = true;
                
                if (success) {
                    // 打卡成功
                    Toast.makeText(getContext(), "打卡成功！坚持每日记录更有效哦", Toast.LENGTH_SHORT).show();
                    clockInButton.setText("今日已打卡");
                    clockInButton.setEnabled(false);
                    
                    // 显示成功动画
                    showSuccessAnimation();
                    
                    // 更新连续打卡天数显示
                    updateStreakDays();
                    
                    // 更新日历显示
                    updateCalendar();
                } else {
                    Toast.makeText(getContext(), "打卡失败，请重试", Toast.LENGTH_SHORT).show();
                    clockInButton.setText("今日打卡");
                    clockInButton.setEnabled(true);
                }
            }, 800);
        } else {
            Toast.makeText(getContext(), "请先登录再进行打卡", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveClockInRecord() {
        if (userManager.isLoggedIn()) {
            if (photoUris.isEmpty()) {
                Toast.makeText(getContext(), "请至少上传一张皮肤照片", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 显示加载状态
            saveRecordButton.setEnabled(false);
            saveRecordButton.setText("保存中...");
            
            // 获取所选状态
            String skinStatus;
            if (statusGood.isChecked()) {
                skinStatus = "良好";
            } else if (statusNormal.isChecked()) {
                skinStatus = "一般";
            } else {
                skinStatus = "不佳";
            }
            
            // 获取备注信息
            String notes = notesEditText.getText().toString().trim();
            
            // 确保照片URI可以长期访问
            savePhotosToLocalStorage();
            
            // 将多张照片URI转换为JSON字符串
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Uri.class, new UriSerializer())
                    .registerTypeAdapter(Uri.class, new UriDeserializer())
                    .create();
            String photosJson = gson.toJson(photoUris);
            
            // 记录JSON数据以便调试
            Log.d(TAG, "照片JSON数据: " + photosJson);
            
            // 创建打卡记录，使用第一张照片作为主图
            Uri mainPhotoUri = photoUris.get(0);
            String mainPhotoPath = mainPhotoUri.toString();
            
            // 记录主图路径，用于调试
            Log.d(TAG, "主图URI路径: " + mainPhotoPath);
            
            ClockInRecord record = new ClockInRecord(
                    userManager.getUserId(),
                    skinStatus,
                    mainPhotoPath,
                    notes
            );
            
            // 将所有照片路径保存在备注字段中
            record.setNotes(notes + "|||" + photosJson); // 使用分隔符分开实际备注和照片JSON
            
            // 保存到数据库（在后台线程中执行）
            new Thread(() -> {
                // 保存到数据库
                long recordId = dbHelper.addClockInRecord(record);
                
                // 在UI线程更新界面
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (recordId > 0) {
                            // 保存成功
                            Toast.makeText(getContext(), "记录已保存", Toast.LENGTH_SHORT).show();
                            
                            // 重置表单
                            photoUris.clear();
                            photoAdapter.notifyDataSetChanged();
                            statusNormal.setChecked(true);
                            notesEditText.setText("");
                            
                            // 恢复按钮状态
                            saveRecordButton.setEnabled(true);
                            saveRecordButton.setText("保存记录");
                            
                            // 更新打卡状态
                            checkTodayClockInStatus();
                            
                            // 更新连续打卡天数
                            updateStreakDays();
                            
                            // 更新日历
                            updateCalendar();
                        } else {
                            Toast.makeText(getContext(), "记录保存失败", Toast.LENGTH_SHORT).show();
                            saveRecordButton.setEnabled(true);
                            saveRecordButton.setText("保存记录");
                        }
                    });
                }
            }).start();
        } else {
            Toast.makeText(getContext(), "请先登录再保存记录", Toast.LENGTH_SHORT).show();
        }
    }
    
    // 保存照片到本地存储，获取持久化的URI
    private void savePhotosToLocalStorage() {
        List<Uri> persistentUris = new ArrayList<>();
        
        for (Uri uri : photoUris) {
            try {
                // 如果是内容URI，复制到本地存储
                if ("content".equals(uri.getScheme())) {
                    Log.d(TAG, "处理内容URI: " + uri);
                    try {
                        // 创建本地文件
                        File localImageFile = createImageFile();
                        // 复制内容到本地文件
                        copyContentUriToFile(uri, localImageFile);
                        // 从本地文件创建新URI
                        Uri localUri = FileProvider.getUriForFile(
                                requireContext(),
                                "com.example.tor210.fileprovider",
                                localImageFile);
                        
                        Log.d(TAG, "内容URI已复制到本地: " + localUri);
                        persistentUris.add(localUri);
                        
                        // 获取永久读取权限
                        try {
                            int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                            requireContext().getContentResolver().takePersistableUriPermission(
                                    localUri, takeFlags);
                            Log.d(TAG, "已获取持久化URI权限");
                        } catch (SecurityException e) {
                            Log.e(TAG, "获取持久权限失败: " + e.getMessage(), e);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "复制图片时出错: " + e.getMessage(), e);
                        // 如果失败，继续使用原始URI
                        persistentUris.add(uri);
                    }
                } else {
                    // 不是内容URI的情况下，直接添加
                    persistentUris.add(uri);
                }
            } catch (Exception e) {
                Log.e(TAG, "处理URI时出错: " + e.getMessage(), e);
                persistentUris.add(uri);
            }
        }
        
        // 更新照片URI列表
        if (!persistentUris.isEmpty()) {
            photoUris.clear();
            photoUris.addAll(persistentUris);
            Log.d(TAG, "照片URI已更新为持久化存储");
        }
    }
    
    // 复制内容URI到文件
    private void copyContentUriToFile(Uri contentUri, File destFile) throws IOException {
        try (InputStream is = requireContext().getContentResolver().openInputStream(contentUri);
             OutputStream os = new FileOutputStream(destFile)) {
                
            if (is == null) {
                throw new IOException("无法打开输入流");
            }
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
        }
    }
    
    private void showImagePickOptions() {
        String[] options = {"拍照", "从相册选择"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("选择图片来源")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        dispatchTakePictureIntent();
                    } else {
                        openGallery();
                    }
                })
                .show();
    }
    
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY_IMAGE);
    }
    
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(getContext(), "创建图片文件失败", Toast.LENGTH_SHORT).show();
            }
            
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(requireContext(),
                        "com.example.tor210.fileprovider",
                        photoFile);
                currentPhotoUri = photoURI;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }
    
    private File createImageFile() throws IOException {
        // 创建图片文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* 前缀 */
                ".jpg",   /* 后缀 */
                storageDir      /* 目录 */
        );
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if (currentPhotoUri != null) {
                    addPhotoToList(currentPhotoUri);
                }
            } else if (requestCode == REQUEST_GALLERY_IMAGE && data != null) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    addPhotoToList(selectedImageUri);
                }
            }
        }
    }
    
    private void addPhotoToList(Uri photoUri) {
        if (photoUris.size() < MAX_PHOTOS) {
            photoUris.add(photoUri);
            photoAdapter.notifyItemInserted(photoUris.size() - 1);
        } else {
            Toast.makeText(getContext(), "最多只能上传" + MAX_PHOTOS + "张照片", Toast.LENGTH_SHORT).show();
        }
    }
    
    // 显示打卡成功动画
    private void showSuccessAnimation() {
        // 实际项目中可以使用Lottie动画库
        View rootView = getView();
        if (rootView != null) {
            // 简单的缩放动画
            ImageView checkIcon = new ImageView(getContext());
            checkIcon.setImageResource(android.R.drawable.ic_menu_send);
            checkIcon.setColorFilter(ContextCompat.getColor(getContext(), android.R.color.holo_green_light));
            
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.gravity = Gravity.CENTER;
            
            FrameLayout container = new FrameLayout(getContext());
            container.setBackgroundColor(Color.parseColor("#80000000"));
            container.addView(checkIcon, params);
            
            ((ViewGroup) rootView).addView(container, 
                    new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    )
            );
            
            ScaleAnimation scaleAnimation = new ScaleAnimation(
                    0.0f, 1.0f, 0.0f, 1.0f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
            );
            scaleAnimation.setDuration(500);
            
            AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
            alphaAnimation.setDuration(500);
            
            AnimationSet animationSet = new AnimationSet(true);
            animationSet.addAnimation(scaleAnimation);
            animationSet.addAnimation(alphaAnimation);
            animationSet.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}
                
                @Override
                public void onAnimationEnd(Animation animation) {
                    // 动画结束后延迟移除动画视图
                    new Handler().postDelayed(() -> {
                        if (getActivity() != null && !getActivity().isFinishing()) {
                            ((ViewGroup) rootView).removeView(container);
                        }
                    }, 500);
                }
                
                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            
            checkIcon.startAnimation(animationSet);
        }
    }
    
    /**
     * 跳转到打卡历史记录页面
     */
    private void navigateToHistoryPage() {
        // 创建历史记录Fragment
        ClockInHistoryFragment historyFragment = new ClockInHistoryFragment();
        
        // 执行Fragment跳转
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, historyFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
    
    /**
     * 加载打卡历史记录（保留空方法以避免修改其他调用代码）
     */
    private void loadClockInHistory() {
        // 历史记录已移至专门的Fragment中显示
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
}
