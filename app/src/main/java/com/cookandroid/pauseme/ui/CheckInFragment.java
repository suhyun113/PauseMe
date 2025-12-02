package com.cookandroid.pauseme.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cookandroid.pauseme.R;

import java.util.Calendar;

public class CheckInFragment extends Fragment {

    private CalendarView calendarView;
    private TextView txtMonthTitle;
    private ImageView btnPrevMonth, btnNextMonth;

    private Calendar currentCal;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_checkin, container, false);

        // include 안의 뷰들 연결
        calendarView   = root.findViewById(R.id.calendar_view);

        return root;
    }

    private void updateMonthTitle() {
        int year  = currentCal.get(Calendar.YEAR);
        int month = currentCal.get(Calendar.MONTH) + 1; // Calendar는 0부터 시작
        String text = year + "년 " + month + "월";
        txtMonthTitle.setText(text);
    }
}

