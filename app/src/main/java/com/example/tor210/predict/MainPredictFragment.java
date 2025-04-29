package com.example.tor210.predict;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.tor210.R;

public class MainPredictFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 自动打开整体检测界面
        openPredictFragment();
        // 返回空视图，因为我们立即就跳转到了整体检测界面
        return inflater.inflate(R.layout.fragment_empty, container, false);
    }

    private void openPredictFragment() {
        Fragment fragment = new OverallPredictFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}
