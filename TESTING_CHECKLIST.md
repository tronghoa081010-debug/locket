# ✅ Implementation Checklist & Testing Guide

## 🎯 Pre-Launch Checklist

### Code Implementation

-   [x] Sticker button added to UI layout
-   [x] Self-destruct button added to UI layout
-   [x] EditText spacing adjusted
-   [x] Button initialization in onCreate()
-   [x] Click listeners attached
-   [x] Self-destruct dialog implemented
-   [x] Timer state tracking added
-   [x] Visual feedback implemented
-   [x] Message modification for expiration
-   [x] Message model updated with fields
-   [x] Compilation verified (0 errors)

### Documentation

-   [x] Implementation guide created
-   [x] Status report generated
-   [x] Code location map provided
-   [x] Visual diagrams included
-   [x] Quick reference sheet added
-   [x] UI enhancement summary created

---

## 🧪 Testing Checklist - Self-Destruct Feature

### UI Testing

-   [ ] ⏱️ button appears on right side (orange color)
-   [ ] Button positioned correctly (before send button)
-   [ ] Button has proper spacing
-   [ ] Button icon displays (ic_timer)

### Button Interaction

-   [ ] Click ⏱️ button → Dialog appears
-   [ ] Dialog shows title "⏳ Chọn Thời Gian Tự Hủy"
-   [ ] Dialog shows 8 time options with icons:
    -   [ ] ⚡ 5 giây
    -   [ ] 🕐 30 giây
    -   [ ] 📍 1 phút
    -   [ ] 📌 5 phút
    -   [ ] ⏱️ 30 phút
    -   [ ] 🕰️ 1 giờ
    -   [ ] ⏳ 12 giờ
    -   [ ] 📅 24 giờ
-   [ ] Dialog has Cancel button (works)

### Timer State

