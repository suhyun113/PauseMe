package com.cookandroid.pauseme.login;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cookandroid.pauseme.R;
import com.cookandroid.pauseme.util.PreferenceManager;

public class LoginActivity extends AppCompatActivity {

    private EditText edtNickname;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtNickname = findViewById(R.id.edt_nickname);
        btnLogin = findViewById(R.id.btn_login);

        btnLogin.setOnClickListener(v -> {
            String nickname = edtNickname.getText().toString().trim();

            if (TextUtils.isEmpty(nickname)) {
                Toast.makeText(this, "닉네임을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // 로컬 저장
            PreferenceManager.setNickname(this, nickname);
            PreferenceManager.setLoggedIn(this, true);

            Toast.makeText(this,
                    nickname + "님 환영합니다!",
                    Toast.LENGTH_SHORT).show();
        });
    }
}
