package com.cookandroid.pauseme;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EmailVerifyActivity extends AppCompatActivity {

    private EditText edtEmail, edtCode;
    private TextView txtTimer;
    private Button btnSendCode, btnVerifyNext;

    private String currentCode = null;
    private CountDownTimer countDownTimer;
    private boolean timerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verify);

        // 상단바
        TextView topTitle = findViewById(R.id.txt_title);
        topTitle.setText("회원가입");
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        edtEmail = findViewById(R.id.edt_verify_email);
        edtCode = findViewById(R.id.edt_verify_code);
        txtTimer = findViewById(R.id.txt_timer);
        btnSendCode = findViewById(R.id.btn_send_code);
        btnVerifyNext = findViewById(R.id.btn_verify_next);

        btnSendCode.setOnClickListener(v -> sendCode());
        btnVerifyNext.setOnClickListener(v -> verifyAndNext());
    }

    private void sendCode() {
        String email = edtEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "이메일을 입력해 주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!email.contains("@")) {
            Toast.makeText(this, "이메일 형식을 확인해 주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 실제 서비스에서는 서버에서 이메일 발송해야 함.
        // 지금은 로컬에서 6자리 코드 생성 + Toast로만 보여줌 (테스트용).
        int code = (int) (Math.random() * 900000) + 100000;
        currentCode = String.valueOf(code);

        Toast.makeText(this,
                "테스트용 인증 코드: " + currentCode,
                Toast.LENGTH_LONG).show();

        // 타이머 시작
        startTimer(3 * 60 * 1000L);
        edtCode.setEnabled(true);
        btnVerifyNext.setEnabled(true);
    }

    private void startTimer(long millis) {
        if (timerRunning && countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long l) {
                timerRunning = true;
                int totalSec = (int) (l / 1000);
                int min = totalSec / 60;
                int sec = totalSec % 60;
                txtTimer.setText(String.format("%02d:%02d", min, sec));
            }

            @Override
            public void onFinish() {
                timerRunning = false;
                txtTimer.setText("00:00");
                currentCode = null;
                Toast.makeText(EmailVerifyActivity.this,
                        "인증 시간이 만료되었어요. 다시 요청해 주세요.",
                        Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void verifyAndNext() {
        if (currentCode == null) {
            Toast.makeText(this, "먼저 인증 번호를 받아 주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        String inputCode = edtCode.getText().toString().trim();
        if (TextUtils.isEmpty(inputCode)) {
            Toast.makeText(this, "인증 번호를 입력해 주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!inputCode.equals(currentCode)) {
            Toast.makeText(this, "인증 번호가 올바르지 않습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        String email = edtEmail.getText().toString().trim();

        // 다음 화면(회원정보)으로 이메일 전달
        Intent intent = new Intent(this, SignupInfoActivity.class);
        intent.putExtra("email", email);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) countDownTimer.cancel();
        super.onDestroy();
    }
}
