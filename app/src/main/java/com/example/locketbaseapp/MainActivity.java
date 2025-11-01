package com.example.locketbaseapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnCreate = findViewById(R.id.btnCreateAccount);
        Button btnLogin = findViewById(R.id.btnLogin);

        btnCreate.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.example.locketbaseapp.AuthActivity.class);
            intent.putExtra("mode", "create");
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.example.locketbaseapp.AuthActivity.class);
            intent.putExtra("mode", "login");
            startActivity(intent);
        });
    }
}
