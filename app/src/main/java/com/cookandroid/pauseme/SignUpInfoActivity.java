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

import com.cookandroid.pauseme.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SignUpInfoActivity extends AppCompatActivity {

    private EditText edtUserId, edtNickname, edtPassword, edtPasswordConfirm;
    private TextView txtIdStatus, txtPwStatus;
    private Button btnCheckId, btnSignupDone;

    private FirebaseAuth mAuth;
    private DatabaseReference rootRef;

    private String email;  // 이전 화면에서 받은 인증된 이메일

    private boolean idCheckedOk = false;
    private String checkedUserId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_info);

        // 상단바
        TextView topTitle = findViewById(R.id.txt_title);
        topTitle.setText("회원정보");
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        email = getIntent().getStringExtra("email");

        mAuth = FirebaseAuth.getInstance();
        rootRef = FirebaseDatabase.getInstance().getReference();

        edtUserId = findViewById(R.id.edt_user_id);
        edtNickname = findViewById(R.id.edt_nickname);
        edtPassword = findViewById(R.id.edt_password);
        edtPasswordConfirm = findViewById(R.id.edt_password_confirm);

        txtIdStatus = findViewById(R.id.txt_id_status);
        txtPwStatus = findViewById(R.id.txt_pw_status);

        btnCheckId = findViewById(R.id.btn_check_id);
        btnSignupDone = findViewById(R.id.btn_signup_done);

        btnCheckId.setOnClickListener(v -> checkUserId());
        btnSignupDone.setOnClickListener(v -> doSignup());
    }

    private void checkUserId() {
        String userId = edtUserId.getText().toString().trim();

        if (TextUtils.isEmpty(userId)) {
            txtIdStatus.setTextColor(getColor(R.color.poseme_text_sub));
            txtIdStatus.setText("아이디를 입력해 주세요");
            return;
        }

        if (userId.length() < 3 || userId.length() > 12) {
            txtIdStatus.setTextColor(getColor(android.R.color.holo_red_dark));
            txtIdStatus.setText("아이디는 3~12자로 입력해 주세요");
            return;
        }

        // 영문/숫자만 허용
        if (!userId.matches("^[a-zA-Z0-9_]+$")) {
            txtIdStatus.setTextColor(getColor(android.R.color.holo_red_dark));
            txtIdStatus.setText("영문, 숫자, 밑줄(_)만 사용할 수 있어요");
            return;
        }

        // Realtime DB에서 userIds/{userId} 존재 여부 확인
        DatabaseReference idRef = rootRef.child("userIds").child(userId);
        idRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    txtIdStatus.setTextColor(getColor(android.R.color.holo_red_dark));
                    txtIdStatus.setText("이미 사용 중인 아이디예요");
                    idCheckedOk = false;
                    checkedUserId = null;
                } else {
                    txtIdStatus.setTextColor(getColor(R.color.poseme_purple));
                    txtIdStatus.setText("사용 가능한 아이디예요");
                    idCheckedOk = true;
                    checkedUserId = userId;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SignUpInfoActivity.this,
                        "아이디 확인 중 오류가 발생했어요",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doSignup() {
        String userId = edtUserId.getText().toString().trim();
        String nickname = edtNickname.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String pwConfirm = edtPasswordConfirm.getText().toString().trim();

        if (email == null || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "이메일 인증 단계가 올바르지 않습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!idCheckedOk || checkedUserId == null || !checkedUserId.equals(userId)) {
            Toast.makeText(this, "아이디 중복확인을 완료해 주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(nickname)) {
            Toast.makeText(this, "닉네임을 입력해 주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 8) {
            txtPwStatus.setTextColor(getColor(android.R.color.holo_red_dark));
            txtPwStatus.setText("비밀번호는 8자 이상으로 입력해 주세요");
            return;
        }

        if (!password.equals(pwConfirm)) {
            txtPwStatus.setTextColor(getColor(android.R.color.holo_red_dark));
            txtPwStatus.setText("비밀번호가 서로 일치하지 않습니다");
            return;
        }

        txtPwStatus.setText("");

        btnSignupDone.setEnabled(false);

        // 1) FirebaseAuth에 email + password로 계정 생성
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        btnSignupDone.setEnabled(true);

                        if (task.isSuccessful()) {
                            String uid = task.getResult().getUser().getUid();

                            // 2) Realtime DB에 프로필 저장
                            User user = new User(uid, email, userId, nickname);
                            DatabaseReference usersRef = rootRef.child("users").child(uid);
                            DatabaseReference idRef = rootRef.child("userIds").child(userId);

                            usersRef.setValue(user).addOnCompleteListener(t1 -> {
                                if (t1.isSuccessful()) {
                                    // 아이디 -> 이메일 매핑
                                    idRef.setValue(email).addOnCompleteListener(t2 -> {
                                        if (t2.isSuccessful()) {
                                            Toast.makeText(SignUpInfoActivity.this,
                                                    "회원가입이 완료되었어요. 로그인해 주세요.",
                                                    Toast.LENGTH_LONG).show();
                                            finish();  // Login 화면으로 돌아가기
                                        } else {
                                            Toast.makeText(SignUpInfoActivity.this,
                                                    "계정 저장 중 오류가 발생했어요.",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    Toast.makeText(SignUpInfoActivity.this,
                                            "계정 저장 중 오류가 발생했어요.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });

                        } else {
                            String msg = "회원가입에 실패했어요.";
                            if (task.getException() != null) {
                                msg = task.getException().getMessage();
                            }
                            Toast.makeText(SignUpInfoActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
