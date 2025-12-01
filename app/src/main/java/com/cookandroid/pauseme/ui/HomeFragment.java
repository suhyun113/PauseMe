package com.cookandroid.pauseme.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cookandroid.pauseme.R;
import com.cookandroid.pauseme.util.PreferenceManager;

public class HomeFragment extends Fragment {

    private TextView txtWelcome;
    private TextView txtTodayCondition;
    private TextView btnFocus;
    private TextView btnRoutine;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        txtWelcome = view.findViewById(R.id.txt_welcome);

        String nickname = PreferenceManager.getNickname(requireContext());
        if (nickname == null || nickname.isEmpty()) nickname = "사용자";

        txtWelcome.setText(nickname + "님, 오늘도 고생 많았어요 🌿");

        return view;
    }
}
