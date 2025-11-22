# 📍 Code Location Reference

## Quick Navigation

### Self-Destruct Timer Feature

**1. Button Declaration (ChatActivity.java)**

-   **Line**: ~42
-   **Code**: `private ImageButton btnSticker, btnSelfDestruct;`
-   **Also Added**:
    ```
    private boolean isSelfDestructEnabled = false;
    private long selfDestructDuration = 0;
    ```

**2. Button Initialization (ChatActivity.java)**

-   **Location**: `onCreate()` method, around line 100
-   **Code**:
    ```java
    btnSticker = findViewById(R.id.btnSticker);
    btnSelfDestruct = findViewById(R.id.btnSelfDestruct);
    btnSelfDestruct.setOnClickListener(v -> toggleSelfDestructMode());
    ```

**3. Dialog Implementation (ChatActivity.java)**

-   **Method**: `showSelfDestructDialog()`
-   **Code**: Shows AlertDialog with 8 time options
-   **Options**:
    -   5s, 30s, 1m, 5m, 30m, 1h, 12h, 24h

**4. Toggle Method (ChatActivity.java)**

-   **Method**: `toggleSelfDestructMode()`
-   **Logic**: Click once to enable → Click again to disable

**5. Button Visual Update (ChatActivity.java)**

-   **Method**: `updateSelfDestructButton()`
-   **Effect**: Changes opacity and color based on state

**6. Message Modification (ChatActivity.java)**

-   **Location**: `sendMessage()` method
-   **Change**: Adds expiresAt and selfDestructDuration fields to messageData

---

### Sticker Feature

**1. Button Declaration (ChatActivity.java)**

-   **Line**: ~42
-   **Code**: `private ImageButton btnSticker, btnSelfDestruct;`

**2. Button Initialization (ChatActivity.java)**

-   **Location**: `onCreate()` method, around line 100
-   **Code**:
    ```java
    btnSticker = findViewById(R.id.btnSticker);
    btnSticker.setOnClickListener(v -> showStickerPicker());
    ```

**3. Placeholder Implementation (ChatActivity.java)**

-   **Method**: `showStickerPicker()`
-   **Current**: Shows dialog with instructions
-   **TODO**: Replace with actual Supabase fetching code
-   **Placeholder URL**:
    ```java
    String stickerUrl = "https://example-placeholder.supabase.co/storage/v1/object/public/stickers/";
    // ↑ User must change this line to their Supabase URL
    ```

---

### UI Layout

**File**: `activity_chat.xml`

**Sticker Button Section**:

```xml
<ImageButton
    android:id="@+id/btnSticker"
    android:layout_alignParentStart="true"
    android:tint="#FF6B9D" />
```

**Self-Destruct Button Section**:

```xml
<ImageButton
    android:id="@+id/btnSelfDestruct"
    android:layout_toStartOf="@id/btnSend"
    android:tint="#FFB74D" />
```

**Modified EditText**:

```xml
<EditText
    android:layout_toEndOf="@id/btnSticker"
    android:layout_toStartOf="@id/btnSelfDestruct" />
```

---

### Data Model

**File**: `Message.java`

**Added Fields** (around line 15):

```java
public Long expiresAt;
public Long selfDestructDuration;
```

---

## 🎯 Search Tips

### Find Self-Destruct Code

Use VS Code Find (Ctrl+F):

-   Search: `isSelfDestructEnabled`
-   Found in: ChatActivity.java (multiple locations)

### Find Timer Dialog

Use VS Code Find (Ctrl+F):

-   Search: `showSelfDestructDialog`
-   Found in: ChatActivity.java (1 location)

### Find Time Options

Use VS Code Find (Ctrl+F):

-   Search: `5 giây` or `CharSequence[] items`
-   Found in: ChatActivity.java (showSelfDestructDialog method)

### Find Sticker Button

Use VS Code Find (Ctrl+F):

-   Search: `showStickerPicker`
-   Found in: ChatActivity.java (1 location)

### Find Message Modification

Use VS Code Find (Ctrl+F):

-   Search: `expiresAt`
-   Found in:
    -   ChatActivity.java (sendMessage method)
    -   Message.java (field definition)

---

## 📱 Visual Code Map

### ChatActivity.java Structure:

```
Line 42:     Field declarations (btnSticker, btnSelfDestruct, counters)
Line 100:    findViewById + setOnClickListener in onCreate()
Line 391:    sendMessage() method → adds expiresAt logic
Line 524:    showStickerPicker() - NEW METHOD
Line 560:    toggleSelfDestructMode() - NEW METHOD
Line 575:    showSelfDestructDialog() - NEW METHOD
Line 620:    updateSelfDestructButton() - NEW METHOD
```

### Message.java Structure:

```
Line 15:     expiresAt field
Line 16:     selfDestructDuration field
```

### activity_chat.xml Structure:

```
Line XX:     btnSticker ImageButton
Line XX:     btnSelfDestruct ImageButton
Line XX:     etMessage EditText (modified)
Line XX:     btnSend ImageButton
```

---

## 🔧 How to Find Each Component

### Looking for the main timer logic?

→ Search for `showSelfDestructDialog()` in ChatActivity.java

### Looking for button initialization?

→ Search for `btnSelfDestruct = findViewById` in ChatActivity.java

### Looking for the message modification?

→ Search for `expiresAt` in ChatActivity.java (sendMessage method)

### Looking for the data model?

→ Search for `expiresAt` in Message.java

### Looking for button styling?

→ Search for `btnSticker` or `btnSelfDestruct` in activity_chat.xml

### Looking for timer constants?

→ Search for `durations` in ChatActivity.java (showSelfDestructDialog method)

---

## 📋 Complete Feature Checklist

### For Self-Destruct Feature:

-   [ ] Field declarations added ✅
-   [ ] Button initialized ✅
-   [ ] Click listener attached ✅
-   [ ] Toggle method working ✅
-   [ ] Dialog showing 8 options ✅
-   [ ] Duration stored correctly ✅
-   [ ] Message includes expiresAt ✅
-   [ ] Button visual feedback working ✅

### For Sticker Feature:

-   [ ] Field declarations added ✅
-   [ ] Button initialized ✅
-   [ ] Click listener attached ✅
-   [ ] Placeholder dialog showing ✅
-   [ ] Ready for Supabase integration ⏳

### For Data Model:

-   [ ] expiresAt field added ✅
-   [ ] selfDestructDuration field added ✅

---

## 🚀 Next Steps Location

### To Add Message Auto-Deletion:

See: `STICKER_SELF_DESTRUCT_GUIDE.md`
Method: Create Cloud Function or add Timer in ChatActivity.java

### To Add Supabase Integration:

See: `STICKER_SELF_DESTRUCT_GUIDE.md`
Method: Replace placeholder URL, add Retrofit code

### To Debug Issues:

Search logcat for: `ChatActivity` or `d/TAG`
All new code has comprehensive logging

---

## 💾 Files Modified Summary

| File              | Lines Modified | Type    | Status      |
| ----------------- | -------------- | ------- | ----------- |
| ChatActivity.java | ~300           | Feature | ✅ Complete |
| Message.java      | 2              | Model   | ✅ Complete |
| activity_chat.xml | ~50            | UI      | ✅ Complete |

---

**Total Code Added**: ~350 lines | **Compilation**: ✅ No errors | **Status**: Ready for testing!
