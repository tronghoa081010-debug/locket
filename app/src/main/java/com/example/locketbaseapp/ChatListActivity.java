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

        try {
            Log.d(TAG, "onCreate: Starting ChatListActivity");

            setContentView(R.layout.activity_chat_list);

            db = FirebaseFirestore.getInstance();
            FirebaseAuth auth = FirebaseAuth.getInstance();

            if (auth.getCurrentUser() == null) {
                Log.e(TAG, "User not logged in!");
                Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            currentUserId = auth.getCurrentUser().getUid();
            Log.d(TAG, "Current User ID: " + currentUserId);

            btnBack = findViewById(R.id.btnBack);
            rvChatList = findViewById(R.id.rvChatList);

            if (btnBack == null) {
                Log.e(TAG, "btnBack is null!");
            } else {
                btnBack.setOnClickListener(v -> finish());
            }

            if (rvChatList == null) {
                Log.e(TAG, "rvChatList is null!");
                return;
            }

            adapter = new ChatListAdapter(chatList, this, chat -> {
                Log.d(TAG, "Chat clicked: " + chat.friendName);

                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("friendId", chat.friendId);
                intent.putExtra("friendName", chat.friendName);
                intent.putExtra("friendPhoto", chat.friendPhoto);
                startActivity(intent);
            });

            rvChatList.setLayoutManager(new LinearLayoutManager(this));
            rvChatList.setAdapter(adapter);

            Log.d(TAG, "RecyclerView setup complete");

            loadFriendsAndChats();

        } catch (Exception e) {
            Log.e(TAG, "onCreate: Error", e);
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadFriendsAndChats() {
        try {
            Log.d(TAG, "=== START LOADING ===");

            // Bước 1: Load tất cả bạn bè
            db.collection("users")
                    .document(currentUserId)
                    .collection("friends")
                    .get()
                    .addOnSuccessListener(friendsSnapshot -> {
                        try {
                            List<User> friends = new ArrayList<>();

                            Log.d(TAG, "Friends snapshot size: " + friendsSnapshot.size());

                            for (DocumentSnapshot doc : friendsSnapshot) {
                                User friend = doc.toObject(User.class);
                                if (friend != null) {
                                    Log.d(TAG, "Friend found: " + friend.displayName + " (" + friend.uid + ")");
                                    friends.add(friend);
                                } else {
                                    Log.e(TAG, "Friend is null for doc: " + doc.getId());
                                }
                            }

                            Log.d(TAG, "Total friends loaded: " + friends.size());

                            if (friends.isEmpty()) {
                                Log.w(TAG, "No friends found!");
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Bạn chưa có bạn bè nào!", Toast.LENGTH_SHORT).show();
                                });
                                return;
                            }

                            // Bước 2: Load các chat
                            loadChatsAndMerge(friends);

                        } catch (Exception e) {
                            Log.e(TAG, "Error processing friends", e);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading friends", e);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Lỗi tải bạn bè: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    });

        } catch (Exception e) {
            Log.e(TAG, "loadFriendsAndChats: Error", e);
        }
    }

    private void loadChatsAndMerge(List<User> friends) {
        try {
            db.collection("chats")
                    .whereArrayContains("participants", currentUserId)
                    .get()
                    .addOnSuccessListener(chatsSnapshot -> {
                        try {
                            Map<String, Chat> existingChats = new HashMap<>();

                            Log.d(TAG, "Chats snapshot size: " + chatsSnapshot.size());

                            for (DocumentSnapshot doc : chatsSnapshot) {
                                Chat chat = doc.toObject(Chat.class);
                                if (chat != null && chat.participants != null && chat.participants.size() >= 2) {
                                    chat.chatId = doc.getId();

                                    String friendId = chat.participants.get(0).equals(currentUserId)
                                            ? chat.participants.get(1)
                                            : chat.participants.get(0);

                                    existingChats.put(friendId, chat);
                                    Log.d(TAG, "Existing chat with: " + friendId);
                                }
                            }

                            Log.d(TAG, "Total existing chats: " + existingChats.size());

                            // Merge
                            chatList.clear();
                            for (User friend : friends) {
                                Chat chat;

                                if (existingChats.containsKey(friend.uid)) {
                                    chat = existingChats.get(friend.uid);
                                    Log.d(TAG, "Using existing chat for: " + friend.displayName);
                                } else {
                                    chat = new Chat();
                                    chat.chatId = generateChatId(currentUserId, friend.uid);
                                    chat.participants = new ArrayList<>();
                                    chat.participants.add(currentUserId);
                                    chat.participants.add(friend.uid);
                                    chat.lastMessage = "";
                                    chat.lastMessageTime = null;
                                    Log.d(TAG, "Creating new chat for: " + friend.displayName);
                                }

                                chat.friendId = friend.uid;
                                chat.friendName = friend.displayName != null ? friend.displayName : friend.email;
                                chat.friendPhoto = friend.photoUrl;

                                chatList.add(chat);
                            }

                            Log.d(TAG, "Final chat list size: " + chatList.size());

                            // Sắp xếp
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
                                Log.d(TAG, "Adapter notified");
                            });

                        } catch (Exception e) {
                            Log.e(TAG, "Error merging chats", e);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading chats", e);
                    });

        } catch (Exception e) {
            Log.e(TAG, "loadChatsAndMerge: Error", e);
        }
    }

    private String generateChatId(String userId1, String userId2) {
        return userId1.compareTo(userId2) < 0
                ? userId1 + "_" + userId2
                : userId2 + "_" + userId1;
    }
}