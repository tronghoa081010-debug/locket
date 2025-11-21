package com.example.locketbaseapp.model;

import com.google.firebase.Timestamp;

public class Message {
    public String messageId;
    public String senderId;
    public String text;
    public Timestamp timestamp;
    public boolean isRead;
    public String imageUrl;  // optional for image messages

    public Message() {}

    public Message(String messageId, String senderId, String text,
                   Timestamp timestamp, boolean isRead, String imageUrl) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.imageUrl = imageUrl;
    }
}