package com.example.locketbaseapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.locketbaseapp.model.Message;
import com.example.locketbaseapp.ui.MessageAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend, btnBack, btnMenu;
    private ImageView ivFriendAvatar;
    private TextView tvFriendName;
    private MessageAdapter adapter;
    private List<Message> messageList = new ArrayList<>();

    private FirebaseFirestore db;
    private String chatId;
    private String currentUserId;
    private String friendId;
    private String friendName;
    private String friendPhoto;

    private ListenerRegistration messageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        try {
            db = FirebaseFirestore.getInstance();
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            // Nhận thông tin từ Intent
            friendId = getIntent().getStringExtra("friendId");
            friendName = getIntent().getStringExtra("friendName");
            friendPhoto = getIntent().getStringExtra("friendPhoto");

            if (friendId == null || friendName == null) {
                Log.e(TAG, "Missing friend info!");
                Toast.makeText(this, "Lỗi: Thiếu thông tin bạn bè", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            chatId = generateChatId(currentUserId, friendId);
            Log.d(TAG, "Chat ID: " + chatId);

            // Setup UI
            btnBack = findViewById(R.id.btnBack);
            btnMenu = findViewById(R.id.btnMenu);
            ivFriendAvatar = findViewById(R.id.ivFriendAvatar);
            tvFriendName = findViewById(R.id.tvFriendName);
            rvMessages = findViewById(R.id.rvMessages);
            etMessage = findViewById(R.id.etMessage);
            btnSend = findViewById(R.id.btnSend);

            btnBack.setOnClickListener(v -> finish());
            btnMenu.setOnClickListener(v -> {
                // TODO: Show menu options
            });

            tvFriendName.setText(friendName);

            if (friendPhoto != null && !friendPhoto.isEmpty()) {
                Glide.with(this)
                        .load(friendPhoto)
                        .circleCrop()
                        .into(ivFriendAvatar);
            } else {
                ivFriendAvatar.setImageResource(R.drawable.ic_person_circle);
            }

            adapter = new MessageAdapter(messageList, currentUserId);
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setStackFromEnd(true);
            rvMessages.setLayoutManager(layoutManager);
            rvMessages.setAdapter(adapter);

            // ← QUAN TRỌNG: Focus vào EditText để hiện bàn phím
            etMessage.setOnClickListener(v -> {
                etMessage.requestFocus();
                showKeyboard();
            });

            // ← XỬ LÝ GỬI TIN NHẮN
            btnSend.setOnClickListener(v -> sendMessage());

            // ← GỬI KHI NHẤN ENTER TRÊN BÀN PHÍM
            etMessage.setOnEditorActionListener((v, actionId, event) -> {
                sendMessage();
                return true;
            });

            createChatIfNotExists();
            listenToMessages();

        } catch (Exception e) {
            Log.e(TAG, "onCreate: Error", e);
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private String generateChatId(String userId1, String userId2) {
        return userId1.compareTo(userId2) < 0
                ? userId1 + "_" + userId2
                : userId2 + "_" + userId1;
    }

    private void createChatIfNotExists() {
        db.collection("chats").document(chatId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Map<String, Object> chatData = new HashMap<>();
                        chatData.put("participants", Arrays.asList(currentUserId, friendId));
                        chatData.put("lastMessage", "");
                        chatData.put("lastMessageTime", FieldValue.serverTimestamp());

                        db.collection("chats").document(chatId).set(chatData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Chat created: " + chatId);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error creating chat", e);
                                });
                    }
                });
    }

    private void listenToMessages() {
        messageListener = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed", error);
                        return;
                    }

                    messageList.clear();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Message msg = doc.toObject(Message.class);
                            if (msg != null) {
                                msg.messageId = doc.getId();
                                messageList.add(msg);
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (messageList.size() > 0) {
                        rvMessages.smoothScrollToPosition(messageList.size() - 1);
                    }

                    Log.d(TAG, "Messages loaded: " + messageList.size());
                });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();

        if (text.isEmpty()) {
            Log.d(TAG, "Empty message, not sending");
            return;
        }

        Log.d(TAG, "Sending message: " + text);

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderId", currentUserId);
        messageData.put("text", text);
        messageData.put("timestamp", FieldValue.serverTimestamp());
        messageData.put("isRead", false);
        messageData.put("imageUrl", "");

        db.collection("chats").document(chatId)
                .collection("messages")
                .add(messageData)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Message sent: " + docRef.getId());

                    // Cập nhật lastMessage trong chat
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("lastMessage", text);
                    updateData.put("lastMessageTime", FieldValue.serverTimestamp());

                    db.collection("chats").document(chatId).update(updateData);

                    etMessage.setText("");
                    hideKeyboard();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message", e);
                    Toast.makeText(this, "Lỗi gửi tin nhắn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && etMessage != null) {
            etMessage.requestFocus();
            imm.showSoftInput(etMessage, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
        }
    }
}