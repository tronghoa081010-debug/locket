package com.example.locketbaseapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.EditText;

import android.widget.Button;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.locketbaseapp.ui.FriendsBottomSheet;


import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CameraActivity extends AppCompatActivity {
    //
    private EditText etCaption;
    private ImageButton btnCancel;

    private ImageButton btnFriends;

    //
    private PreviewView previewView;
    private boolean isUploading = false;
    private ImageView photoPreview;
    private FrameLayout cameraContainer;
    private ImageButton btnCapture, btnPost, btnSwitchCamera, btnFlash, btnSetting;
    private  Button btnHistory;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private CameraSelector cameraSelector;
    private Camera camera;
    private boolean flashEnabled = false;
    private File photoFile;

    private final int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private static final String SUPABASE_URL = "https://gqfavqyhvdbarymqulkd.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdxZmF2cXlodmRiYXJ5bXF1bGtkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjE3NTQ5ODksImV4cCI6MjA3NzMzMDk4OX0.5aDWgzMuiiOeO1dWe_jS0ySC8FvaF9gAbbuHYOoxaa4";
    private static final String BUCKET_NAME = "mobile_locket";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        //
        etCaption = findViewById(R.id.etCaption);
        btnSetting= findViewById(R.id.btnSettings);

        btnSetting.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingActivity.class);
            startActivity(intent);
        });

        btnCancel = findViewById(R.id.btnCancel);

        btnCancel.setOnClickListener(v -> {
            resetUI();
        });

        btnFriends= findViewById(R.id.btnFriends);

        btnFriends.setOnClickListener(v -> {
            FriendsBottomSheet bottomSheet = new FriendsBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "FriendsBottomSheet");
        });


        cameraContainer = findViewById(R.id.cameraFrameContainer);
        previewView = findViewById(R.id.previewView);
        photoPreview = findViewById(R.id.photoPreview);
        btnCapture = findViewById(R.id.btnCapture);
        btnPost = findViewById(R.id.btnPost);
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera);
        btnFlash = findViewById(R.id.btnFlash);
        btnHistory = findViewById(R.id.btnHistory);
        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryImage.class);
            startActivity(intent);
        });
        cameraExecutor = Executors.newSingleThreadExecutor();
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        btnCapture.setOnClickListener(v -> takePhoto());
        btnPost.setOnClickListener(v -> {
            uploadPhoto(photoFile);
        });


        btnSwitchCamera.setOnClickListener(v -> switchCamera());
        btnFlash.setOnClickListener(v -> toggleFlash());

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                Preview preview = new Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setTargetRotation(previewView.getDisplay().getRotation())
                        .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                        .build();

                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void switchCamera() {
        cameraSelector = (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                ? CameraSelector.DEFAULT_FRONT_CAMERA
                : CameraSelector.DEFAULT_BACK_CAMERA;
        startCamera();
    }

    private void toggleFlash() {
        flashEnabled = !flashEnabled;
        Toast.makeText(this,
                flashEnabled ? "Flash sẽ bật khi chụp" : "Flash tắt",
                Toast.LENGTH_SHORT).show();
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        photoFile = new File(getCacheDir(), "photo_" + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // Flash chỉ bật trong lúc chụp
        if (flashEnabled && camera != null) {
            camera.getCameraControl().enableTorch(true);
        }

        imageCapture.takePicture(outputOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                runOnUiThread(() -> {
                    Toast.makeText(CameraActivity.this, "Đã chụp!", Toast.LENGTH_SHORT).show();
                    if (camera != null) {
                        camera.getCameraControl().enableTorch(false);
                    }
                    flashEnabled = false;
                    btnPost.setVisibility(View.VISIBLE);
                    photoPreview.setVisibility(View.VISIBLE);
                    photoPreview.setAlpha(0f);
                    photoPreview.setVisibility(View.VISIBLE);
                    photoPreview.animate().alpha(1f).setDuration(300).start();
                    photoPreview.setImageBitmap(android.graphics.BitmapFactory.decodeFile(photoFile.getAbsolutePath()));
                    etCaption.setVisibility(View.VISIBLE);
                    btnPost.setVisibility(View.VISIBLE);
                    btnCancel.setVisibility(View.VISIBLE);

                    btnCapture.setVisibility(View.GONE);
                    btnFlash.setVisibility(View.GONE);
                    btnSwitchCamera.setVisibility(View.GONE);

                });

            }
            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() ->
                        Toast.makeText(CameraActivity.this, "Lỗi: " + exception.getMessage(), Toast.LENGTH_SHORT).show());
                if (camera != null) {
                    camera.getCameraControl().enableTorch(false);
                }
            }
        });
    }

    private void uploadPhoto(File photoFile) {
        if (photoFile == null || !photoFile.exists()) {
            runOnUiThread(() ->
                    Toast.makeText(this, "Không có ảnh để upload!", Toast.LENGTH_SHORT).show());
            return;
        }
        //
        String caption = etCaption.getText().toString().trim();
        //
        cameraExecutor.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                RequestBody body = RequestBody.create(photoFile, MediaType.parse("image/jpeg"));
                String fileName = "photo_" + System.currentTimeMillis() + ".jpg";
                String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/" + fileName;

                Request request = new Request.Builder()
                        .url(uploadUrl)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("x-upsert", "true")
                        .put(body)
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    String publicUrl = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/" + fileName;
                    //
                    OkHttpClient dbClient = new OkHttpClient();
                    MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                    String jsonBody = "{ \"image_url\": \"" + publicUrl + "\",\"caption\":\""+caption+"\", \"user_id\": \"anonymous\" }";

                    RequestBody dbBody = RequestBody.create(jsonBody, JSON);
                    Request dbRequest = new Request.Builder()
                            .url(SUPABASE_URL + "/rest/v1/posts")
                            .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                            .addHeader("apikey", SUPABASE_KEY)
                            .addHeader("Content-Type", "application/json")
                            .post(dbBody)
                            .build();

                    Response dbResponse = dbClient.newCall(dbRequest).execute();
                    dbResponse.close();
                    //
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Upload thành công:\n" + publicUrl, Toast.LENGTH_LONG).show();
                        resetUI();
                    });
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "unknown";
                    runOnUiThread(() ->
                            Toast.makeText(this, "Upload thất bại (" + response.code() + "): " + errorBody,
                                    Toast.LENGTH_LONG).show());
                }

                response.close();
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Lỗi upload: " + e.getMessage(), Toast.LENGTH_LONG).show());
                e.printStackTrace();
            }
        });

    }
    private void resetUI() {
        photoPreview.setVisibility(View.GONE);
        btnPost.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);
        etCaption.setVisibility(View.GONE);
        etCaption.setText("");

        btnCapture.setVisibility(View.VISIBLE);
        btnFlash.setVisibility(View.VISIBLE);
        btnSwitchCamera.setVisibility(View.VISIBLE);

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
