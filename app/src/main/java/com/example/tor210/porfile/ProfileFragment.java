package com.example.tor210.porfile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tor210.ClockInFragment;
import com.example.tor210.R;
import com.example.tor210.predict.MainPredictFragment;
import com.squareup.picasso.Picasso;

public class ProfileFragment extends Fragment {

    private SharedPreferences sharedPreferences;
    private ImageView profileImageView;
    private TextView profileNameTextView;
    private TextView profileIdTextView;
    private TextView profileBioTextView;
    private TextView versionTextView;
    private LinearLayout skinRecordBtn, skinAnalysisBtn;
    private LinearLayout settings, helpFeedback, aboutUs;
    private Button editProfileButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        // 初始化控件
        initViews(view);
        
        // 加载用户数据
        loadUserData();
        
        // 设置点击事件
        setupClickListeners();
        
        // 显示版本号
        showVersionInfo();
        
        return view;
    }
    
    private void initViews(View view) {
        // 个人信息相关
        profileImageView = view.findViewById(R.id.profile_image_view);
        profileNameTextView = view.findViewById(R.id.profile_name_text_view);
        profileIdTextView = view.findViewById(R.id.profile_id_text_view);
        profileBioTextView = view.findViewById(R.id.profile_bio_text_view);
        editProfileButton = view.findViewById(R.id.edit_profile_button);
        
        // 皮肤记录相关
        skinRecordBtn = view.findViewById(R.id.skin_record_btn);
        skinAnalysisBtn = view.findViewById(R.id.skin_analysis_btn);
        
        // 设置与帮助相关
        settings = view.findViewById(R.id.settings);
        helpFeedback = view.findViewById(R.id.help_feedback);
        aboutUs = view.findViewById(R.id.about_us);
        
        // 版本信息
        versionTextView = view.findViewById(R.id.version_text);

        // 设置编辑按钮点击事件
        editProfileButton.setOnClickListener(v -> {
            getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new EditProfileFragment())
                .addToBackStack(null)
                .commit();
        });
    }
    
    private void loadUserData() {
        // 从SharedPreferences加载用户数据
        sharedPreferences = getActivity().getSharedPreferences("user_profile", Context.MODE_PRIVATE);
        String profileName = sharedPreferences.getString("profile_name", "小马");
        String profileBio = sharedPreferences.getString("profile_bio", "健康肌肤，从我做起");
        String profileImageUri = sharedPreferences.getString("profile_image_uri", null);

        profileNameTextView.setText(profileName);
        profileBioTextView.setText(profileBio);
        
        if (profileImageUri != null) {
            Picasso.get().load(Uri.parse(profileImageUri)).into(profileImageView);
        } else {
            profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }
    
    private void setupClickListeners() {
        // 皮肤记录点击事件
        skinRecordBtn.setOnClickListener(v -> {
            getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ClockInFragment())
                .addToBackStack(null)
                .commit();
        });
        
        skinAnalysisBtn.setOnClickListener(v -> {
            getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new MainPredictFragment())
                .addToBackStack(null)
                .commit();
        });
        
        // 设置与帮助点击事件
        settings.setOnClickListener(v -> showToast("打开设置"));
        helpFeedback.setOnClickListener(v -> showToast("打开帮助与反馈"));
        aboutUs.setOnClickListener(v -> showToast("关于我们"));
    }
    
    private void showVersionInfo() {
        try {
            PackageInfo packageInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            versionTextView.setText("V " + packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            versionTextView.setText("V 1.0.0");
        }
    }
    
    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 每次界面可见时重新加载用户数据，确保编辑后能够刷新
        loadUserData();
    }
}