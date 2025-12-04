package com.cookandroid.pauseme.ui;

import android.os.Bundle;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class RoutineFragment extends Fragment {

    // Firebase 경로 상수
    private static final String DB_ROOT_ROUTINE_STATS = "routine_stats";
    private static final String DB_ROOT_USER_POINTS = "user_points";
    private static final int POINT_PER_ROUTINE = 50; // 루틴 완료 시 50P 적립

    // 루틴 데이터 구조를 정의하는 내부 클래스
    private static class RoutineData {
        String name;
        String emoji;
        String time; // "3분"
        String[] steps; // 각 스텝의 설명
        public RoutineData(String name, String emoji, String time, String[] steps) {
            this.name = name;
            this.emoji = emoji;
            this.time = time;
            this.steps = steps;
        }
    }

    // --- 4가지 루틴 데이터 정의 ---
    private final RoutineData[] ALL_ROUTINES = {
            new RoutineData(
                    "호흡 명상", "🧘", "3분",
                    new String[]{
                            "1단계: 편안하게 앉아 눈을 감고 호흡에 집중하세요.",
                            "2단계: 4초 동안 숨을 들이마시고, 6초 동안 내쉬세요.",
                            "3단계: 호흡의 느낌에 감사하며, 천천히 눈을 뜨세요."
                    }
            ),
            new RoutineData(
                    "목/어깨 스트레칭", "🤸🏻‍♀️", "3분",
                    new String[]{
                            "1단계: 어깨를 위로 끌어올린 후 힘껏 뒤로 젖히세요.",
                            "2단계: 고개를 좌우로 천천히 기울여 목 근육을 늘이세요.",
                            "3단계: 두 팔을 깍지 끼고 기지개를 켜며 마무리합니다."
                    }
            ),
            new RoutineData(
                    "5분 낮잠", "😴", "3분",
                    new String[]{
                            "1단계: 알람을 설정하고 가장 편안한 자세를 취하세요.",
                            "2단계: 잠이 오지 않아도 모든 생각을 멈추고 휴식합니다.",
                            "3단계: 알람 소리에 맞춰 일어나 천천히 움직이세요."
                    }
            ),
            new RoutineData(
                    "명상 산책", "🌳", "3분",
                    new String[]{
                            "1단계: 주변의 소리와 냄새 등 감각에 집중하세요.",
                            "2단계: 휴대폰을 내려놓고 느린 걸음으로 걷습니다.",
                            "3단계: 걷는 동안 발이 땅에 닿는 느낌에만 집중하세요."
                    }
            )
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_routine, container, false);

        // 상단바 타이틀 설정
        TextView topTitle = root.findViewById(R.id.txt_title);
        if (topTitle != null) {
            topTitle.setText("회복 루틴");
        }

        // --- 루틴 시작 버튼 연결 ---
        Button btnBreathing = root.findViewById(R.id.btn_start_routine_breathing);
        Button btnStretching = root.findViewById(R.id.btn_start_routine_stretching);
        Button btnQuickNap = root.findViewById(R.id.btn_start_routine_quick_nap);
        Button btnMindfulWalk = root.findViewById(R.id.btn_start_routine_mindful_walk);

        // 버튼 클릭 리스너 연결 (데이터 객체 전달)
        if (btnBreathing != null) {
            btnBreathing.setOnClickListener(v -> showRoutinePlayer(ALL_ROUTINES[0]));
        }
        if (btnStretching != null) {
            btnStretching.setOnClickListener(v -> showRoutinePlayer(ALL_ROUTINES[1]));
        }
        if (btnQuickNap != null) {
            btnQuickNap.setOnClickListener(v -> showRoutinePlayer(ALL_ROUTINES[2]));
        }
        if (btnMindfulWalk != null) {
            btnMindfulWalk.setOnClickListener(v -> showRoutinePlayer(ALL_ROUTINES[3]));
        }

        return root;
    }

    /** 루틴 플레이어 바텀시트 표시 및 스텝 제어 */
    private void showRoutinePlayer(RoutineData routine) {
        if (getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = LayoutInflater.from(getContext()).inflate(R.layout.bottomsheet_routine_player, null);
        dialog.setContentView(sheet);

        TextView txtTitle = sheet.findViewById(R.id.txt_player_title);
        TextView txtStep = sheet.findViewById(R.id.txt_routine_step);
        Button btnCancel = sheet.findViewById(R.id.btn_routine_cancel);
        Button btnNextComplete = sheet.findViewById(R.id.btn_routine_next_complete);

        txtTitle.setText(routine.emoji + " " + routine.name + " (" + routine.time + ")");

        final int[] currentStep = {0};
        final int totalSteps = routine.steps.length;

        // 스텝 업데이트 함수
        Runnable updateStep = () -> {
            if (currentStep[0] < totalSteps) {
                txtStep.setText(String.format("단계 %d: %s", currentStep[0] + 1, routine.steps[currentStep[0]]));
                btnNextComplete.setText(String.format("다음 (%d/%d)", currentStep[0] + 1, totalSteps));
            } else {
                txtStep.setText("루틴 완료! 수고하셨습니다.");
                btnNextComplete.setText("완료 및 포인트 적립");
            }
        };

        updateStep.run(); // 초기 스텝 설정

        // 다음/완료 버튼 리스너
        btnNextComplete.setOnClickListener(v -> {
            if (currentStep[0] == totalSteps) {
                // 최종 단계 완료 후 적립
                completeRoutine(routine.name);
                dialog.dismiss();
            } else {
                currentStep[0]++;
                updateStep.run(); // 다음 스텝 또는 완료 텍스트 표시
            }
        });

        // 나가기/취소 버튼 리스너
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


    /** 루틴 완료 시 횟수 저장 및 포인트 적립 */
    private void completeRoutine(String routineName) {
        // ... (기존 Firebase 로직 유지) ...
        if (getContext() == null) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "로그인 후 루틴을 완료해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(Calendar.getInstance().getTime());

        // 1. 루틴 횟수 저장 (하루 누적)
        DatabaseReference routineRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid).child(DB_ROOT_ROUTINE_STATS).child(todayDate);

        routineRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long existingCount = snapshot.getValue(Long.class);
                long newCount = (existingCount != null ? existingCount : 0) + 1;

                routineRef.setValue(newCount).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), routineName + " 완료! (오늘 " + newCount + "회)", Toast.LENGTH_SHORT).show();
                        // 2. 포인트 적립 호출
                        addPoints(uid, POINT_PER_ROUTINE);
                    } else {
                        Toast.makeText(getContext(), "루틴 기록에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "데이터 오류: 루틴 기록 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** 사용자 포인트 누적 증가 */
    private void addPoints(String uid, int points) {
        DatabaseReference pointRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid).child(DB_ROOT_USER_POINTS);

        pointRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long existingPoints = snapshot.getValue(Long.class);
                long newTotalPoints = (existingPoints != null ? existingPoints : 0) + points;

                pointRef.setValue(newTotalPoints);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }
}