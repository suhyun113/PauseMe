package com.cookandroid.pauseme;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cookandroid.pauseme.util.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail;
    private EditText edtPassword;
    private Button btnLogin;
    private Button btnSignupLink;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        edtEmail      = findViewById(R.id.edt_email);
        edtPassword   = findViewById(R.id.edt_password);
        btnLogin      = findViewById(R.id.btn_login);
        btnSignupLink = findViewById(R.id.btn_signup_link);

        // 처음에는 비활성화
        setButtonEnabled(btnLogin, false);

        // 이메일 / 비밀번호 입력 감시 → 둘 다 채워지면 버튼 활성화
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String email = edtEmail.getText().toString().trim();
                String pw    = edtPassword.getText().toString().trim();

                boolean canLogin =
                        !TextUtils.isEmpty(email)
                                && Patterns.EMAIL_ADDRESS.matcher(email).matches()
                                && !TextUtils.isEmpty(pw);
                setButtonEnabled(btnLogin, canLogin);
            }
        };
        edtEmail.addTextChangedListener(watcher);
        edtPassword.addTextChangedListener(watcher);

        // 로그인 버튼
        btnLogin.setOnClickListener(v -> doLogin());

        // 회원가입 화면으로 이동
        btnSignupLink.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    /** 공통 버튼 스타일: 활성 = 진보라, 비활성 = 연보라, 글씨는 항상 흰색 */
    private void setButtonEnabled(Button button, boolean enabled) {
        button.setEnabled(enabled);
        if (enabled) {
            button.setBackgroundResource(R.drawable.bg_button_primary_dark);
        } else {
            button.setBackgroundResource(R.drawable.bg_button_primary);
        }
        button.setTextColor(getResources().getColor(android.R.color.white));
    }

    private void doLogin() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "이메일을 정확히 입력해 주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "비밀번호를 입력해 주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) {
                                // 이메일 인증 완료한 계정만 로그인 허용
                                PreferenceManager.setNickname(LoginActivity.this, email);
                                PreferenceManager.setLoggedIn(LoginActivity.this, true);

                                Toast.makeText(LoginActivity.this,
                                        "로그인 성공!", Toast.LENGTH_SHORT).show();

                                Intent intent =
                                        new Intent(LoginActivity.this, HomeActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        "이메일 인증 후 로그인할 수 있어요.\n메일함을 확인해 주세요.",
                                        Toast.LENGTH_LONG).show();
                                if (user != null) {
                                    mAuth.signOut();
                                }
                            }
                        } else {
                            String msg = "로그인에 실패했어요. 이메일/비밀번호를 확인해 주세요.";
                            if (task.getException() != null) {
                                msg = task.getException().getMessage();
                            }
                            Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
