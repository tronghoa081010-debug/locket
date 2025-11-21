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

        // Query posts mà user nằm trong visibleTo (bao gồm cả posts của chính mình)
        postListener = db.collection("posts")
                .whereArrayContains("visibleTo", currentUserId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading posts", error);
                        Toast.makeText(this, "Lỗi tải ảnh: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    postList.clear();

                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Post post = doc.toObject(Post.class);
                            if (post != null) {
                                post.postId = doc.getId();
                                postList.add(post);
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

                    Log.d(TAG, "Total posts loaded: " + postList.size());
                    adapter.notifyDataSetChanged();

                    if (postList.isEmpty()) {
                        Toast.makeText(this, "Chưa có ảnh nào!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (postListener != null) {
            postListener.remove();
        }
    }
}