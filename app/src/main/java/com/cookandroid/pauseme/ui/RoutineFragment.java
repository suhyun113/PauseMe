package com.cookandroid.pauseme.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView; // 상단바 제목 설정을 위해 추가

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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class RoutineFragment extends Fragment {

    // Firebase 경로 상수
    private static final String DB_ROOT_ROUTINE_STATS = "routine_stats";
    private static final String DB_ROOT_USER_POINTS = "user_points";
    private static final int POINT_PER_ROUTINE = 50; // 루틴 완료 시 50P 적립

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

        // 임시 루틴 완료 버튼 (테스트용)
        Button tempRoutineButton = root.findViewById(R.id.temp_routine_button);
        if (tempRoutineButton != null) {
            tempRoutineButton.setOnClickListener(v -> completeRoutine("임시 호흡 루틴"));
        }

        // 실제 루틴 시작 버튼 (실제 앱에서는 이 버튼 클릭 시 루틴 플레이어로 이동)
        Button btnBreathing = root.findViewById(R.id.btn_start_routine_breathing);
        if (btnBreathing != null) {
            btnBreathing.setOnClickListener(v -> Toast.makeText(getContext(), "호흡 루틴 플레이어 실행...", Toast.LENGTH_SHORT).show());
        }
        Button btnStretching = root.findViewById(R.id.btn_start_routine_stretching);
        if (btnStretching != null) {
            btnStretching.setOnClickListener(v -> Toast.makeText(getContext(), "스트레칭 루틴 플레이어 실행...", Toast.LENGTH_SHORT).show());
        }

        return root;
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