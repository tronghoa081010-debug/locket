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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import androidx.appcompat.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Switch;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend, btnBack, btnMenu, btnSticker;
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

    // Clipboard manager for copy text
    private ClipboardManager clipboardManager;
    
    // SharedPreferences for self-destruct settings
    private SharedPreferences selfDestructPrefs;
    private static final String PREF_SELF_DESTRUCT = "self_destruct_prefs";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_DURATION = "duration";
    
    // Self-destruct timer fields
    private boolean isSelfDestructEnabled = false;
    private long selfDestructDuration = 0; // in milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        try {
            db = FirebaseFirestore.getInstance();
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            
            // Initialize clipboard manager
            clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            
            // Initialize self-destruct preferences
            selfDestructPrefs = getSharedPreferences(PREF_SELF_DESTRUCT, MODE_PRIVATE);
            isSelfDestructEnabled = selfDestructPrefs.getBoolean(KEY_ENABLED, false);
            selfDestructDuration = selfDestructPrefs.getLong(KEY_DURATION, 0);
            
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
            btnSticker = findViewById(R.id.btnSticker);
            ivFriendAvatar = findViewById(R.id.ivFriendAvatar);
            tvFriendName = findViewById(R.id.tvFriendName);
            rvMessages = findViewById(R.id.rvMessages);
            etMessage = findViewById(R.id.etMessage);
            btnSend = findViewById(R.id.btnSend);

            btnBack.setOnClickListener(v -> finish());

            // ← MENU: Xóa bạn, Báo cáo, Chặn, Self-Destruct
            btnMenu.setOnClickListener(v -> showMenuDialog());
            
            // ← STICKER: Mở danh sách sticker từ Supabase
            btnSticker.setOnClickListener(v -> showStickerPicker());

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
            
            // ✅ SET LONG-PRESS LISTENER
            adapter.setOnMessageLongClickListener((message, view) -> {
                showMessageActionsDialog(message, view);
            });

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

    // ═══════════════════════════════════════════════════════════════
    // 📱 MESSAGE ACTIONS - Long Press Features
    // ═══════════════════════════════════════════════════════════════

    private void showMessageActionsDialog(Message message, View anchorView) {
        // Create popup window
        View popupView = getLayoutInflater().inflate(R.layout.popup_message_actions, null);
        PopupWindow popupWindow = new PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        );
        
        // Setup emoji reactions
        LinearLayout emojiRow = popupView.findViewById(R.id.emojiRow);
        String[] emojis = {"❤️", "😂", "😢", "😮", "😡"};
        
        for (String emoji : emojis) {
            TextView emojiView = new TextView(this);
            emojiView.setText(emoji);
            emojiView.setTextSize(24);
            emojiView.setPadding(16, 16, 16, 16);
            emojiView.setOnClickListener(v -> {
                addReaction(message, emoji);
                popupWindow.dismiss();
            });
            emojiRow.addView(emojiView);
        }
        
        // Copy Text button
        LinearLayout btnCopy = popupView.findViewById(R.id.btnCopy);
        btnCopy.setOnClickListener(v -> {
            copyMessageText(message.text);
            popupWindow.dismiss();
        });
        
        // Recall button (only for own messages)
        LinearLayout btnRecall = popupView.findViewById(R.id.btnRecall);
        if (message.senderId.equals(currentUserId) && !message.isRecalled()) {
            btnRecall.setVisibility(View.VISIBLE);
            btnRecall.setOnClickListener(v -> {
                popupWindow.dismiss();
                confirmRecallMessage(message);
            });
        } else {
            btnRecall.setVisibility(View.GONE);
        }
        
        // Show popup
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(8);
        popupWindow.showAsDropDown(anchorView, 0, -anchorView.getHeight());
    }

    private void copyMessageText(String text) {
        if (text == null || text.isEmpty()) {
            Toast.makeText(this, "Không có nội dung để sao chép", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ClipData clip = ClipData.newPlainText("message", text);
        clipboardManager.setPrimaryClip(clip);
        Toast.makeText(this, "Đã sao chép", Toast.LENGTH_SHORT).show();
    }

    private void confirmRecallMessage(Message message) {
        new AlertDialog.Builder(this)
            .setTitle("Thu hồi tin nhắn")
            .setMessage("Bạn có chắc chắn muốn thu hồi tin nhắn này?")
            .setPositiveButton("Thu hồi", (dialog, which) -> {
                recallMessage(message);
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void recallMessage(Message message) {
        if (message.messageId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID tin nhắn", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("recalled", true);
        updates.put("recalledAt", Timestamp.now());
        
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .document(message.messageId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Message recalled successfully");
                Toast.makeText(this, "Đã thu hồi tin nhắn", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Error recalling message", e);
                Toast.makeText(this, "Lỗi thu hồi tin nhắn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void addReaction(Message message, String emoji) {
        if (message.messageId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID tin nhắn", Toast.LENGTH_SHORT).show();
            return;
        }
        
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .document(message.messageId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                Map<String, List<String>> reactions = (Map<String, List<String>>) documentSnapshot.get("reactions");
                if (reactions == null) {
                    reactions = new HashMap<>();
                }
                
                // Get list of users who reacted with this emoji
                List<String> userList = reactions.get(emoji);
                if (userList == null) {
                    userList = new ArrayList<>();
                }
                
                // Toggle reaction
                if (userList.contains(currentUserId)) {
                    userList.remove(currentUserId);
                    if (userList.isEmpty()) {
                        reactions.remove(emoji);
                    }
                } else {
                    userList.add(currentUserId);
                    reactions.put(emoji, userList);
                }
                
                // Update Firestore
                db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(message.messageId)
                    .update("reactions", reactions)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ Reaction added");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Error adding reaction", e);
                    });
            });
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎯 MENU DIALOG - With Self-Destruct Toggle
    // ═══════════════════════════════════════════════════════════════

    private void showMenuDialog() {
        // Inflate custom layout
        View popupView = getLayoutInflater().inflate(R.layout.popup_chat_menu, null);

        // Tạo PopupWindow
        PopupWindow popupWindow = new PopupWindow(
                popupView,
                (int) (280 * getResources().getDisplayMetrics().density),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(12);
        popupWindow.setOutsideTouchable(true);
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
        
        // ✅ ADD SELF-DESTRUCT TOGGLE TO MENU
        LinearLayout menuContainer = popupView.findViewById(R.id.menuContainer);
        if (menuContainer != null) {
            // Add divider
            View divider = new View(this);
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                2
            );
            divider.setLayoutParams(dividerParams);
            divider.setBackgroundColor(0xFFEEEEEE);
            menuContainer.addView(divider);

            // Self-Destruct Toggle
            LinearLayout selfDestructItem = new LinearLayout(this);
            selfDestructItem.setOrientation(LinearLayout.HORIZONTAL);
            selfDestructItem.setPadding(48, 32, 48, 32);
            selfDestructItem.setGravity(Gravity.CENTER_VERTICAL);
            selfDestructItem.setClickable(true);
            selfDestructItem.setFocusable(true);

            TextView tvSelfDestruct = new TextView(this);
            tvSelfDestruct.setText("Tin nhắn tự hủy");
            tvSelfDestruct.setTextSize(16);
            tvSelfDestruct.setTextColor(0xFF000000);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
            );
            tvSelfDestruct.setLayoutParams(textParams);

            Switch switchSelfDestruct = new Switch(this);
            switchSelfDestruct.setChecked(isSelfDestructEnabled);
            switchSelfDestruct.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    popupWindow.dismiss();
                    showSelfDestructTimePickerDialog();
                } else {
                    disableSelfDestruct();
                }
            });

            selfDestructItem.addView(tvSelfDestruct);
            selfDestructItem.addView(switchSelfDestruct);
            menuContainer.addView(selfDestructItem);
        }

        // Tính toán vị trí hiển thị
        int[] location = new int[2];
        btnMenu.getLocationOnScreen(location);

        int xOffset = location[0] - (int) (280 * getResources().getDisplayMetrics().density) + btnMenu.getWidth();
        int yOffset = location[1] + btnMenu.getHeight() + (int) (8 * getResources().getDisplayMetrics().density);

        popupWindow.showAtLocation(btnMenu, Gravity.NO_GRAVITY, xOffset, yOffset);
    }

    // ═══════════════════════════════════════════════════════════════
    // ⏱️ SELF-DESTRUCT IMPROVEMENTS
    // ═══════════════════════════════════════════════════════════════

    private void showSelfDestructTimePickerDialog() {
        String[] options = {
            "5 giây",
            "30 giây",
            "1 phút",
            "5 phút",
            "30 phút",
            "1 giờ",
            "12 giờ",
            "24 giờ"
        };
        
        long[] durations = {
            5 * 1000,           // 5s
            30 * 1000,          // 30s
            60 * 1000,          // 1m
            5 * 60 * 1000,      // 5m
            30 * 60 * 1000,     // 30m
            60 * 60 * 1000,     // 1h
            12 * 60 * 60 * 1000, // 12h
            24 * 60 * 60 * 1000  // 24h
        };
        
        new AlertDialog.Builder(this)
            .setTitle("Chọn thời gian tự hủy")
            .setItems(options, (dialog, which) -> {
                enableSelfDestruct(durations[which]);
            })
            .setNegativeButton("Hủy", (dialog, which) -> {
                // User cancelled, turn off the switch
                disableSelfDestruct();
            })
            .show();
    }

    private void enableSelfDestruct(long durationMs) {
        isSelfDestructEnabled = true;
        selfDestructDuration = durationMs;
        
        // Save to SharedPreferences
        selfDestructPrefs.edit()
            .putBoolean(KEY_ENABLED, true)
            .putLong(KEY_DURATION, durationMs)
            .apply();
        
        String durationText = formatDuration(durationMs);
        Toast.makeText(this, "Tin nhắn tự hủy sau " + durationText, Toast.LENGTH_SHORT).show();
        
        Log.d(TAG, "✅ Self-destruct enabled: " + durationMs + "ms");
    }

    private void disableSelfDestruct() {
        isSelfDestructEnabled = false;
        selfDestructDuration = 0;
        
        // Save to SharedPreferences
        selfDestructPrefs.edit()
            .putBoolean(KEY_ENABLED, false)
            .putLong(KEY_DURATION, 0)
            .apply();
        
        Toast.makeText(this, "Đã tắt tin nhắn tự hủy", Toast.LENGTH_SHORT).show();
        
        Log.d(TAG, "✅ Self-destruct disabled");
    }

    private String formatDuration(long ms) {
        if (ms < 60 * 1000) {
            return (ms / 1000) + " giây";
        } else if (ms < 60 * 60 * 1000) {
            return (ms / (60 * 1000)) + " phút";
        } else {
            return (ms / (60 * 60 * 1000)) + " giờ";
        }
    }

    private void checkAndDeleteExpiredMessages() {
        long currentTime = System.currentTimeMillis();
        
        for (Message message : messageList) {
            if (message.expiresAt != null && currentTime > message.expiresAt) {
                // Delete expired message
                db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(message.messageId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ Expired message deleted: " + message.messageId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Error deleting expired message", e);
                    });
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 👥 USER ACTIONS
    // ═══════════════════════════════════════════════════════════════

    private void unfriend() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bạn")
                .setMessage("Bạn có chắc muốn xóa " + friendName + " khỏi danh sách bạn bè?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    db.collection("users").document(currentUserId)
                            .collection("friends").document(friendId).delete();

                    db.collection("users").document(friendId)
                            .collection("friends").document(currentUserId).delete();

                    Toast.makeText(this, "Đã xóa " + friendName, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void reportUser() {
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("reporterId", currentUserId);
        reportData.put("reportedUserId", friendId);
        reportData.put("reportedUserName", friendName);
        reportData.put("reason", "Reported from chat");
        reportData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("reports").add(reportData)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Báo cáo đã được gửi", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi gửi báo cáo", Toast.LENGTH_SHORT).show();
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
                                finish();
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ═══════════════════════════════════════════════════════════════
    // 💬 CHAT FUNCTIONS
    // ═══════════════════════════════════════════════════════════════

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

                                // Mark as delivered
                                if (!msg.senderId.equals(currentUserId) && msg.deliveredAt == null) {
                                    markAsDelivered(msg.messageId);
                                }

                                // Mark as read
                                if (!msg.senderId.equals(currentUserId) && !msg.isRead) {
                                    markAsRead(msg.messageId);
                                }
                            }
                        }
                    }
                    
                    // ✅ CHECK AND DELETE EXPIRED MESSAGES
                    checkAndDeleteExpiredMessages();

                    adapter.notifyDataSetChanged();

                    if (messageList.size() > 0) {
                        rvMessages.smoothScrollToPosition(messageList.size() - 1);
                    }
                });
    }

    private void markAsDelivered(String messageId) {
        Map<String, Object> update = new HashMap<>();
        update.put("deliveredAt", FieldValue.serverTimestamp());

        db.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update(update);
    }

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

        // ✅ ADD SELF-DESTRUCT IF ENABLED
        if (isSelfDestructEnabled && selfDestructDuration > 0) {
            long expiresAt = System.currentTimeMillis() + selfDestructDuration;
            messageData.put("expiresAt", expiresAt);
            messageData.put("selfDestructDuration", selfDestructDuration);
            Log.d(TAG, "📅 Message will expire at: " + expiresAt);
        }

        db.collection("chats").document(chatId).get()
                .addOnSuccessListener(chatDoc -> {
                    if (!chatDoc.exists()) {
                        Map<String, Object> chatData = new HashMap<>();
                        chatData.put("participants", Arrays.asList(currentUserId, friendId));
                        chatData.put("lastMessage", "");
                        chatData.put("lastMessageTime", FieldValue.serverTimestamp());

                        db.collection("chats").document(chatId).set(chatData)
                                .addOnSuccessListener(aVoid -> {
                                    sendMessageToExistingChat(messageData, text);
                                });
                    } else {
                        sendMessageToExistingChat(messageData, text);
                    }
                });
    }

    private void sendMessageToExistingChat(Map<String, Object> messageData, String text) {
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
                    Toast.makeText(this, "Lỗi gửi tin nhắn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    // ═══════════════════════════════════════════════════════════════
    // 🎨 STICKER FEATURE
    // ═══════════════════════════════════════════════════════════════
    
    private void showStickerPicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📦 Chọn Sticker");
        builder.setMessage("💡 Chức năng sticker từ Supabase đang được triển khai.");
        builder.setPositiveButton("Đóng", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
        }
    }
}
