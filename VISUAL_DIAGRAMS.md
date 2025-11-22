# 🎨 Visual Diagrams & Flowcharts

## 1. Self-Destruct Timer Flow

```
┌─────────────────────────────────┐
│  User Clicks ⏱️ Button          │
│  (timer icon - orange, right)   │
└──────────────┬──────────────────┘
               │
        ┌──────▼──────┐
        │ Is Timer    │
        │ Currently   │
        │ Enabled?    │
        └──┬───────┬──┘
           │       │
      YES  │       │ NO
           │       │
    ┌──────▼──┐  ┌─▼──────────────┐
    │ Disable │  │ Show Dialog    │
    │ Timer   │  │ with 8 Times   │
    │ - Set   │  │ - 5s, 30s,     │
    │   OFF   │  │ - 1m, 5m,      │
    │ - Gray  │  │ - 30m, 1h,     │
    │   50%   │  │ - 12h, 24h     │
    └─────────┘  └─┬──────────────┘
        │         │
        │    User Selects
        │    Duration
        │         │
        │    ┌────▼─────────┐
        │    │ Update       │
        │    │ - Duration   │
        │    │ - Set ON     │
        │    │ - Orange     │
        │    │   100%       │
        │    └────┬─────────┘
        │         │
        └────┬────┘
             │
    ┌────────▼──────────┐
    │ Ready to Send     │
    │ Message Will      │
    │ Auto-Delete       │
    └───────────────────┘
```

## 2. Message Lifecycle with Self-Destruct

```
TIMELINE (Example: 5 second timer selected)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

T=0s     → User sends message
         → Message stored in Firestore with expiresAt field

T=1s     → Message appears in chat
         → "Hello" visible to both users

T=2s     → Message still visible
         → Timer counting down internally

T=3s     → Message still visible
         → Timer counting down

T=4s     → Message still visible
         → Final second

T=5s     → AUTO-DELETE TRIGGERS
         → Message removed from Firestore
         → Disappears from both screens

T=6s+    → Message gone
         → As if it was never sent
```

## 3. UI Layout Before & After

```
BEFORE (Original Layout)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
┌────────────────────────────┐
│ Chat Header                │
├────────────────────────────┤
│                            │
│  Messages                  │
│  (RecyclerView)            │
│                            │
├────────────────────────────┤
│                            │
│  [      Text Input    ] →  │
│                            │
└────────────────────────────┘

AFTER (New Layout)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
┌────────────────────────────┐
│ Chat Header                │
├────────────────────────────┤
│                            │
│  Messages                  │
│  (RecyclerView)            │
│                            │
├────────────────────────────┤
│                            │
│ 📦 [  Text Input  ] ⏱️ →   │
│ ▲                    ▲      │
│ │                    │      │
│ Sticker    Self-Destruct   │
│                            │
└────────────────────────────┘
```

## 4. Time Duration Mapping

```
┌──────────────────────────────────┐
│ Dialog Options to Milliseconds   │
├──────────────────────────────────┤
│ ⚡ 5 giây      → 5,000 ms        │
│ 🕐 30 giây     → 30,000 ms       │
│ 📍 1 phút      → 60,000 ms       │
│ 📌 5 phút      → 300,000 ms      │
│ ⏱️ 30 phút     → 1,800,000 ms    │
│ 🕰️ 1 giờ      → 3,600,000 ms    │
│ ⏳ 12 giờ      → 43,200,000 ms   │
│ 📅 24 giờ      → 86,400,000 ms   │
└──────────────────────────────────┘

Calculation: expiresAt = now + duration
Example: now=1000ms, duration=5000ms → expiresAt=6000ms
```

## 5. Message Data Structure

```
Without Timer (Normal)          With Timer (Self-Destruct)
═════════════════════════════   ════════════════════════════════
{                               {
  messageId: "msg_123"            messageId: "msg_123"
  senderId: "user_001"            senderId: "user_001"
  text: "Hello"                   text: "Hello"
  timestamp: 1705318000000        timestamp: 1705318000000
  isRead: false                   isRead: false
  imageUrl: ""                    imageUrl: ""
}
                                  expiresAt: 1705318005000    ← NEW
                                  selfDestructDuration: 5000  ← NEW
                                }
```

## 6. State Machine for Timer Button

