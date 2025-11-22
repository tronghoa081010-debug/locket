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
            btnSelfDestruct = findViewById(R.id.btnSelfDestruct);
            ivFriendAvatar = findViewById(R.id.ivFriendAvatar);
            tvFriendName = findViewById(R.id.tvFriendName);
            rvMessages = findViewById(R.id.rvMessages);
            etMessage = findViewById(R.id.etMessage);
            btnSend = findViewById(R.id.btnSend);

            btnBack.setOnClickListener(v -> finish());

            // ← MENU: Xóa bạn, Báo cáo, Chặn
            btnMenu.setOnClickListener(v -> showMenuDialog());
            
            // ← STICKER: Mở danh sách sticker từ Supabase
            btnSticker.setOnClickListener(v -> showStickerPicker());
            
            // ← SELF-DESTRUCT: Toggle timer
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
        Log.d(TAG, "🔧 createChatIfNotExists() called");
        Log.d(TAG, "   Chat ID: " + chatId);
        
        db.collection("chats").document(chatId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Log.d(TAG, "📝 Chat doesn't exist yet, creating...");
                        Map<String, Object> chatData = new HashMap<>();
                        chatData.put("participants", Arrays.asList(currentUserId, friendId));
                        chatData.put("lastMessage", "");
                        chatData.put("lastMessageTime", FieldValue.serverTimestamp());

                        db.collection("chats").document(chatId).set(chatData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "✅ Chat created successfully");
                                    Log.d(TAG, "   - Participants: [" + currentUserId + ", " + friendId + "]");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "❌ Error creating chat in createChatIfNotExists");
                                    Log.e(TAG, "   Error: " + e.getMessage());
                                });
                    } else {
                        Log.d(TAG, "✅ Chat already exists");
                        Log.d(TAG, "   - Participants: " + doc.get("participants"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error checking chat in createChatIfNotExists");
                    Log.e(TAG, "   Error: " + e.getMessage());
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

        Log.d(TAG, "═══════════════════════════════════════");
        Log.d(TAG, "📤 sendMessage() called");
        Log.d(TAG, "   Text: '" + text + "'");
        Log.d(TAG, "   Chat ID: " + chatId);
        Log.d(TAG, "   Current User ID: " + currentUserId);
        Log.d(TAG, "   Friend ID: " + friendId);

        if (text.isEmpty()) {
            Log.w(TAG, "⚠️ Message is empty, ignoring");
            return;
        }

        Log.d(TAG, "✅ Message not empty, proceeding with send");

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderId", currentUserId);
        messageData.put("text", text);
        messageData.put("timestamp", FieldValue.serverTimestamp());
        messageData.put("isRead", false);
        messageData.put("imageUrl", "");

        // 🔥 ADD SELF-DESTRUCT FIELDS IF ENABLED
        if (isSelfDestructEnabled && selfDestructDuration > 0) {
            long expiresAt = System.currentTimeMillis() + selfDestructDuration;
            messageData.put("expiresAt", expiresAt);
            messageData.put("selfDestructDuration", selfDestructDuration);
            Log.d(TAG, "⏰ Self-destruct enabled: " + (selfDestructDuration / 1000) + " seconds");
        }

        Log.d(TAG, "📊 Message data prepared:");
        Log.d(TAG, "   - senderId: " + currentUserId);
        Log.d(TAG, "   - text: " + text);
        if (isSelfDestructEnabled) {
            Log.d(TAG, "   - Self-destruct: " + (selfDestructDuration / 1000) + "s");
        }
        
        // 🔥 VERIFY CHAT EXISTS BEFORE ADDING MESSAGE
        db.collection("chats").document(chatId).get()
                .addOnSuccessListener(chatDoc -> {
                    if (!chatDoc.exists()) {
                        Log.e(TAG, "❌ CRITICAL: Chat document does NOT exist!");
                        Log.e(TAG, "   - Chat ID: " + chatId);
                        Log.e(TAG, "   Creating chat now...");
                        
                        Map<String, Object> chatData = new HashMap<>();
                        chatData.put("participants", Arrays.asList(currentUserId, friendId));
                        chatData.put("lastMessage", "");
                        chatData.put("lastMessageTime", FieldValue.serverTimestamp());
                        
                        db.collection("chats").document(chatId).set(chatData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "✅ Chat created, now sending message...");
                                    sendMessageToExistingChat(messageData, text);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "❌ Failed to create chat");
                                    Log.e(TAG, "   Error: " + e.getMessage());
                                });
                    } else {
                        Log.d(TAG, "✅ Chat exists, participants: " + chatDoc.get("participants"));
                        sendMessageToExistingChat(messageData, text);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error checking chat existence");
                    Log.e(TAG, "   Error class: " + e.getClass().getSimpleName());
                    Log.e(TAG, "   Error: " + e.getMessage());
                    if (e.getMessage().contains("PERMISSION_DENIED")) {
                        Log.e(TAG, "❌ PERMISSION_DENIED on read /chats/" + chatId);
                    }
                });
    }
    
    private void sendMessageToExistingChat(Map<String, Object> messageData, String text) {
        Log.d(TAG, "📮 Adding message to existing chat...");
        
        db.collection("chats").document(chatId)
                .collection("messages")
                .add(messageData)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "───────────────────────────────────────");
                    Log.d(TAG, "✅ Message saved successfully");
                    Log.d(TAG, "   - Message ID: " + docRef.getId());
                    Log.d(TAG, "───────────────────────────────────────");

                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("lastMessage", text);
                    updateData.put("lastMessageTime", FieldValue.serverTimestamp());

                    Log.d(TAG, "🔄 Updating chat metadata...");
                    db.collection("chats").document(chatId).update(updateData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ Chat metadata updated");
                                Log.d(TAG, "═══════════════════════════════════════");

                                etMessage.setText("");
                                hideKeyboard();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Error updating chat metadata");
                                Log.e(TAG, "   Error: " + e.getMessage());
                                Toast.makeText(this, "Lỗi cập nhật tin nhắn", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "───────────────────────────────────────");
                    Log.e(TAG, "❌ sendMessage() FAILED on .add()");
                    Log.e(TAG, "   - Error class: " + e.getClass().getSimpleName());
                    Log.e(TAG, "   - Error message: " + e.getMessage());
                    Log.e(TAG, "   - Chat ID: " + chatId);
                    Log.e(TAG, "   - Path: /chats/" + chatId + "/messages");
                    Log.e(TAG, "   - Sender ID: " + messageData.get("senderId"));

                    if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                        Log.e(TAG, "❌ PERMISSION_DENIED - Check:");
                        Log.e(TAG, "      1. Firestore rules for /chats/{chatId}/messages");
                        Log.e(TAG, "      2. Chat participants array");
                        Log.e(TAG, "      3. User authentication status");
                    }

                    Log.e(TAG, "───────────────────────────────────────");
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
    // 🎨 STICKER FEATURE - Open sticker list from Supabase
    // ═══════════════════════════════════════════════════════════════
    private void showStickerPicker() {
        Log.d(TAG, "🎨 showStickerPicker() called");
        
        // 🔗 PLACEHOLDER URL - User to replace with actual Supabase bucket URL
        // Example format:
        // https://tjvvywmzihpzmgdzvcof.supabase.co/storage/v1/object/public/stickers/
        
        String stickerUrl = "https://example-placeholder.supabase.co/storage/v1/object/public/stickers/";
        
        // For now, show a dialog with sticker options
        // TODO: Fetch actual stickers list from Supabase API
        // You can use Retrofit or HttpURLConnection to call:
        // GET https://[YOUR-SUPABASE-URL]/storage/v1/object/list/stickers
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📦 Chọn Sticker");
        builder.setMessage("💡 Chức năng sticker từ Supabase đang được triển khai.\n\n" +
                "Hãy thay đổi URL placeholder trong code:\n" +
                "showStickerPicker() method\n\n" +
                "Khi hoàn thành, sticker sẽ hiển thị dưới dạng lưới.");
        
        builder.setPositiveButton("Đóng", (dialog, which) -> {
            Log.d(TAG, "Sticker picker dialog closed");
            dialog.dismiss();
        });
        
        builder.show();
        
        // 📝 IMPLEMENTATION NOTES FOR USER:
        // 1. Get Supabase project URL and API key
        // 2. Create a "stickers" bucket in Supabase Storage
        // 3. Upload sticker images (PNG/WebP format recommended)
        // 4. Fetch sticker list using Supabase SDK or REST API
        // 5. Display in GridView or RecyclerView
        // 6. On selection: send as message (either as image URL or sticker code)
        
        // Example Supabase fetch code (to be implemented):
        /*
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://[YOUR-SUPABASE-URL]/storage/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        StorageApi api = retrofit.create(StorageApi.class);
        Call<List<Sticker>> call = api.listStickers("Bearer " + supabaseToken);
        call.enqueue(new Callback<List<Sticker>>() {
            @Override
            public void onResponse(Call<List<Sticker>> call, Response<List<Sticker>> response) {
                if (response.isSuccessful()) {
                    List<Sticker> stickers = response.body();
                    // Display stickers in dialog
                }
            }
            
            @Override
            public void onFailure(Call<List<Sticker>> call, Throwable t) {
                Log.e(TAG, "Failed to fetch stickers", t);
            }
        });
        */
    }

    // ═══════════════════════════════════════════════════════════════
    // ⏰ SELF-DESTRUCT FEATURE - Toggle and select timer duration
    // ═══════════════════════════════════════════════════════════════
    private void toggleSelfDestructMode() {
        Log.d(TAG, "⏰ toggleSelfDestructMode() called");
        Log.d(TAG, "   Current state: " + (isSelfDestructEnabled ? "ON" : "OFF"));
        
        if (isSelfDestructEnabled) {
            // Already on - clicking again disables it
            isSelfDestructEnabled = false;
            selfDestructDuration = 0;
            updateSelfDestructButton();
            Toast.makeText(this, "❌ Tự hủy: TẮT", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "   → Self-destruct disabled");
        } else {
            // Show dialog to select duration
            showSelfDestructDialog();
        }
    }

    private void showSelfDestructDialog() {
        Log.d(TAG, "⏰ showSelfDestructDialog() called");
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⏳ Chọn Thời Gian Tự Hủy");
        builder.setMessage("Tin nhắn sẽ tự động xóa sau khoảng thời gian chọn:");
        
        // Time options and their durations in milliseconds
        CharSequence[] items = {
            "⚡ 5 giây",
            "🕐 30 giây",
            "📍 1 phút",
            "📌 5 phút",
            "⏱️ 30 phút",
            "🕰️ 1 giờ",
            "⏳ 12 giờ",
            "📅 24 giờ"
        };
        
        long[] durations = {
            5 * 1000,                    // 5 seconds
            30 * 1000,                   // 30 seconds
            1 * 60 * 1000,               // 1 minute
            5 * 60 * 1000,               // 5 minutes
            30 * 60 * 1000,              // 30 minutes
            1 * 60 * 60 * 1000,          // 1 hour
            12 * 60 * 60 * 1000,         // 12 hours
            24 * 60 * 60 * 1000          // 24 hours
        };
        
        builder.setSingleChoiceItems(items, -1, (dialog, which) -> {
            selfDestructDuration = durations[which];
            isSelfDestructEnabled = true;
            updateSelfDestructButton();
            
            String durationText = items[which].toString();
            Log.d(TAG, "✅ Self-destruct timer set to: " + durationText + " (" + selfDestructDuration + "ms)");
            Toast.makeText(ChatActivity.this, "⏰ Tự hủy: " + durationText, Toast.LENGTH_SHORT).show();
            
            dialog.dismiss();
        });
        
        builder.setNegativeButton("❌ Hủy", (dialog, which) -> {
            Log.d(TAG, "Self-destruct dialog cancelled");
            dialog.dismiss();
        });
        
        builder.show();
    }

    private void updateSelfDestructButton() {
        Log.d(TAG, "🔄 updateSelfDestructButton() - State: " + (isSelfDestructEnabled ? "ON" : "OFF"));
        
        if (isSelfDestructEnabled) {
            // Visual indication that self-destruct is active
            btnSelfDestruct.setAlpha(1.0f);  // Full opacity - ACTIVE
            btnSelfDestruct.setColorFilter(getResources().getColor(android.R.color.holo_orange_light), 
                    android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            // Visual indication that self-destruct is inactive
            btnSelfDestruct.setAlpha(0.5f);  // Half opacity - INACTIVE
            btnSelfDestruct.clearColorFilter();
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