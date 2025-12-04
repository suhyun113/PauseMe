package com.cookandroid.pauseme;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.cookandroid.pauseme.ui.CheckInFragment;
import com.cookandroid.pauseme.ui.HomeFragment;
import com.cookandroid.pauseme.ui.MyPageFragment;
import com.cookandroid.pauseme.ui.RoutineFragment;
import com.cookandroid.pauseme.ui.TimerFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity
        implements TimerFragment.TimerControlListener { // 👈 1. 인터페이스 구현 선언

    private BottomNavigationView bottomNavigationView;

    // --- 2. Activity 내부에 타이머 상태 유지 변수 ---
    private CountDownTimer activityCountDownTimer;
    private long activityRemainingMillis = 0; // 남은 시간 (초기에는 0)
    private boolean isTimerRunning = false;
    private boolean isTimerPaused = false;
    private long selectedDuration = 25 * 60 * 1000; // 선택된 프리셋 시간 (기본 25분)
    private boolean isRestMode = false;

    // UI 갱신을 위한 핸들러
    private Handler uiUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable uiUpdateRunnable;
    // ------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bottomNavigationView = findViewById(R.id.bottom_nav);

        // 🔹 탭 선택 시 프래그먼트 교체
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            String tag = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_checkin) {
                selected = new CheckInFragment();
                tag = "CheckInFragmentTag";
            } else if (itemId == R.id.nav_timer) {
                selected = new TimerFragment();
                tag = "TimerFragmentTag"; // 👈 타이머 Fragment Tag 설정
            } else if (itemId == R.id.nav_home) {
                selected = new HomeFragment();
                tag = "HomeFragmentTag";
            } else if (itemId == R.id.nav_routine) {
                selected = new RoutineFragment();
                tag = "RoutineFragmentTag";
            } else if (itemId == R.id.nav_mypage) {
                selected = new MyPageFragment();
                tag = "MyPageFragmentTag";
            }

            return loadFragment(selected, tag);
        });

        // 처음 진입 시 "홈" 탭을 선택 상태로 만들기
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.getMenu().findItem(R.id.nav_home).setChecked(true);
    }

    private boolean loadFragment(Fragment fragment, String tag) {
        if (fragment == null) return false;

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_frame, fragment, tag) // 👈 Tag를 사용하여 Fragment 관리
                .commit();

        // TimerFragment로 이동하면 UI 갱신 시작 (Fragment가 onResume될 때 UI를 로드하도록 보장)
        if (tag != null && tag.equals("TimerFragmentTag") && (isTimerRunning || isTimerPaused)) {
            startUiUpdates();
        } else {
            stopUiUpdates();
        }

        return true;
    }

    // ----------------------------------------------------------------------------------
    //             TimerControlListener 구현 (타이머 상태 유지 로직)
    // ----------------------------------------------------------------------------------

    @Override
    public void startTimer(long durationMillis, boolean isRestMode) {
        if (isTimerRunning) return;

        // durationMillis가 0이거나 초기 실행이 아니면, 남은 시간으로 시작합니다.
        if (!isTimerPaused) {
            activityRemainingMillis = durationMillis;
            selectedDuration = durationMillis;
            this.isRestMode = isRestMode;
        }

        activityCountDownTimer = new CountDownTimer(activityRemainingMillis, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                activityRemainingMillis = millisUntilFinished;
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                isTimerPaused = false;

                // 타이머 완료 처리 (TimerFragment에 알림)
                TimerFragment fragment = (TimerFragment) getSupportFragmentManager()
                        .findFragmentByTag("TimerFragmentTag");
                if (fragment != null) {
                    fragment.handleTimerFinish(); // Firebase 저장 및 UI 초기화
                }
                stopUiUpdates(); // UI 갱신 중지
            }
        }.start();

        isTimerRunning = true;
        isTimerPaused = false;
        startUiUpdates(); // UI 갱신 시작
    }

    @Override
    public void pauseTimer() {
        if (activityCountDownTimer != null) {
            activityCountDownTimer.cancel();
        }
        isTimerRunning = false;
        isTimerPaused = true;
        stopUiUpdates(); // UI 갱신 중지

        // Fragment UI를 즉시 업데이트
        notifyFragmentOfStateChange();
    }

    @Override
    public void resetTimer() {
        if (activityCountDownTimer != null) {
            activityCountDownTimer.cancel();
        }
        isTimerRunning = false;
        isTimerPaused = false;
        activityRemainingMillis = selectedDuration; // 선택된 프리셋 시간으로 리셋
        stopUiUpdates(); // UI 갱신 중지

        // Fragment UI를 즉시 업데이트
        notifyFragmentOfStateChange();
    }

    @Override
    public long getRemainingMillis() {
        if (isTimerRunning || isTimerPaused) {
            return activityRemainingMillis;
        }
        return selectedDuration;
    }

    @Override
    public boolean isTimerRunning() {
        return isTimerRunning;
    }

    @Override
    public boolean isTimerPaused() {
        return isTimerPaused;
    }

    // --- UI 갱신 관리 (Fragment로 상태를 주기적으로 전달) ---

    private void startUiUpdates() {
        if (uiUpdateRunnable == null) {
            uiUpdateRunnable = new Runnable() {
                @Override
                public void run() {
                    notifyFragmentOfStateChange();
                    uiUpdateHandler.postDelayed(this, 100); // 100ms마다 갱신 (UI 부드러움 향상)
                }
            };
        }
        uiUpdateHandler.post(uiUpdateRunnable);
    }

    private void stopUiUpdates() {
        if (uiUpdateRunnable != null) {
            uiUpdateHandler.removeCallbacks(uiUpdateRunnable);
        }
    }

    // Fragment에 현재 상태를 전달
    private void notifyFragmentOfStateChange() {
        // 현재 화면에 TimerFragment가 있는지 확인하고 갱신
        TimerFragment fragment = (TimerFragment) getSupportFragmentManager()
                .findFragmentByTag("TimerFragmentTag");
        if (fragment != null && fragment.isVisible()) {
            fragment.updateTimerDisplay(activityRemainingMillis, isTimerRunning, isTimerPaused);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (activityCountDownTimer != null) {
            activityCountDownTimer.cancel();
        }
        stopUiUpdates();
    }
}