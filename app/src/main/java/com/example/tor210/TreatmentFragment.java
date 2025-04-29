package com.example.tor210;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TreatmentFragment extends Fragment {

    private Button startConsultationButton;
    private Button getLocationButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_treatment, container, false);
        
        // 初始化控件
        startConsultationButton = view.findViewById(R.id.start_consultation_button);
        getLocationButton = view.findViewById(R.id.get_location_button);
        
        // 设置点击事件
        startConsultationButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "正在连接医生，请稍等...", Toast.LENGTH_SHORT).show();
        });
        
        getLocationButton.setOnClickListener(v -> {
            // 模拟获取位置
            Toast.makeText(getContext(), "正在获取您的位置...", Toast.LENGTH_SHORT).show();
            // 实际中应该通过LocationManager获取位置
        });
        
        // 设置医生咨询点击事件
        View doctor1 = view.findViewById(R.id.doctor_1);
        View doctor2 = view.findViewById(R.id.doctor_2);
        
        doctor1.setOnClickListener(v -> {
            Toast.makeText(getContext(), "正在连接张医生...", Toast.LENGTH_SHORT).show();
        });
        
        doctor2.setOnClickListener(v -> {
            Toast.makeText(getContext(), "正在连接李医生...", Toast.LENGTH_SHORT).show();
        });
        
        return view;
    }
}