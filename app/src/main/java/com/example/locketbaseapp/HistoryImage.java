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
import com.google.firebase.auth.FirebaseUser;
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

        Log.d(TAG, "═══════════════════════════════════════");
        Log.d(TAG, "HistoryImage onCreate() started");
        Log.d(TAG, "═══════════════════════════════════════");

        // ✅ Log Firebase Auth state
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "❌ ERROR: No authenticated user!");
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = currentUser.getUid();
        Log.d(TAG, "✅ Current User ID: " + currentUserId);
        Log.d(TAG, "✅ User Email: " + currentUser.getEmail());

        // ✅ Log Firestore settings
        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = db.getFirestoreSettings();
        Log.d(TAG, "Firestore Cache Enabled: " + settings.isPersistenceEnabled());
        Log.d(TAG, "Firestore Host: " + settings.getHost());

        btnBack = findViewById(R.id.btnBack);
        viewPagerPosts = findViewById(R.id.viewPagerPosts);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Log.d(TAG, "Back button clicked");
                finish();
            });
        }

        adapter = new PostFullscreenAdapter(postList, this);
        viewPagerPosts.setAdapter(adapter);
        viewPagerPosts.setOrientation(ViewPager2.ORIENTATION_VERTICAL);

        Log.d(TAG, "ViewPager initialized");
        loadPosts();
    }

    private void loadPosts() {
        Log.d(TAG, "───────────────────────────────────────");
        Log.d(TAG, "📥 loadPosts() called");
        Log.d(TAG, "📥 Querying posts for userId: " + currentUserId);
        Log.d(TAG, "───────────────────────────────────────");

        // ✅ Remove old listener nếu có
        if (postListener != null) {
            Log.d(TAG, "⚠️ Removing old listener");
            postListener.remove();
        }

        // ✅ Use addSnapshotListener with MetadataChanges
        postListener = db.collection("posts")
                .whereArrayContains("visibleTo", currentUserId)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshots, error) -> {
                    Log.d(TAG, "───────────────────────────────────────");
                    Log.d(TAG, "📬 Snapshot listener triggered");

                    if (error != null) {
                        Log.e(TAG, "❌ ERROR loading posts", error);
                        Log.e(TAG, "❌ Error code: " + error.getClass().getSimpleName());
                        Log.e(TAG, "❌ Error message: " + error.getMessage());

                        // ✅ Check if it's a permission error
                        if (error.getMessage() != null &&
                                error.getMessage().contains("PERMISSION_DENIED")) {
                            Log.e(TAG, "❌ PERMISSION DENIED - Check Firebase Rules!");
                            Toast.makeText(this, "Lỗi quyền truy cập! Kiểm tra Firebase Rules", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Lỗi tải ảnh: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    if (snapshots == null) {
                        Log.w(TAG, "⚠️ Snapshots is null");
                        return;
                    }

                    // ✅ Log metadata
                    SnapshotMetadata metadata = snapshots.getMetadata();
                    Log.d(TAG, "📊 Snapshot metadata:");
                    Log.d(TAG, "   - From cache: " + metadata.isFromCache());
                    Log.d(TAG, "   - Has pending writes: " + metadata.hasPendingWrites());
                    Log.d(TAG, "   - Documents count: " + snapshots.size());

                    // ✅ Chỉ xử lý data từ server (tránh cache cũ)
                    if (!metadata.isFromCache()) {
                        Log.d(TAG, "✅ Processing data from SERVER");

                        postList.clear();
                        int validPosts = 0;

                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Log.d(TAG, "   📄 Document ID: " + doc.getId());

                            Post post = doc.toObject(Post.class);
                            if (post != null) {
                                post.postId = doc.getId();
                                postList.add(post);
                                validPosts++;

                                Log.d(TAG, "      ✅ Post added:");
                                Log.d(TAG, "         - postId: " + post.postId);
                                Log.d(TAG, "         - userId: " + post.userId);
                                Log.d(TAG, "         - imageUrl: " + (post.imageUrl != null ? "exists" : "null"));
                                Log.d(TAG, "         - timestamp: " + post.timestamp);
                            } else {
                                Log.w(TAG, "      ⚠️ Post is null for doc: " + doc.getId());
                            }
                        }

                        // ✅ Sắp xếp theo thời gian
                        postList.sort((p1, p2) -> {
                            if (p1.timestamp == null && p2.timestamp == null) return 0;
                            if (p1.timestamp == null) return 1;
                            if (p2.timestamp == null) return -1;
                            return p2.timestamp.compareTo(p1.timestamp);
                        });

                        Log.d(TAG, "✅ Total valid posts loaded: " + validPosts);
                        Log.d(TAG, "✅ PostList size: " + postList.size());

                        adapter.notifyDataSetChanged();
                        Log.d(TAG, "✅ Adapter notified");

                        if (postList.isEmpty()) {
                            Log.w(TAG, "⚠️ No posts found for user: " + currentUserId);
                            Toast.makeText(this, "Chưa có ảnh nào!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d(TAG, "⏳ Data from CACHE, waiting for server...");
                    }

                    Log.d(TAG, "───────────────────────────────────────");
                });

        Log.d(TAG, "✅ Snapshot listener registered");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "═══════════════════════════════════════");
        Log.d(TAG, "onDestroy() called");

        if (postListener != null) {
            postListener.remove();
            Log.d(TAG, "✅ Post listener removed");
        }

        Log.d(TAG, "═══════════════════════════════════════");
    }
}