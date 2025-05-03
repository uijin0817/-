package com.example.smiti;

import android.content.Intent; // Intent 클래스 import
import android.net.Uri; // Uri 클래스 import
import android.os.Bundle; // Bundle 클래스 import
import android.text.util.Linkify; // Linkify 클래스 import
import android.util.Log; // Log 클래스 import
import android.view.View; // View 클래스 import
import android.widget.TextView; // TextView 클래스 import
import android.widget.Toast; // Toast 클래스 import

import androidx.annotation.NonNull; // NonNull 어노테이션 import
import androidx.appcompat.app.AlertDialog; // AlertDialog 클래스 import
import androidx.appcompat.app.AppCompatActivity; // AppCompatActivity 클래스 import

import org.json.JSONException; // JSONException 클래스 import
import org.json.JSONObject; // JSONObject 클래스 import

import java.io.IOException; // IOException 클래스 import

import okhttp3.Call; // OkHttp Call 클래스 import
import okhttp3.Callback; // OkHttp Callback 인터페이스 import
import okhttp3.MediaType; // OkHttp MediaType 클래스 import
import okhttp3.OkHttpClient; // OkHttp OkHttpClient 클래스 import
import okhttp3.Request; // OkHttp Request 클래스 import
import okhttp3.RequestBody; // OkHttp RequestBody 클래스 import
import okhttp3.Response; // OkHttp Response 클래스 import

public class PostDetailActivity extends AppCompatActivity {

    private TextView titleTextView; // 게시글 제목 표시 TextView
    private TextView contentTextView; // 게시글 내용 표시 TextView
    private TextView attachmentLabelTextView; // 첨부 파일 라벨 표시 TextView
    private TextView attachmentNameTextView; // 첨부 파일 이름/링크 표시 TextView
    private String postId; // 현재 게시글 ID
    private String serverAttachmentPath = ""; // 서버에서 제공하는 첨부 파일 경로
    private String currentTitle = ""; // 현재 게시글 제목
    private String currentContent = ""; // 현재 게시글 내용
    private final OkHttpClient httpClient = new OkHttpClient(); // HTTP 클라이언트
    private static final int EDIT_POST_REQUEST_CODE = 100; // 수정 요청 코드

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // AppCompatActivity의 onCreate() 호출
        setContentView(R.layout.activity_post_detail); // 레이아웃 설정

        // UI 요소 초기화
        titleTextView = findViewById(R.id.detail_title);
        contentTextView = findViewById(R.id.detail_content);
        attachmentLabelTextView = findViewById(R.id.detail_attachment_label);
        attachmentNameTextView = findViewById(R.id.detail_attachment_name);

        // 게시글 ID 가져오기
        postId = getIntent().getStringExtra("postId");

        // 첨부 파일명 가져오기 (onCreate에서 전달받은 값)
        serverAttachmentPath = getIntent().getStringExtra("attachmentFileName");

        // 게시글 상세 정보 가져오기
        if (postId != null) fetchPostDetails(postId);

        // 수정 버튼 클릭 리스너 설정
        findViewById(R.id.editPostButtonDetail).setOnClickListener(v -> startEditPostActivity());

        // 삭제 버튼 클릭 리스너 설정
        findViewById(R.id.deletePostButtonDetail).setOnClickListener(v -> showDeleteConfirmationDialog());

