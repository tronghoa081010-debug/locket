package com.example.locketbaseapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.locketbaseapp.LoginPasswordActivity;
import com.example.locketbaseapp.R;
import com.google.firebase.auth.FirebaseAuth;

public class AuthActivity extends AppCompatActivity {
    private EditText editEmail;
    private Button btnContinue;
    private ImageButton btnBack;
    private String mode;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        editEmail = findViewById(R.id.editEmail);
        btnContinue = findViewById(R.id.btnContinue);
        btnBack = findViewById(R.id.btnBack);
        auth = FirebaseAuth.getInstance();

        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "login";

        btnBack.setOnClickListener(v -> finish());

        // Khi nhập email, kích hoạt nút nếu hợp lệ
        editEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean valid = s.toString().contains("@");
                btnContinue.setEnabled(valid);
                btnContinue.setBackgroundResource(valid ?
                        R.drawable.rounded_button : R.drawable.rounded_button_disabled);
            }
        });

        btnContinue.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (mode.equals("login")) {
                Intent i = new Intent(this, LoginPasswordActivity.class);
                i.putExtra("email", email);
                startActivity(i);

            } else {
                auth.fetchSignInMethodsForEmail(email)
                        .addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) {
                                Toast.makeText(this, "Lỗi kết nối Firebase", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            boolean hasAccount = !task.getResult().getSignInMethods().isEmpty();

                            if (hasAccount) {
                                Toast.makeText(this, "Tài khoản đã tồn tại!", Toast.LENGTH_SHORT).show();
                            } else {
                                Intent i = new Intent(this, CreatePasswordActivity.class);
                                i.putExtra("email", email);
                                startActivity(i);
                            }
                        });
            }
        });
    }
}