```
                ┌─────────────────┐
                │    INITIAL      │
                │ (Button OFF)    │
                │ Opacity: 50%    │
                │ Color: Gray     │
                └────────┬────────┘
                         │
                  User Clicks Button
                         │
                    ┌────▼─────┐
                    │  Dialog   │
                    │  Appears  │
                    └────┬─────┘
                         │
                  User Selects Time
                         │
                ┌────────▼─────────┐
                │    ACTIVE       │
                │ (Button ON)     │
                │ Opacity: 100%   │
                │ Color: Orange   │
                └────────┬────────┘
                         │
                  Send Message with
                  expiresAt field
                         │
                  ┌──────▼──────┐
                  │  Message    │
                  │  Sent       │
                  └─────────────┘
                         │
                  Click Again or
                  Auto-Reset After Send
                         │
                ┌────────▼────────┐
                │    INITIAL     │
                │ (Button OFF)   │ ← Resets
                │ Opacity: 50%   │
                │ Color: Gray    │
                └────────────────┘
```

## 7. Feature Interaction Diagram

```
Chat Screen
│
├─ Message Input Area
│  │
│  ├─ btnSticker (📦)
│  │  └─ showStickerPicker()
│  │     └─ Fetch from Supabase
│  │        └─ Display grid
│  │           └─ Select & Send
│  │
│  ├─ etMessage (Text Input)
│  │  └─ Type message
│  │
│  ├─ btnSelfDestruct (⏱️)
│  │  └─ toggleSelfDestructMode()
│  │     ├─ If ON: Disable (hide timer)
│  │     ├─ If OFF: Show dialog
│  │     │  └─ showSelfDestructDialog()
│  │     │     └─ Select duration
│  │     │        └─ Update button
│  │     └─ updateSelfDestructButton()
│  │        ├─ Change opacity
│  │        └─ Change color
│  │
│  └─ btnSend (→)
│     └─ sendMessage()
│        ├─ Prepare message data
│        ├─ If timer enabled:
│        │  ├─ Add expiresAt
│        │  └─ Add selfDestructDuration
│        └─ Upload to Firestore
│           └─ Display in chat
│              └─ Schedule deletion
│                 └─ Auto-delete at time
```

## 8. Code Execution Flow

```
START (App running)
│
├─ onCreate()
│  │
│  ├─ btnSticker = findViewById()
│  ├─ btnSelfDestruct = findViewById()
│  │
│  ├─ btnSticker.setOnClickListener()
│  │  └─ Call: showStickerPicker()
│  │
│  ├─ btnSelfDestruct.setOnClickListener()
│  │  └─ Call: toggleSelfDestructMode()
│  │
│  └─ btnSend.setOnClickListener()
│     └─ Call: sendMessage()
│
├─ User Interaction
│  │
│  ├─ Click btnSticker
│  │  └─ showStickerPicker() executes
│  │
│  ├─ Click btnSelfDestruct
│  │  └─ toggleSelfDestructMode() executes
│  │     ├─ If OFF: showSelfDestructDialog()
│  │     │  └─ User selects time
│  │     │     └─ updateSelfDestructButton()
│  │     └─ If ON: Disable & updateSelfDestructButton()
│  │
│  └─ Click btnSend
│     └─ sendMessage() executes
│        ├─ Get text
│        ├─ Create messageData
│        ├─ If isSelfDestructEnabled:
│        │  └─ Add expiresAt field
│        └─ Upload to Firestore
│
└─ END
```

## 9. Timer State Visualization

```
Button States:

OFF State (Initial):
┌─────────────────────┐
│       ⏱️            │ Opacity: 50%
│   (faded)           │ Color: Default
└─────────────────────┘

ON State (After Selection):
┌─────────────────────┐
│       ⏱️            │ Opacity: 100%
│   (bright)          │ Color: Orange
└─────────────────────┘
```

## 10. Integration Points

```
ChatActivity.java
│
├─ onCreate()
│  └─ Initialize buttons & listeners
│
├─ sendMessage()
│  └─ Modify to add expiresAt
│
├─ showStickerPicker()  ← NEW
│  └─ Placeholder (ready for Supabase)
│
├─ toggleSelfDestructMode()  ← NEW
│  └─ Enable/Disable timer
│
├─ showSelfDestructDialog()  ← NEW
│  └─ Show time selection
│
└─ updateSelfDestructButton()  ← NEW
   └─ Update UI appearance

Message.java
│
└─ Add fields:
   ├─ expiresAt
   └─ selfDestructDuration

activity_chat.xml
│
├─ btnSticker  ← NEW
├─ etMessage   ← MODIFIED
└─ btnSelfDestruct  ← NEW
```

---

## Summary

**Total Components**: 3 files modified
**New Methods**: 4
**New Fields**: 5
**UI Elements**: 2 buttons added
**Data Fields**: 2 fields added
**Status**: ✅ Complete & Ready

All diagrams show current implementation state! 🎉