        // 첨부 파일 링크 설정
        setupAttachmentLink(serverAttachmentPath);
    }

    // 수정 액티비티 시작
    private void startEditPostActivity() {
        Intent intent = new Intent(this, WritePostActivity.class); // 수정 액티비티로 이동하는 Intent 생성
        intent.putExtra("isEditMode", true); // 수정 모드임을 알림
        intent.putExtra("postId", postId); // 게시글 ID 전달
        intent.putExtra("title", currentTitle); // 현재 제목 전달
        intent.putExtra("content", currentContent); // 현재 내용 전달
        intent.putExtra("attachmentFileName", serverAttachmentPath); // 현재 첨부 파일명 전달
        startActivityForResult(intent, EDIT_POST_REQUEST_CODE); // 수정 액티비티 시작 및 결과 받기
    }

    // 삭제 확인 다이얼로그 표시
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this) // AlertDialog 빌더 생성
                .setMessage("정말로 삭제하시겠습니까?") // 메시지 설정
                .setPositiveButton("삭제", (dialog, which) -> deletePost()) // 삭제 버튼 설정 및 클릭 리스너
                .setNegativeButton("취소", null) // 취소 버튼 설정
                .show(); // 다이얼로그 표시
    }

    // 첨부 파일 링크 설정
    private void setupAttachmentLink(String attachmentPath) {
        if (attachmentPath != null && !attachmentPath.isEmpty()) { // 첨부 파일 경로가 존재하면
            attachmentLabelTextView.setVisibility(View.VISIBLE); // 라벨 보이기
            attachmentNameTextView.setVisibility(View.VISIBLE); // 파일명 보이기
            String fullUrl = "http://202.31.246.51:80/" + attachmentPath; // 전체 URL 생성
            attachmentNameTextView.setText(fullUrl); // TextView에 URL 설정
            Linkify.addLinks(attachmentNameTextView, Linkify.WEB_URLS); // URL을 클릭 가능한 링크로 만들기
            attachmentNameTextView.setOnClickListener(v -> openFileInBrowser(fullUrl)); // 클릭 리스너 설정
        } else { // 첨부 파일 경로가 없으면
            attachmentLabelTextView.setVisibility(View.GONE); // 라벨 숨기기
            attachmentNameTextView.setVisibility(View.GONE); // 파일명 숨기기
        }
    }

    // 웹 브라우저로 파일 열기
    private void openFileInBrowser(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); // 웹 브라우저를 여는 Intent 시작
    }

    // 수정 액티비티 결과 처리
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_POST_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getBooleanExtra("isUpdated", false)) { // 수정 성공 시
            titleTextView.setText(data.getStringExtra("title")); // 제목 업데이트
            contentTextView.setText(data.getStringExtra("content")); // 내용 업데이트
            serverAttachmentPath = data.getStringExtra("attachmentFileName"); // 첨부 파일명 업데이트
            setupAttachmentLink(serverAttachmentPath); // 첨부 파일 링크 재설정
            setResult(RESULT_OK, new Intent() // MainActivity로 결과 전달
                    .putExtra("isUpdated", true)
                    .putExtra("postId", postId)
                    .putExtra("title", titleTextView.getText().toString())
                    .putExtra("content", contentTextView.getText().toString())
                    .putExtra("attachmentFileName", serverAttachmentPath));
            finish(); // 액티비티 종료
        }
    }

    // 게시글 상세 정보 가져오기
    private void fetchPostDetails(String postId) {
        String url = "http://202.31.246.51:80/posts/" + postId; // API 엔드포인트 URL

        Request request = new Request.Builder() // HTTP 요청 빌더 생성
                .url(url)
                .build();

        httpClient.newCall(request).enqueue(new Callback() { // 비동기 요청 실행
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) { // 요청 실패 시
                Log.e("PostDetailActivity", "fetchPostDetails() - onFailure: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(PostDetailActivity.this, "게시글 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException { // 요청 성공 시
                if (response.isSuccessful()) { // 응답 성공
                    try {
                        JSONObject postData = new JSONObject(response.body().string()).getJSONObject("post"); // JSON 파싱
                        currentTitle = postData.getString("title"); // 제목 추출
                        currentContent = postData.getString("content"); // 내용 추출
                        String serverAttachmentFilePathFromServer = postData.optString("file_path"); // 첨부 파일 경로 추출
                        runOnUiThread(() -> { // UI 스레드에서 UI 업데이트
                            titleTextView.setText(currentTitle);
                            contentTextView.setText(currentContent);
                            serverAttachmentPath = serverAttachmentFilePathFromServer;
                            setupAttachmentLink(serverAttachmentPath); // 첨부 파일 링크 설정
                        });
                    } catch (JSONException e) { // JSON 파싱 오류 시
                        Log.e("PostDetailActivity", "fetchPostDetails() - JSON 파싱 오류: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(PostDetailActivity.this, "게시글 정보를 불러오는데 실패했습니다. (JSON 오류)", Toast.LENGTH_SHORT).show());
                    }
                } else { // 응답 실패 시
                    Log.e("PostDetailActivity", "fetchPostDetails() - 응답 실패, 코드: " + response.code());
                    runOnUiThread(() -> Toast.makeText(PostDetailActivity.this, "게시글 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    // 게시글 삭제
    private void deletePost() {
        String url = "http://202.31.246.51:80/posts/" + postId; // API 엔드포인트 URL
        MediaType JSON = MediaType.parse("application/json; charset=utf-8"); // JSON 타입 정의
        RequestBody requestBody = RequestBody.create(JSON, String.format("{\"post_id\": %s, \"email\": \"%s\" }",
                postId, "user1@example.com")); // 요청 Body 생성 (JSON 형태)

        Request request = new Request.Builder() // HTTP 요청 빌더 생성
                .url(url)
                .delete(requestBody) // DELETE 메서드 설정 및 요청 Body 설정
                .build();

        httpClient.newCall(request).enqueue(new Callback() { // 비동기 요청 실행
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) { // 요청 실패 시
                Log.e("PostDetailActivity", "deletePost() - onFailure: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(PostDetailActivity.this, "게시글 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException { // 요청 성공 시
                if (response.isSuccessful()) { // 응답 성공
                    Log.d("PostDetailActivity", "deletePost() - onResponse: " + response.code());
                    runOnUiThread(() -> { // UI 스레드에서 UI 업데이트
                        Toast.makeText(PostDetailActivity.this, "게시글이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                        Intent resultIntent = new Intent(); // MainActivity로 결과 전달
                        resultIntent.putExtra("isDeleted", true); // 삭제됨을 알림
                        resultIntent.putExtra("deletedPostId", postId); // 삭제된 게시글 ID 전달
                        setResult(RESULT_OK, resultIntent); // 결과 코드 설정
                        finish(); // 액티비티 종료
                    });
                } else { // 응답 실패 시
                    Log.e("PostDetailActivity", "deletePost() - 응답 실패, 코드: " + response.code());
                    if (response.body() != null) {
                        Log.e("PostDetailActivity", "deletePost() - 오류 응답 Body: " + response.body().string());
                    }
                    runOnUiThread(() -> Toast.makeText(PostDetailActivity.this, "게시글 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}