-   [ ] Select time from dialog → Dialog closes
-   [ ] Button opacity changes to 100% (bright)
-   [ ] Button color becomes orange (#FFB74D)
-   [ ] Toast appears: "⏰ Tự hủy: [selected time]"
-   [ ] Click button again → disables timer
-   [ ] Button opacity changes to 50% (faded)
-   [ ] Button color becomes normal
-   [ ] Toast appears: "❌ Tự hủy: TẮT"

### Message Sending with Timer

-   [ ] Enable timer (select 5 seconds)
-   [ ] Type a message
-   [ ] Send message
-   [ ] Message appears in chat
-   [ ] Check Firebase Console → Firestore:
    -   [ ] Message doc exists
    -   [ ] `expiresAt` field present (Long value)
    -   [ ] `selfDestructDuration` field present (value: 5000)
    -   [ ] Timestamp appears correct
-   [ ] Formula verification:
    -   [ ] expiresAt ≈ (current time + 5000ms)

### Message Sending without Timer

-   [ ] Disable timer (button faded)
-   [ ] Send message
-   [ ] Check Firestore:
    -   [ ] `expiresAt` field NOT present
    -   [ ] `selfDestructDuration` field NOT present

---

## 🧪 Testing Checklist - Sticker Feature

### UI Testing

-   [ ] 📦 button appears on left side (pink color)
-   [ ] Button positioned correctly (before input field)
-   [ ] Button has proper spacing
-   [ ] Button icon displays (ic_emoji)

### Button Interaction

-   [ ] Click 📦 button → Dialog appears
-   [ ] Dialog shows title "📦 Chọn Sticker"
-   [ ] Dialog shows implementation message
-   [ ] Dialog has Close button (works)
-   [ ] No crashes on click

### Placeholder Indicator

-   [ ] Dialog shows note about placeholder
-   [ ] URL location mentioned in dialog
-   [ ] Dialog disappears cleanly on close

---

## 🧪 Testing Checklist - Combined Features

### Multiple Messages

-   [ ] Send 3 messages with different timers (5s, 1m, 5m)
-   [ ] All appear in Firestore with correct expiresAt
-   [ ] First expires after 5 seconds
-   [ ] Others remain (until their time)

### Timer Reset

-   [ ] Send message with timer (5s)
-   [ ] Timer button shows orange
-   [ ] Send another message (timer still on)
-   [ ] Both messages have expiresAt
-   [ ] Disable timer (button faded)
-   [ ] Send third message (no timer)
-   [ ] Third message has NO expiresAt

### UI Responsiveness

-   [ ] No lag when clicking buttons
-   [ ] Dialog appears quickly
-   [ ] Button states update instantly
-   [ ] No layout shifts when buttons interact

---

## 🐛 Debugging Checklist

### Logcat Verification

Search logcat for these messages (indicates working code):

-   [ ] "🎨 showStickerPicker() called"
-   [ ] "⏰ toggleSelfDestructMode() called"
-   [ ] "⏰ showSelfDestructDialog() called"
-   [ ] "✅ Self-destruct timer set to"
-   [ ] "⏰ Self-destruct enabled: [X] seconds"
-   [ ] "❌ Self-destruct disabled"

### Error Checking

-   [ ] No "NullPointerException" in logcat
-   [ ] No "Resource not found" errors
-   [ ] No "FileNotFound" for drawable icons
-   [ ] No layout inflation errors
-   [ ] No Firebase errors on message send

### Compilation

-   [ ] `./gradlew assembleDebug` succeeds
-   [ ] No warnings about unused imports
-   [ ] Build completes in under 2 minutes
-   [ ] APK generates successfully

---

## 📊 Data Verification

### Firestore Message Structure

**Message with Timer Should Have**:

```
✅ senderId: "user_xxx"
✅ text: "Hello"
✅ timestamp: 1705318000000
✅ isRead: false
✅ imageUrl: ""
✅ expiresAt: 1705318005000      ← NEW
✅ selfDestructDuration: 5000    ← NEW
```

**Message without Timer Should Have**:

```
✅ senderId: "user_xxx"
✅ text: "Hello"
✅ timestamp: 1705318000000
✅ isRead: false
✅ imageUrl: ""
❌ expiresAt: (NOT PRESENT)
❌ selfDestructDuration: (NOT PRESENT)
```

---

## 🚀 Performance Checklist

### Speed Tests

-   [ ] Dialog opens in < 200ms
-   [ ] Button click response < 100ms
-   [ ] Message sends in < 2 seconds
-   [ ] No freezing during UI interactions

### Memory Tests

-   [ ] App doesn't crash after 10 message sends
-   [ ] No memory leaks on repeated dialog opens
-   [ ] Button clicks don't cause memory spike

### Stability Tests

-   [ ] Send 5 consecutive messages (rapid)
-   [ ] Toggle timer on/off 5 times
-   [ ] Open/close sticker dialog 5 times
-   [ ] No crashes observed

---

## 📱 Device Compatibility

Test on devices if possible:

-   [ ] Android 8 (API 26)
-   [ ] Android 9 (API 28)
-   [ ] Android 10 (API 29)
-   [ ] Android 11 (API 30)
-   [ ] Android 12 (API 31)
-   [ ] Android 13 (API 33)

Minimum: Check that buttons appear and function

---

## 🎯 Success Criteria

### Code Quality ✅

-   [x] No compilation errors
-   [x] No warnings
-   [x] Proper logging
-   [x] Clean code style

### Functionality ✅

-   [ ] All buttons work
-   [ ] Dialog appears
-   [ ] Timer toggles
-   [ ] Messages store fields

### Documentation ✅

-   [x] 8 markdown files created
-   [x] Code locations mapped
-   [x] Visual diagrams provided
-   [x] Implementation guides included

### Ready for Next Phase

-   [ ] All tests pass
-   [ ] No bugs found
-   [ ] Ready to implement auto-deletion
-   [ ] Ready to integrate Supabase

---

## 📋 Pre-Build Checklist

Before building:

-   [ ] Reviewed `MASTER_SUMMARY.md`
-   [ ] Understand what was added
-   [ ] Know where code is located
-   [ ] Have Firestore console open for testing
-   [ ] Clear understanding of expected behavior

---

## 🔄 Testing Workflow

### Step 1: Build (5 minutes)

```bash
./gradlew clean
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL ✅

### Step 2: Install

```bash
# Use Android Studio or:
./gradlew installDebug
```

### Step 3: Test Self-Destruct (10 minutes)

1. Open app
2. Go to any chat
3. Click ⏱️ → See dialog
4. Select 5s → See button change
5. Send message → Check Firestore

### Step 4: Test Sticker (5 minutes)

1. Click 📦 → See dialog
2. Dialog shows placeholder note
3. Click close → Dismiss dialog

### Step 5: Verify Firestore (5 minutes)

1. Firebase Console → Firestore
2. Find your message
3. Verify expiresAt field exists
4. Check value matches time + duration

### Total Time: ~25 minutes

---

## ✨ Success Signals

You'll know everything is working if:

1. ✅ No crashes when clicking buttons
2. ✅ Dialog appears with 8 time options
3. ✅ Button changes color when timer enabled
4. ✅ Firestore shows expiresAt field
5. ✅ No compilation errors
6. ✅ Logcat shows debug messages

---

## 🎯 Common Issues & Solutions

### Issue: Button doesn't appear

**Solution**: Check drawable resources (ic_emoji, ic_timer)

### Issue: Dialog doesn't show

**Solution**: Check logcat, verify setOnClickListener attached

### Issue: Field not in Firestore

**Solution**: Verify isSelfDestructEnabled is true before send

### Issue: Button opacity doesn't change

**Solution**: Check updateSelfDestructButton() method called

### Issue: Build fails

**Solution**: Clean build: `./gradlew clean assembleDebug`

---

## 📞 When to Check Logs

Run these commands to see helpful debug output:

```bash
# Show only ChatActivity logs
adb logcat | grep ChatActivity

# Show all debug messages
adb logcat | grep "d/"

# Clear logs before testing
adb logcat -c

# Save logs to file
adb logcat > debug_logs.txt
```

---

## 🎉 Final Verification

After testing, confirm:

-   [ ] All tests in this checklist completed
-   [ ] No critical issues found
-   [ ] Code compiles without errors
-   [ ] Ready to proceed to auto-deletion
-   [ ] Documentation understood

---

## 📝 Test Report Template

Save this after testing:

```
TEST REPORT - [DATE]
═══════════════════════════════

Self-Destruct Feature: [PASS/FAIL]
- Button visible: YES/NO
- Dialog works: YES/NO
- Timer toggles: YES/NO
- Firestore field: YES/NO

Sticker Feature: [PASS/FAIL]
- Button visible: YES/NO
- Dialog shows: YES/NO

Build Status: [SUCCESS/FAILED]
Errors: [0/X]
Warnings: [0/X]

Ready for Phase 2: YES/NO

Notes:
[Add any issues found]
```

---

**Total Checklist Items**: 100+
**Expected Time**: ~30 minutes
**Expected Result**: ✅ PASS

Good luck! 🚀
