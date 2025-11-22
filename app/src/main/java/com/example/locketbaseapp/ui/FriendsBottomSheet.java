package com.example.locketbaseapp.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.locketbaseapp.R;
import com.example.locketbaseapp.SearchActivity;
import com.example.locketbaseapp.model.User;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FriendsBottomSheet extends BottomSheetDialogFragment {

    private RecyclerView rvFriendsList;
    private RecyclerView rvFriendRequests;
    private RecyclerView rvSentRequests;  // MỚI THÊM
    private RecyclerView rvBlockedUsers;

    private LinearLayout searchBox;
    private LinearLayout btnSeeMore;
    private LinearLayout friendRequestSection;
    private LinearLayout sentRequestsSection;  // MỚI THÊM
    private LinearLayout blockedUsersSection;

    private TextView tvSeeMore;
    private TextView tvTitle;
    private TextView tvBlockedCount;
    private TextView tvFriendsCount;

    private FirestoreFriendAdapter friendAdapter;
    private FirestoreFriendAdapter requestAdapter;
    private FirestoreFriendAdapter sentAdapter;  // MỚI THÊM
    private FirestoreFriendAdapter blockedAdapter;

    private List<User> allFriends = new ArrayList<>();
    private List<User> displayedFriends = new ArrayList<>();
    private List<User> friendRequests = new ArrayList<>();
    private List<User> sentRequests = new ArrayList<>();  // MỚI THÊM
    private List<User> blockedUsers = new ArrayList<>();

    private boolean isExpanded = false;
    private static final int MAX_INITIAL_FRIENDS = 3;

    @SuppressLint("MissingInflatedId")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.Theme_LocketClone_BottomSheet);
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends_bottom_sheet, container, false);
        loadFriendsCount();
        // Khởi tạo views
        rvFriendsList = view.findViewById(R.id.rvFriendsList);
        rvFriendRequests = view.findViewById(R.id.rvFriendRequests);
        rvSentRequests = view.findViewById(R.id.rvSentRequests);  // MỚI THÊM
        rvBlockedUsers = view.findViewById(R.id.rvBlockedUsers);

        searchBox = view.findViewById(R.id.searchBox);
        btnSeeMore = view.findViewById(R.id.btnSeeMore);
        friendRequestSection = view.findViewById(R.id.friendRequestSection);
        sentRequestsSection = view.findViewById(R.id.sentRequestsSection);  // MỚI THÊM
        blockedUsersSection = view.findViewById(R.id.blockedUsersSection);

        tvSeeMore = view.findViewById(R.id.tvSeeMore);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvBlockedCount = view.findViewById(R.id.tvBlockedCount);
        tvFriendsCount=view.findViewById(R.id.tvFriendsCount);

        // Setup RecyclerView cho danh sách bạn bè
        rvFriendsList.setLayoutManager(new LinearLayoutManager(getContext()));
        friendAdapter = new FirestoreFriendAdapter(
                displayedFriends,
                getContext(),
                FirestoreFriendAdapter.Mode.FRIENDS,
                createFriendsCallback()
        );
        rvFriendsList.setAdapter(friendAdapter);

        // Setup RecyclerView cho yêu cầu kết bạn
        rvFriendRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        requestAdapter = new FirestoreFriendAdapter(
                friendRequests,
                getContext(),
                FirestoreFriendAdapter.Mode.INCOMING,
                createRequestsCallback()
        );
        rvFriendRequests.setAdapter(requestAdapter);

        // Setup RecyclerView cho yêu cầu đã gửi - MỚI THÊM
        rvSentRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        sentAdapter = new FirestoreFriendAdapter(
                sentRequests,
                getContext(),
                FirestoreFriendAdapter.Mode.SENT,
                createSentCallback()
        );
        rvSentRequests.setAdapter(sentAdapter);

        // Setup RecyclerView cho danh sách đã chặn
        rvBlockedUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        blockedAdapter = new FirestoreFriendAdapter(
                blockedUsers,
                getContext(),
                FirestoreFriendAdapter.Mode.BLOCKED,
                createBlockedCallback()
        );
        rvBlockedUsers.setAdapter(blockedAdapter);

        searchBox.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SearchActivity.class);
            startActivity(intent);
        });

        btnSeeMore.setOnClickListener(v -> toggleFriendsList());

        // Load dữ liệu
        loadFriends();
        loadFriendRequests();
        loadSentRequests();  // MỚI THÊM
        loadBlockedUsers();

        return view;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set background transparent cho dialog để bỏ rìa trắng
        View parent = (View) view.getParent();
        parent.setBackgroundColor(Color.TRANSPARENT);
    }

    private void loadFriendsCount() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d("HomeActivity", "Loading friends count for user: " + currentUserId);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(currentUserId)
                .collection("friends")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    Log.d("HomeActivity", "Friends count: " + count);
                    if (count == 0) {
                        tvFriendsCount.setText("Chưa có bạn bè");
                    } else {
                        tvFriendsCount.setText("Bạn bè của bạn ("+ count + ")");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeActivity", "Error loading friends count", e);
                    tvFriendsCount.setText("... bạn bè");
                });
    }

    // ========== CALLBACKS ==========

    private FirestoreFriendAdapter.Callback createFriendsCallback() {
        return new FirestoreFriendAdapter.Callback() {
            @Override public void onAccept(User user) { }
            @Override public void onDecline(User user) { }
            @Override public void onCancel(User user) { }
            @Override public void onRemove(User user) { removeFriend(user); }
            @Override public void onSendRequest(User user) { }
            @Override public void onBlock(User user) { blockUser(user); }
            @Override public void onUnblock(User user) { }
        };
    }

    private FirestoreFriendAdapter.Callback createRequestsCallback() {
        return new FirestoreFriendAdapter.Callback() {
            @Override public void onAccept(User user) { acceptFriendRequest(user); }
            @Override public void onDecline(User user) { declineFriendRequest(user); }
            @Override public void onCancel(User user) { }
            @Override public void onRemove(User user) { }
            @Override public void onSendRequest(User user) { }
            @Override public void onBlock(User user) { }
            @Override public void onUnblock(User user) { }
        };
    }

    // MỚI THÊM: Callback cho Sent Requests
    private FirestoreFriendAdapter.Callback createSentCallback() {
        return new FirestoreFriendAdapter.Callback() {
            @Override public void onAccept(User user) { }
            @Override public void onDecline(User user) { }
            @Override public void onCancel(User user) { cancelSentRequest(user); }  // NÚT HỦY
            @Override public void onRemove(User user) { }
            @Override public void onSendRequest(User user) { }
            @Override public void onBlock(User user) { }
            @Override public void onUnblock(User user) { }
        };
    }

    private FirestoreFriendAdapter.Callback createBlockedCallback() {
        return new FirestoreFriendAdapter.Callback() {
            @Override public void onAccept(User user) { }
            @Override public void onDecline(User user) { }
            @Override public void onCancel(User user) { }
            @Override public void onRemove(User user) { }
            @Override public void onSendRequest(User user) { }
            @Override public void onBlock(User user) { }
            @Override public void onUnblock(User user) { unblockUser(user); }
        };
    }

    // ========== LOAD DATA ==========

    private void loadFriends() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        Log.d("FriendsBottomSheet", "═══════════════════════════════════════");
        Log.d("FriendsBottomSheet", "📥 loadFriends() started");
        Log.d("FriendsBottomSheet", "   User ID: " + currentUserId);
        Log.d("FriendsBottomSheet", "═══════════════════════════════════════");

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(currentUserId)
                .collection("friends")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("FriendsBottomSheet", "───────────────────────────────────────");
                    Log.d("FriendsBottomSheet", "✅ Friends query SUCCESS");
                    Log.d("FriendsBottomSheet", "   - Snapshot size: " + queryDocumentSnapshots.size());
                    Log.d("FriendsBottomSheet", "   - From cache: " + queryDocumentSnapshots.getMetadata().isFromCache());

                    allFriends.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User friend = document.toObject(User.class);
                        if (friend != null) {
                            friend.uid = document.getId();  // ✅ Ensure UID is set
                            Log.d("FriendsBottomSheet", "   ✅ Friend loaded: " + friend.displayName + " (UID: " + friend.uid + ")");
                            allFriends.add(friend);
                        } else {
                            Log.w("FriendsBottomSheet", "   ⚠️ Friend is null for doc: " + document.getId());
                        }
                    }
                    
                    Log.d("FriendsBottomSheet", "✅ Total friends: " + allFriends.size());
                    Log.d("FriendsBottomSheet", "───────────────────────────────────────");
                    
                    updateDisplayedFriends();
                    updateTitle();
                })
                .addOnFailureListener(e -> {
                    Log.e("FriendsBottomSheet", "───────────────────────────────────────");
                    Log.e("FriendsBottomSheet", "❌ Friends query FAILED");
                    Log.e("FriendsBottomSheet", "   - Error class: " + e.getClass().getSimpleName());
                    Log.e("FriendsBottomSheet", "   - Error message: " + e.getMessage());
                    Log.e("FriendsBottomSheet", "   - Path: /users/" + currentUserId + "/friends");

                    if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                        Log.e("FriendsBottomSheet", "❌ PERMISSION_DENIED - Check Firestore rules!");
                    }

                    Log.e("FriendsBottomSheet", "───────────────────────────────────────");
                    Toast.makeText(getContext(), "Lỗi tải danh sách bạn bè: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void loadFriendRequests() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Log.d("FriendsBottomSheet", "═══════════════════════════════════════");
        Log.d("FriendsBottomSheet", "📨 loadFriendRequests() started");
        Log.d("FriendsBottomSheet", "   User ID: " + currentUserId);

        db.collection("users")
                .document(currentUserId)
                .collection("friendRequests")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("FriendsBottomSheet", "───────────────────────────────────────");
                    Log.d("FriendsBottomSheet", "✅ Friend requests query SUCCESS");
                    Log.d("FriendsBottomSheet", "   - Count: " + queryDocumentSnapshots.size());

                    friendRequests.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            user.uid = document.getId();
                            Log.d("FriendsBottomSheet", "   ✅ Request from: " + user.displayName + " (UID: " + user.uid + ")");
                            friendRequests.add(user);
                        }
                    }

                    if (!friendRequests.isEmpty()) {
                        friendRequestSection.setVisibility(View.VISIBLE);
                        requestAdapter.notifyDataSetChanged();
                        Log.d("FriendsBottomSheet", "✅ Friend requests section visible");
                    } else {
                        friendRequestSection.setVisibility(View.GONE);
                        Log.d("FriendsBottomSheet", "ℹ️ No friend requests");
                    }

                    Log.d("FriendsBottomSheet", "───────────────────────────────────────");
                })
                .addOnFailureListener(e -> {
                    Log.e("FriendsBottomSheet", "───────────────────────────────────────");
                    Log.e("FriendsBottomSheet", "❌ Friend requests query FAILED");
                    Log.e("FriendsBottomSheet", "   - Error: " + e.getMessage());
                    Log.e("FriendsBottomSheet", "───────────────────────────────────────");
                    Toast.makeText(getContext(), "Lỗi tải yêu cầu kết bạn: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // MỚI THÊM: Load Sent Requests
    private void loadSentRequests() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Log.d("FriendsBottomSheet", "═══════════════════════════════════════");
        Log.d("FriendsBottomSheet", "📤 loadSentRequests() started");
        Log.d("FriendsBottomSheet", "   User ID: " + currentUserId);

        db.collection("users")
                .document(currentUserId)
                .collection("sentRequests")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("FriendsBottomSheet", "───────────────────────────────────────");
                    Log.d("FriendsBottomSheet", "✅ Sent requests query SUCCESS");
                    Log.d("FriendsBottomSheet", "   - Count: " + queryDocumentSnapshots.size());

                    sentRequests.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            user.uid = document.getId();
                            Log.d("FriendsBottomSheet", "   ✅ Sent request to: " + user.displayName + " (UID: " + user.uid + ")");
                            sentRequests.add(user);
                        }
                    }

                    if (!sentRequests.isEmpty()) {
                        sentRequestsSection.setVisibility(View.VISIBLE);
                        sentAdapter.notifyDataSetChanged();
                        Log.d("FriendsBottomSheet", "✅ Sent requests section visible");
                    } else {
                        sentRequestsSection.setVisibility(View.GONE);
                        Log.d("FriendsBottomSheet", "ℹ️ No sent requests");
                    }

                    Log.d("FriendsBottomSheet", "───────────────────────────────────────");
                })
                .addOnFailureListener(e -> {
                    Log.e("FriendsBottomSheet", "───────────────────────────────────────");
                    Log.e("FriendsBottomSheet", "❌ Sent requests query FAILED");
                    Log.e("FriendsBottomSheet", "   - Error: " + e.getMessage());
                    Log.e("FriendsBottomSheet", "───────────────────────────────────────");
                    Toast.makeText(getContext(), "Lỗi tải yêu cầu đã gửi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void loadBlockedUsers() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(currentUserId)
                .collection("blockedUsers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    blockedUsers.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        blockedUsers.add(user);
                    }

                    if (!blockedUsers.isEmpty()) {
                        blockedUsersSection.setVisibility(View.VISIBLE);
                        tvBlockedCount.setText(String.valueOf(blockedUsers.size()));
                        blockedAdapter.notifyDataSetChanged();
                    } else {
                        blockedUsersSection.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FriendsBottomSheet", "Error loading blocked users", e);
                });
    }

    private void updateDisplayedFriends() {
        displayedFriends.clear();

        int displayCount = isExpanded ? allFriends.size() : Math.min(MAX_INITIAL_FRIENDS, allFriends.size());

        for (int i = 0; i < displayCount; i++) {
            displayedFriends.add(allFriends.get(i));
        }

        friendAdapter.notifyDataSetChanged();

        if (allFriends.size() > MAX_INITIAL_FRIENDS) {
            btnSeeMore.setVisibility(View.VISIBLE);
            updateSeeMoreText();
        } else {
            btnSeeMore.setVisibility(View.GONE);
        }
    }

    private void toggleFriendsList() {
        isExpanded = !isExpanded;
        updateDisplayedFriends();
    }

    private void updateSeeMoreText() {
        if (isExpanded) {
            tvSeeMore.setText("Thu gọn");
        } else {
            int remainingCount = allFriends.size() - MAX_INITIAL_FRIENDS;
            tvSeeMore.setText("Xem thêm (" + remainingCount + ")");
        }
    }

    private void updateTitle() {
        int count = allFriends.size();
        if (count == 0) {
            tvTitle.setText("Bạn bè");
        } else {
            tvTitle.setText(count + " bạn bè");
        }
    }

    // ========== FRIEND REQUEST ACTIONS ==========

    private void acceptFriendRequest(User user) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Log.d("FriendsBottomSheet", "Accepting friend request from: " + user.uid);

        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User myUser = documentSnapshot.toObject(User.class);
                    if (myUser == null) {
                        Toast.makeText(getContext(), "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("users")
                            .document(currentUserId)
                            .collection("friends")
                            .document(user.uid)
                            .set(user)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("FriendsBottomSheet", "Added friend to my list");

                                db.collection("users")
                                        .document(user.uid)
                                        .collection("friends")
                                        .document(currentUserId)
                                        .set(myUser)
                                        .addOnSuccessListener(aVoid1 -> {
                                            Log.d("FriendsBottomSheet", "Added me to friend's list");

                                            db.collection("users")
                                                    .document(currentUserId)
                                                    .collection("friendRequests")
                                                    .document(user.uid)
                                                    .delete()
                                                    .addOnSuccessListener(aVoid2 -> {
                                                        Log.d("FriendsBottomSheet", "Deleted from my friendRequests");

                                                        db.collection("users")
                                                                .document(user.uid)
                                                                .collection("sentRequests")
                                                                .document(currentUserId)
                                                                .delete()
                                                                .addOnSuccessListener(aVoid3 -> {
                                                                    Log.d("FriendsBottomSheet", "Deleted from friend's sentRequests");

                                                                    Toast.makeText(getContext(), "Đã chấp nhận lời mời kết bạn", Toast.LENGTH_SHORT).show();
                                                                    loadFriends();
                                                                    loadFriendRequests();
                                                                    loadSentRequests();  // refresh sent requests
                                                                })
                                                                .addOnFailureListener(e -> {
                                                                    Log.e("FriendsBottomSheet", "Error deleting sentRequests", e);
                                                                });
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("FriendsBottomSheet", "Error adding to friend's list", e);
                                            Toast.makeText(getContext(), "Lỗi thêm bạn bè: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FriendsBottomSheet", "Error adding friend", e);
                                Toast.makeText(getContext(), "Lỗi chấp nhận yêu cầu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("FriendsBottomSheet", "Error getting user info", e);
                    Toast.makeText(getContext(), "Lỗi lấy thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void declineFriendRequest(User user) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Log.d("FriendsBottomSheet", "Declining friend request from: " + user.uid);

        db.collection("users")
                .document(currentUserId)
                .collection("friendRequests")
                .document(user.uid)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("FriendsBottomSheet", "Deleted from my friendRequests");

                    db.collection("users")
                            .document(user.uid)
                            .collection("sentRequests")
                            .document(currentUserId)
                            .delete()
                            .addOnSuccessListener(aVoid1 -> {
                                Log.d("FriendsBottomSheet", "Deleted from their sentRequests");
                                Toast.makeText(getContext(), "Đã từ chối lời mời kết bạn", Toast.LENGTH_SHORT).show();
                                loadFriendRequests();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FriendsBottomSheet", "Error deleting sentRequests", e);
                                Toast.makeText(getContext(), "Lỗi từ chối yêu cầu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("FriendsBottomSheet", "Error declining request", e);
                    Toast.makeText(getContext(), "Lỗi từ chối yêu cầu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // MỚI THÊM: Cancel Sent Request (NÚT HỦY)
    private void cancelSentRequest(User user) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Log.d("FriendsBottomSheet", "Cancelling sent request to: " + user.uid);

        db.collection("users")
                .document(currentUserId)
                .collection("sentRequests")
                .document(user.uid)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("FriendsBottomSheet", "Deleted from my sentRequests");

                    db.collection("users")
                            .document(user.uid)
                            .collection("friendRequests")
                            .document(currentUserId)
                            .delete()
                            .addOnSuccessListener(aVoid1 -> {
                                Log.d("FriendsBottomSheet", "Deleted from their friendRequests");
                                Toast.makeText(getContext(), "Đã hủy yêu cầu kết bạn", Toast.LENGTH_SHORT).show();
                                loadSentRequests();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FriendsBottomSheet", "Error deleting their friendRequests", e);
                                Toast.makeText(getContext(), "Lỗi hủy yêu cầu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("FriendsBottomSheet", "Error cancelling sent request", e);
                    Toast.makeText(getContext(), "Lỗi hủy yêu cầu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ========== FRIEND MANAGEMENT ==========

    private void removeFriend(User user) {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Xóa bạn bè")
                .setMessage("Bạn có chắc chắn muốn xóa " + user.displayName + " khỏi danh sách bạn bè?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    performRemoveFriend(user);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performRemoveFriend(User user) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(currentUserId)
                .collection("friends")
                .document(user.uid)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("FriendsBottomSheet", "Removed friend from my list");

                    db.collection("users")
                            .document(user.uid)
                            .collection("friends")
                            .document(currentUserId)
                            .delete()
                            .addOnSuccessListener(aVoid1 -> {
                                Log.d("FriendsBottomSheet", "Removed me from friend's list");
                                Toast.makeText(getContext(), "Đã xóa bạn bè", Toast.LENGTH_SHORT).show();
                                loadFriends();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FriendsBottomSheet", "Error removing from friend's list", e);
                                Toast.makeText(getContext(), "Lỗi xóa bạn bè: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("FriendsBottomSheet", "Error removing friend", e);
                    Toast.makeText(getContext(), "Lỗi xóa bạn bè: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void blockUser(User user) {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Chặn người dùng")
                .setMessage("Bạn có chắc chắn muốn chặn " + user.displayName + "?\n\nNgười này sẽ không thể gửi lời mời kết bạn cho bạn nữa.")
                .setPositiveButton("Chặn", (dialog, which) -> {
                    performBlockUser(user);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performBlockUser(User user) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Log.d("FriendsBottomSheet", "Blocking user: " + user.uid);

        db.collection("users")
                .document(currentUserId)
                .collection("friends")
                .document(user.uid)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("FriendsBottomSheet", "Removed from my friends");
                });

        db.collection("users")
                .document(user.uid)
                .collection("friends")
                .document(currentUserId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("FriendsBottomSheet", "Removed from their friends");
                });

        db.collection("users")
                .document(currentUserId)
                .collection("friendRequests")
                .document(user.uid)
                .delete();

        db.collection("users")
                .document(currentUserId)
                .collection("sentRequests")
                .document(user.uid)
                .delete();

        db.collection("users")
                .document(user.uid)
                .collection("friendRequests")
                .document(currentUserId)
                .delete();

        db.collection("users")
                .document(user.uid)
                .collection("sentRequests")
                .document(currentUserId)
                .delete();

        db.collection("users")
                .document(currentUserId)
                .collection("blockedUsers")
                .document(user.uid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FriendsBottomSheet", "Added to blockedUsers");
                    Toast.makeText(getContext(), "Đã chặn " + user.displayName, Toast.LENGTH_SHORT).show();
                    loadFriends();
                    loadFriendRequests();
                    loadSentRequests();  // refresh sent requests
                    loadBlockedUsers();
                })
                .addOnFailureListener(e -> {
                    Log.e("FriendsBottomSheet", "Error blocking user", e);
                    Toast.makeText(getContext(), "Lỗi chặn người dùng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void unblockUser(User user) {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Gỡ chặn")
                .setMessage("Bạn có chắc chắn muốn gỡ chặn " + user.displayName + "?")
                .setPositiveButton("Gỡ chặn", (dialog, which) -> {
                    performUnblockUser(user);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performUnblockUser(User user) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(currentUserId)
                .collection("blockedUsers")
                .document(user.uid)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Đã gỡ chặn " + user.displayName, Toast.LENGTH_SHORT).show();
                    loadBlockedUsers();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi gỡ chặn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}