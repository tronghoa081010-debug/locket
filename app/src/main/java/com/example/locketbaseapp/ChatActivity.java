package com.example.locketbaseapp;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.locketbaseapp.model.Message;
import com.example.locketbaseapp.ui.MessageAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.widget.PopupWindow;
import android.view.ViewGroup;
import android.view.Gravity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import androidx.appcompat.app.AlertDialog;
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

            // ← MENU: Xóa bạn, Báo cáo, Chặn
            btnMenu.setOnClickListener(v -> showMenuDialog());

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

            // ← FIX 1: Click vào EditText → Hiện bàn phím
            etMessage.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    showKeyboard();
                }
            });

            etMessage.setOnClickListener(v -> {
                etMessage.requestFocus();
                showKeyboard();
            });

            btnSend.setOnClickListener(v -> sendMessage());

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

    // ← MENU DIALOG
    private void showMenuDialog() {
        // Inflate custom layout
        View popupView = getLayoutInflater().inflate(R.layout.popup_chat_menu, null);

        // Tạo PopupWindow
        PopupWindow popupWindow = new PopupWindow(
                popupView,
                (int) (280 * getResources().getDisplayMetrics().density), // 280dp converted to pixels
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true  // focusable = true để dismiss khi click bên ngoài
        );

        // Set background trong suốt để hiện được rounded corners
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(12);
        popupWindow.setOutsideTouchable(true);

        // Animation khi hiện/ẩn
        popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);

        // Tìm views
        ImageView ivPopupAvatar = popupView.findViewById(R.id.ivPopupAvatar);
        TextView tvPopupName = popupView.findViewById(R.id.tvPopupName);
        LinearLayout btnPopupUnfriend = popupView.findViewById(R.id.btnPopupUnfriend);
        LinearLayout btnPopupReport = popupView.findViewById(R.id.btnPopupReport);
        LinearLayout btnPopupBlock = popupView.findViewById(R.id.btnPopupBlock);

        // Set thông tin friend
        tvPopupName.setText(friendName);
        if (friendPhoto != null && !friendPhoto.isEmpty()) {
            Glide.with(this).load(friendPhoto).circleCrop().into(ivPopupAvatar);
        } else {
            ivPopupAvatar.setImageResource(R.drawable.ic_person_circle);
        }

        // Click listeners
        btnPopupUnfriend.setOnClickListener(v -> {
            popupWindow.dismiss();
            unfriend();
        });

        btnPopupReport.setOnClickListener(v -> {
            popupWindow.dismiss();
            reportUser();
        });

        btnPopupBlock.setOnClickListener(v -> {
            popupWindow.dismiss();
            blockUser();
        });

        // Tính toán vị trí hiển thị (bên dưới nút 3 chấm, căn phải)
        int[] location = new int[2];
        btnMenu.getLocationOnScreen(location);

        int xOffset = location[0] - (int) (280 * getResources().getDisplayMetrics().density) + btnMenu.getWidth();
        int yOffset = location[1] + btnMenu.getHeight() + (int) (8 * getResources().getDisplayMetrics().density);

        // Hiển thị popup
        popupWindow.showAtLocation(btnMenu, Gravity.NO_GRAVITY, xOffset, yOffset);
    }

    private void unfriend() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bạn")
                .setMessage("Bạn có chắc muốn xóa " + friendName + " khỏi danh sách bạn bè?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // Xóa friend từ cả 2 phía
                    db.collection("users").document(currentUserId)
                            .collection("friends").document(friendId).delete()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Deleted friend from current user");
                            });

                    db.collection("users").document(friendId)
                            .collection("friends").document(currentUserId).delete()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Deleted current user from friend");
                            });

                    Toast.makeText(this, "Đã xóa " + friendName, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void reportUser() {
        // Tạo report document
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("reporterId", currentUserId);
        reportData.put("reportedUserId", friendId);
        reportData.put("reportedUserName", friendName);
        reportData.put("reason", "Reported from chat");
        reportData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("reports").add(reportData)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Báo cáo đã được gửi", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Report created: " + docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi gửi báo cáo", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error creating report", e);
                });
    }


    private void blockUser() {
        new AlertDialog.Builder(this)
                .setTitle("Chặn người dùng")
                .setMessage("Bạn sẽ không nhận được tin nhắn từ " + friendName + " nữa.")
                .setPositiveButton("Chặn", (dialog, which) -> {
                    Map<String, Object> blockData = new HashMap<>();
                    blockData.put("blockedAt", FieldValue.serverTimestamp());
                    blockData.put("blockedUserName", friendName);

                    db.collection("users").document(currentUserId)
                            .collection("blockedUsers").document(friendId)
                            .set(blockData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Đã chặn " + friendName, Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Blocked user: " + friendId);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Lỗi chặn người dùng", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Error blocking user", e);
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
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

                        db.collection("chats").document(chatId).set(chatData);
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

                                // ← ĐÁNH DẤU "ĐÃ NHẬN" khi tin nhắn load lần đầu
                                if (!msg.senderId.equals(currentUserId) && msg.deliveredAt == null) {
                                    markAsDelivered(msg.messageId);
                                }

                                // ← ĐÁNH DẤU "ĐÃ XEM" khi user mở chat
                                if (!msg.senderId.equals(currentUserId) && !msg.isRead) {
                                    markAsRead(msg.messageId);
                                }
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (messageList.size() > 0) {
                        rvMessages.smoothScrollToPosition(messageList.size() - 1);
                    }
                });
    }

    // ← ĐÁnh dấu đã nhận
    private void markAsDelivered(String messageId) {
        Map<String, Object> update = new HashMap<>();
        update.put("deliveredAt", FieldValue.serverTimestamp());

        db.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update(update);
    }

    // ← Đánh dấu đã xem
    private void markAsRead(String messageId) {
        Map<String, Object> update = new HashMap<>();
        update.put("isRead", true);
        update.put("readAt", FieldValue.serverTimestamp());

        db.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update(update);
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();

        if (text.isEmpty()) {
            return;
        }

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
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("lastMessage", text);
                    updateData.put("lastMessageTime", FieldValue.serverTimestamp());

                    db.collection("chats").document(chatId).update(updateData);

                    etMessage.setText("");
                    hideKeyboard();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi gửi tin nhắn", Toast.LENGTH_SHORT).show();
                });
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && etMessage != null) {
            etMessage.requestFocus();
            etMessage.post(() -> {
                imm.showSoftInput(etMessage, InputMethodManager.SHOW_IMPLICIT);
            });
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