package com.cookandroid.pauseme.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cookandroid.pauseme.R;

public class RoutinePlayerActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "extra_title_player";

    private ProgressBar progressBar;
    private TextView txtStepIndex;
    private TextView txtTitle;
    private TextView txtDesc;
    private TextView txtTime;

    private int currentStep = 0;
    private final int totalStep = 4;

    // 간단히 배열로 단계 정의
    private final String[] stepTitles = {
            "편안한 자세로 앉기",
            "코로 깊게 들이마시기",
            "잠시 멈추기",
            "입으로 천천히 내쉬기"
    };

    private final String[] stepDescs = {
            "등을 펴고 어깨의 긴장을 풀어주세요",
            "4초간 천천히 숨을 들이마시세요",
            "2초간 숨을 멈춰주세요",
            "6초간 천천히 숨을 내쉬세요"
    };

    private final String[] stepTimes = {
            "30초", "1분", "30초", "1분"
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_player);

        progressBar = findViewById(R.id.progress_player);
        txtStepIndex = findViewById(R.id.txt_player_step_index);
        txtTitle = findViewById(R.id.txt_player_step_title);
        txtDesc = findViewById(R.id.txt_player_step_desc);
        txtTime = findViewById(R.id.txt_player_step_time);

        Button btnStop = findViewById(R.id.btn_player_stop);
        Button btnNext = findViewById(R.id.btn_player_next);

        updateStepUI();

        btnStop.setOnClickListener(v -> finish());

        btnNext.setOnClickListener(v -> {
            if (currentStep < totalStep - 1) {
                currentStep++;
                updateStepUI();
            } else {
                // 마지막 단계면 종료
                finish();
            }
        });
    }

    private void updateStepUI() {
        int stepNumber = currentStep + 1;
        txtStepIndex.setText(stepNumber + " / " + totalStep);
        txtTitle.setText(stepTitles[currentStep]);
        txtDesc.setText(stepDescs[currentStep]);
        txtTime.setText(stepTimes[currentStep]);

        int progress = (int) ((stepNumber * 100f) / totalStep);
        progressBar.setProgress(progress);
    }
}
