package com.example.smiti;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 2000; // 2초 동안 스플래시 화면 표시

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView logoTextView = findViewById(R.id.logoTextView);
        logoTextView.setText("SMITI"); // 원하는 텍스트로 변경 가능

        new Handler().postDelayed(() -> {
            Intent i; // 변수 i를 if-else 블록 밖에서 선언
            // 로그인 상태 확인
            if (isUserLoggedIn()) {
                i = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                i = new Intent(SplashActivity.this, LoginActivity.class);
            }
            startActivity(i);
            finish(); // 스플래시 액티비티 종료
        }, SPLASH_TIME_OUT);
    }

    private boolean isUserLoggedIn() {
        SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
        return prefs.getString("email", null) != null; // 이메일이 저장되어 있으면 로그인된 것으로 간주
    }
}