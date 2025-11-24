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
import android.os.Handler;
import android.os.Looper;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.DocumentReference;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Date;
import com.google.android.material.bottomsheet.BottomSheetDialog;
public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend, btnBack, btnMenu, btnSticker, btnSelfDestruct;
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
    private static final String PREF_SELF_DESTRUCT_PREFIX = "self_destruct_";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_DURATION = "duration";
    
    // Self-destruct timer fields
    private boolean isSelfDestructEnabled = false;
    private long selfDestructDuration = 0; // in milliseconds
    
    // ⚡ PERFORMANCE OPTIMIZATION FIELDS
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private Set<String> pendingDeliveredMarks = new HashSet<>();
    private Set<String> pendingReadMarks = new HashSet<>();
    private Handler markHandler = new Handler(Looper.getMainLooper());
    private Set<String> markedMessageIds = new HashSet<>();
    private ExecutorService messageExecutor = Executors.newSingleThreadExecutor();
    private boolean timersStarted = false;  // Flag to prevent duplicate timer starts
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        try {
            db = FirebaseFirestore.getInstance();
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    
            // ✅ GET friendId FIRST (from Intent)
            friendId = getIntent().getStringExtra("friendId");
            friendName = getIntent().getStringExtra("friendName");
            friendPhoto = getIntent().getStringExtra("friendPhoto");
    
            if (friendId == null || friendName == null) {
                Log.e(TAG, "Missing friend info!");
                Toast.makeText(this, "Lỗi: Thiếu thông tin bạn bè", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
    
            // ✅ THEN generate chatId (needs friendId)
            chatId = generateChatId(currentUserId, friendId);
            Log.d(TAG, "Chat ID: " + chatId);
    
            // Initialize clipboard manager
            clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    
            // Initialize self-destruct preferences (PER-CHAT)
            selfDestructPrefs = getSharedPreferences(PREF_SELF_DESTRUCT_PREFIX + chatId, MODE_PRIVATE);
            isSelfDestructEnabled = selfDestructPrefs.getBoolean(KEY_ENABLED, false);
            selfDestructDuration = selfDestructPrefs.getLong(KEY_DURATION, 0);
            // Setup UI
            btnBack = findViewById(R.id.btnBack);
            btnMenu = findViewById(R.id.btnMenu);
            btnSticker = findViewById(R.id.btnSticker);
            btnSelfDestruct = findViewById(R.id.btnSelfDestruct);
            ivFriendAvatar = findViewById(R.id.ivFriendAvatar);
            tvFriendName = findViewById(R.id.tvFriendName);
            rvMessages = findViewById(R.id.rvMessages);
            etMessage = findViewById(R.id.etMessage);
            btnSend = findViewById(R.id.btnSend);
            btnBack.setOnClickListener(v -> finish());
            btnMenu.setOnClickListener(v -> showMenuDialog());
            btnSticker.setOnClickListener(v -> showStickerPicker());
            btnSelfDestruct.setOnClickListener(v -> toggleSelfDestructMode());
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
            
            // ✅ SET STABLE IDS (MUST be BEFORE setAdapter!)
            adapter.setHasStableIds(true);
            
            // ✅ SET LONG-PRESS LISTENER
            adapter.setOnMessageLongClickListener((message, view) -> {
                showMessageActionsDialog(message, view);
            });
            
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setStackFromEnd(true);
            rvMessages.setLayoutManager(layoutManager);
            rvMessages.setAdapter(adapter);  // ← Set adapter SAU CÙNG
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
            
            // Timer will be started in listenToMessages() after messages load
            startTimerUpdater();
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Error", e);
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
        
    }
    // ═══════════════════════════════════════════════════════════════
    // 📱 LONG-PRESS MESSAGE ACTIONS
    // ═══════════════════════════════════════════════════════════════
    
    // ✅ ADD THIS IN onCreate() - Line ~141 (after adapter.setHasStableIds(true);)
    // adapter.setOnMessageLongClickListener((message, view) -> {
    //     showMessageActionsDialog(message, view);
    // });
    
    private void showMessageActionsDialog(Message message, View anchorView) {
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
            TextView tvEmoji = new TextView(this);
            tvEmoji.setText(emoji);
            tvEmoji.setTextSize(24);
            tvEmoji.setPadding(16, 16, 16, 16);
            tvEmoji.setOnClickListener(v -> {
                popupWindow.dismiss();
                addReaction(message, emoji);
            });
            emojiRow.addView(tvEmoji);
        }
        
        // Copy button
        LinearLayout btnCopy = popupView.findViewById(R.id.btnCopy);
        btnCopy.setOnClickListener(v -> {
            popupWindow.dismiss();
            copyMessageText(message);
        });
        
        // Recall button (only for sender)
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
    
    private void copyMessageText(Message message) {
        if (message.text != null && !message.text.isEmpty()) {
            ClipData clip = ClipData.newPlainText("message", message.text);
            clipboardManager.setPrimaryClip(clip);
            Toast.makeText(this, "Đã sao chép tin nhắn", Toast.LENGTH_SHORT).show();
        }
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
        updates.put("recalledAt", FieldValue.serverTimestamp());
        updates.put("text", "Tin nhắn đã được thu hồi");
        
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .document(message.messageId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Đã thu hồi tin nhắn", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "✅ Message recalled: " + message.messageId);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi thu hồi tin nhắn", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "❌ Error recalling message", e);
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
                
                List<String> userList = reactions.get(emoji);
                if (userList == null) {
                    userList = new ArrayList<>();
                }
                
                if (userList.contains(currentUserId)) {
                    userList.remove(currentUserId);
                    if (userList.isEmpty()) {
                        reactions.remove(emoji);
                    } else {
                        reactions.put(emoji, userList);
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
    private void showMenuDialog() {
        View popupView = getLayoutInflater().inflate(R.layout.popup_chat_menu, null);
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
        ImageView ivPopupAvatar = popupView.findViewById(R.id.ivPopupAvatar);
        TextView tvPopupName = popupView.findViewById(R.id.tvPopupName);
        LinearLayout btnPopupUnfriend = popupView.findViewById(R.id.btnPopupUnfriend);
        LinearLayout btnPopupReport = popupView.findViewById(R.id.btnPopupReport);
        LinearLayout btnPopupBlock = popupView.findViewById(R.id.btnPopupBlock);
        tvPopupName.setText(friendName);
        if (friendPhoto != null && !friendPhoto.isEmpty()) {
            Glide.with(this).load(friendPhoto).circleCrop().into(ivPopupAvatar);
        } else {
            ivPopupAvatar.setImageResource(R.drawable.ic_person_circle);
        }
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
        int[] location = new int[2];
        btnMenu.getLocationOnScreen(location);
        int xOffset = location[0] - (int) (280 * getResources().getDisplayMetrics().density) + btnMenu.getWidth();
        int yOffset = location[1] + btnMenu.getHeight() + (int) (8 * getResources().getDisplayMetrics().density);
        popupWindow.showAtLocation(btnMenu, Gravity.NO_GRAVITY, xOffset, yOffset);
    }
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
                
                if (snapshots != null) {
                    // ✅ CHỈ XỬ LÝ THAY ĐỔI, KHÔNG CLEAR TẤT CẢ
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        Message msg = dc.getDocument().toObject(Message.class);
                        if (msg != null) {
                            msg.messageId = dc.getDocument().getId();
                            
                            switch (dc.getType()) {
                                case ADDED:
                                    // Thêm tin nhắn mới
                                    messageList.add(msg);
                                    
                                    // Mark as delivered/read
                                    if (!msg.senderId.equals(currentUserId) && msg.deliveredAt == null) {
                                        markAsDelivered(msg.messageId);
                                    }
                                    if (!msg.senderId.equals(currentUserId) && !msg.isRead) {
                                        markAsRead(msg.messageId);
                                    }
                                    break;
                                    
                                case MODIFIED:
    // Cập nhật tin nhắn đã có
    for (int i = 0; i < messageList.size(); i++) {
        if (messageList.get(i).messageId.equals(msg.messageId)) {
            Message oldMsg = messageList.get(i);
            messageList.set(i, msg);
            
            // ✅ BẮT ĐẦU TIMER KHI readAt VỪA ĐƯỢC CẬP NHẬT
            if (oldMsg.readAt == null && msg.readAt != null && 
                msg.selfDestructDuration != null && msg.selfDestructDuration > 0 &&
                msg.expiresAt == null) {
                
                // Tính expiresAt
                long readTime = msg.readAt.toDate().getTime();
                long expiresAt = readTime + msg.selfDestructDuration;
                
                // Update locally
                msg.expiresAt = expiresAt;
                
                // Update Firestore
                db.collection("chats").document(chatId)
                    .collection("messages").document(msg.messageId)
                    .update("expiresAt", expiresAt)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ Timer started for message: " + msg.messageId);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "⚠️ Error updating timer: " + e.getMessage());
                    });
            }
            break;
        }
    }
    break;
                                    
                                case REMOVED:
                                    // Xóa tin nhắn
                                    messageList.removeIf(m -> m.messageId.equals(msg.messageId));
                                    break;
                            }
                        }
                    }
                }
                
                // ✅ START TIMERS AFTER MESSAGES LOADED (only once)
                if (!timersStarted && !messageList.isEmpty()) {
                    timersStarted = true;
                    messageExecutor.execute(() -> {
                        startSelfDestructTimers();
                    });
                }
                
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
            // Don't set expiresAt yet - will be set when chat opens
            messageData.put("selfDestructDuration", selfDestructDuration);
            messageData.put("expiresAt", null);
            Log.d(TAG, "📅 Message will self-destruct in: " + (selfDestructDuration/1000) + "s");
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
    private void toggleSelfDestructMode() {
        Log.d(TAG, "🔄 toggleSelfDestructMode() called");
        Log.d(TAG, "   Current state: " + (isSelfDestructEnabled ? "ON" : "OFF"));
        
        if (isSelfDestructEnabled) {
            // Clicking again disables it
            disableSelfDestruct();
        } else {
            // Show time picker
            showSelfDestructTimePickerDialog();
        }
    }
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
            5 * 1000,
            30 * 1000,
            60 * 1000,
            5 * 60 * 1000,
            30 * 60 * 1000,
            60 * 60 * 1000,
            12 * 60 * 60 * 1000,
            24 * 60 * 60 * 1000
        };
        
        new AlertDialog.Builder(this)
            .setTitle("Chọn thời gian tự hủy")
            .setItems(options, (dialog, which) -> {
                enableSelfDestruct(durations[which]);
            })
            .setNegativeButton("Hủy", (dialog, which) -> {
                disableSelfDestruct();
            })
            .show();
    }
    private void enableSelfDestruct(long durationMs) {
        isSelfDestructEnabled = true;
        selfDestructDuration = durationMs;
        
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
    private void showStickerPicker() {
    // Tạo BottomSheetDialog
    BottomSheetDialog dialog = new BottomSheetDialog(this);
    View view = getLayoutInflater().inflate(R.layout.dialog_sticker_picker, null);
    dialog.setContentView(view);
    androidx.recyclerview.widget.RecyclerView rvStickers = view.findViewById(R.id.rvStickers);
    android.widget.ProgressBar progressBar = view.findViewById(R.id.progressBar);
    TextView tvError = view.findViewById(R.id.tvError);
    // Setup GridLayoutManager (4 cột)
    androidx.recyclerview.widget.GridLayoutManager gridLayoutManager = 
        new androidx.recyclerview.widget.GridLayoutManager(this, 4);
    rvStickers.setLayoutManager(gridLayoutManager);
    // Fetch stickers từ Supabase
    com.example.locketbaseapp.service.StickerService.fetchStickers(
        new com.example.locketbaseapp.service.StickerService.StickerCallback() {
            @Override
            public void onSuccess(List<com.example.locketbaseapp.model.Sticker> stickers) {
                progressBar.setVisibility(View.GONE);
                
                if (stickers.isEmpty()) {
                    tvError.setText("Chưa có sticker nào");
                    tvError.setVisibility(View.VISIBLE);
                } else {
                    rvStickers.setVisibility(View.VISIBLE);
                    
                    com.example.locketbaseapp.ui.StickerAdapter adapter = 
                        new com.example.locketbaseapp.ui.StickerAdapter(stickers, sticker -> {
                            sendStickerMessage(sticker);
                            dialog.dismiss();
                        });
                    rvStickers.setAdapter(adapter);
                }
            }
            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                tvError.setText("Lỗi: " + error);
                tvError.setVisibility(View.VISIBLE);
            }
        }
    );
    dialog.show();
}
private void sendStickerMessage(com.example.locketbaseapp.model.Sticker sticker) {
    Map<String, Object> messageData = new HashMap<>();
    messageData.put("senderId", currentUserId);
    messageData.put("text", ""); // Empty text for sticker
    messageData.put("imageUrl", sticker.url);
   messageData.put("timestamp", new Timestamp(new Date())); 
    messageData.put("isRead", false);
    messageData.put("type", "sticker"); // Đánh dấu là sticker
    
    db.collection("chats").document(chatId)
            .collection("messages")
            .add(messageData)
            .addOnSuccessListener(docRef -> {
                // Update last message
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("lastMessage", "[Sticker]");
                updateData.put("lastMessageTime", FieldValue.serverTimestamp());
                db.collection("chats").document(chatId).update(updateData);
                
                Log.d(TAG, "✅ Sticker sent: " + sticker.url);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi gửi sticker", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "❌ Error sending sticker", e);
            });
}
    // ═══════════════════════════════════════════════════════════════
    // 🔥 FIREBASE BATCH OPERATIONS
    // ═══════════════════════════════════════════════════════════════
    private void markAsDeliveredBatched(String messageId) {
        pendingDeliveredMarks.add(messageId);
        markHandler.removeCallbacks(batchMarkRunnable);
        markHandler.postDelayed(batchMarkRunnable, 2000);
    }
    private void markAsReadBatched(String messageId) {
        pendingReadMarks.add(messageId);
        markHandler.removeCallbacks(batchMarkRunnable);
        markHandler.postDelayed(batchMarkRunnable, 2000);
    }
    private Runnable batchMarkRunnable = new Runnable() {
        @Override
        public void run() {
            if (pendingDeliveredMarks.isEmpty() && pendingReadMarks.isEmpty()) {
                return;
            }
            
            WriteBatch batch = db.batch();
            
            for (String msgId : pendingDeliveredMarks) {
                DocumentReference ref = db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(msgId);
                batch.update(ref, "deliveredAt", FieldValue.serverTimestamp());
            }
            
            for (String msgId : pendingReadMarks) {
                DocumentReference ref = db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(msgId);
                batch.update(ref, "isRead", true);
                batch.update(ref, "readAt", FieldValue.serverTimestamp());
            }
            
            batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Batched " + pendingDeliveredMarks.size() + " delivered + " + 
                          pendingReadMarks.size() + " read marks");
                    pendingDeliveredMarks.clear();
                    pendingReadMarks.clear();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Batch mark failed", e);
                });
        }
    };
    // ═══════════════════════════════════════════════════════════════
    // ⏱️ TIMER LOGIC
    // ═══════════════════════════════════════════════════════════════
    private void startSelfDestructTimers() {
    for (Message msg : messageList) {
        if (msg.selfDestructDuration != null && msg.selfDestructDuration > 0) {
            // ✅ CHỈ BẮT ĐẦU TIMER KHI NGƯỜI NHẬN ĐÃ XEM
            if (msg.readAt != null && (msg.expiresAt == null || msg.expiresAt == 0)) {
                // Calculate expiry time from readAt
                long readTime = msg.readAt.toDate().getTime();
                long expiresAt = readTime + msg.selfDestructDuration;
                
                // Update locally
                msg.expiresAt = expiresAt;
                
                // Update Firestore
                db.collection("chats").document(chatId)
                    .collection("messages").document(msg.messageId)
                    .update("expiresAt", expiresAt)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ Timer started for message: " + msg.messageId);
                    });
            }
        }
        }
    }
    private void startTimerUpdater() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                // ⚡ OPTIMIZED: Only update items with active timers
                for (int i = 0; i < messageList.size(); i++) {
                    Message msg = messageList.get(i);
                    if (msg.expiresAt != null && msg.expiresAt > System.currentTimeMillis()) {
                        adapter.notifyItemChanged(i);
                    }
                }
                
                checkAndDeleteExpiredMessages();
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }
    private void checkAndDeleteExpiredMessages() {
        long currentTime = System.currentTimeMillis();
        List<Message> toDelete = new ArrayList<>();
        
        for (Message message : messageList) {
            if (message.expiresAt != null && currentTime > message.expiresAt) {
                toDelete.add(message);
            }
        }
        
        for (Message msg : toDelete) {
            db.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(msg.messageId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Expired message deleted: " + msg.messageId);
                });
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
        }
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        if (markHandler != null && batchMarkRunnable != null) {
            markHandler.removeCallbacks(batchMarkRunnable);
        }
        if (messageExecutor != null) {
            messageExecutor.shutdown();
        }
    }
}