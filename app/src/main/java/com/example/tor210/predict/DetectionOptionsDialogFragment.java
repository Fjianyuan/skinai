package com.example.tor210.predict;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.tor210.R;

public class DetectionOptionsDialogFragment extends DialogFragment {

    public static final int OPTION_CAMERA = 1;
    public static final int OPTION_GALLERY = 2;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("选择检测方式")
                .setItems(R.array.detection_options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (getTargetFragment() != null) {
                            ((OverallPredictFragment) getTargetFragment()).onOptionSelected(which + 1);
                        }
                    }
                });
        return builder.create();
    }
}
