package com.example.tor210.predict;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.tor210.R;
import com.example.tor210.api.ApiConfig;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OverallPredictFragment extends Fragment {

    private static final int PICK_IMAGE = 1;
    private static final int CAPTURE_IMAGE = 2;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final String TAG = "OverallPredictFragment";
    private Uri photoURI;
    private String currentPhotoPath;

    private ImageView selectedImageView;
    private MaterialButton buttonStartDetection;
    private MaterialButton buttonHistory;

    private View scanOverlay;
    private View scanLine;

    private Handler handler = new Handler();  // 用于延迟处理
    private String resultImageUrl;
    private JSONObject predictionResult;  // 存储预测结果

    private EditText etSkinDescription;
    private TextView tvDescriptionLabel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_predict, container, false);

        // 初始化控件
        buttonStartDetection = view.findViewById(R.id.button_start_detection);
        buttonHistory = view.findViewById(R.id.button_history);

        // 创建扫描界面
        scanOverlay = inflater.inflate(R.layout.fragment_scan_overlay, null);
        selectedImageView = scanOverlay.findViewById(R.id.selected_image_view);
        scanLine = scanOverlay.findViewById(R.id.scan_line);

        // 初始化描述输入框
        etSkinDescription = view.findViewById(R.id.et_skin_description);
        tvDescriptionLabel = view.findViewById(R.id.tv_description_label);

        // 设置点击事件
        buttonStartDetection.setOnClickListener(v -> showDetectionOptions());
        
        // 设置检测记录按钮点击事件
        buttonHistory.setOnClickListener(v -> openDetectionHistory());

        return view;
    }

    private void openDetectionHistory() {
        // 打开检测记录界面
        DetectionHistoryFragment historyFragment = new DetectionHistoryFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, historyFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void showDetectionOptions() {
        DetectionOptionsDialogFragment dialogFragment = new DetectionOptionsDialogFragment();
        dialogFragment.setTargetFragment(OverallPredictFragment.this, 0);
        dialogFragment.show(getFragmentManager(), "DetectionOptionsDialog");
    }

    public void onOptionSelected(int option) {
        if (option == DetectionOptionsDialogFragment.OPTION_CAMERA) {
            openCamera();
        } else if (option == DetectionOptionsDialogFragment.OPTION_GALLERY) {
            openGallery();
        }
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
        }
    }

    private void startCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
                Log.d(TAG, "Image file created at: " + photoFile.getAbsolutePath());
            } catch (IOException ex) {
                Toast.makeText(getActivity(), "创建图片文件失败", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error creating image file: ", ex);
                return;
            }

            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(getActivity(),
                        "com.example.tor210.fileprovider", photoFile);  // 确保与AndroidManifest.xml中的authorities一致
                getActivity().grantUriPermission(getActivity().getPackageName(), photoURI, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(intent, CAPTURE_IMAGE);
            } else {
                Log.e(TAG, "Photo file is null");
            }
        } else {
            Toast.makeText(getActivity(), "无法启动相机", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Camera activity not resolved");
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getActivity().getExternalFilesDir(null);
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Log.e(TAG, "Failed to create directory for storing images");
            return null;
        }
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == getActivity().RESULT_OK) {
            try {
                if (requestCode == PICK_IMAGE && data != null && data.getData() != null) {
                    photoURI = data.getData();
                    handleImageResult(photoURI);
                } else if (requestCode == CAPTURE_IMAGE && photoURI != null) {
                    // 确保photoURI不为空
                    handleImageResult(photoURI);
                } else {
                    Log.w(TAG, "无法处理活动结果：photoURI为空或未知请求代码");
                }
            } catch (Exception e) {
                Log.e(TAG, "处理活动结果时出错: " + e.getMessage(), e);
                Toast.makeText(getActivity(), "处理图片失败，请重试", Toast.LENGTH_SHORT).show();
                stopScanAnimation();
            }
        } else {
            Log.w(TAG, "操作取消或失败，requestCode: " + requestCode + ", resultCode: " + resultCode);
            stopScanAnimation();
        }
    }

    private void handleImageResult(Uri imageUri) {
        if (imageUri != null) {
            photoURI = imageUri;
            displaySelectedImage(imageUri);
            
            // 显示描述输入框
            if (tvDescriptionLabel != null) tvDescriptionLabel.setVisibility(View.VISIBLE);
            if (etSkinDescription != null) {
                View tilSkinDescription = (View) etSkinDescription.getParent().getParent();
                if (tilSkinDescription != null) {
                    tilSkinDescription.setVisibility(View.VISIBLE);
                }
                etSkinDescription.setVisibility(View.VISIBLE);
            }
            
            buttonStartDetection.setEnabled(true);
            buttonStartDetection.setOnClickListener(v -> {
                startScanAnimation();
                uploadImageToServer(photoURI);
            });
        }
    }

    private void displaySelectedImage(Uri imageUri) {
        selectedImageView.setImageURI(imageUri);
        selectedImageView.setVisibility(View.VISIBLE);
    }

    private void startScanAnimation() {
        if (getActivity() == null) return;
        
        Log.d(TAG, "开始扫描动画");
        buttonStartDetection.setVisibility(View.INVISIBLE);  // 隐藏"开始检测"按钮
        buttonHistory.setVisibility(View.INVISIBLE);  // 隐藏"检测记录"按钮
        
        // 将扫描界面添加到当前Activity的内容视图
        ViewGroup rootView = (ViewGroup) getActivity().findViewById(android.R.id.content);
        
        // 先移除之前可能存在的scanOverlay
        if (scanOverlay.getParent() != null) {
            ((ViewGroup) scanOverlay.getParent()).removeView(scanOverlay);
        }
        
        // 添加扫描界面到根视图
        rootView.addView(scanOverlay);
        scanOverlay.setVisibility(View.VISIBLE);
        selectedImageView.setVisibility(View.VISIBLE);  // 显示选择的图片

        // 设置扫描线动画
        Animation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0f, Animation.RELATIVE_TO_PARENT, 0f,
                Animation.RELATIVE_TO_PARENT, 0f, Animation.RELATIVE_TO_PARENT, 1f);
        animation.setDuration(2000);
        animation.setRepeatCount(Animation.INFINITE);
        scanLine.startAnimation(animation);
        
        // 设置分析进度条动画
        ProgressBar progressAnalysis = scanOverlay.findViewById(R.id.progress_analysis);
        if (progressAnalysis != null) {
            progressAnalysis.setVisibility(View.VISIBLE);
        }
    }

    private void stopScanAnimation() {
        if (scanLine != null) {
            scanLine.clearAnimation();
        }
        
        // 从父视图中移除扫描界面
        if (scanOverlay != null) {
            scanOverlay.setVisibility(View.GONE);
            if (scanOverlay.getParent() != null) {
                ((ViewGroup) scanOverlay.getParent()).removeView(scanOverlay);
            }
        }
        
        if (buttonStartDetection != null) {
            buttonStartDetection.setVisibility(View.VISIBLE);
        }
        if (buttonHistory != null) {
            buttonHistory.setVisibility(View.VISIBLE);
        }
    }

    private void uploadImageToServer(Uri imageUri) {
        if (imageUri == null) {
            Log.e(TAG, "上传失败：imageUri为空");
            Toast.makeText(getActivity(), "无法上传空图像", Toast.LENGTH_SHORT).show();
            stopScanAnimation();
            return;
        }

        Log.d(TAG, "开始上传图片至服务器: " + ApiConfig.PREDICT_URL);
        
        File imageFile = createFileFromUri(imageUri);
        if (imageFile == null) {
            Toast.makeText(getActivity(), "无法读取图片文件", Toast.LENGTH_SHORT).show();
            stopScanAnimation();
            return;
        }

        Log.d(TAG, "已创建临时文件: " + imageFile.getAbsolutePath() + " 大小: " + imageFile.length() + " 字节");

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)  // 增加连接超时时间
                .writeTimeout(60, TimeUnit.SECONDS)    // 增加写超时时间
                .readTimeout(60, TimeUnit.SECONDS)     // 增加读超时时间
                .build();

        try {
            // 获取用户描述文本
            String skinDescription = "";
            if (etSkinDescription != null) {
                skinDescription = etSkinDescription.getText().toString().trim();
            }
            
            // 创建多部分请求体，包含图片和描述
            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "image.jpg",
                            RequestBody.create(MediaType.parse("image/jpeg"), imageFile))
                    .addFormDataPart("description", skinDescription) // 添加用户描述
                    .build();


            Request request = new Request.Builder()
                    .url(ApiConfig.PREDICT_URL)
                    .post(requestBody)
                    .build();

            Log.d(TAG, "正在发送HTTP请求...");
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "图片上传失败: " + e.getMessage(), e);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            stopScanAnimation();
                            String errorMsg = e.getMessage();
                            if (errorMsg != null && errorMsg.contains("CLEARTEXT communication")) {
                                Toast.makeText(getActivity(), "网络安全策略错误: 请检查AndroidManifest.xml中的网络安全配置", Toast.LENGTH_LONG).show();
                            } else if (errorMsg != null && (errorMsg.contains("failed to connect") || 
                                    errorMsg.contains("timeout"))) {
                                // 创建一个连接错误对话框
                                new AlertDialog.Builder(getActivity())
                                    .setTitle("连接失败")
                                    .setMessage("无法连接到服务器(" + ApiConfig.BASE_URL + ")。\n\n" +
                                            "可能原因:\n" +
                                            "1. 服务器未启动\n" +
                                            "2. 手机与服务器不在同一网络\n" +
                                            "3. IP地址或端口配置错误\n\n" +
                                            "您可以修改ApiConfig.java中的IP地址配置。")
                                    .setPositiveButton("重试", (dialog, which) -> {
                                        // 重试上传
                                        if (photoURI != null) {
                                            handleImageResult(photoURI);
                                        }
                                    })
                                    .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                                    .setCancelable(false)
                                    .show();
                            } else {
                                Toast.makeText(getActivity(), "上传失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful()) {
                            String responseBody = response.body().string();
                            Log.d(TAG, "API响应: " + responseBody);
                            
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            
                            // 检查API响应状态
                            if (jsonResponse.getString("status").equals("success")) {
                                // 获取data对象
                                JSONObject data = jsonResponse.getJSONObject("data");
                                
                                // 获取图片URL
                                String resultImageUrl = data.getString("result_image_url");
                                Log.d(TAG, "获取到图片URL: " + resultImageUrl);
                                
                                // 获取痘痘数量、严重程度和类型
                                int acneCount = data.getInt("acne_count");
                                String severity = data.getString("severity");
                                JSONArray acneTypes = data.getJSONArray("acne_types");
                                
                                // 检查是否包含AI诊断数据
                                boolean hasAiDiagnosis = data.has("ai_diagnosis");
                                Log.d(TAG, "是否包含AI诊断数据: " + hasAiDiagnosis);
                                if (hasAiDiagnosis) {
                                    JSONObject aiDiagnosis = data.getJSONObject("ai_diagnosis");
                                    Log.d(TAG, "AI诊断数据: " + aiDiagnosis.toString());
                                }
                                
                                // 构建预测结果对象
                                JSONObject predictionResult = new JSONObject();
                                predictionResult.put("acne_count", acneCount);
                                predictionResult.put("severity", severity);
                                predictionResult.put("acne_types", acneTypes);
                                
                                // 如果有AI诊断数据，也添加到预测结果中
                                if (hasAiDiagnosis) {
                                    predictionResult.put("ai_diagnosis", data.getJSONObject("ai_diagnosis"));
                                }
                                
                                Log.d(TAG, "完整预测结果: " + predictionResult.toString());
                                
                                // 延迟3秒后显示结果
                                handler.postDelayed(() -> {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            stopScanAnimation();
                                            showResult(resultImageUrl, predictionResult);
                                        });
                                    }
                                }, 3000);
                            } else {
                                String errorMsg = jsonResponse.optString("error", "未知错误");
                                Log.e(TAG, "API返回错误: " + errorMsg);
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        stopScanAnimation();
                                        Toast.makeText(getActivity(), "检测失败: " + errorMsg, Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }
                        } else {
                            Log.e(TAG, "服务器响应错误码: " + response.code() + ", 消息: " + response.message());
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    stopScanAnimation();
                                    Toast.makeText(getActivity(), "服务器响应错误: " + response.message(), Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "解析服务器响应失败: " + e.getMessage(), e);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                stopScanAnimation();
                                Toast.makeText(getActivity(), "解析服务器响应失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "处理响应时发生异常: " + e.getMessage(), e);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                stopScanAnimation();
                                Toast.makeText(getActivity(), "处理响应时发生错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "构建和发送请求时发生异常: " + e.getMessage(), e);
            if (getActivity() != null) {
                Toast.makeText(getActivity(), "上传过程中出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                stopScanAnimation();
            }
        }
    }

    private File createFileFromUri(Uri uri) {
        if (uri == null) {
            Log.e(TAG, "无法从null URI创建文件");
            return null;
        }
        
        InputStream inputStream = null;
        OutputStream outputStream = null;
        
        try {
            File file = new File(getContext().getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".jpg");
            Log.d(TAG, "正在从URI创建临时文件: " + file.getAbsolutePath());
            
            inputStream = getContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e(TAG, "无法打开输入流从URI: " + uri);
                return null;
            }
            
            outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[8192]; // 使用更大的缓冲区提高性能
            int bytesRead;
            long totalBytes = 0;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
            
            Log.d(TAG, "文件创建成功，大小: " + totalBytes + " 字节");
            return file;
        } catch (IOException e) {
            Log.e(TAG, "从URI创建文件时发生IO错误: " + e.getMessage(), e);
            return null;
        } catch (SecurityException e) {
            Log.e(TAG, "从URI创建文件时发生安全错误，可能需要请求权限: " + e.getMessage(), e);
            Toast.makeText(getContext(), "没有足够的权限来读取图片", Toast.LENGTH_SHORT).show();
            return null;
        } catch (Exception e) {
            Log.e(TAG, "从URI创建文件时发生未知错误: " + e.getMessage(), e);
            return null;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "关闭流时出错: " + e.getMessage(), e);
            }
        }
    }

    private void showResult(String imageUrl, JSONObject prediction) {
        // 创建结果Fragment并传递图片URL和预测结果
        OverallPredictResultFragment resultFragment = OverallPredictResultFragment.newInstance(imageUrl, prediction.toString());
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, resultFragment);
        transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }
}
