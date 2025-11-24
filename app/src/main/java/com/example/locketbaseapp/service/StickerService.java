package com.example.locketbaseapp.service;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.locketbaseapp.model.Sticker;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.*;

public class StickerService {
    private static final String TAG = "StickerService";
    
    // ✅ UPDATED - User's Supabase credentials
    private static String SUPABASE_URL = "https://uanrbarcdmlzhchxsgpx.supabase.co";
    private static String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVhbnJiYXJjZG1semhjaHhzZ3B4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjM4NjE0MTcsImV4cCI6MjA3OTQzNzQxN30.HVzjG6kJyliobuwAVm-XRj7u-XdoSbDCHwCNXFuY7Ec";

    
    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface StickerCallback {
        void onSuccess(List<Sticker> stickers);
        void onError(String error);
    }

    /**
     * Cập nhật credentials Supabase
     * Gọi method này TRƯỚC KHI fetch stickers
     */
    public static void setCredentials(String url, String key) {
        SUPABASE_URL = url;
        SUPABASE_KEY = key;
        Log.d(TAG, "✅ Credentials updated: " + url);
    }

    /**
     * Fetch danh sách stickers từ Supabase
     */
    public static void fetchStickers(StickerCallback callback) {
        new Thread(() -> {
            try {
                String endpoint = SUPABASE_URL + "/rest/v1/stickers?select=*";
                
                Request request = new Request.Builder()
                        .url(endpoint)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .get()
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    JSONArray array = new JSONArray(json);

                    List<Sticker> stickers = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        Sticker sticker = new Sticker(
                                obj.getString("id"),
                                obj.getString("name"),
                                obj.getString("url")
                        );
                        stickers.add(sticker);
                    }

                    Log.d(TAG, "✅ Fetched " + stickers.size() + " stickers");
                    
                    // Callback on main thread
                    mainHandler.post(() -> callback.onSuccess(stickers));
                } else {
                    String error = "HTTP " + response.code();
                    Log.e(TAG, "❌ Failed to fetch: " + error);
                    mainHandler.post(() -> callback.onError(error));
                }
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "❌ Error fetching stickers", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }
}
