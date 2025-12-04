package com.cookandroid.pauseme.ui;

import android.os.Bundle;
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

    // 통계
    private TextView txtMonthDays;
    private TextView txtMonthTopMood;

    // 선택한 날짜 카드
    private TextView txtSelectedDate;
    private TextView txtSelectedMood;

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

        // 달력 뷰
        calendarView = root.findViewById(R.id.calendar_view);
        TextView txtCalendarMonth = root.findViewById(R.id.txt_calendar_month);

        // 오늘 날짜 기준으로 월 타이틀 세팅
        // NullPointerException 방지를 위해 currentCal 초기화
        currentCal = Calendar.getInstance();
        if (txtCalendarMonth != null) {
            int y = currentCal.get(Calendar.YEAR);
            int m = currentCal.get(Calendar.MONTH) + 1;
            txtCalendarMonth.setText(String.format(Locale.getDefault(), "%d년 %d월", y, m));
        }

        // 선택 날짜 카드
        txtSelectedDate = root.findViewById(R.id.txt_selected_date);
        txtSelectedMood = root.findViewById(R.id.txt_selected_mood);

        // 이번 달 통계
        txtMonthDays    = root.findViewById(R.id.txt_month_checkin_days);
        txtMonthTopMood = root.findViewById(R.id.txt_month_top_mood);

        // 초기: 오늘 기준 월 통계 로딩
        int initYear  = currentCal.get(Calendar.YEAR);
        int initMonth = currentCal.get(Calendar.MONTH) + 1;
        loadMonthStats(initYear, initMonth);

        // 달력 날짜 선택
        if (calendarView != null) {
            calendarView.setOnDateChangeListener(
                    new CalendarView.OnDateChangeListener() {
                        @Override
                        public void onSelectedDayChange(@NonNull CalendarView view,
                                                        int year, int month, int dayOfMonth) {
                            int realMonth = month + 1;
                            currentCal.set(year, month, dayOfMonth);

                            // 월 타이틀 변경
                            if (txtCalendarMonth != null) {
                                txtCalendarMonth.setText(
                                        String.format(Locale.getDefault(),
                                                "%d년 %d월", year, realMonth));
                            }

                            // 선택한 날짜의 기존 기분 불러오기
                            loadDayMood(year, realMonth, dayOfMonth);

                            // 바텀시트로 기분 선택
                            showMoodBottomSheet(year, realMonth, dayOfMonth);

                            // 이번 달 통계 갱신
                            loadMonthStats(year, realMonth);
                        }
                    }
            );
        }

        return root;
    }

    /** 날짜 클릭 시 기분 선택 바텀시트 */
    private void showMoodBottomSheet(int year, int month, int day) {
        if (getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(
                requireContext(),
                com.google.android.material.R.style.Theme_Design_Light_BottomSheetDialog
        );

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

    /** 선택한 날짜 기분 저장 */
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

                // 선택 카드 / 통계 갱신
                updateSelectedDayUI(year, month, day, moodCode);
                loadMonthStats(year, month);
            } else {
                Toast.makeText(getContext(),
                        "기분 기록에 실패했어요.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** 특정 날짜의 기존 기분 로딩 → 카드에 반영 */
    private void loadDayMood(int year, int month, int day) {
        if (getContext() == null) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String ymKey  = String.format(Locale.getDefault(), "%04d-%02d", year, month);
        String dayKey = String.format(Locale.getDefault(), "%02d", day);

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference(DB_ROOT_MOODS)
                .child(user.getUid())
                .child(ymKey)
                .child(dayKey);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long val = snapshot.getValue(Long.class);
                if (val == null) {
                    updateSelectedDayUI(year, month, day, -1);
                } else {
                    updateSelectedDayUI(year, month, day, val.intValue());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    /** 선택한 날짜 카드 UI */
    private void updateSelectedDayUI(int year, int month, int day, int moodCode) {
        if (txtSelectedDate == null || txtSelectedMood == null) return;

        String dateText = String.format(Locale.getDefault(),
                "%d년 %d월 %d일", year, month, day);
        txtSelectedDate.setText(dateText);

        if (moodCode <= 0) {
            txtSelectedMood.setText("기록 없음");
        } else {
            txtSelectedMood.setText(getMoodEmoji(moodCode));
        }
    }

    /** 월 통계 로딩 */
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

    /** moodCode → 이모지 */
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