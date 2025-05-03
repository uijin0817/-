package com.example.smiti;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WritePostActivity extends AppCompatActivity {

    private Spinner boardTypeSpinner;
    private EditText titleEditText;
    private EditText contentEditText;
    private Button submitButton;
    private TextView selectedFileNameTextView;
    private ImageView previewImageView;
    private Uri selectedFileUri;
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private String postIdForEdit = null;
    private InputStream inputStream = null;
    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::handleFilePickerResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_post);

        TextView writePostTitle = findViewById(R.id.writePostTitle);
        boardTypeSpinner = findViewById(R.id.boardTypeSpinner);
        titleEditText = findViewById(R.id.titleEditText);
        contentEditText = findViewById(R.id.contentEditText);
        submitButton = findViewById(R.id.submitButton);
        Button selectFileButton = findViewById(R.id.selectFileButton);
        selectedFileNameTextView = findViewById(R.id.selectedFileNameTextView);
        previewImageView = findViewById(R.id.previewImageView);

        boolean isEditMode = getIntent().getBooleanExtra("isEditMode", false);
        if (isEditMode) {
            writePostTitle.setText("게시글 수정");
            postIdForEdit = getIntent().getStringExtra("postId");
            titleEditText.setText(getIntent().getStringExtra("title"));
            contentEditText.setText(getIntent().getStringExtra("content"));
        }

        selectFileButton.setOnClickListener(v -> openFilePicker());

        submitButton.setOnClickListener(v -> submitPostOrEdit());
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
                .putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"})
                .setType("*/*");
        filePickerLauncher.launch(intent);
    }

    private String getNameFromUri(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) return cursor.getString(nameIndex);
            }
        }
        return null;
    }

    private void previewImage(Uri uri) {
        String mimeType = getContentResolver().getType(uri);
        previewImageView.setVisibility(mimeType != null && mimeType.startsWith("image/") ? View.VISIBLE : View.GONE);
        if (mimeType != null && mimeType.startsWith("image/")) {
            try {
                previewImageView.setImageBitmap(MediaStore.Images.Media.getBitmap(getContentResolver(), uri));
            } catch (IOException e) {
                Log.e("WritePostActivity", "이미지 미리보기 실패", e);
                Toast.makeText(this, "이미지 미리보기 실패", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleFilePickerResult(androidx.activity.result.ActivityResult result) {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            selectedFileUri = result.getData().getData();
            if (selectedFileUri != null) {
                String fileName = getNameFromUri(selectedFileUri);
                if (fileName != null) {
                    selectedFileNameTextView.setText("선택된 파일: " + fileName);
                }
                previewImage(selectedFileUri);
                try {
                    inputStream = getContentResolver().openInputStream(selectedFileUri);
                } catch (IOException e) {
                    Log.e("WritePostActivity", "파일 읽기 실패", e);
                    Toast.makeText(this, "파일을 읽는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
                    selectedFileUri = null;
                }
            }
        }
    }

    private void submitPostOrEdit() {
        String title = titleEditText.getText().toString();
        String content = contentEditText.getText().toString();
        String boardType = boardTypeSpinner.getSelectedItem().toString();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "제목과 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedFileUri != null) {
            uploadFileAndPost(title, content, boardType, selectedFileUri);
        } else if (postIdForEdit != null) {
            editPost(postIdForEdit, title, content, boardType, null);
        } else {
            submitPost(title, content, boardType, null);
        }
    }

    private void submitPost(String title, String content, String boardType, Uri fileUri) {
        executePostRequest("http://202.31.246.51:80/posts", buildMultipartBody(title, content, boardType, fileUri), title, content, boardType);
    }

    private void uploadFileAndPost(String title, String content, String boardType, Uri fileUri) {
        executePostRequest("http://202.31.246.51:80/posts", buildMultipartBodyWithFile(title, content, boardType, fileUri), title, content, boardType);
    }

    private void editPost(String postId, String title, String content, String boardType, Uri fileUri) {
        String url = "http://202.31.246.51:80/posts/" + postId;

        RequestBody requestBody;
        if (fileUri != null && inputStream != null) {
            requestBody = buildMultipartBodyWithFile(title, content, boardType, fileUri); // postId 제거
        } else {
            requestBody = buildJsonRequestBody(postId, title, content, boardType);
        }

        executePutRequest("http://202.31.246.51:80/posts/" + postId, requestBody, title, content, boardType);
    }

    private RequestBody buildMultipartBody(String title, String content, String boardType, Uri fileUri) {
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("email", "user1@example.com")
                .addFormDataPart("board_type", boardType)
                .addFormDataPart("title", title)
                .addFormDataPart("content", content);
        if (fileUri != null && inputStream != null) {
            try {
                builder.addPart(MultipartBody.Part.createFormData("file", getNameFromUri(fileUri),
                        RequestBody.create(MediaType.parse(getContentResolver().getType(fileUri)),
                                getBytesFromInputStream(inputStream))));
            } catch (IOException e) {
                Log.e("WritePostActivity", "파일 처리 오류", e);
                runOnUiThread(() -> Toast.makeText(WritePostActivity.this, "파일 처리 오류", Toast.LENGTH_SHORT).show());
                closeInputStream();
                return null;
            } finally {
                closeInputStream();
            }
        }
        return builder.build();
    }

    private RequestBody buildMultipartBodyWithFile(String title, String content, String boardType, Uri fileUri) {
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("email", "user1@example.com")
                .addFormDataPart("board_type", boardType)
                .addFormDataPart("title", title)
                .addFormDataPart("content", content);
        if (fileUri != null && inputStream != null) {
            try {
                builder.addPart(MultipartBody.Part.createFormData("file", getNameFromUri(fileUri),
                        RequestBody.create(MediaType.parse(getContentResolver().getType(fileUri)),
                                getBytesFromInputStream(inputStream))));
            } catch (IOException e) {
                Log.e("WritePostActivity", "파일 처리 오류", e);
                runOnUiThread(() -> Toast.makeText(WritePostActivity.this, "파일 처리 오류", Toast.LENGTH_SHORT).show());
                closeInputStream();
                return null;
            } finally {
                closeInputStream();
            }
        }
        return builder.build();
    }

    private RequestBody buildJsonRequestBody(String postId, String title, String content, String boardType) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("post_id", postId);
            jsonBody.put("email", "user1@example.com");
            jsonBody.put("title", title);
            jsonBody.put("content", content);
            jsonBody.put("board_type", boardType);
        } catch (JSONException e) {
            Log.e("WritePostActivity", "JSON 생성 오류 (수정)", e);
        }
        return RequestBody.create(JSON, jsonBody.toString());
    }

    private void executePostRequest(String url, RequestBody requestBody, String title, String content, String boardType) {
        Request request = new Request.Builder().url(url).post(requestBody).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("WritePostActivity", "executePostRequest - onFailure", e);
                runOnUiThread(() -> Toast.makeText(WritePostActivity.this, "게시글 작성에 실패했습니다.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleResponse(response, title, content, boardType);
            }
        });
    }

    private void executePutRequest(String url, RequestBody requestBody, String title, String content, String boardType) {
        Request request = new Request.Builder().url(url).put(requestBody).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("WritePostActivity", "executePutRequest - onFailure", e);
                runOnUiThread(() -> Toast.makeText(WritePostActivity.this, "게시글 수정에 실패했습니다.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleResponse(response, title, content, boardType);
            }
        });
    }

    private byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        inputStream.transferTo(byteBuffer);
        return byteBuffer.toByteArray();
    }

    private void handleResponse(Response response, String title, String content, String boardType) throws IOException {
        final String responseBody = response.body().string();
        final int responseCode = response.code();
        Log.d("WritePostActivity", "handleResponse: Code=" + responseCode + ", Body=" + responseBody);
        runOnUiThread(() -> {
            TextView uploadResponseTextView = findViewById(R.id.uploadResponseTextView);
            TextView uploadResponseBodyTextView = findViewById(R.id.uploadResponseBodyTextView);

            uploadResponseTextView.setVisibility(View.VISIBLE);
            uploadResponseBodyTextView.setVisibility(View.VISIBLE);
            uploadResponseBodyTextView.setText("응답 코드: " + responseCode + "\n응답 내용: " + responseBody);

            if (response.isSuccessful()) {
                Toast.makeText(WritePostActivity.this, "게시글 작성 성공", Toast.LENGTH_SHORT).show();
                Intent resultIntent = new Intent();
                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    if (jsonResponse.has("post_id")) {
                        resultIntent.putExtra("postId", jsonResponse.getString("post_id"));
                    }
                    resultIntent.putExtra("title", title);
                    resultIntent.putExtra("content", content);
                    resultIntent.putExtra("boardType", boardType);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } catch (JSONException e) {
                    Log.e("WritePostActivity", "handleResponse - JSON 파싱 오류", e);
                }
            } else {
                Toast.makeText(WritePostActivity.this, "게시글 작성 실패 (코드: " + responseCode + ")", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void closeInputStream() {
        try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
        } catch (IOException e) {
            Log.e("WritePostActivity", "closeInputStream - 오류", e);
        }
    }
}