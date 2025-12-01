package com.cookandroid.pauseme;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 로그인 화면 호출
        Intent intent = new Intent(this, com.cookandroid.pauseme.LoginActivity.class);
        startActivity(intent);
        finish();
    }
}