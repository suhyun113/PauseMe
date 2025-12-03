package com.cookandroid.pauseme.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cookandroid.pauseme.R;

public class RoutineDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "extra_title";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_detail);

        TextView txtTitle = findViewById(R.id.txt_detail_title);
        TextView txtEmoji = findViewById(R.id.txt_detail_emoji);
        TextView btnClose = findViewById(R.id.btn_detail_close);
        Button btnStart = findViewById(R.id.btn_detail_start);

        // 간단히 제목만 인텐트로 받아서 표시
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (title != null) {
            txtTitle.setText(title);
        }

        btnClose.setOnClickListener(v -> finish());

        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(this, RoutinePlayerActivity.class);
            // 플레이어로도 루틴 이름 전달
            intent.putExtra(RoutinePlayerActivity.EXTRA_TITLE, title);
            startActivity(intent);
        });
    }
}
