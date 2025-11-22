# 🎨 Sticker & ⏰ Self-Destruct Message Feature Guide

## Overview

This guide explains the new UI features added to ChatActivity:

1. **🎨 Sticker Picker** - Send stickers from Supabase
2. **⏰ Self-Destruct Messages** - Auto-delete messages after selected duration

---

## ✅ What's Already Done

### 1. **UI Layout** (activity_chat.xml)

-   ✅ Added `btnSticker` (📦 emoji icon, left side)
-   ✅ Added `btnSelfDestruct` (⏱️ timer icon, orange color)
-   ✅ Proper spacing and layout constraints

### 2. **ChatActivity Java Code**

-   ✅ Added button field declarations
-   ✅ Added initialization in `onCreate()`
-   ✅ Added click listeners
-   ✅ Added `showStickerPicker()` method (placeholder)
-   ✅ Added `showSelfDestructDialog()` method
-   ✅ Added `toggleSelfDestructMode()` method
-   ✅ Added `updateSelfDestructButton()` method
-   ✅ Modified `sendMessage()` to include self-destruct fields

### 3. **Message Model** (Message.java)

-   ✅ Added `expiresAt` field (Long) - Timestamp of auto-delete
-   ✅ Added `selfDestructDuration` field (Long) - Duration in milliseconds

---

## 🎨 Sticker Feature - Implementation TODO

### Current State

-   Button exists and opens a placeholder dialog
-   Ready for Supabase integration

### How to Complete

#### Step 1: Set Up Supabase Storage

