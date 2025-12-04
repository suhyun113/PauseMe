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
import com.google.android.material.bottomsheet.BottomSheetDialog; // BottomSheetDialog 추가
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
    private static final String ROUTINE_TIME = " (3분)";

    // 호흡 명상 스텝 정의
    private final String[] breathingSteps = {
            "1단계: 편안하게 앉아 눈을 감고, 호흡에 집중하세요. (1분)",
            "2단계: 4초 동안 숨을 들이마시고, 6초 동안 내쉬세요. (1분)",
            "3단계: 호흡의 느낌에 감사하며, 천천히 눈을 뜨세요. (1분)"
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
        Button btnQuickNap = root.findViewById(R.id.btn_start_routine_quick_nap); // XML에 추가 가정
        Button btnMindfulWalk = root.findViewById(R.id.btn_start_routine_mindful_walk); // XML에 추가 가정

        // 호흡 명상 (3단계 스텝 필요)
        if (btnBreathing != null) {
            btnBreathing.setOnClickListener(v -> showRoutinePlayer("호흡 명상", 0));
        }
        // 나머지 루틴 (단순 토스트/이동 처리)
        if (btnStretching != null) {
            btnStretching.setOnClickListener(v -> Toast.makeText(getContext(), "스트레칭 루틴 플레이어 실행...", Toast.LENGTH_SHORT).show());
        }
        if (btnQuickNap != null) {
            btnQuickNap.setOnClickListener(v -> Toast.makeText(getContext(), "5분 낮잠 루틴 플레이어 실행...", Toast.LENGTH_SHORT).show());
        }
        if (btnMindfulWalk != null) {
            btnMindfulWalk.setOnClickListener(v -> Toast.makeText(getContext(), "명상 산책 루틴 플레이어 실행...", Toast.LENGTH_SHORT).show());
        }

        return root;
    }

    /** 루틴 플레이어 바텀시트 표시 (호흡 명상용) */
    private void showRoutinePlayer(String routineName, int routineId) {
        if (getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = LayoutInflater.from(getContext()).inflate(R.layout.bottomsheet_routine_player, null);
        dialog.setContentView(sheet);

        TextView txtTitle = sheet.findViewById(R.id.txt_player_title);
        TextView txtStep = sheet.findViewById(R.id.txt_routine_step);
        Button btnCancel = sheet.findViewById(R.id.btn_routine_cancel);
        Button btnNextComplete = sheet.findViewById(R.id.btn_routine_next_complete);

        txtTitle.setText(routineName + ROUTINE_TIME);

        final int[] currentStep = {0};

        // 스텝 업데이트 함수
        Runnable updateStep = () -> {
            if (currentStep[0] < breathingSteps.length) {
                txtStep.setText(breathingSteps[currentStep[0]]);
                btnNextComplete.setText(String.format("다음 (%d/%d)", currentStep[0] + 1, breathingSteps.length));
            } else {
                txtStep.setText("루틴 완료! 수고하셨습니다.");
                btnNextComplete.setText("완료 및 포인트 적립");
            }
        };

        updateStep.run(); // 초기 스텝 설정

        // 다음/완료 버튼 리스너
        btnNextComplete.setOnClickListener(v -> {
            currentStep[0]++;

            if (currentStep[0] > breathingSteps.length) {
                // 3단계 완료 후 최종 적립
                completeRoutine(routineName);
                dialog.dismiss();
            } else {
                updateStep.run(); // 다음 스텝 또는 완료 텍스트 표시
            }
        });

        // 나가기/취소 버튼 리스너
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


    /** 루틴 완료 시 횟수 저장 및 포인트 적립 */
    private void completeRoutine(String routineName) {
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