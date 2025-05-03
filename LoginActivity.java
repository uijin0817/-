package com.example.smiti;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText; // 이메일 입력 필드
    private EditText passwordEditText; // 비밀번호 입력 필드
    private Button loginButton; // 로그인 버튼
    private TextView registerTextView; // 회원가입 텍스트 뷰

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // 레이아웃 설정

        emailEditText = findViewById(R.id.emailEditTextLogin); // 이메일 입력 필드 초기화
        passwordEditText = findViewById(R.id.passwordEditTextLogin); // 비밀번호 입력 필드 초기화
        loginButton = findViewById(R.id.loginButton); // 로그인 버튼 초기화
        registerTextView = findViewById(R.id.registerTextView); // 회원가입 텍스트 뷰 초기화

        // **주의:** 네트워크 요청을 메인 스레드에서 실행하는 것을 임시적으로 허용 (실제 앱에서는 안됨)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        loginButton.setOnClickListener(v -> { // 로그인 버튼 클릭 리스너
            String email = emailEditText.getText().toString().trim(); // 입력된 이메일 가져오기 및 공백 제거
            String password = passwordEditText.getText().toString().trim(); // 입력된 비밀번호 가져오기 및 공백 제거

            if (!email.isEmpty() && !password.isEmpty()) { // 이메일과 비밀번호가 비어있지 않으면
                try {
                    URL url = new URL("http://202.31.246.51:80/users/login"); // 로그인 API URL 생성
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection(); // HTTP 연결 열기
                    connection.setRequestMethod("POST"); // HTTP POST 메서드 설정
                    connection.setRequestProperty("Content-Type", "application/json"); // 요청 Content-Type 설정 (JSON)
                    connection.setDoOutput(true); // 출력 스트림 사용 설정

                    JSONObject jsonInput = new JSONObject(); // JSON 객체 생성
                    jsonInput.put("email", email); // JSON에 이메일 추가
                    jsonInput.put("password", password); // JSON에 비밀번호 추가

                    String jsonInputString = jsonInput.toString(); // JSON 객체를 문자열로 변환

                    try (OutputStream outputStream = connection.getOutputStream()) { // 출력 스트림 얻기
                        byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8); // JSON 문자열을 UTF-8 바이트 배열로 변환
                        outputStream.write(input, 0, input.length); // 데이터를 출력 스트림에 쓰기
                    }

                    int responseCode = connection.getResponseCode(); // 응답 코드 얻기
                    if (responseCode == HttpURLConnection.HTTP_OK) { // 응답 코드가 200 (성공)이면
                        // 로그인 성공
                        runOnUiThread(() -> { // UI 스레드에서 UI 업데이트
                            saveUserEmail(email); // 로그인 성공 시 이메일 저장
                            Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show(); // 성공 토스트 메시지 표시
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class); // 메인 액티비티로 이동하는 Intent 생성
                            startActivity(intent); // 메인 액티비티 시작
                            finish(); // 로그인 액티비티 종료
                        });
                    } else { // 응답 코드가 200이 아니면 (실패)
                        // 로그인 실패
                        runOnUiThread(() -> { // UI 스레드에서 UI 업데이트
                            Toast.makeText(LoginActivity.this, "로그인 실패: 이메일 또는 비밀번호를 확인해주세요.", Toast.LENGTH_SHORT).show(); // 실패 토스트 메시지 표시
                            // 필요하다면 오류 상세 내용 로그 출력
                        });
                    }
                    connection.disconnect(); // HTTP 연결 끊기

                } catch (Exception e) { // 예외 발생 시
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "네트워크 오류 발생: " + e.getMessage(), Toast.LENGTH_SHORT).show()); // 네트워크 오류 토스트 메시지 표시
                    e.printStackTrace(); // 에러 스택 트레이스 출력
                }
            } else { // 이메일 또는 비밀번호가 비어있으면
                Toast.makeText(LoginActivity.this, "이메일과 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show(); // 입력 요청 토스트 메시지 표시
            }
        });

        registerTextView.setOnClickListener(v -> { // 회원가입 텍스트 뷰 클릭 리스너
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class); // 회원가입 액티비티로 이동하는 Intent 생성
            startActivity(intent); // 회원가입 액티비티 시작
        });
    }

    // 로그인 성공 시 사용자 이메일 저장
    private void saveUserEmail(String email) {
        SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE); // SharedPreferences 객체 얻기
        SharedPreferences.Editor editor = prefs.edit(); // SharedPreferences 편집기 얻기
        editor.putString("email", email); // 이메일 저장
        editor.apply(); // 변경 사항 적용
    }
}