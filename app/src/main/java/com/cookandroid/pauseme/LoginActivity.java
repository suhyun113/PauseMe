package com.cookandroid.pauseme;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private EditText edtId;
    private EditText edtPassword;
    private Button btnLogin;
    private Button btnSignupLink;

    private FirebaseAuth mAuth;
    private DatabaseReference rootRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        rootRef = FirebaseDatabase.getInstance().getReference();

        edtId = findViewById(R.id.edt_nickname);  // XML의 아이디 입력 EditText
        edtPassword = findViewById(R.id.edt_password);
        btnLogin = findViewById(R.id.btn_login);
        btnSignupLink = findViewById(R.id.btn_signup_link);

        btnLogin.setOnClickListener(v -> doLogin());

        btnSignupLink.setOnClickListener(v -> {
            Intent intent = new Intent(this, EmailVerifyActivity.class);
            startActivity(intent);
        });
    }

    private void doLogin() {
        String loginId = edtId.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(loginId)) {
            Toast.makeText(this, "아이디를 입력해 주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "비밀번호를 입력해 주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);

        // 1) userIds/{loginId} 에서 email 조회
        DatabaseReference idRef = rootRef.child("userIds").child(loginId);
        idRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this,
                            "존재하지 않는 아이디예요",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                String email = snapshot.getValue(String.class);
                if (email == null) {
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this,
                            "로그인 정보를 찾을 수 없어요",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // 2) 이메일 + 비밀번호로 FirebaseAuth 로그인
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(LoginActivity.this,
                                new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        btnLogin.setEnabled(true);

                                        if (task.isSuccessful()) {
                                            PreferenceManager.setNickname(LoginActivity.this, loginId);
                                            PreferenceManager.setLoggedIn(LoginActivity.this, true);

                                            Toast.makeText(LoginActivity.this,
                                                    loginId + "님 환영합니다!",
                                                    Toast.LENGTH_SHORT).show();

                                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            String msg = "로그인에 실패했어요.";
                                            if (task.getException() != null) {
                                                msg = task.getException().getMessage();
                                            }
                                            Toast.makeText(LoginActivity.this,
                                                    msg, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this,
                        "로그인 중 오류가 발생했어요",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
