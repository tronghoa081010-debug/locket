package com.example.locketbaseapp;

import com.example.locketbaseapp.model.User;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class CreatePasswordActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private EditText editPassword;
    private EditText editName;
    private Button btnContinue;
    private String email;
    private FirebaseAuth auth;

    private FirebaseFirestore db;

    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_password);

        editPassword = findViewById(R.id.edtPassword);
        editName = findViewById(R.id.edtName);
        btnContinue = findViewById(R.id.btnContinue);
        btnBack = findViewById(R.id.btnBack);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        email = getIntent().getStringExtra("email");

        btnBack.setOnClickListener(v -> finish());

        btnContinue.setOnClickListener(v -> {
            String password = editPassword.getText().toString().trim();
            String name = editName.getText().toString().trim(); // THÊM MỚI: Lấy tên

            // THÊM MỚI: Kiểm tra tên
            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Kiểm tra mật khẩu (đã có)
            if (password.length() < 8) {
                Toast.makeText(this, "Mật khẩu phải ít nhất 8 ký tự!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Bắt đầu quá trình tạo Auth
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {

                            FirebaseUser firebaseUser = auth.getCurrentUser();
                            if (firebaseUser == null) {
                                Toast.makeText(this, "Lỗi: Không lấy được thông tin người dùng", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            String uid = firebaseUser.getUid();


                            User newUser = new User(uid, email, name, "");

                            // Lưu user document vào Firestore
                            db.collection("users").document(uid).set(newUser)
                                    .addOnSuccessListener(aVoid -> {

                                        Log.d(TAG, "User document created successfully");
                                        Toast.makeText(this, "Tạo tài khoản thành công!", Toast.LENGTH_SHORT).show();


                                        startActivity(new Intent(this, CameraActivity.class));
                                        finishAffinity();
                                    })
                                    .addOnFailureListener(e -> {

                                        Log.e(TAG, "Lỗi khi lưu vào Firestore", e);
                                        Toast.makeText(this, "Lỗi khi lưu dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();


                                        firebaseUser.delete().addOnCompleteListener(deleteTask -> {
                                            if (deleteTask.isSuccessful()) {
                                                Log.d(TAG, "User (Auth) deleted successfully after Firestore failure.");
                                            } else {
                                                Log.e(TAG, "Failed to delete User (Auth) after Firestore failure.", deleteTask.getException());
                                            }
                                        });
                                    });

                        } else {

                            Log.e(TAG, "Lỗi tạo Auth", task.getException());

                            Toast.makeText(this, "Tạo tài khoản thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
