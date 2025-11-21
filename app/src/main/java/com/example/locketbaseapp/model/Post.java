package com.example.locketbaseapp.model;

import com.google.firebase.Timestamp;
import java.util.List;

public class Post {
    public String postId;
    public String userId;
    public String imageUrl;
    public String caption;
    public Timestamp timestamp;
    public List<String> visibleTo;

    public Post() {}

    public Post(String postId, String userId, String imageUrl, String caption,
                Timestamp timestamp, List<String> visibleTo) {
        this.postId = postId;
        this.userId = userId;
        this.imageUrl = imageUrl;
        this.caption = caption;
        this.timestamp = timestamp;
        this.visibleTo = visibleTo;
    }
}