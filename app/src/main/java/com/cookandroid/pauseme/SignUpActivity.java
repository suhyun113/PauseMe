package com.cookandroid.pauseme;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class SignUpActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnSignup;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();

        TextView title = findViewById(R.id.txt_title);
        title.setText("회원가입");

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        edtEmail = findViewById(R.id.edt_email);
        edtPassword = findViewById(R.id.edt_password);
        btnSignup = findViewById(R.id.btn_signup);

        btnSignup.setOnClickListener(v -> doSignup());
    }

    private void doSignup() {
        String email = edtEmail.getText().toString().trim();
        String pw = edtPassword.getText().toString().trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "올바른 이메일을 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pw.length() < 8) {
            Toast.makeText(this, "비밀번호는 8자 이상이어야 합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, pw)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {

                        // 이메일 인증 메일 발송
                        mAuth.getCurrentUser().sendEmailVerification();

                        Toast.makeText(SignUpActivity.this,
                                "인증 이메일이 전송되었습니다. 메일을 확인해 주세요!",
                                Toast.LENGTH_LONG).show();

                        finish(); // 로그인 화면으로 이동
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "회원가입 실패";
                        Toast.makeText(SignUpActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
