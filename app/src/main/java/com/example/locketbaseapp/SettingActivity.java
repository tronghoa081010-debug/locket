package com.example.locketbaseapp;

import static androidx.core.content.ContextCompat.startActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SettingActivity extends AppCompatActivity {
    private Button btnLogOut;
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        mAuth = FirebaseAuth.getInstance();
        btnLogOut=findViewById(R.id.btnLogOut);
        btnLogOut.setOnClickListener(v -> {
            showLogoutDialog();
        });
    }
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    logout();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    private void logout() {
        // Đăng xuất Firebase
        mAuth.signOut();

        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();

        // Chuyển về màn hình đăng nhập
        Intent intent = new Intent(SettingActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
