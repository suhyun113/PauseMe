package com.cookandroid.pauseme.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cookandroid.pauseme.R;
import com.cookandroid.pauseme.util.PreferenceManager;

public class HomeFragment extends Fragment {

    private TextView txtWelcomeName;
    private TextView txtWelcomeSub;
    private TextView txtTodayRestMinutes;
    private TextView txtWeekCountValue;
    private TextView txtWeekTimeValue;
    private Button btnStartRest;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        txtWelcomeName = view.findViewById(R.id.txt_welcome_name);
        txtWelcomeSub = view.findViewById(R.id.txt_welcome_sub);
        txtTodayRestMinutes = view.findViewById(R.id.txt_today_rest_minutes);
        txtWeekCountValue = view.findViewById(R.id.txt_week_count_value);
        txtWeekTimeValue = view.findViewById(R.id.txt_week_time_value);
        btnStartRest = view.findViewById(R.id.btn_start_rest);

        // 닉네임 불러와서 "OOO님," 표시
        String nickname = PreferenceManager.getNickname(requireContext());
        if (nickname == null || nickname.isEmpty()) {
            nickname = "포즈미";
        }
        txtWelcomeName.setText(nickname + "님,");
        txtWelcomeSub.setText("오늘도 고생 많았어요 ✨");

        // 지금은 더미 값 (나중에 타이머/기록 연동하면 됨)
        txtTodayRestMinutes.setText("0분");
        txtWeekCountValue.setText("0회");
        txtWeekTimeValue.setText("0시간");

        // 휴식 시작하기 버튼 클릭 → 타이머 탭으로 이동 등은 나중에 구현
        btnStartRest.setOnClickListener(v -> {
            // TODO: HomeActivity의 bottomNav를 이용해 타이머 탭으로 이동시키기
        });

        return view;
    }
}
