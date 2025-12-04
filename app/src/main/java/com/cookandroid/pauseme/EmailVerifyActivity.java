package com.cookandroid.pauseme;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;
import java.util.Random;

public class EmailVerifyActivity extends AppCompatActivity {

    private EditText edtEmail;
    private EditText edtCode;
    private TextView txtTimer;
    private Button btnSendCode;
    private Button btnVerifyNext;

    private CountDownTimer countDownTimer;
    private String generatedCode = null;   // 생성한 인증번호
    private static final long VERIFY_TIME_MS = 3 * 60 * 1000; // 3분

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verify);

        TextView topTitle = findViewById(R.id.txt_title);
        if (topTitle != null) topTitle.setText("회원가입");

        ImageButton topBack = findViewById(R.id.btn_back);
        if (topBack != null) {
            topBack.setOnClickListener(v -> finish());
        }

        edtEmail      = findViewById(R.id.edt_verify_email);
        edtCode       = findViewById(R.id.edt_verify_code);
        txtTimer      = findViewById(R.id.txt_timer);
        btnSendCode   = findViewById(R.id.btn_send_code);
        btnVerifyNext = findViewById(R.id.btn_verify_next);

        // 처음 상태
        setButtonEnabled(btnSendCode, false);
        setButtonEnabled(btnVerifyNext, false);
        edtCode.setEnabled(false);
        txtTimer.setEnabled(false);

        // 이메일 입력 → 이메일 정상이면 "인증 번호 받기" 활성화
        edtEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String email = s.toString().trim();
                boolean valid = !TextUtils.isEmpty(email)
                        && Patterns.EMAIL_ADDRESS.matcher(email).matches();
                setButtonEnabled(btnSendCode, valid);
            }
        });

        // 인증번호 입력 → 값이 있으면 "확인" 버튼 활성화
        edtCode.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String code = s.toString().trim();
                setButtonEnabled(btnVerifyNext, !TextUtils.isEmpty(code));
            }
        });

        // 이메일 인증 번호 받기 클릭
        btnSendCode.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "유효한 이메일 주소를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1) 인증번호 생성
            generatedCode = generateCode();

            // 2) TODO: 여기서 실제 서버/Cloud Functions 로 API 요청해서 메일 전송
            // sendVerificationCodeToEmail(email, generatedCode);
            // 지금은 디버깅용으로 토스트로만 보여줌
            Toast.makeText(this, "인증번호(테스트용): " + generatedCode, Toast.LENGTH_LONG).show();

            // 3) 타이머 시작 + 입력창 활성화
            startTimer();
            edtCode.setEnabled(true);
            edtCode.requestFocus();
        });

        // 확인 버튼 클릭
        btnVerifyNext.setOnClickListener(v -> {
            if (generatedCode == null) {
                Toast.makeText(this, "먼저 인증번호를 받아주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            String inputCode = edtCode.getText().toString().trim();
            if (inputCode.equals(generatedCode)) {
                Toast.makeText(this, "이메일 인증이 완료되었습니다.", Toast.LENGTH_SHORT).show();

                if (countDownTimer != null) countDownTimer.cancel();

                // 다음 단계: 아이디/비밀번호/닉네임 설정 화면으로 이동
                Intent intent = new Intent(this, SignUpInfoActivity.class);
                intent.putExtra("verifiedEmail", edtEmail.getText().toString().trim());
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "인증번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setButtonEnabled(Button button, boolean enabled) {
        button.setEnabled(enabled);
        if (enabled) {
            button.setBackgroundResource(R.drawable.bg_button_primary_dark);
        } else {
            button.setBackgroundResource(R.drawable.bg_button_primary);
        }
        button.setTextColor(getResources().getColor(android.R.color.white));
    }

    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(VERIFY_TIME_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int totalSec = (int) (millisUntilFinished / 1000);
                int min = totalSec / 60;
                int sec = totalSec % 60;
                txtTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));
            }

            @Override
            public void onFinish() {
                txtTimer.setText("00:00");
                generatedCode = null;
                Toast.makeText(EmailVerifyActivity.this,
                        "인증번호가 만료되었습니다. 다시 받아주세요.", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private String generateCode() {
        Random random = new Random();
        int num = 100000 + random.nextInt(900000); // 100000~999999
        return String.valueOf(num);
    }
}
