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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.Locale;
import java.text.SimpleDateFormat;

public class TimerFragment extends Fragment {

    private TextView txtTimerTime;
    private TextView btnPreset25, btnPreset50, btnPreset5;
    private Button btnStartPause, btnReset;

    private CountDownTimer countDownTimer;
    private long selectedDurationMillis = 25 * 60 * 1000; // 기본 25분
    private long remainingMillis = selectedDurationMillis;
    private boolean isRunning = false;
    private boolean isRestMode = false; // 휴식 모드 플래그

    private long startTimeMillis = 0;
    private static final long REST_PRESET_MILLIS = 5 * 60 * 1000; // 5분 휴식 프리셋

    // Firebase Database Root 정의
    private static final String DB_ROOT_TIMER_STATS = "timer_stats";
    private static final String DB_ROOT_REST_STATS = "rest_stats";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_timer, container, false);

        // 상단바 타이틀 설정
        TextView topTitle = root.findViewById(R.id.txt_title);
        if (topTitle != null) {
            topTitle.setText("집중 타이머");
        }

        txtTimerTime = root.findViewById(R.id.txt_timer_time);
        btnPreset25 = root.findViewById(R.id.btn_preset_25);
        btnPreset50 = root.findViewById(R.id.btn_preset_50);
        btnPreset5  = root.findViewById(R.id.btn_preset_5);
        btnStartPause = root.findViewById(R.id.btn_timer_start_pause);
        btnReset      = root.findViewById(R.id.btn_timer_reset);

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
            isRestMode = false; // 집중 모드
            updateTimeText(remainingMillis);
            updatePresetSelection(btnPreset25);
        });

        btnPreset50.setOnClickListener(v -> {
            if (isRunning) return;
            selectedDurationMillis = 50 * 60 * 1000;
            remainingMillis = selectedDurationMillis;
            isRestMode = false; // 집중 모드
            updateTimeText(remainingMillis);
            updatePresetSelection(btnPreset50);
        });

        btnPreset5.setOnClickListener(v -> {
            if (isRunning) return;
            selectedDurationMillis = REST_PRESET_MILLIS; // 5분 휴식
            remainingMillis = selectedDurationMillis;
            isRestMode = true; // 휴식 모드
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

        startTimeMillis = remainingMillis; // 시작 시간 기록

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

                long completedDurationMinutes = (startTimeMillis / 1000) / 60;

                if (completedDurationMinutes > 0) {
                    if (isRestMode) {
                        saveTime(completedDurationMinutes, DB_ROOT_REST_STATS, "휴식");
                    } else {
                        saveTime(completedDurationMinutes, DB_ROOT_TIMER_STATS, "집중");
                    }
                }

                remainingMillis = 0;
                updateTimeText(remainingMillis);

                if (getContext() != null) {
                    String msg = isRestMode ? "휴식 시간이 끝났어요! 😊" : "집중 시간이 끝났어요! 🎉";
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    // HomeFragment의 경고 상태 갱신을 위해 액티비티에 알림 로직 필요
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

    /** Firebase에 시간 저장 (집중/휴식 공용) */
    private void saveTime(long minutes, String dbRoot, String activityName) {
        if (getContext() == null || minutes <= 0) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "로그인이 필요하여 시간을 저장할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(Calendar.getInstance().getTime());

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .child(dbRoot)
                .child(todayDate);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long existingMinutes = snapshot.getValue(Long.class);
                long totalMinutesToday = (existingMinutes != null ? existingMinutes : 0) + minutes;

                ref.setValue(totalMinutesToday).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(),
                                minutes + "분 " + activityName + " 시간이 기록되었어요! (오늘 총 " + totalMinutesToday + "분)",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(),
                                activityName + " 시간 기록에 실패했어요.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "데이터 로딩 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
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