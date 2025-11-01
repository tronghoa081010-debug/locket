package com.example.locketbaseapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class LoginPasswordActivity extends AppCompatActivity {
    private EditText editPassword;
    private Button btnLogin, btnForgotPassword;
    private ImageButton btnBack;
    private String email;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_password);

        // Khởi tạo view
        editPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnContinue);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);
        btnBack = findViewById(R.id.btnBack);
        auth = FirebaseAuth.getInstance();

        email = getIntent().getStringExtra("email");

        // Quay lại màn hình trước
        btnBack.setOnClickListener(v -> finish());

        // Đăng nhập
        btnLogin.setOnClickListener(v -> {
            String password = editPassword.getText().toString().trim();

            if (password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập mật khẩu!", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, CameraActivity.class));
                            finishAffinity();
                        } else {
                            Toast.makeText(this, "Mật khẩu hoặc email chưa đúng!", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Quên mật khẩu
        btnForgotPassword.setOnClickListener(v -> {
            if (email == null || email.isEmpty()) {
                Toast.makeText(this, "Không tìm thấy email để đặt lại mật khẩu!", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this,
                                    "Đã gửi email khôi phục mật khẩu đến: " + email,
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this,
                                    "Không thể gửi email khôi phục. Vui lòng thử lại.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
