# 🔍 DEBUGGING GUIDE - CUSTOMER SERVICE MESSAGING

## STATUS BUILD
✅ **BUILD SUCCESS** - 13.908 seconds
- Compiled: 93 source files
- JAR: `target/pmb-system-1.0.0.jar`

## LANGKAH-LANGKAH DEBUGGING

### 1️⃣ BUKA BROWSER DEVELOPER TOOLS
```
tekan F12 atau Ctrl+Shift+I
pilih tab "Console"
```

### 2️⃣ LOGIN KE DASHBOARD MAHASISWA
- Akses: `http://localhost:9500/dashboard-camaba.html`
- Pastikan sudah login

### 3️⃣ KLIK TOMBOL "💬 Customer Service"
- Tunggu modal terbuka
- Anda seharusnya melihat warning:
  ```
  ⚠️ Perhatian!
  Pesan kemungkinan lama akan dibalas. Jika Anda terburu-buru, silahkan hubungi:
  📱 +62 798 3872 4679
  ```

### 4️⃣ COBA KIRIM PESAN DI CONSOLE

**Di Console (F12), ketik pesan ini:**
```javascript
// Trigger sendMessage dengan data test
document.getElementById('messageInput').value = 'Ini adalah pesan test lebih dari 5 karakter untuk testing sistem messaging';
document.querySelector('form').dispatchEvent(new Event('submit'));
```

### 5️⃣ LIHAT OUTPUT DI CONSOLE

**Anda akan melihat debug output seperti ini:**

```
🔍 DEBUG sendMessage:
  messageInput element: <textarea id="messageInput">...
  Raw value: "Ini adalah pesan test lebih dari 5 karakter untuk testing sistem messaging"
  Length raw: 77
  After trim: "Ini adalah pesan test lebih dari 5 karakter untuk testing sistem messaging"
  Length after trim: 77
  authToken exists: true

📤 Mengirim request:
  URL: /api/camaba/messages/send-to-admin
  Method: POST
  Body: {messageContent: "Ini adalah pesan test lebih dari 5 karakter untuk testing sistem messaging", messageType: "QUESTION"}

📬 Response status: 200 OK

📬 Response body: {
  success: true,
  message: "Pesan berhasil dikirim ke Admin Customer Service",
  messageId: 123,
  sentAt: "2026-03-28T01:43:27.123456"
}

✅ Pesan berhasil dikirim ke admin
```

---

## ❌ TROUBLESHOOTING - JIKA ERROR

### ERROR 1: "Pesan minimal 5 karakter"
**PENYEBAB**: Input kurang dari 5 karakter
**SOLUSI**: Ketik minimal 5 karakter

**DEBUG OUTPUT AKAN MUNCUL**:
```
⚠️ Pesan minimal 5 karakter (Anda ketik: 3 karakter)
❌ Validasi gagal: pesan kurang dari 5 karakter
```

### ERROR 2: "Gagal mengirim pesan"
**PENYEBAB**: Backend error (bisa admin tidak ada, atau error lain)
**SOLUSI**: Lihat Response body di console

**DEBUG OUTPUT AKAN MUNCUL**:
```
❌ Error response: {
  success: false,
  message: "Maaf, tidak ada admin yang tersedia untuk menerima pesan. Hubungi admin secara langsung."
}
```

**SOLUSI**:
- Pastikan admin sudah login ke dashboard
- Buat minimal 1 user dengan role `ADMIN_PUSAT` atau `ADMIN_VALIDASI`

### ERROR 3: "authToken exists: false"
**PENYEBAB**: User tidak login atau token expired
**SOLUSI**: 
- Logout dan login kembali
- Clear localStorage dan cache browser
- Cek di Application tab → Local Storage → apakah ada `authToken`?

**DEBUG OUTPUT**:
```
authToken exists: false
❌ Error sending message: Error: Failed to fetch
```

### ERROR 4: "Response status: 400, 401, 403, 500"
**PENYEBAB**: Berbeda-beda tergantung status
**SOLUSI**: Lihat exact message di Response body

**Untuk status 401** (Unauthorized):
```
📬 Response status: 401 Unauthorized
```
👉 Login kembali

**Untuk status 500** (Internal Server Error):
```
📬 Response status: 500 Internal Server Error
📬 Response body: {
  success: false,
  message: "..."
}
```
👉 Cek backend logs dengan:
```bash
java -jar target/pmb-system-1.0.0.jar
```

