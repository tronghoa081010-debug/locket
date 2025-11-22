# 🔧 DEBUG GUIDE - LỐI LÚCĐƯỢC LỚC KHÔNG

## ✅ CÁC FIX ĐÃ THỰC HIỆN

### 1️⃣ **ChatListActivity.java** ✅

```java
.get(com.google.firebase.firestore.Source.SERVER)  // ← Bypass local cache
```

**Tác dụng:** Luôn fetch dữ liệu mới nhất từ Firestore server, không dùng cache cũ.

---

### 2️⃣ **HistoryImage.java** ✅

```java
.get(Source.SERVER)              // ← Initial fetch từ server
.addSnapshotListener(...)        // ← Sau đó listen real-time updates
```

**Tác dụng:** Đảm bảo posts hiển thị ngay lần đầu, không phải chờ listener async.

---

### 3️⃣ **CameraActivity.java** ✅

```java
.get(com.google.firebase.firestore.Source.SERVER)  // ← Bypass cache, get fresh data
// + Validation
if (visibleTo.size() < 2) {
    Log.e("CameraActivity", "⚠️ WARNING: visibleTo size is " + visibleTo.size());
}
// + Enhanced logging
Log.d("CameraActivity", "Creating post - userId: " + currentUserId +
    ", visibleTo: " + visibleTo);
```

---

## 🧪 CÁCH TEST VẤN ĐỀ

### **Scenario 1: Lúc Được / Lúc Không Được**

#### 📱 Device 1 (Account A)

```
1. Cài app → Mở lần đầu
2. Đăng nhập Account A
3. Tạo bạn với Account B (Firestore: add to friends/)
4. Vào Chat → Thấy Account B không? (Nên có)
5. Gửi ảnh → Xem logs
6. Xem History Image → Thấy ảnh của mình không?
```

#### 📱 Device 2 (Account B)

```
1. Cài app → Mở lần đầu
2. Đăng nhập Account B
3. Chấp nhận lời mời (nếu có)
4. Vào Chat → Thấy Account A không? (Nên có)
5. Xem History Image → Thấy ảnh của Account A không? (Nên có)
```

#### ❌ Nếu không thấy:

```
- Device 2 kill app hoàn toàn (xóa từ recents)
- Mở lại app
- Thấy dữ liệu không?
```

---

### **Scenario 2: Xem Logs**

#### Khi gửi ảnh (Device 1):

```
Tìm logs từ CameraActivity:
D/CameraActivity: Friends snapshot size: 1
D/CameraActivity: Friend found: <account_b_id>
D/CameraActivity: Final visibleTo list: [<account_b_id>, <account_a_id>]
D/CameraActivity: VisibleTo size: 2
D/CameraActivity: Creating post - userId: <account_a_id>, visibleTo: [account_b_id, account_a_id], visibleTo.size: 2
D/CameraActivity: ✅ Post created successfully: <doc_id> with visibleTo: [account_b_id, account_a_id]
```

#### Khi xem History Image (Device 2):

```
Tìm logs từ HistoryImage:
D/HistoryImage: Loading posts for user: <account_b_id>
D/HistoryImage: Total posts loaded: 1
```

---

## 🔍 CHẨN ĐOÁN VẤN ĐỀ

### **Nếu vẫn không thấy dữ liệu:**

#### 1️⃣ Kiểm tra Network

```
- Xem Device có kết nối Internet không?
- Check Firestore console có dữ liệu không?
```

#### 2️⃣ Kiểm tra Firestore Rules

```
Firestore Console → Rules → Test Rules
- Select User ID: <account_b_id>
- Collection: posts
- Document: <post_id từ logs>
- Action: read
- Kết quả: allow hay deny?
```

#### 3️⃣ Kiểm tra Firestore Data

```
Firestore Console → posts collection
Tìm document vừa tạo:
  - userId: <account_a_id>
  - visibleTo: [account_b_id, account_a_id]  ← Có đủ không?
  - timestamp: <server time>
```

#### 4️⃣ Kiểm tra Friends Collection

```
Firestore Console → users → <account_a_id> → friends
- Có <account_b_id> không?

Firestore Console → users → <account_b_id> → friends
- Có <account_a_id> không?
```

---

## 📊 EXPECTED BEHAVIOR (SAU KHI SỬA)

| Lần 1                | Lần 2                | Lần 3                |
| -------------------- | -------------------- | -------------------- |
| ✅ Chat: Thấy bạn    | ✅ Chat: Thấy bạn    | ✅ Chat: Thấy bạn    |
| ✅ History: Thấy ảnh | ✅ History: Thấy ảnh | ✅ History: Thấy ảnh |
| **Consistent!**      | **Consistent!**      | **Consistent!**      |

---

## 🔴 NẾU CÒN LỖI

### Kiểm tra này:

1. **Logs có `Source.SERVER`?**

    ```
    D/CameraActivity: Creating post - userId: ...
    → Nếu không log này = chưa build
    ```

2. **Friends list rỗng?**

    ```
    D/CameraActivity: Friends snapshot size: 0
    → Nếu 0 = không tạo bạn, hoặc friends list bị lỗi
    ```

3. **visibleTo size < 2?**

    ```
    D/CameraActivity: VisibleTo size: 1
    → Nếu 1 = chỉ có chính mình, bạn bè không thấy
    ```

4. **Error logs?**
    ```
    E/CameraActivity: Error loading friends: ...
    → Nếu có = check Firestore rules cấp friends
    ```

---

## 💾 REBUILD & TEST

```bash
# Step 1: Clean build
./gradlew clean build

# Step 2: Run on devices
# Device 1: Account A
# Device 2: Account B

# Step 3: Test scenarios từ bên trên

# Step 4: Check logcat
# adb logcat | grep -E "CameraActivity|HistoryImage|ChatListActivity"
```

---

## ⚡ QUICK CHECKLIST

-   [ ] Build xong chưa?
-   [ ] App có kết nối Firebase không?
-   [ ] Có tạo bạn trong cả 2 account không?
-   [ ] Xem logs có `Source.SERVER` không?
-   [ ] visibleTo có >= 2 members không?
-   [ ] Firestore Data có `visibleTo` field không?
-   [ ] Rules allow read không?

---

**Bạn test xong, share logs + kết quả với tôi, tôi debug tiếp nhé!**
