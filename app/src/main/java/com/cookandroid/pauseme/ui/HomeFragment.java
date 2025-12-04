package com.cookandroid.pauseme.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity; // 네비게이션을 위해 필요할 수 있습니다.

import com.cookandroid.pauseme.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.cookandroid.pauseme.util.PreferenceManager; // 닉네임 유틸 가정

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView txtWelcomeName;
    private TextView txtWelcomeSub;
    private TextView txtTodayRestMinutes;

    // 휴식 경고 UI 요소
    private LinearLayout layoutWarning;
    private TextView txtRestWarning;
    private Button btnStartRest;

    // Firebase 경로 상수
    private static final String DB_ROOT_TIMER_STATS = "timer_stats";
    private static final String DB_ROOT_REST_STATS = "rest_stats";
    private static final long FOCUS_WARNING_THRESHOLD_MINUTES = 120; // 2시간 = 120분
    private static final long REST_CLEAR_THRESHOLD_MINUTES = 5;      // 5분 휴식 시 경고 제거

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        // 뷰 초기화
        txtWelcomeName = root.findViewById(R.id.txt_welcome_name);
        txtWelcomeSub = root.findViewById(R.id.txt_welcome_sub);
        txtTodayRestMinutes = root.findViewById(R.id.txt_today_rest_minutes);

        // 경고/휴식 관련 뷰
        layoutWarning = root.findViewById(R.id.layout_warning);
        txtRestWarning = root.findViewById(R.id.txt_rest_warning);
        btnStartRest = root.findViewById(R.id.btn_start_rest);

        // 닉네임 설정
        String nickname = PreferenceManager.getNickname(requireContext());
        if (nickname == null || nickname.isEmpty()) {
            nickname = "포즈미";
        }
        txtWelcomeName.setText(nickname + "님,");
        txtWelcomeSub.setText("오늘도 고생 많았어요 ✨");

        // 휴식 시작하기 버튼 클릭 리스너 (TimerFragment로 이동하여 5분 타이머 시작 유도)
        if (btnStartRest != null) {
            btnStartRest.setOnClickListener(v -> {
                // TODO: HomeActivity의 bottomNav를 이용하여 TimerFragment로 이동하는 실제 내비게이션 로직 구현 필요.
                Toast.makeText(getContext(), "타이머 탭으로 이동하여 5분 휴식을 시작해주세요.", Toast.LENGTH_LONG).show();
            });
        }

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkAndShowRestWarning();
    }

    /** Firebase에서 집중/휴식 시간을 확인하고 경고 UI를 업데이트합니다. */
    private void checkAndShowRestWarning() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || getContext() == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(Calendar.getInstance().getTime());
        String uid = user.getUid();

        // 1. 오늘의 총 집중 시간 읽기
        DatabaseReference focusRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid).child(DB_ROOT_TIMER_STATS).child(todayDate);

        focusRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot focusSnapshot) {
                Long totalFocusMinutes = focusSnapshot.getValue(Long.class);
                long currentFocus = totalFocusMinutes != null ? totalFocusMinutes : 0;

                // 2시간 초과 여부 확인
                if (currentFocus >= FOCUS_WARNING_THRESHOLD_MINUTES) {
                    // 2시간 초과 시: 휴식 기록 확인
                    checkRestTime(uid, todayDate);
                } else {
                    // 2시간 미만이면 경고 숨김
                    updateWarningUI(false, 0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                updateWarningUI(false, 0);
            }
        });
    }

    /** 오늘의 휴식 시간을 확인하고 경고 표시 여부를 결정합니다. */
    private void checkRestTime(String uid, String todayDate) {
        DatabaseReference restRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid).child(DB_ROOT_REST_STATS).child(todayDate);

        restRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot restSnapshot) {
                Long totalRestMinutes = restSnapshot.getValue(Long.class);
                long currentRest = totalRestMinutes != null ? totalRestMinutes : 0;

                // 오늘의 휴식 시간 업데이트
                if (txtTodayRestMinutes != null) {
                    txtTodayRestMinutes.setText(String.format("%d분", currentRest));
                }

                // 경고 표시 조건: 집중 2시간 초과 && 휴식 5분 미만
                boolean showWarning = (currentRest < REST_CLEAR_THRESHOLD_MINUTES);
                updateWarningUI(showWarning, currentRest);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                updateWarningUI(true, 0);
            }
        });
    }

    /** 경고 UI를 업데이트합니다. */
    private void updateWarningUI(boolean showWarning, long restMinutes) {
        if (layoutWarning == null || txtRestWarning == null) return;

        if (showWarning) {
            layoutWarning.setVisibility(View.VISIBLE);
            txtRestWarning.setText("🚨 휴식이 필요해요! (오늘 집중 시간 2시간 초과)");
        } else {
            layoutWarning.setVisibility(View.GONE);
            if (restMinutes >= REST_CLEAR_THRESHOLD_MINUTES && getContext() != null) {
                // 5분 이상 휴식 시, 사용자에게 알려주는 토스트 메시지
                Toast.makeText(getContext(), "오늘 충분한 휴식을 하셨으므로 경고를 숨깁니다. 😊", Toast.LENGTH_SHORT).show();
            }
        }
    }
}