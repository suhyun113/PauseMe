package com.cookandroid.pauseme;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.cookandroid.pauseme.util.PreferenceManager;

public class LoginActivity extends AppCompatActivity {

    private EditText edtNickname;
    private EditText edtPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtNickname = findViewById(R.id.edt_nickname);
        edtPassword = findViewById(R.id.edt_password);
        btnLogin = findViewById(R.id.btn_login);

        btnLogin.setOnClickListener(v -> {
            String nickname = edtNickname.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (TextUtils.isEmpty(nickname)) {
                Toast.makeText(this, "아이디를 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(password)) {
                Toast.makeText(this, "비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // 지금은 서버 연동 없으니까 단순 저장만
            PreferenceManager.setNickname(this, nickname);
            PreferenceManager.setLoggedIn(this, true);

            Toast.makeText(this, nickname + "님 환영합니다!", Toast.LENGTH_SHORT).show();

            // 로그인 성공 → 홈 화면(HomeActivity) 호출
            Intent intent = new Intent(this, com.cookandroid.pauseme.HomeActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
