package com.example.tor210.predict;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.GlideException;
import com.example.tor210.R;
import com.example.tor210.utils.ImageLoadCallback;
import com.example.tor210.utils.ImageLoadHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OverallPredictResultFragment extends Fragment {

    private static final String TAG = "PredictResultFragment";
    private static final String ARG_IMAGE_URL = "imageUrl";
    private static final String ARG_PREDICTION_RESULT = "predictionResult";
    private static final String ARG_FROM_HISTORY = "fromHistory";

    private String imageUrl;
    private String predictionResultJson;
    private JSONObject predictionResult;
    
    private DetectionDatabaseHelper dbHelper;
    private boolean isSavedToDatabase = false;
    private boolean isFromHistory = false;

    private ImageView resultImageView;
    private TextView tvAcneCount;
    private TextView tvSeverity;
    private TextView tvAcneTypes;
    private TextView tvRecommendations;
    private View severityIndicator;
    private Button btnSave;
    private Button btnBack;
    private ImageButton btnClose;
    
    // AI诊断文本视图
    private TextView tvAiDiagnosis;
    private TextView tvAiSkinCareTips;
    private TextView tvAiLifestyleSuggestions;
    private TextView tvAiMedicalAdvice;
    private View cardAiDiagnosis;

    public static OverallPredictResultFragment newInstance(String imageUrl, String predictionResult) {
        return newInstance(imageUrl, predictionResult, false);
    }
    
    public static OverallPredictResultFragment newInstance(String imageUrl, String predictionResult, boolean fromHistory) {
        OverallPredictResultFragment fragment = new OverallPredictResultFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_URL, imageUrl);
        args.putString(ARG_PREDICTION_RESULT, predictionResult);
        args.putBoolean(ARG_FROM_HISTORY, fromHistory);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imageUrl = getArguments().getString(ARG_IMAGE_URL);
            predictionResultJson = getArguments().getString(ARG_PREDICTION_RESULT);
            isFromHistory = getArguments().getBoolean(ARG_FROM_HISTORY, false);
            try {
                predictionResult = new JSONObject(predictionResultJson);
            } catch (JSONException e) {
                Log.e(TAG, "解析预测结果JSON失败: " + e.getMessage(), e);
            }
        }
        
        // 初始化数据库助手
        dbHelper = DetectionDatabaseHelper.getInstance(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_overall_predict_result, container, false);

        // 初始化视图
        resultImageView = view.findViewById(R.id.result_image_view);
        tvAcneCount = view.findViewById(R.id.tv_acne_count);
        tvSeverity = view.findViewById(R.id.tv_severity);
        tvAcneTypes = view.findViewById(R.id.tv_acne_types);
        tvRecommendations = view.findViewById(R.id.tv_recommendations);
        severityIndicator = view.findViewById(R.id.severity_indicator);
        btnSave = view.findViewById(R.id.btn_save);
        btnBack = view.findViewById(R.id.btn_back);
        btnClose = view.findViewById(R.id.btn_close);
        
        // 初始化AI诊断视图
        tvAiDiagnosis = view.findViewById(R.id.tv_ai_diagnosis);
        tvAiSkinCareTips = view.findViewById(R.id.tv_ai_skin_care_tips);
        tvAiLifestyleSuggestions = view.findViewById(R.id.tv_ai_lifestyle_suggestions);
        tvAiMedicalAdvice = view.findViewById(R.id.tv_ai_medical_advice);
        cardAiDiagnosis = view.findViewById(R.id.card_ai_diagnosis);

        // 设置数据
        loadResultImage();
        displayPredictionResults();

        // 设置按钮点击事件
        btnSave.setOnClickListener(v -> saveResultToStorage());
        btnBack.setOnClickListener(v -> getActivity().getSupportFragmentManager().popBackStack());
        btnClose.setOnClickListener(v -> getActivity().getSupportFragmentManager().popBackStack());

        // 自动保存结果到数据库（只保存一次且不是从历史记录打开）
        if (!isSavedToDatabase && !isFromHistory) {
            saveResultToDatabase();
        }

        return view;
    }

    /**
     * 加载结果图片
     */
    private void loadResultImage() {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            ImageLoadHelper.loadImage(getContext(), imageUrl, resultImageView, new ImageLoadCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "结果图片加载成功");
                }

                @Override
                public void onError(GlideException e) {
                    Log.e(TAG, "结果图片加载失败: " + e.getMessage(), e);
                }
            });
        } else {
            Log.e(TAG, "图片URL为空");
        }
    }

    /**
     * 显示预测结果
     */
    private void displayPredictionResults() {
        try {
            if (predictionResult != null) {
                // 获取痘痘数量
                int acneCount = predictionResult.getInt("acne_count");
                tvAcneCount.setText("痘痘数量: " + acneCount);

                // 获取严重程度
                String severity = predictionResult.getString("severity");
                tvSeverity.setText("严重程度: " + severity);

                // 根据严重程度设置指示器颜色
                setSeverityIndicator(severity);

                // 获取痘痘类型
                JSONArray acneTypesArray = predictionResult.getJSONArray("acne_types");
                List<String> acneTypesList = new ArrayList<>();
                for (int i = 0; i < acneTypesArray.length(); i++) {
                    acneTypesList.add(acneTypesArray.getString(i));
                }
                String acneTypes = acneTypesList.isEmpty() ? "无" : String.join(", ", acneTypesList);
                tvAcneTypes.setText("痘痘类型: " + acneTypes);

                // 设置护理建议
                setRecommendationsBySeverity(severity);
                
                // 显示AI诊断报告 (如果有)
                displayAiDiagnosis();
            }
        } catch (JSONException e) {
            Log.e(TAG, "解析预测结果失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 显示AI诊断报告
     */
    private void displayAiDiagnosis() {
        try {
            Log.d(TAG, "尝试显示AI诊断报告...");
            Log.d(TAG, "预测结果JSON: " + predictionResult.toString());
            
            if (predictionResult.has("ai_diagnosis")) {
                Log.d(TAG, "找到AI诊断数据，开始处理");
                // 显示AI诊断卡片
                cardAiDiagnosis.setVisibility(View.VISIBLE);
                
                JSONObject aiDiagnosis = predictionResult.getJSONObject("ai_diagnosis");
                Log.d(TAG, "AI诊断数据: " + aiDiagnosis.toString());
                
                // 诊断分析
                if (aiDiagnosis.has("diagnosis")) {
                    String diagnosis = aiDiagnosis.getString("diagnosis");
                    Log.d(TAG, "诊断分析: " + diagnosis);
                    tvAiDiagnosis.setText(diagnosis);
                } else {
                    Log.d(TAG, "未找到诊断分析数据");
                    tvAiDiagnosis.setText("未提供诊断分析");
                }
                
                // 皮肤护理建议
                if (aiDiagnosis.has("skin_care_tips")) {
                    JSONArray tips = aiDiagnosis.getJSONArray("skin_care_tips");
                    Log.d(TAG, "皮肤护理建议: " + tips.toString());
                    StringBuilder skinCareTips = new StringBuilder();
                    for (int i = 0; i < tips.length(); i++) {
                        skinCareTips.append("• ").append(tips.getString(i)).append("\n");
                    }
                    tvAiSkinCareTips.setText(skinCareTips.toString().trim());
                } else {
                    Log.d(TAG, "未找到皮肤护理建议数据");
                    tvAiSkinCareTips.setText("未提供皮肤护理建议");
                }
                
                // 生活习惯改善建议
                if (aiDiagnosis.has("lifestyle_suggestions")) {
                    JSONArray suggestions = aiDiagnosis.getJSONArray("lifestyle_suggestions");
                    Log.d(TAG, "生活习惯改善建议: " + suggestions.toString());
                    StringBuilder lifestyleSuggestions = new StringBuilder();
                    for (int i = 0; i < suggestions.length(); i++) {
                        lifestyleSuggestions.append("• ").append(suggestions.getString(i)).append("\n");
                    }
                    tvAiLifestyleSuggestions.setText(lifestyleSuggestions.toString().trim());
                } else {
                    Log.d(TAG, "未找到生活习惯改善建议数据");
                    tvAiLifestyleSuggestions.setText("未提供生活习惯改善建议");
                }
                
                // 医疗干预建议
                if (aiDiagnosis.has("medical_advice")) {
                    JSONArray advice = aiDiagnosis.getJSONArray("medical_advice");
                    Log.d(TAG, "医疗干预建议: " + advice.toString());
                    StringBuilder medicalAdvice = new StringBuilder();
                    for (int i = 0; i < advice.length(); i++) {
                        medicalAdvice.append("• ").append(advice.getString(i)).append("\n");
                    }
                    tvAiMedicalAdvice.setText(medicalAdvice.toString().trim());
                } else {
                    Log.d(TAG, "未找到医疗干预建议数据");
                    tvAiMedicalAdvice.setText("未提供医疗干预建议");
                }
            } else {
                // 没有AI诊断数据，隐藏卡片
                Log.d(TAG, "未找到AI诊断数据，隐藏卡片");
                cardAiDiagnosis.setVisibility(View.GONE);
            }
        } catch (JSONException e) {
            Log.e(TAG, "解析AI诊断报告失败: " + e.getMessage(), e);
            cardAiDiagnosis.setVisibility(View.GONE);
        }
    }

    /**
     * 根据严重程度设置指示器颜色
     */
    private void setSeverityIndicator(String severity) {
        switch (severity.toLowerCase()) {
            case "轻度":
                severityIndicator.setBackgroundResource(R.drawable.severity_indicator_green);
                break;
            case "中度":
                severityIndicator.setBackgroundResource(R.drawable.severity_indicator_yellow);
                break;
            case "重度":
                severityIndicator.setBackgroundResource(R.drawable.severity_indicator_orange);
                break;
            case "严重":
                severityIndicator.setBackgroundResource(R.drawable.severity_indicator_red);
                break;
            default:
                severityIndicator.setBackgroundResource(R.drawable.severity_indicator_green);
                break;
        }
    }

    /**
     * 根据严重程度设置护理建议
     */
    private void setRecommendationsBySeverity(String severity) {
        String recommendations;
        switch (severity.toLowerCase()) {
            case "轻度":
                recommendations = "• 温和清洁面部，避免过度清洁\n• 使用含水杨酸的护肤品\n• 保持充足睡眠和均衡饮食\n• 定期补水保湿";
                break;
            case "中度":
                recommendations = "• 使用专业抗痘洁面产品\n• 添加含视黄醇的护肤品\n• 避免使用油性化妆品\n• 每周使用温和去角质产品\n• 保持健康生活习惯";
                break;
            case "重度":
                recommendations = "• 咨询皮肤科医生专业建议\n• 避免挤压或触碰痘痘\n• 使用医用级护肤品\n• 注意饮食控制，减少高糖食物\n• 保持面部清洁，避免过度清洁";
                break;
            case "严重":
                recommendations = "• 建议尽快就医寻求专业治疗\n• 可能需要口服药物治疗\n• 使用医生推荐的专业护肤品\n• 避免使用刺激性产品\n• 保持良好作息和饮食习惯";
                break;
            default:
                recommendations = "• 温和清洁面部，避免过度清洁\n• 选择不含油脂的护肤品\n• 不要用手挤压痘痘\n• 保持健康饮食和充足睡眠";
                break;
        }
        tvRecommendations.setText(recommendations);
    }

    /**
     * 保存结果到数据库
     */
    private void saveResultToDatabase() {
        try {
            if (predictionResult != null && imageUrl != null) {
                // 创建检测记录对象
                DetectionRecord record = new DetectionRecord();
                record.setImageUrl(imageUrl);
                record.setRecordTime(new Date());
                record.setAcneCount(predictionResult.getInt("acne_count"));
                record.setSeverity(predictionResult.getString("severity"));
                record.setAcneTypes(predictionResult.getJSONArray("acne_types").toString());
                
                // 如果有AI诊断，将完整的AI诊断对象保存为JSON字符串
                if (predictionResult.has("ai_diagnosis")) {
                    // 将完整的AI诊断对象转为字符串保存
                    JSONObject aiDiagnosis = predictionResult.getJSONObject("ai_diagnosis");
                    Log.d(TAG, "保存AI诊断数据到数据库: " + aiDiagnosis.toString());
                    record.setRecommendation(aiDiagnosis.toString());
                } else {
                    // 没有AI诊断，保存基本建议
                    record.setRecommendation(tvRecommendations.getText().toString());
                }
                
                // 保存到数据库
                long recordId = dbHelper.addDetectionRecord(record);
                if (recordId > 0) {
                    isSavedToDatabase = true;
                    Log.d(TAG, "检测结果已保存到数据库，ID: " + recordId);
                } else {
                    Log.e(TAG, "保存检测结果到数据库失败");
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "保存到数据库时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 保存结果到设备存储
     */
    private void saveResultToStorage() {
        if (resultImageView.getDrawable() == null) {
            Toast.makeText(getContext(), "图片尚未加载完成，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Bitmap bitmap = ((BitmapDrawable) resultImageView.getDrawable()).getBitmap();
            String fileName = "ACNE_DETECT_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg";

            boolean saved = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10及以上使用MediaStore API
                ContentResolver resolver = getContext().getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AcneDetection");
                
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri != null) {
                    OutputStream outputStream = resolver.openOutputStream(imageUri);
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                        outputStream.close();
                        saved = true;
                    }
                }
            } else {
                // Android 9及以下使用传统File API
                File imagesDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AcneDetection");
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs();
                }
                
                File imageFile = new File(imagesDir, fileName);
                FileOutputStream outputStream = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                outputStream.flush();
                outputStream.close();
                
                // 通知图库更新
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(Uri.fromFile(imageFile));
                getContext().sendBroadcast(mediaScanIntent);
                saved = true;
            }

            if (saved) {
                Toast.makeText(getContext(), "图片已保存到相册", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "图片保存失败", Toast.LENGTH_SHORT).show();
            }
            
        } catch (IOException e) {
            Log.e(TAG, "保存图片时出错: " + e.getMessage(), e);
            Toast.makeText(getContext(), "图片保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "保存图片时发生未知错误: " + e.getMessage(), e);
            Toast.makeText(getContext(), "图片保存失败", Toast.LENGTH_SHORT).show();
        }
    }
}
