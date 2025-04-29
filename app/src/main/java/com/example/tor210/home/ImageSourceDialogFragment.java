package com.example.tor210.home;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.tor210.R;

/**
 * 图片来源选择对话框
 */
public class ImageSourceDialogFragment extends DialogFragment {

    public static final int SOURCE_CAMERA = 0;
    public static final int SOURCE_GALLERY = 1;

    /**
     * 图片来源选择监听器接口
     */
    public interface ImageSourceListener {
        void onImageSourceSelected(int source);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("选择图片来源")
                .setItems(new String[]{"拍照", "从相册选择"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Fragment targetFragment = getTargetFragment();
                        if (targetFragment instanceof ImageSourceListener) {
                            ((ImageSourceListener) targetFragment).onImageSourceSelected(which);
                        }
                    }
                })
                .setNegativeButton("取消", null);
        return builder.create();
    }
} 