---

## 📋 CHECKLIST DEBUGGING

Gunakan checklist ini untuk verifikasi:

- [ ] BUILD SUCCESS (Maven)
- [ ] Java app berjalan
- [ ] Login sebagai CAMABA (mahasiswa)
- [ ] Buka dashboard-camaba.html
- [ ] Klik button "💬 Customer Service"
- [ ] Modal terbuka dengan warning message
- [ ] F12 → Console bersih (tidak ada error)
- [ ] Ketik pesan > 5 karakter
- [ ] Klik tombol "Kirim"
- [ ] Lihat Console Output:
  - [ ] Debug logs muncul
  - [ ] authToken exists: true
  - [ ] Response status: 200
  - [ ] Response body: success: true
- [ ] Pesan masuk ke Admin Dashboard
- [ ] Admin bisa balas pesan

---

## 🔄 DEBUGGING FLOW

```
1. Ketik pesan di textarea
   ↓
2. Klik tombol "Kirim" (atau submit form)
   ↓
3. JavaScript function `sendMessage()` dipanggil
   ↓
4. Validasi panjang pesan (minimal 5 karakter)
   ↓
5. Jika error → tampilkan alert
   ↓
6. Jika valid → ambil authToken dari localStorage
   ↓
7. Kirim POST request ke `/api/camaba/messages/send-to-admin`
   ↓
   Request body:
   {
     messageContent: "pesan dari user",
     messageType: "QUESTION"
   }
   ↓
8. Backend validasi ulang (5 karakter, tidak kosong)
   ↓
9. Backend cari admin (ADMIN_PUSAT atau ADMIN_VALIDASI)
   ↓
10. Jika admin tidak ada → error 500
    ↓
11. Jika ada → buat AdminMessage record di database
    ↓
12. Return response dengan status 200 + success: true
    ↓
13. Frontend clear input textarea
    ↓
14. Frontend reload messages dengan GET `/api/camaba/messages`
    ↓
15. Frontend render chat history
    ↓
16. Modal auto-close setelah 4 detik
```

---

## 🔧 COMMAND UNTUK TESTING

### Terminal 1: Jalankan App
```bash
cd d:\all code\tugasakhir
java -jar target/pmb-system-1.0.0.jar
```

Output yang benar akan seperti ini:
```
═══════════════════════════════════════════════════════════
  PMB HKBP Nommensen - Sistem Penerimaan Mahasiswa Baru
═══════════════════════════════════════════════════════════
2026-03-28 01:43:00 INFO  StartupLog : Aplikasi berjalan di port 9500
2026-03-28 01:43:00 INFO  StartupLog : Database initialized
2026-03-28 01:43:00 INFO  StartupLog : ✅ Sistem siap. Akses: http://localhost:9500
```

### Akses Dashboard
```
Browser: http://localhost:9500
```

---

## 📊 MONITORING BACKEND

**Saat mengirim pesan, lihat di backend logs:**

```
INFO  CamabaController : 🔍 Got send-to-admin request...
INFO  CamabaController : ✅ Message sent from student [email_mahasiswa] to admin [email_admin]
```

**Jika ada error:**

```
ERROR CamabaController : ❌ Error sending message: [error message]
```

---

## ✅ FINAL CHECKLIST

Sebelum production, pastikan:

- [ ] Minimal 1 Admin dengan role ADMIN_PUSAT atau ADMIN_VALIDASI sudah terdaftar
- [ ] Admin sudah login ke dashboard-admin-validasi.html
- [ ] Mahasiswa bisa kirim pesan (lihat console logs)
- [ ] Admin bisa melihat pesan di tab "Pesan CS"
- [ ] Admin bisa reply pesan
- [ ] Mahasiswa melihat reply dari admin

---

## 📞 SUPPORT INFO

Jika masih error setelah semua langkah di atas:

1. **Screenshot console output** (klik kanan → copy all logs)
2. **Screenshot backend console** (output dari java -jar)
3. **Sebutkan error message yang muncul**
4. **Sebutkan langkah yang Anda lakukan**

Nantinya saya bisa debug lebih dalam.

---

**Last Updated**: 2026-03-28 01:43:03 UTC+7  
**Build Status**: ✅ SUCCESS
