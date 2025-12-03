package com.cookandroid.pauseme.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cookandroid.pauseme.R;

public class RoutineFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_routine, container, false);

        // fragment_routine.xml 안의 include 루트들(id) 가져오기
        View cardBreath      = root.findViewById(R.id.card_routine_calm_breath);
        View cardEyeRelax    = root.findViewById(R.id.card_routine_eye_relax);
        View cardStretch     = root.findViewById(R.id.card_routine_stretch);
        View cardHeadMassage = root.findViewById(R.id.card_routine_head_massage);

        // 공통 클릭 리스너
        View.OnClickListener listener = v -> {
            if (getContext() == null) return;

            String title;

            int id = v.getId();
            if (id == R.id.card_routine_calm_breath) {
                title = "안정 호흡법";
            } else if (id == R.id.card_routine_eye_relax) {
                title = "시각 힐링";
            } else if (id == R.id.card_routine_stretch) {
                title = "가벼운 스트레칭";
            } else {
                title = "두피 마사지";
            }

            // 상세 화면으로 이동
            Intent intent = new Intent(getContext(), RoutineDetailActivity.class);
            intent.putExtra(RoutineDetailActivity.EXTRA_TITLE, title);
            startActivity(intent);
        };

        // 카드 4개에 리스너 연결
        cardBreath.setOnClickListener(listener);
        cardEyeRelax.setOnClickListener(listener);
        cardStretch.setOnClickListener(listener);
        cardHeadMassage.setOnClickListener(listener);

        return root;
    }
}
