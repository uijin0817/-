package com.example.smiti;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements PostAdapter.OnPostClickListener {

    private RecyclerView recyclerView; // 게시글 목록을 표시하는 RecyclerView
    private PostAdapter postAdapter; // RecyclerView에 사용될 어댑터
    private List<Post> postList = new ArrayList<>(); // 게시글 데이터를 담는 List
    private SwipeRefreshLayout swipeRefreshLayout; // 새로고침 기능을 제공하는 SwipeRefreshLayout
    private Button addPostButton; // 새 게시글 작성 버튼
    private Spinner boardTypeSpinner; // 게시판 종류 선택 Spinner
    private Button refreshPostListButton; // 게시글 목록 새로고침 버튼
    public static final int WRITE_POST_REQUEST_CODE = 100; // 새 게시글 작성 요청 코드

    private final OkHttpClient client = new OkHttpClient(); // HTTP 클라이언트
    private final Gson gson = new Gson(); // JSON 파싱 라이브러리

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 레이아웃 설정

        // RecyclerView 초기화
        recyclerView = findViewById(R.id.postRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this)); // LinearLayoutManager 설정
        postAdapter = new PostAdapter(postList, this); // 어댑터 생성 및 데이터, 리스너 설정
        recyclerView.setAdapter(postAdapter); // RecyclerView에 어댑터 설정

        // SwipeRefreshLayout 초기화 및 리스너 설정
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::fetchPosts); // 당겨서 새로고침 리스너 설정 (fetchPosts 메서드 호출)

        // 새 게시글 작성 버튼 초기화 및 클릭 리스너 설정
        addPostButton = findViewById(R.id.addPostButton);
        addPostButton.setOnClickListener(v -> {
            Log.d("MainActivity", "새 게시글 작성 액티비티 시작");
            Intent intent = new Intent(this, WritePostActivity.class); // 새 게시글 작성 액티비티로 이동하는 Intent 생성
            startActivityForResult(intent, WRITE_POST_REQUEST_CODE); // 액티비티 시작 및 결과 받기
        });

        // 게시판 종류 Spinner 초기화 및 선택 리스너 설정
        boardTypeSpinner = findViewById(R.id.boardTypeSpinner);
        boardTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d("MainActivity", "게시판 종류 변경됨: " + parent.getItemAtPosition(position).toString());
                fetchPosts(); // 게시판 종류가 변경되면 게시글 목록 새로고침
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 아무것도 선택되지 않았을 때의 동작 (필요하다면 구현)
            }
        });

        // 게시글 목록 새로고침 버튼 초기화 및 클릭 리스너 설정
        refreshPostListButton = findViewById(R.id.refreshPostListButton);
        refreshPostListButton.setOnClickListener(v -> {
            Log.d("MainActivity", "새로고침 버튼 클릭");
            fetchPosts(); // 새로고침 버튼 클릭 시 게시글 목록 새로고침
        });

        fetchPosts(); // 초기 게시글 목록 로드
    }

    // 서버에서 게시글 목록을 가져오는 메서드
    private void fetchPosts() {
        Log.d("MainActivity", "fetchPosts() 호출");
        swipeRefreshLayout.setRefreshing(true); // 새로고침 애니메이션 시작

        String selectedBoardType = boardTypeSpinner.getSelectedItem().toString(); // 선택된 게시판 종류 가져오기
        Log.d("MainActivity", "선택된 게시판 종류: " + selectedBoardType);

        String url = "http://202.31.246.51:80/posts?board_type=" + selectedBoardType; // API 엔드포인트 URL 생성

        Request request = new Request.Builder() // HTTP 요청 빌더 생성
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() { // 비동기 요청 실행
            @Override
            public void onFailure(Call call, IOException e) { // 요청 실패 시
                Log.e("MainActivity", "fetchPosts() - onFailure() 호출, 에러: " + e.getMessage());
                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false); // 새로고침 애니메이션 종료
                    Toast.makeText(MainActivity.this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException { // 요청 성공 시
                swipeRefreshLayout.setRefreshing(false); // 새로고침 애니메이션 종료
                if (response.isSuccessful()) { // 응답 성공
                    String responseBody = response.body().string(); // 응답 Body 문자열로 읽기
                    Log.d("MainActivity", "fetchPosts() - 응답 Body: " + responseBody);

                    try {
                        JSONObject jsonObject = new JSONObject(responseBody); // JSON 객체로 파싱
                        JSONArray postsArray = jsonObject.getJSONArray("posts"); // "posts" 키의 JSON 배열 가져오기
                        Type postListType = new TypeToken<List<Post>>() {}.getType(); // Gson으로 파싱할 List 타입 정의
                        List<Post> fetchedPosts = gson.fromJson(postsArray.toString(), postListType); // JSON 배열을 List<Post>로 파싱

                        for (Post post : fetchedPosts) {
                            Log.d("MainActivity", "fetchPosts() - 불러온 게시글 제목: " + post.getTitle());
                            Log.d("MainActivity", "fetchPosts() - 불러온 게시글 전체 데이터: " + gson.toJson(post));
                        }

                        Log.d("MainActivity", "fetchPosts() - 받은 게시글 목록 개수: " + fetchedPosts.size());
                        runOnUiThread(() -> {
                            postList.clear(); // 기존 게시글 목록 비우기
                            postList.addAll(fetchedPosts); // 새로 가져온 게시글 목록 추가
                            postAdapter.updateList(fetchedPosts); // 어댑터에 새 목록 업데이트
                            Log.d("MainActivity", "fetchPosts() - 어댑터 업데이트 완료");
                        });

                    } catch (JSONException e) { // JSON 파싱 오류 시
                        Log.e("MainActivity", "fetchPosts() - JSON 파싱 오류: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "게시글 목록을 불러오는데 실패했습니다. (JSON 오류)", Toast.LENGTH_SHORT).show());
                    }
                } else { // 응답 실패 시
                    Log.e("MainActivity", "fetchPosts() - 응답 실패, 코드: " + response.code());
                    if (response.body() != null) {
                        try {
                            Log.e("MainActivity", "fetchPosts() - 오류 응답 Body 내용: " + response.body().string());
                        } catch (IOException e) {
                            Log.e("MainActivity", "fetchPosts() - 오류 응답 Body 내용 읽기 오류: " + e.getMessage());
                        }
                    } else {
                        Log.e("MainActivity", "fetchPosts() - 오류 응답 Body가 null입니다.");
                    }
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "게시글 목록을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    // 게시글 상세 정보 액티비티를 시작하는 메서드
    public void fetchPostDetails(String postId) {
        Log.d("MainActivity", "fetchPostDetails() 호출, postId: " + postId);
        Intent intent = new Intent(this, PostDetailActivity.class); // 게시글 상세 액티비티로 이동하는 Intent 생성
        intent.putExtra("postId", postId); // 게시글 ID 전달
        intent.putExtra("isEditMode", true); // 수정 모드임을 알림
        startActivityForResult(intent, WRITE_POST_REQUEST_CODE); // 액티비티 시작 및 결과 받기
    }

    // 게시글 삭제 액션을 처리하는 메서드
    public void deletePost(String postId) {
        Log.d("MainActivity", "deletePost() 호출, postId: " + postId);
        // PostDetailActivity에서 삭제 기능을 처리하므로 여기서는 별도의 API 호출이 없습니다.
        // PostDetailActivity로 postId를 전달하여 삭제 로직을 수행합니다.
    }

    // 액티비티 결과 처리 메서드 (새 게시글 작성, 수정, 삭제 후)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("MainActivity", "onActivityResult() 호출, requestCode: " + requestCode + ", resultCode: " + resultCode);
        if (requestCode == WRITE_POST_REQUEST_CODE) { // 새 게시글 작성/수정 액티비티에서 돌아온 경우
            Log.d("MainActivity", "onActivityResult() - WRITE_POST_REQUEST_CODE로 돌아옴");
            if (resultCode == RESULT_OK && data != null) { // 결과가 OK이고 데이터가 있는 경우
                Log.d("MainActivity", "onActivityResult() - RESULT_OK, data: " + data.getExtras());
                if (data.hasExtra("isUpdated")) { // 게시글 수정 완료 후
                    String updatedPostId = data.getStringExtra("postId"); // 수정된 게시글 ID
                    String updatedTitle = data.getStringExtra("title"); // 수정된 제목
                    String updatedContent = data.getStringExtra("content"); // 수정된 내용

                    for (int i = 0; i < postList.size(); i++) { // 게시글 목록 순회
                        if (postList.get(i).getId().equals(updatedPostId)) { // ID가 일치하는 게시글 찾기
                            postList.get(i).setTitle(updatedTitle); // 제목 업데이트
                            postList.get(i).setContent(updatedContent); // 내용 업데이트
                            postAdapter.notifyItemChanged(i); // 어댑터에 변경 알림
                            Log.d("MainActivity", "onActivityResult() - 게시글 수정됨, ID: " + updatedPostId + ", 제목: " + updatedTitle);
                            return; // 수정된 아이템을 찾았으면 종료
                        }
                    }
                    Log.d("MainActivity", "onActivityResult() - 수정된 게시글 ID를 찾을 수 없음: " + updatedPostId);
                    fetchPosts(); // 수정된 게시글을 찾지 못하면 전체 목록을 다시 로드 (예외 처리)
                } else if (data.hasExtra("postId")) { // 새 게시글 작성 완료 후
                    String newPostId = data.getStringExtra("postId"); // 새 게시글 ID
                    String newTitle = data.getStringExtra("title"); // 새 게시글 제목
                    String newContent = data.getStringExtra("content"); // 새 게시글 내용
                    String newBoardType = data.getStringExtra("boardType"); // 새 게시글의 게시판 종류

                    String selectedBoardType = boardTypeSpinner.getSelectedItem().toString(); // 현재 선택된 게시판 종류
                    if (selectedBoardType.equals(newBoardType)) { // 현재 게시판과 새 게시글의 게시판 종류가 같으면
                        Post newPost = new Post(newPostId, newTitle, newContent); // 새 Post 객체 생성
                        postList.add(0, newPost); // 목록의 맨 앞에 추가
                        postAdapter.notifyItemInserted(0); // 어댑터에 삽입 알림
                        recyclerView.scrollToPosition(0); // 목록의 맨 위로 스크롤
                        Log.d("MainActivity", "onActivityResult() - 새 게시글 목록에 추가됨, ID: " + newPostId + ", 제목: " + newTitle + ", Board Type: " + newBoardType);
                    }
                    fetchPosts(); // 새 게시글 작성 후에도 목록을 다시 불러와 최신 상태 유지
                } else if (data.getBooleanExtra("isDeleted", false)) { // PostDetailActivity에서 게시글이 삭제되었을 경우
                    String deletedPostId = data.getStringExtra("deletedPostId"); // 삭제된 게시글 ID
                    for (int i = 0; i < postList.size(); i++) { // 게시글 목록 순회
                        if (postList.get(i).getId().equals(deletedPostId)) { // ID가 일치하는 게시글 찾기
                            postList.remove(i); // 목록에서 제거
                            postAdapter.notifyItemRemoved(i); // 어댑터에 제거 알림
                            Log.d("MainActivity", "onActivityResult() - 게시글 삭제됨, ID: " + deletedPostId);
                            break;
                        }
                    }
                    // 삭제 후에는 일반적으로 목록을 다시 불러오는 것이 좋습니다.
                    // fetchPosts();
                }
            } else {
                Log.d("MainActivity", "onActivityResult() - RESULT_OK 아님");
            }
        }
    }

    // 게시글 클릭 시 호출되는 콜백 메서드 (PostAdapter.OnPostClickListener 인터페이스 구현)
    @Override
    public void onPostSelected(String postId) {
        Log.d("MainActivity", "onPostSelected() 호출, postId: " + postId);
        Intent intent = new Intent(this, PostDetailActivity.class); // 게시글 상세 액티비티로 이동하는 Intent 생성
        intent.putExtra("postId", postId); // 게시글 ID 전달
        intent.putExtra("isEditMode", true); // 수정 모드임을 알림 (상세 페이지에서 수정 가능)
        startActivityForResult(intent, WRITE_POST_REQUEST_CODE); // 액티비티 시작 및 결과 받기
    }
}
