package com.example.locketbaseapp.model;

import com.google.firebase.Timestamp;
import java.util.List;

public class Chat {
    public String chatId;
    public List<String> participants;
    public String lastMessage;
    public Timestamp lastMessageTime;

    // ← THÊM 3 FIELD MỚI
    public String friendId;       // UID của friend
    public String friendName;     // Tên hiển thị
    public String friendPhoto;    // URL avatar

    public Chat() {}

    public Chat(String chatId, List<String> participants, String lastMessage, Timestamp lastMessageTime) {
        this.chatId = chatId;
        this.participants = participants;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }
}