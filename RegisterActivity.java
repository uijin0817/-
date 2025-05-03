package com.example.smiti;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText nameEditText;
    private EditText smbtiEditText;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        emailEditText = findViewById(R.id.emailEditTextRegister);
        passwordEditText = findViewById(R.id.passwordEditTextRegister);
        nameEditText = findViewById(R.id.nameEditTextRegister);
        smbtiEditText = findViewById(R.id.smbtiEditTextRegister);
        registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String name = nameEditText.getText().toString().trim();
            String smbti = smbtiEditText.getText().toString().trim().toUpperCase();

            // TODO: 실제 회원가입 API 연동 및 처리 구현
            if (!email.isEmpty() && !password.isEmpty() && !name.isEmpty() && smbti.length() == 4) {
                Log.d("Register", "이메일: " + email + ", 비밀번호: " + password + ", 이름: " + name + ", SMIBTI: " + smbti);
                Toast.makeText(this, "회원가입 시도: " + email, Toast.LENGTH_SHORT).show();
                finish(); // 성공 시 액티비티 종료 (실제로는 서버 응답에 따라 처리)
            } else {
                Toast.makeText(this, "모든 필드를 올바르게 입력해주세요 (SMIBTI는 4글자).", Toast.LENGTH_SHORT).show();
            }
        });
    }
}