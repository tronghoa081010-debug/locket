package com.example.locketbaseapp.model;

import com.google.firebase.Timestamp;
import java.util.List;
import java.util.Map;

public class Message {
    public String messageId;
    public String senderId;
    public String text;
    public Timestamp timestamp;
    public boolean isRead;
    public String imageUrl;

    // ← THÊM 2 FIELD MỚI
    public Timestamp deliveredAt;  // Thời điểm tin nhắn đến
    public Timestamp readAt;       // Thời điểm đọc tin nhắn
    
    // 🔥 SELF-DESTRUCT FIELDS (Auto-delete message)
    public Long expiresAt;                  // Timestamp khi tin nhắn tự hủy (milliseconds)
    public Long selfDestructDuration;       // Thời gian tồn tại trước khi hủy (milliseconds)
    
    // 🔄 RECALL FIELDS (Thu hồi tin nhắn)
    public boolean recalled = false;        // Tin nhắn đã bị thu hồi?
    public Timestamp recalledAt;            // Thời gian thu hồi
    
    // ❤️ REACTION FIELDS (React emoji)
    public Map<String, List<String>> reactions;  // emoji -> [userId1, userId2, ...]

    public Message() {}

    public Message(String senderId, String text, Timestamp timestamp, boolean isRead) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.imageUrl = "";
    }

    // ← THÊM METHOD HELPER
    public String getStatus(String currentUserId) {
        if (!senderId.equals(currentUserId)) {
            return ""; // Tin nhắn nhận không hiện status
        }

        if (readAt != null) {
            return "Đã xem";
        } else if (deliveredAt != null) {
            return "Đã nhận";
        } else if (timestamp != null) {
            return "Đã gửi";
        } else {
            return "Đang gửi...";
        }
    }
    
    // Helper method to check if message is recalled
    public boolean isRecalled() {
        return recalled;
    }
    
    // Helper method to check if message has expired (self-destruct)
    public boolean hasExpired() {
        if (expiresAt == null) return false;
        return System.currentTimeMillis() > expiresAt;
    }
}