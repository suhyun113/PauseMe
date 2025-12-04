package com.cookandroid.pauseme.ui;

import android.content.Context; // Context import 추가
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
// ... (Firebase import 유지)

import java.util.Calendar;
import java.util.Locale;
import java.text.SimpleDateFormat;

public class TimerFragment extends Fragment {

    // --- 인터페이스 정의: Activity와 통신하여 타이머 상태를 유지합니다. ---
    public interface TimerControlListener {
        void startTimer(long durationMillis, boolean isRestMode);
        void pauseTimer();
        void resetTimer();
        long getRemainingMillis();
        boolean isTimerRunning();
        boolean isTimerPaused();
        // Activity로부터 주기적으로 남은 시간을 업데이트 받는 메서드도 필요합니다.
        // Activity에서 Handler/Thread를 통해 updateTimerDisplay(long millis)를 호출해야 합니다.
    }

    private TimerControlListener timerListener;
    // ----------------------------------------------------------------------

    private TextView txtTimerTime;
    private TextView btnPreset25, btnPreset50, btnPreset5;
    private Button btnStartPause, btnReset;

    // CountDownTimer와 isRunning은 이제 Activity에서 관리합니다.
    // private CountDownTimer countDownTimer;
    private long selectedDurationMillis = 25 * 60 * 1000;
    // private long remainingMillis = selectedDurationMillis;
    // private boolean isRunning = false;
    private boolean isRestMode = false;

    private static final long REST_PRESET_MILLIS = 5 * 60 * 1000;

    // Firebase Database Root 정의 (Activity/ViewModel로 이동할 수도 있음)
    private static final String DB_ROOT_TIMER_STATS = "timer_stats";
    private static final String DB_ROOT_REST_STATS = "rest_stats";

    // 프래그먼트가 Activity에 Attach 될 때 리스너 연결
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof TimerControlListener) {
            timerListener = (TimerControlListener) context;
        } else {
            // Activity가 TimerControlListener를 구현하지 않았을 때 예외 처리
            throw new RuntimeException(context.toString()
                    + " must implement TimerControlListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_timer, container, false);

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

        // 초기 상태 로드 및 UI 업데이트
        long initialMillis = timerListener.getRemainingMillis();
        if (initialMillis <= 0) initialMillis = selectedDurationMillis;
        updateTimeText(initialMillis);

        // 버튼 상태 업데이트
        updateStartPauseButton(timerListener.isTimerRunning(), timerListener.isTimerPaused());

        setupPresetButtons();
        setupControlButtons();

        return root;
    }

    // Activity로부터 남은 시간을 받아 UI를 업데이트하는 public 메서드 (Activity가 호출)
    public void updateTimerDisplay(long remainingMillis, boolean isRunning, boolean isPaused) {
        updateTimeText(remainingMillis);
        updateStartPauseButton(isRunning, isPaused);
    }


    private void setupPresetButtons() {
        btnPreset25.setOnClickListener(v -> {
            if (timerListener.isTimerRunning() || timerListener.isTimerPaused()) return;
            selectedDurationMillis = 1 * 60 * 1000;
            isRestMode = false;
            timerListener.resetTimer(); // Activity에 reset 요청
            updateTimeText(selectedDurationMillis);
            updatePresetSelection(btnPreset25);
        });

        btnPreset50.setOnClickListener(v -> {
            if (timerListener.isTimerRunning() || timerListener.isTimerPaused()) return;
            selectedDurationMillis = 50 * 60 * 1000;
            isRestMode = false;
            timerListener.resetTimer();
            updateTimeText(selectedDurationMillis);
            updatePresetSelection(btnPreset50);
        });

        btnPreset5.setOnClickListener(v -> {
            if (timerListener.isTimerRunning() || timerListener.isTimerPaused()) return;
            selectedDurationMillis = REST_PRESET_MILLIS;
            isRestMode = true;
            timerListener.resetTimer();
            updateTimeText(selectedDurationMillis);
            updatePresetSelection(btnPreset5);
        });
    }

    private void setupControlButtons() {
        btnStartPause.setOnClickListener(v -> {
            if (timerListener.isTimerRunning()) {
                timerListener.pauseTimer();
            } else {
                // Activity에 시작 요청 (현재 선택된 시간과 모드 전달)
                long currentDuration = timerListener.isTimerPaused() ?
                        timerListener.getRemainingMillis() :
                        selectedDurationMillis;

                timerListener.startTimer(currentDuration, isRestMode);
            }
        });

        btnReset.setOnClickListener(v -> {
            timerListener.resetTimer();
            // 리셋 후 선택된 프리셋 시간으로 UI 복원
            updateTimeText(selectedDurationMillis);
            updateStartPauseButton(false, false);
        });
    }

    // 이 메서드는 Activity에서 타이머 로직이 완료될 때 호출되어야 합니다.
    public void handleTimerFinish() {
        long completedDurationMinutes = (selectedDurationMillis / 1000) / 60;

        if (completedDurationMinutes > 0) {
            if (isRestMode) {
                saveTime(completedDurationMinutes, DB_ROOT_REST_STATS, "휴식");
            } else {
                saveTime(completedDurationMinutes, DB_ROOT_TIMER_STATS, "집중");
            }
        }

        if (getContext() != null) {
            String msg = isRestMode ? "휴식 시간이 끝났어요! 😊" : "집중 시간이 끝났어요! 🎉";
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }

        // 완료 후 UI 초기화 (선택된 프리셋으로)
        updateTimeText(selectedDurationMillis);
        updateStartPauseButton(false, false);
    }

    // 시작/일시정지 버튼 텍스트를 업데이트하는 도우미 메서드
    private void updateStartPauseButton(boolean isRunning, boolean isPaused) {
        if (isRunning) {
            btnStartPause.setText("일시정지");
        } else if (isPaused) {
            btnStartPause.setText("다시 시작");
        } else {
            btnStartPause.setText("시작하기");
        }
    }


    private void saveTime(long minutes, String dbRoot, String activityName) {
        // ... (기존 Firebase saveTime 로직 유지)
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

        // ... (기존 addListenerForSingleValueEvent 로직 유지)
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
        if (txtTimerTime != null) { // Fragment가 View를 가지고 있는지 확인
            txtTimerTime.setText(text);
        }
    }

    private void updatePresetSelection(TextView selected) {
        // ... (기존 Preset Selection 로직 유지)
        btnPreset25.setBackgroundResource(R.drawable.bg_chip_solid_lavender);
        btnPreset50.setBackgroundResource(R.drawable.bg_chip_solid_lavender);
        btnPreset5.setBackgroundResource(R.drawable.bg_chip_solid_lavender);
        selected.setBackgroundResource(R.drawable.bg_chip_solid_purple);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        timerListener = null;
    }

    // onPause()나 onDestroyView()에서 타이머를 취소하는 로직을 제거하여 Activity가 상태를 유지하도록 합니다.
    // @Override
    // public void onDestroyView() {
    //     super.onDestroyView();
    // }
}