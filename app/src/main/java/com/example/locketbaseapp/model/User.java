package com.example.locketbaseapp.model;

import com.google.firebase.firestore.PropertyName;
import java.util.ArrayList;
import java.util.List;

public class User {
    public String uid;
    public String email;
    public String displayName;
    public String photoUrl;
    
    // 🔥 FIX: Map Firestore field names that might differ
    @PropertyName("profileImageUrl")
    public String profileImageUrl;
    
    @PropertyName("userId")
    public String userId;
    
    @PropertyName("username")
    public String username;

    public List<String> friends = new ArrayList<>();
    public List<String> incomingRequests = new ArrayList<>();
    public List<String> sentRequests = new ArrayList<>();

    // Constructor mặc định (bắt buộc cho Firestore)
    public User() {
    }

    public User(String uid, String email, String displayName, String photoUrl) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
    }

    // ← THÊM CÁC GETTER/SETTER CHO FIRESTORE

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    // ← THÊM GETTER CHO CÁC METHOD CŨ (để tương thích)
    public String getUserId() {
        return uid;
    }

    public String getUsername() {
        return displayName != null ? displayName : email;
    }

    public String getProfileImageUrl() {
        return photoUrl;
    }

    public List<String> getFriends() {
        return friends;
    }

    public void setFriends(List<String> friends) {
        this.friends = friends;
    }

    public List<String> getIncomingRequests() {
        return incomingRequests;
    }

    public void setIncomingRequests(List<String> incomingRequests) {
        this.incomingRequests = incomingRequests;
    }

    public List<String> getSentRequests() {
        return sentRequests;
    }

    public void setSentRequests(List<String> sentRequests) {
        this.sentRequests = sentRequests;
    }
}