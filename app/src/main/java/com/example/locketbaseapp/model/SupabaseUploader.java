package com.example.locketbaseapp.model;

import android.util.Log;
import java.io.File;
import java.io.IOException;
import okhttp3.*;

public class SupabaseUploader {

    private static final String SUPABASE_URL = "https://gqfavqyhvdbarymqulkd.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdxZmF2cXlodmRiYXJ5bXF1bGtkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjE3NTQ5ODksImV4cCI6MjA3NzMzMDk4OX0.5aDWgzMuiiOeO1dWe_jS0ySC8FvaF9gAbbuHYOoxaa4";
    private static final String BUCKET_NAME = "mobile_locket";

    private static final OkHttpClient client = new OkHttpClient();

    public static String uploadFile(File file) throws IOException {
        // Endpoint upload đúng của Supabase
        String endpoint = SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/" + file.getName();

        MediaType mediaType = MediaType.parse("image/jpeg");
        RequestBody fileBody = RequestBody.create(file, mediaType);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/" + file.getName())
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("x-upsert", "true")
                .put(RequestBody.create(file, MediaType.parse("image/jpeg"))) // ✅ PUT thay vì POST
                .build();

        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            response.close();
            // Trả về link public
            return SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/" + file.getName();
        } else {
            String error = response.body() != null ? response.body().string() : "Unknown error";
            response.close();
            throw new IOException("Upload failed: " + error);
        }
    }
}