1. Go to [Supabase Dashboard](https://app.supabase.com)
2. Create a new storage bucket called `stickers`
3. Upload your sticker images (PNG/WebP format recommended)
4. Set bucket to public for easy access
5. Get your Supabase URL and API Key

#### Step 2: Update Placeholder URL

Find this line in `ChatActivity.java` → `showStickerPicker()` method:

```java
String stickerUrl = "https://example-placeholder.supabase.co/storage/v1/object/public/stickers/";
```

Replace with your actual URL:

```java
String stickerUrl = "https://YOUR-PROJECT-ID.supabase.co/storage/v1/object/public/stickers/";
```

#### Step 3: Fetch Stickers (Using Retrofit)

Add to `build.gradle` (app level):

```gradle
dependencies {
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
}
```

Create `StickerApi.java` interface:

```java
package com.example.locketbaseapp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface StickerApi {
    @GET("storage/v1/object/list/stickers")
    Call<StickerListResponse> listStickers(@Header("Authorization") String token);
}

class StickerListResponse {
    @SerializedName("name")
    public List<String> stickerNames;
}
```

#### Step 4: Replace Placeholder Implementation

Replace the current `showStickerPicker()` method with:

```java
private void showStickerPicker() {
    Log.d(TAG, "🎨 showStickerPicker() called");

    String stickerUrl = "https://YOUR-PROJECT-ID.supabase.co/storage/v1/object/public/stickers/";
    String supabaseKey = "YOUR_SUPABASE_KEY"; // Get from Supabase settings

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://YOUR-PROJECT-ID.supabase.co/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    StickerApi api = retrofit.create(StickerApi.class);
    Call<StickerListResponse> call = api.listStickers("Bearer " + supabaseKey);

    call.enqueue(new Callback<StickerListResponse>() {
        @Override
        public void onResponse(Call<StickerListResponse> call, Response<StickerListResponse> response) {
            if (response.isSuccessful() && response.body() != null) {
                List<String> stickers = response.body().stickerNames;
                showStickerGrid(stickers, stickerUrl);
            }
        }

        @Override
        public void onFailure(Call<StickerListResponse> call, Throwable t) {
            Log.e(TAG, "Failed to fetch stickers", t);
            Toast.makeText(ChatActivity.this, "Lỗi tải sticker", Toast.LENGTH_SHORT).show();
        }
    });
}

private void showStickerGrid(List<String> stickers, String baseUrl) {
    // Create AlertDialog with GridView showing sticker thumbnails
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("📦 Chọn Sticker");

    // Create GridView
    GridView gridView = new GridView(this);
    gridView.setNumColumns(4);
    gridView.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            600
    ));

    StickerAdapter adapter = new StickerAdapter(this, stickers, baseUrl);
    gridView.setAdapter(adapter);

    gridView.setOnItemClickListener((parent, view, position, id) -> {
        String stickerName = stickers.get(position);
        sendStickerMessage(baseUrl + stickerName);
    });

    builder.setView(gridView);
    builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
    builder.show();
}

private void sendStickerMessage(String stickerUrl) {
    Log.d(TAG, "📤 Sending sticker: " + stickerUrl);

    Map<String, Object> messageData = new HashMap<>();
    messageData.put("senderId", currentUserId);
    messageData.put("text", "[sticker]");
    messageData.put("timestamp", FieldValue.serverTimestamp());
    messageData.put("isRead", false);
    messageData.put("imageUrl", stickerUrl);

    // Add self-destruct fields if enabled
    if (isSelfDestructEnabled && selfDestructDuration > 0) {
        messageData.put("expiresAt", System.currentTimeMillis() + selfDestructDuration);
        messageData.put("selfDestructDuration", selfDestructDuration);
    }

    // Send to Firebase
    db.collection("chats").document(chatId)
            .collection("messages").add(messageData)
            .addOnSuccessListener(ref -> {
                Log.d(TAG, "✅ Sticker sent");
                etMessage.setText("");
                isSelfDestructEnabled = false;
                updateSelfDestructButton();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to send sticker: " + e.getMessage());
                Toast.makeText(ChatActivity.this, "Lỗi gửi sticker", Toast.LENGTH_SHORT).show();
            });
}
```

---

## ⏰ Self-Destruct Feature - Usage Guide

### How It Works

1. User clicks ⏱️ timer icon (orange button)
2. Dialog appears with 8 time options
3. User selects duration (5s, 30s, 1m, 5m, 30m, 1h, 12h, 24h)
4. Timer button becomes fully opaque (indicating ON)
5. When message is sent, it includes `expiresAt` and `selfDestructDuration` fields
6. Message will auto-delete at the specified time

### Current State

-   ✅ UI fully implemented
-   ✅ Dialog with all time options
-   ✅ Timer state tracking
-   ✅ Visual feedback (button opacity)
-   ⏳ Message deletion logic (needs Cloud Function or scheduled cleanup)

### TODO: Implement Message Deletion

#### Option A: Cloud Function (Server-side - RECOMMENDED)

Create Firestore Cloud Function to delete expired messages:

```javascript
// In Firebase Console → Functions
const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.deleteExpiredMessages = functions.pubsub
    .schedule("every 5 minutes")
    .onRun(async (context) => {
        const db = admin.firestore();
        const now = Date.now();

        const snapshot = await db
            .collectionGroup("messages")
            .where("expiresAt", "<", now)
            .get();

        const batch = db.batch();
        snapshot.docs.forEach((doc) => {
            batch.delete(doc.ref);
        });

        await batch.commit();
        console.log(`Deleted ${snapshot.size} expired messages`);
        return null;
    });
```

#### Option B: Client-side Timer (Local - simpler)

Add to `ChatActivity.java`:

```java
private void scheduleMessageDeletion(String messageId, long expiresAt) {
    long delayMs = expiresAt - System.currentTimeMillis();

    if (delayMs > 0) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            db.collection("chats").document(chatId)
                    .collection("messages").document(messageId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ Message auto-deleted: " + messageId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Failed to delete message", e);
                    });
        }, delayMs);
    }
}
```

Then call in `loadMessages()` when displaying each message:

```java
if (message.expiresAt != null) {
    scheduleMessageDeletion(message.messageId, message.expiresAt);
}
```

---

## 🧪 Testing Checklist

### Sticker Feature

-   [ ] Upload test stickers to Supabase
-   [ ] Click sticker icon → Dialog appears
-   [ ] Stickers load from Supabase
-   [ ] Select sticker → Message sent with sticker
-   [ ] Sticker appears in chat

### Self-Destruct Feature

-   [ ] Click ⏱️ icon → Dialog shows 8 options
-   [ ] Select 5 seconds → Button becomes opaque
-   [ ] Send message → Message displays in chat
-   [ ] After 5 seconds → Message auto-deletes
-   [ ] Click ⏱️ again → Toggle OFF
-   [ ] Button becomes half-transparent

### Combined Features

-   [ ] Enable self-destruct + send sticker
-   [ ] Sticker deletes after selected time
-   [ ] Works with both text and sticker messages

---

## 🔗 Important Notes

### Security

-   ⚠️ Don't hardcode Supabase key in code
-   ✅ Use secure key management (use gradle.properties or Firebase Remote Config)

### Performance

-   ⚠️ Fetching sticker list repeatedly can be slow
-   ✅ Cache sticker list in SharedPreferences
-   ✅ Show loading indicator while fetching

### Message Deletion

-   ⚠️ Cloud Function runs every 5 minutes → slight delay
-   ✅ Client-side timer is immediate but only works if app is open
-   ✅ Combine both for best UX

---

## 📱 Files Modified

1. **activity_chat.xml** - Added UI buttons
2. **ChatActivity.java** - Added feature logic
3. **Message.java** - Added data model fields

## 📝 Files to Create

1. **StickerAdapter.java** - GridView adapter for stickers
2. **StickerApi.java** - Retrofit API interface
3. **Cloud Function** (optional but recommended)

---

## 💬 Example Usage Flow

```
User A in Chat with User B:
1. Clicks sticker icon 🎨
2. Selects sticker from grid
3. Clicks timer icon ⏱️ → 5 minutes
4. Sends sticker
5. Sticker appears in chat as image
6. After 5 minutes → automatically deleted from both sides
```

---

**Questions?** Check logcat output with "TAG_CHAT" for debugging messages.
