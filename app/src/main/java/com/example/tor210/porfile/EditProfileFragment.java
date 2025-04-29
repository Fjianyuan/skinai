package com.example.tor210.porfile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tor210.R;
import com.squareup.picasso.Picasso;

import java.io.IOException;

public class EditProfileFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView profileImageView;
    private EditText profileNameEditText;
    private EditText profileBioEditText;
    private Uri imageUri;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        // 初始化控件
        profileImageView = view.findViewById(R.id.profile_image_view);
        profileNameEditText = view.findViewById(R.id.profile_name_edit_text);
        profileBioEditText = view.findViewById(R.id.profile_bio_edit_text);
        Button saveButton = view.findViewById(R.id.save_button);

        // 加载已有数据
        loadUserData();

        // 设置点击事件
        profileImageView.setOnClickListener(v -> openImageChooser());
        saveButton.setOnClickListener(v -> saveProfile());

        return view;
    }

    private void loadUserData() {
        // 从SharedPreferences加载用户数据
        sharedPreferences = getActivity().getSharedPreferences("user_profile", Context.MODE_PRIVATE);
        String profileName = sharedPreferences.getString("profile_name", "小马");
        String profileBio = sharedPreferences.getString("profile_bio", "健康肌肤，从我做起");
        String profileImageUri = sharedPreferences.getString("profile_image_uri", null);

        // 填充到界面
        profileNameEditText.setText(profileName);
        profileBioEditText.setText(profileBio);
        
        if (profileImageUri != null) {
            imageUri = Uri.parse(profileImageUri);
            Picasso.get().load(imageUri).into(profileImageView);
        } else {
            profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "选择图片"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
                profileImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                Toast.makeText(getContext(), "加载图片失败", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    private void saveProfile() {
        String profileName = profileNameEditText.getText().toString().trim();
        String profileBio = profileBioEditText.getText().toString().trim();
        
        // 验证输入
        if (profileName.isEmpty()) {
            profileNameEditText.setError("请输入昵称");
            return;
        }
        
        // 保存头像、昵称和个性签名到SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("profile_name", profileName);
        editor.putString("profile_bio", profileBio);
        if (imageUri != null) {
            editor.putString("profile_image_uri", imageUri.toString());
        }
        editor.apply();
        
        // 提示保存成功
        Toast.makeText(getContext(), "保存成功", Toast.LENGTH_SHORT).show();
        
        // 返回到ProfileFragment
        getActivity().getSupportFragmentManager().popBackStack();
    }
}
