package com.example.locketbaseapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.locketbaseapp.model.Chat;
import com.example.locketbaseapp.model.User;
import com.example.locketbaseapp.ui.ChatListAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatListActivity extends AppCompatActivity {

    private static final String TAG = "ChatListActivity";

    private RecyclerView rvChatList;
    private ChatListAdapter adapter;
    private List<Chat> chatList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "═══════════════════════════════════════");
        Log.d(TAG, "ChatListActivity onCreate() started");
        Log.d(TAG, "═══════════════════════════════════════");

        try {
            setContentView(R.layout.activity_chat_list);

            db = FirebaseFirestore.getInstance();
            FirebaseAuth auth = FirebaseAuth.getInstance();

            // ✅ Log Firestore settings
            FirebaseFirestoreSettings settings = db.getFirestoreSettings();
            Log.d(TAG, "Firestore Cache Enabled: " + settings.isPersistenceEnabled());
            Log.d(TAG, "Firestore Host: " + settings.getHost());

            // ✅ Check authentication
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser == null) {
                Log.e(TAG, "❌ ERROR: User not logged in!");
                Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            currentUserId = currentUser.getUid();
            Log.d(TAG, "✅ Current User ID: " + currentUserId);
            Log.d(TAG, "✅ User Email: " + currentUser.getEmail());

            btnBack = findViewById(R.id.btnBack);
            rvChatList = findViewById(R.id.rvChatList);

            if (btnBack == null) {
                Log.e(TAG, "❌ btnBack is null!");
            } else {
                btnBack.setOnClickListener(v -> {
                    Log.d(TAG, "Back button clicked");
                    finish();
                });
            }

            if (rvChatList == null) {
                Log.e(TAG, "❌ rvChatList is null!");
                return;
            }

            adapter = new ChatListAdapter(chatList, this, chat -> {
                Log.d(TAG, "───────────────────────────────────────");
                Log.d(TAG, "💬 Chat clicked:");
                Log.d(TAG, "   - Friend ID: " + chat.friendId);
                Log.d(TAG, "   - Friend Name: " + chat.friendName);
                Log.d(TAG, "   - Chat ID: " + chat.chatId);
                Log.d(TAG, "───────────────────────────────────────");

                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("friendId", chat.friendId);
                intent.putExtra("friendName", chat.friendName);
                intent.putExtra("friendPhoto", chat.friendPhoto);
                startActivity(intent);
            });

            rvChatList.setLayoutManager(new LinearLayoutManager(this));
            rvChatList.setAdapter(adapter);

            Log.d(TAG, "✅ RecyclerView setup complete");

            loadFriendsAndChats();

        } catch (Exception e) {
            Log.e(TAG, "❌ onCreate: Error", e);
            Log.e(TAG, "❌ Error message: " + e.getMessage());
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadFriendsAndChats() {
        Log.d(TAG, "═══════════════════════════════════════");
        Log.d(TAG, "📥 loadFriendsAndChats() started");
        Log.d(TAG, "📥 Loading friends for userId: " + currentUserId);
        Log.d(TAG, "═══════════════════════════════════════");

        try {
            // ✅ Bước 1: Load tất cả bạn bè với SOURCE.SERVER
            db.collection("users")
                    .document(currentUserId)
                    .collection("friends")
                    .get(Source.SERVER)  // ✅ Force từ server
                    .addOnSuccessListener(friendsSnapshot -> {
                        Log.d(TAG, "───────────────────────────────────────");
                        Log.d(TAG, "👥 Friends query SUCCESS");
                        Log.d(TAG, "   - Snapshot size: " + friendsSnapshot.size());
                        Log.d(TAG, "   - From cache: " + friendsSnapshot.getMetadata().isFromCache());

                        try {
                            List<User> friends = new ArrayList<>();

                            for (DocumentSnapshot doc : friendsSnapshot) {
                                Log.d(TAG, "   📄 Processing friend document:");
                                Log.d(TAG, "      - Doc ID: " + doc.getId());
                                Log.d(TAG, "      - Doc exists: " + doc.exists());

                                User friend = doc.toObject(User.class);
                                if (friend != null) {
                                    friend.uid = doc.getId();  // ✅ Set UID từ doc ID

                                    Log.d(TAG, "      ✅ Friend loaded:");
                                    Log.d(TAG, "         - UID: " + friend.uid);
                                    Log.d(TAG, "         - Display Name: " + friend.displayName);
                                    Log.d(TAG, "         - Email: " + friend.email);
                                    Log.d(TAG, "         - Photo: " + (friend.photoUrl != null ? "exists" : "null"));

                                    friends.add(friend);
                                } else {
                                    Log.e(TAG, "      ❌ Friend is null for doc: " + doc.getId());
                                }
                            }

                            Log.d(TAG, "✅ Total friends loaded: " + friends.size());

                            if (friends.isEmpty()) {
                                Log.w(TAG, "⚠️ No friends found!");
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Bạn chưa có bạn bè nào!", Toast.LENGTH_SHORT).show();
                                });
                                return;
                            }

                            // ✅ Bước 2: Load các chat
                            loadChatsAndMerge(friends);

                        } catch (Exception e) {
                            Log.e(TAG, "❌ Error processing friends", e);
                            Log.e(TAG, "❌ Error message: " + e.getMessage());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "───────────────────────────────────────");
                        Log.e(TAG, "❌ Friends query FAILED");
                        Log.e(TAG, "❌ Error: " + e.getClass().getSimpleName());
                        Log.e(TAG, "❌ Message: " + e.getMessage());

                        if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                            Log.e(TAG, "❌ PERMISSION DENIED - Check Firebase Rules!");
                            Log.e(TAG, "❌ Path: users/" + currentUserId + "/friends");
                        }

                        Log.e(TAG, "───────────────────────────────────────");

                        runOnUiThread(() -> {
                            Toast.makeText(this, "Lỗi tải bạn bè: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    });

        } catch (Exception e) {
            Log.e(TAG, "❌ loadFriendsAndChats: Exception", e);
            Log.e(TAG, "❌ Error message: " + e.getMessage());
        }
    }

    private void loadChatsAndMerge(List<User> friends) {
        Log.d(TAG, "───────────────────────────────────────");
        Log.d(TAG, "💬 loadChatsAndMerge() started");
        Log.d(TAG, "💬 Friends to merge: " + friends.size());

        try {
            db.collection("chats")
                    .whereArrayContains("participants", currentUserId)
                    .get(Source.SERVER)  // ✅ Force từ server
                    .addOnSuccessListener(chatsSnapshot -> {
                        Log.d(TAG, "───────────────────────────────────────");
                        Log.d(TAG, "💬 Chats query SUCCESS");
                        Log.d(TAG, "   - Snapshot size: " + chatsSnapshot.size());
                        Log.d(TAG, "   - From cache: " + chatsSnapshot.getMetadata().isFromCache());

                        try {
                            Map<String, Chat> existingChats = new HashMap<>();

                            for (DocumentSnapshot doc : chatsSnapshot) {
                                Log.d(TAG, "   📄 Processing chat document:");
                                Log.d(TAG, "      - Doc ID: " + doc.getId());

                                Chat chat = doc.toObject(Chat.class);
                                if (chat != null && chat.participants != null && chat.participants.size() >= 2) {
                                    chat.chatId = doc.getId();

                                    String friendId = chat.participants.get(0).equals(currentUserId)
                                            ? chat.participants.get(1)
                                            : chat.participants.get(0);

                                    existingChats.put(friendId, chat);

                                    Log.d(TAG, "      ✅ Chat loaded:");
                                    Log.d(TAG, "         - Chat ID: " + chat.chatId);
                                    Log.d(TAG, "         - With friend: " + friendId);
                                    Log.d(TAG, "         - Last message: " + chat.lastMessage);
                                } else {
                                    Log.w(TAG, "      ⚠️ Invalid chat data for doc: " + doc.getId());
                                }
                            }

                            Log.d(TAG, "✅ Total existing chats: " + existingChats.size());

                            // ✅ Merge friends with chats
                            chatList.clear();
                            for (User friend : friends) {
                                Chat chat;

                                if (existingChats.containsKey(friend.uid)) {
                                    chat = existingChats.get(friend.uid);
                                    Log.d(TAG, "   ♻️ Using existing chat for: " + friend.displayName);
                                } else {
                                    chat = new Chat();
                                    chat.chatId = generateChatId(currentUserId, friend.uid);
                                    chat.participants = new ArrayList<>();
                                    chat.participants.add(currentUserId);
                                    chat.participants.add(friend.uid);
                                    chat.lastMessage = "";
                                    chat.lastMessageTime = null;
                                    Log.d(TAG, "   ➕ Creating new chat for: " + friend.displayName);
                                }

                                chat.friendId = friend.uid;
                                chat.friendName = friend.displayName != null ? friend.displayName : friend.email;
                                chat.friendPhoto = friend.photoUrl;

                                chatList.add(chat);
                            }

                            Log.d(TAG, "✅ Final chat list size: " + chatList.size());

                            // ✅ Sắp xếp
                            chatList.sort((c1, c2) -> {
                                if (c1.lastMessageTime == null && c2.lastMessageTime == null) {
                                    return c1.friendName.compareTo(c2.friendName);
                                } else if (c1.lastMessageTime == null) {
                                    return 1;
                                } else if (c2.lastMessageTime == null) {
                                    return -1;
                                } else {
                                    return c2.lastMessageTime.compareTo(c1.lastMessageTime);
                                }
                            });

                            runOnUiThread(() -> {
                                adapter.notifyDataSetChanged();
                                Log.d(TAG, "✅ Adapter notified - UI updated");
                            });

                            Log.d(TAG, "───────────────────────────────────────");

                        } catch (Exception e) {
                            Log.e(TAG, "❌ Error merging chats", e);
                            Log.e(TAG, "❌ Error message: " + e.getMessage());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "───────────────────────────────────────");
                        Log.e(TAG, "❌ Chats query FAILED");
                        Log.e(TAG, "❌ Error: " + e.getClass().getSimpleName());
                        Log.e(TAG, "❌ Message: " + e.getMessage());

                        if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                            Log.e(TAG, "❌ PERMISSION DENIED - Check Firebase Rules!");
                            Log.e(TAG, "❌ Collection: chats");
                        }

                        Log.e(TAG, "───────────────────────────────────────");
                    });

        } catch (Exception e) {
            Log.e(TAG, "❌ loadChatsAndMerge: Exception", e);
            Log.e(TAG, "❌ Error message: " + e.getMessage());
        }
    }

    private String generateChatId(String userId1, String userId2) {
        String chatId = userId1.compareTo(userId2) < 0
                ? userId1 + "_" + userId2
                : userId2 + "_" + userId1;

        Log.d(TAG, "🆔 Generated Chat ID: " + chatId);
        Log.d(TAG, "   - User 1: " + userId1);
        Log.d(TAG, "   - User 2: " + userId2);

        return chatId;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "═══════════════════════════════════════");
        Log.d(TAG, "onDestroy() called");
        Log.d(TAG, "═══════════════════════════════════════");
    }
}