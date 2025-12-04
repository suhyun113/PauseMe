package com.cookandroid.pauseme.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Bundle;

import com.cookandroid.pauseme.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.Locale;

public class CheckInFragment extends Fragment {

    private CalendarView calendarView;
    private Calendar currentCal;

    private TextView txtMonthDays;
    private TextView txtMonthTopMood;

    private static final String DB_ROOT_MOODS = "moods";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_checkin, container, false);

        // 상단바
        TextView topTitle = root.findViewById(R.id.txt_title);
        if (topTitle != null) topTitle.setText("일일 체크인");
        ImageButton btnBack = root.findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setVisibility(View.GONE);

        calendarView = root.findViewById(R.id.calendar_view);
        txtMonthDays = root.findViewById(R.id.txt_month_checkin_days);
        txtMonthTopMood = root.findViewById(R.id.txt_month_top_mood);

        // 오늘 기준으로 초기 월 통계 로딩 (calendarView.getDate() 안 씀!)
        currentCal = Calendar.getInstance();
        int initYear  = currentCal.get(Calendar.YEAR);
        int initMonth = currentCal.get(Calendar.MONTH) + 1;
        loadMonthStats(initYear, initMonth);

        // 달력 날짜 선택 리스너 (null 체크)
        if (calendarView != null) {
            calendarView.setOnDateChangeListener(
                    new CalendarView.OnDateChangeListener() {
                        @Override
                        public void onSelectedDayChange(@NonNull CalendarView view,
                                                        int year, int month, int dayOfMonth) {
                            int realMonth = month + 1;
                            currentCal.set(year, month, dayOfMonth);

                            showMoodBottomSheet(year, realMonth, dayOfMonth);
                            loadMonthStats(year, realMonth);
                        }
                    }
            );
        }

        return root;
    }

    private void showMoodBottomSheet(int year, int month, int day) {
        if (getContext() == null) return;

        BottomSheetDialog dialog =
                new BottomSheetDialog(requireContext(),
                        com.google.android.material.R.style.Theme_Design_Light_BottomSheetDialog);

        View sheet = LayoutInflater.from(getContext())
                .inflate(R.layout.bottomsheet_mood_select, null);
        dialog.setContentView(sheet);

        TextView txtTitle = sheet.findViewById(R.id.txt_mood_sheet_title);
        if (txtTitle != null) {
            txtTitle.setText(day + "일의 기분을 선택하세요");
        }

        View.OnClickListener moodClickListener = v -> {
            Object tag = v.getTag();
            if (tag == null) return;
            int moodCode = (int) tag;
            saveMood(year, month, day, moodCode);
            dialog.dismiss();
        };

        TextView m1 = sheet.findViewById(R.id.mood_1);
        TextView m2 = sheet.findViewById(R.id.mood_2);
        TextView m3 = sheet.findViewById(R.id.mood_3);
        TextView m4 = sheet.findViewById(R.id.mood_4);
        TextView m5 = sheet.findViewById(R.id.mood_5);
        TextView m6 = sheet.findViewById(R.id.mood_6);
        TextView m7 = sheet.findViewById(R.id.mood_7);
        TextView m8 = sheet.findViewById(R.id.mood_8);

        if (m1 != null) { m1.setTag(1); m1.setOnClickListener(moodClickListener); }
        if (m2 != null) { m2.setTag(2); m2.setOnClickListener(moodClickListener); }
        if (m3 != null) { m3.setTag(3); m3.setOnClickListener(moodClickListener); }
        if (m4 != null) { m4.setTag(4); m4.setOnClickListener(moodClickListener); }
        if (m5 != null) { m5.setTag(5); m5.setOnClickListener(moodClickListener); }
        if (m6 != null) { m6.setTag(6); m6.setOnClickListener(moodClickListener); }
        if (m7 != null) { m7.setTag(7); m7.setOnClickListener(moodClickListener); }
        if (m8 != null) { m8.setTag(8); m8.setOnClickListener(moodClickListener); }

        Button btnCancel = sheet.findViewById(R.id.btn_mood_cancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void saveMood(int year, int month, int day, int moodCode) {
        if (getContext() == null) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String ymKey  = String.format(Locale.getDefault(), "%04d-%02d", year, month);
        String dayKey = String.format(Locale.getDefault(), "%02d", day);

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference(DB_ROOT_MOODS)
                .child(user.getUid())
                .child(ymKey)
                .child(dayKey);

        ref.setValue(moodCode).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(),
                        month + "월 " + day + "일의 기분이 기록되었어요.",
                        Toast.LENGTH_SHORT).show();
                loadMonthStats(year, month);
            } else {
                Toast.makeText(getContext(),
                        "기분 기록에 실패했어요.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMonthStats(int year, int month) {
        if (txtMonthDays == null || txtMonthTopMood == null) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String ymKey = String.format(Locale.getDefault(), "%04d-%02d", year, month);

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference(DB_ROOT_MOODS)
                .child(user.getUid())
                .child(ymKey);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long dayCount = snapshot.getChildrenCount();
                txtMonthDays.setText(dayCount + "일");

                int[] freq = new int[9]; // 1~8
                for (DataSnapshot child : snapshot.getChildren()) {
                    Long val = child.getValue(Long.class);
                    if (val == null) continue;
                    int m = val.intValue();
                    if (m >= 1 && m <= 8) freq[m]++;
                }

                int bestMood = 0;
                int bestCount = 0;
                for (int i = 1; i <= 8; i++) {
                    if (freq[i] > bestCount) {
                        bestCount = freq[i];
                        bestMood = i;
                    }
                }

                if (bestCount == 0) {
                    txtMonthTopMood.setText("—");
                } else {
                    txtMonthTopMood.setText(getMoodEmoji(bestMood));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private String getMoodEmoji(int code) {
        switch (code) {
            case 1: return "😊";
            case 2: return "🥰";
            case 3: return "😌";
            case 4: return "😴";
            case 5: return "😢";
            case 6: return "😰";
            case 7: return "🤔";
            case 8: return "🥱";
            default: return "🙂";
        }
    }
}
