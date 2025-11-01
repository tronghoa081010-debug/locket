package com.example.locketbaseapp;

import android.os.Bundle;
import android.widget.GridView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.locketbaseapp.model.PhotoAdapter;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HistoryImage extends AppCompatActivity {

    private GridView gridView;
    private PhotoAdapter adapter;
    private ArrayList<String> imageUrls = new ArrayList<>();

    private static final String SUPABASE_URL = "https://gqfavqyhvdbarymqulkd.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdxZmF2cXlodmRiYXJ5bXF1bGtkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjE3NTQ5ODksImV4cCI6MjA3NzMzMDk4OX0.5aDWgzMuiiOeO1dWe_jS0ySC8FvaF9gAbbuHYOoxaa4"; // giữ nguyên key của bạn

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        gridView = findViewById(R.id.gridView);
        adapter = new PhotoAdapter(this, imageUrls);
        gridView.setAdapter(adapter);

        loadImages();
    }

    private void loadImages() {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                String url = SUPABASE_URL + "/rest/v1/posts?select=image_url&order=id.desc";
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JSONArray array = new JSONArray(responseBody);
                    imageUrls.clear();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        imageUrls.add(obj.getString("image_url"));
                    }
                    runOnUiThread(() -> adapter.notifyDataSetChanged());
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Lỗi tải ảnh: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            } catch (IOException | org.json.JSONException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
