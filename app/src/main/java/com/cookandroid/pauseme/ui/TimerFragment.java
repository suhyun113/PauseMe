package com.cookandroid.pauseme.ui;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cookandroid.pauseme.R;

public class TimerFragment extends Fragment {

    private TextView txtTimerTime;
    private TextView btnPreset25, btnPreset50, btnPreset5;
    private Button btnStartPause, btnReset;

    private CountDownTimer countDownTimer;
    private long selectedDurationMillis = 25 * 60 * 1000; // 기본 25분
    private long remainingMillis = selectedDurationMillis;
    private boolean isRunning = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_timer, container, false);

        // 타이머 동그라미
        txtTimerTime = root.findViewById(R.id.txt_timer_time);

        // 프리셋 버튼
        btnPreset25 = root.findViewById(R.id.btn_preset_25);
        btnPreset50 = root.findViewById(R.id.btn_preset_50);
        btnPreset5  = root.findViewById(R.id.btn_preset_5);

        // 시작/리셋 버튼
        btnStartPause = root.findViewById(R.id.btn_timer_start_pause);
        btnReset      = root.findViewById(R.id.btn_timer_reset);

        // 초기 시간 표시
        updateTimeText(remainingMillis);
        updatePresetSelection(btnPreset25);

        setupPresetButtons();
        setupControlButtons();

        return root;
    }

    private void setupPresetButtons() {
        btnPreset25.setOnClickListener(v -> {
            if (isRunning) return;
            selectedDurationMillis = 25 * 60 * 1000;
            remainingMillis = selectedDurationMillis;
            updateTimeText(remainingMillis);
            updatePresetSelection(btnPreset25);
        });

        btnPreset50.setOnClickListener(v -> {
            if (isRunning) return;
            selectedDurationMillis = 50 * 60 * 1000;
            remainingMillis = selectedDurationMillis;
            updateTimeText(remainingMillis);
            updatePresetSelection(btnPreset50);
        });

        btnPreset5.setOnClickListener(v -> {
            if (isRunning) return;
            selectedDurationMillis = 5 * 60 * 1000;
            remainingMillis = selectedDurationMillis;
            updateTimeText(remainingMillis);
            updatePresetSelection(btnPreset5);
        });
    }

    private void setupControlButtons() {
        btnStartPause.setOnClickListener(v -> {
            if (isRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        btnReset.setOnClickListener(v -> {
            resetTimer();
        });
    }

    private void startTimer() {
        if (remainingMillis <= 0) {
            remainingMillis = selectedDurationMillis;
        }

        countDownTimer = new CountDownTimer(remainingMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMillis = millisUntilFinished;
                updateTimeText(remainingMillis);
            }

            @Override
            public void onFinish() {
                isRunning = false;
                btnStartPause.setText("시작하기");
                remainingMillis = 0;
                updateTimeText(remainingMillis);

                if (getContext() != null) {
                    Toast.makeText(getContext(), "집중 시간이 끝났어요! 🎉", Toast.LENGTH_SHORT).show();
                }
            }
        }.start();

        isRunning = true;
        btnStartPause.setText("일시정지");
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning = false;
        btnStartPause.setText("다시 시작");
    }

    private void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning = false;
        remainingMillis = selectedDurationMillis;
        updateTimeText(remainingMillis);
        btnStartPause.setText("시작하기");
    }

    private void updateTimeText(long millis) {
        int totalSeconds = (int) (millis / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        String text = String.format("%02d:%02d", minutes, seconds);
        txtTimerTime.setText(text);
    }

    private void updatePresetSelection(TextView selected) {
        // 기본 배경으로 초기화
        btnPreset25.setBackgroundResource(R.drawable.bg_chip_solid_lavender);
        btnPreset50.setBackgroundResource(R.drawable.bg_chip_solid_lavender);
        btnPreset5.setBackgroundResource(R.drawable.bg_chip_solid_lavender);

        // 선택된 것만 보라색으로
        selected.setBackgroundResource(R.drawable.bg_chip_solid_purple);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
