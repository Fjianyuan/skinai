package com.example.tor210.clock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tor210.R;
import com.example.tor210.database.ClockInDatabaseHelper;
import com.example.tor210.database.models.ClockInRecord;
import com.example.tor210.utils.ImageLoadHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClockInFragment extends Fragment {

    private static final String TAG = "ClockInFragment";
    private static final int MAX_PHOTOS = 3;

    private TextView tvStreakDays;
    private GridLayout calendarGrid;
    private Button btnClockIn;
    private RadioGroup rgSkinCondition;
    private EditText etNotes;
    private RecyclerView rvPhotos;
    private Button btnAddPhoto;
    private Button btnSaveRecord;

    private List<Uri> photoUris = new ArrayList<>();
    private PhotoAdapter photoAdapter;
    private Uri currentPhotoUri;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 初始化Activity Result Launchers
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            addPhotoToList(selectedImageUri);
                        }
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && currentPhotoUri != null) {
                        addPhotoToList(currentPhotoUri);
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clock_in, container, false);

        // 初始化视图
        tvStreakDays = view.findViewById(R.id.tv_streak_days);
        calendarGrid = view.findViewById(R.id.calendar_grid);
        btnClockIn = view.findViewById(R.id.clock_in_button);
        rgSkinCondition = view.findViewById(R.id.status_good).getParent() instanceof RadioGroup ? 
                         (RadioGroup) view.findViewById(R.id.status_good).getParent() : null;
        etNotes = view.findViewById(R.id.notes_edit_text);
        rvPhotos = view.findViewById(R.id.rv_photos);
        btnAddPhoto = view.findViewById(R.id.btn_add_photo);
        btnSaveRecord = view.findViewById(R.id.save_record_button);

        // 设置照片适配器
        photoAdapter = new PhotoAdapter(photoUris, position -> {
            // 删除照片
            photoUris.remove(position);
            photoAdapter.notifyItemRemoved(position);
        });
        
        // 设置RecyclerView
        rvPhotos.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        rvPhotos.setAdapter(photoAdapter);
        
        // 设置添加照片按钮点击事件
        btnAddPhoto.setOnClickListener(v -> {
            if (photoUris.size() >= MAX_PHOTOS) {
                Toast.makeText(requireContext(), "最多只能上传" + MAX_PHOTOS + "张照片", Toast.LENGTH_SHORT).show();
                return;
            }
            showImageSourceDialog();
        });
        
        updateStreakDaysDisplay();
        loadMonthlyClockInDates();

        return view;
    }

    // 获取当前用户ID
    private int getCurrentUserId() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(
                "user_preferences", Context.MODE_PRIVATE);
        return prefs.getInt("user_id", -1);
    }

    // 更新用户连续打卡天数显示
    private void updateStreakDaysDisplay() {
        int userIdInt = getCurrentUserId();
        if (userIdInt == -1) {
            Log.e(TAG, "无法获取用户ID");
            tvStreakDays.setText("连续打卡：0天");
            return;
        }

        // 将int类型的userId转换为String类型
        String userId = String.valueOf(userIdInt);
        
        ClockInDatabaseHelper dbHelper = new ClockInDatabaseHelper(requireContext());
        int streakDays = dbHelper.getUserStreakDays(userId);
        tvStreakDays.setText("连续打卡：" + streakDays + "天");
        Log.d(TAG, "用户" + userId + "的连续打卡天数：" + streakDays);
    }

    // 检查用户今天是否已经打卡
    private boolean hasUserClockedInToday() {
        int userIdInt = getCurrentUserId();
        if (userIdInt == -1) {
            return false;
        }

        // 将int类型的userId转换为String类型
        String userId = String.valueOf(userIdInt);
        
        ClockInDatabaseHelper dbHelper = new ClockInDatabaseHelper(requireContext());
        return dbHelper.hasUserClockedInToday(userId);
    }

    // 加载当前月份的打卡记录
    private void loadMonthlyClockInDates() {
        int userIdInt = getCurrentUserId();
        if (userIdInt == -1) {
            return;
        }

        // 将int类型的userId转换为String类型
        String userId = String.valueOf(userIdInt);
        
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);

        ClockInDatabaseHelper dbHelper = new ClockInDatabaseHelper(requireContext());
        List<Date> clockInDates = dbHelper.getMonthlyClockInDates(userId, year, month);
        
        // 更新日历视图显示打卡日期
        updateCalendarDisplay(clockInDates);
        
        Log.d(TAG, "加载用户" + userId + "的" + (month + 1) + "月打卡记录，共" + clockInDates.size() + "条");
    }
    
    // 更新日历显示，标记已打卡的日期
    private void updateCalendarDisplay(List<Date> clockInDates) {
        if (calendarGrid == null || clockInDates == null) {
            return;
        }
        
        // 删除之前的所有行，但保留星期标题行
        if (calendarGrid.getChildCount() > 1) {
            calendarGrid.removeViews(1, calendarGrid.getChildCount() - 1);
        }
        
        // 获取当前月份
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        
        // 获取当月第一天是星期几
        calendar.set(year, month, 1);
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 让周日为0
        
        // 获取当月总天数
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        // 获取今天日期
        Calendar today = Calendar.getInstance();
        int currentDay = today.get(Calendar.DAY_OF_MONTH);
        
        // 在日历网格中添加日期
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        // 计算需要几行来显示所有日期
        int totalDays = firstDayOfWeek + daysInMonth;
        int totalRows = (int) Math.ceil(totalDays / 7.0);
        
        // 准备日期
        int dayCounter = 1;
        
        // 为每一行创建一个LinearLayout
        for (int row = 0; row < totalRows; row++) {
            LinearLayout weekRow = new LinearLayout(requireContext());
            weekRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            weekRow.setOrientation(LinearLayout.HORIZONTAL);
            weekRow.setWeightSum(7); // 设置7个等宽列
            
            // 添加此行的7个单元格
            for (int col = 0; col < 7; col++) {
                TextView dayView = new TextView(requireContext());
                
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
                    String checkDateStr = sdf.format(checkDate.getTime());
                    
                    for (Date date : clockInDates) {
                        String dateStr = sdf.format(date);
                        if (dateStr.equals(checkDateStr)) {
                            hasClockIn = true;
                            break;
                        }
                    }
                    
                    // 设置样式
                    if (hasClockIn) {
                        dayView.setBackgroundResource(R.drawable.circle_green);
                        dayView.setTextColor(getResources().getColor(android.R.color.white));
                    } 
                    // 当天日期高亮显示
                    else if (dayCounter == currentDay && month == today.get(Calendar.MONTH) && year == today.get(Calendar.YEAR)) {
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
                        0, // 宽度为0，使用权重
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.0f); // 权重为1，确保等宽
                
                // 设置内边距和外边距
                int padding = getResources().getDimensionPixelSize(R.dimen.calendar_day_padding);
                dayView.setPadding(padding, padding, padding, padding);
                
                params.setMargins(4, 4, 4, 4);
                dayView.setLayoutParams(params);
                
                // 添加到行
                weekRow.addView(dayView);
            }
            
            // 在行之间添加一些间距
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, 8); // 底部间距8dp
            weekRow.setLayoutParams(rowParams);
            
            // 添加行到日历网格
            calendarGrid.addView(weekRow);
        }
    }
    
    // 显示图片来源选择对话框
    private void showImageSourceDialog() {
        String[] options = {"拍照", "从相册选择"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("选择图片来源")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // 选择拍照
                        takePhotoFromCamera();
                    } else {
                        // 选择从相册
                        pickPhotoFromGallery();
                    }
                })
                .show();
    }
    
    // 从相机拍摄照片
    private void takePhotoFromCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            // 创建保存照片的文件
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "创建图片文件失败", ex);
                Toast.makeText(requireContext(), "无法创建图片文件", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 如果文件创建成功，继续拍照
            if (photoFile != null) {
                currentPhotoUri = FileProvider.getUriForFile(requireContext(),
                        "com.example.tor210.fileprovider",
                        photoFile);
                
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                cameraLauncher.launch(takePictureIntent);
            }
        } else {
            Toast.makeText(requireContext(), "无法启动相机", Toast.LENGTH_SHORT).show();
        }
    }
    
    // 从相册选择照片
    private void pickPhotoFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }
    
    // 创建保存图片的文件
    private File createImageFile() throws IOException {
        // 创建图片文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        
        // 创建临时文件
        return File.createTempFile(
                imageFileName,  // 前缀
                ".jpg",         // 后缀
                storageDir      // 目录
        );
    }
    
    // 添加照片到列表
    private void addPhotoToList(Uri photoUri) {
        if (photoUris.size() >= MAX_PHOTOS) {
            Toast.makeText(requireContext(), "最多只能上传" + MAX_PHOTOS + "张照片", Toast.LENGTH_SHORT).show();
            return;
        }
        
        photoUris.add(photoUri);
        photoAdapter.notifyItemInserted(photoUris.size() - 1);
        Log.d(TAG, "添加照片：" + photoUri.toString());
    }
} 