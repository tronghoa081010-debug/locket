package com.example.locketbaseapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.example.locketbaseapp.model.Post;
import com.example.locketbaseapp.ui.PostFullscreenAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.List;

public class HistoryImage extends AppCompatActivity {

    private static final String TAG = "HistoryImage";

    private ViewPager2 viewPagerPosts;
    private PostFullscreenAdapter adapter;
    private List<Post> postList = new ArrayList<>();
    private ImageButton btnBack;

    private FirebaseFirestore db;
    private String currentUserId;
    private ListenerRegistration postListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        btnBack = findViewById(R.id.btnBack);
        viewPagerPosts = findViewById(R.id.viewPagerPosts);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        adapter = new PostFullscreenAdapter(postList, this);
        viewPagerPosts.setAdapter(adapter);

        // Set vertical orientation
        viewPagerPosts.setOrientation(ViewPager2.ORIENTATION_VERTICAL);

        loadPosts();
    }

    private void loadPosts() {
        Log.d(TAG, "Loading posts for user: " + currentUserId);

        // First: Fetch from SERVER to bypass cache
        Log.d(TAG, "Fetching from SERVER...");
        db.collection("posts")
                .whereArrayContains("visibleTo", currentUserId)
                .get(com.google.firebase.firestore.Source.SERVER)
                .addOnSuccessListener(snapshot -> {
                    Log.d(TAG, "✅ Initial fetch from SERVER: " + snapshot.size() + " posts");
                    updatePostList(snapshot);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error fetching from SERVER: " + e.getMessage(), e);
                });

        // Then: Add real-time listener for updates
        Log.d(TAG, "Setting up real-time listener...");
        postListener = db.collection("posts")
                .whereArrayContains("visibleTo", currentUserId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ Listener error: " + error.getMessage(), error);
                        return;
                    }

                    if (snapshots != null) {
                        Log.d(TAG, "📡 Listener update: " + snapshots.size() + " posts");
                        updatePostList(snapshots);
                    }
                });
    }

    private void updatePostList(QuerySnapshot snapshot) {
        postList.clear();

        if (snapshot != null) {
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Post post = doc.toObject(Post.class);
                if (post != null) {
                    post.postId = doc.getId();
                    postList.add(post);
                    Log.d(TAG, "Post added: " + post.postId + " | visibleTo: " + post.visibleTo);
                }
            }
        }

        // Sắp xếp theo thời gian (mới nhất trước)
        postList.sort((p1, p2) -> {
            if (p1.timestamp == null && p2.timestamp == null) return 0;
            if (p1.timestamp == null) return 1;
            if (p2.timestamp == null) return -1;
            return p2.timestamp.compareTo(p1.timestamp);
        });

        Log.d(TAG, "✨ Total posts loaded: " + postList.size());
        adapter.notifyDataSetChanged();

        if (postList.isEmpty()) {
            Log.w(TAG, "⚠️ No posts found for user: " + currentUserId);
            Toast.makeText(this, "Chưa có ảnh nào!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (postListener != null) {
            postListener.remove();
        }
    }
}