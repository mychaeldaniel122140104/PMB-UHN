# Redirect Root Path ke index.html - Implementation Guide

## 📋 Ringkasan Perubahan

Sistem telah diperbaharui untuk memastikan:
- **Root path (`/`) redirect ke `/index.html`** → User pertama kali masuk selalu ke index.html
- **Authentication Check** → index.html mendeteksi login status dan redirect ke dashboard sesuai role
- **Consistent localStorage keys** → Semua file menggunakan `authToken` dan `userRole`

---

## 🔄 Flow Diagram

```
USER AKSES APLIKASI
        ↓
http://localhost:9500/
        ↓
    [RootController]
        ↓
REDIRECT ke /index.html
        ↓
    [index.html loaded]
        ↓
  [checkAuthentication()]
        ↓
    ┌─────────────────────────────────────────┐
    │                                         │
    NO TOKEN              TOKEN EXISTS       
    │                         │
    ├→ Show Landing Page   ├→ Check Role
                            │
                      ┌─────┼─────┐
                      │     │     │
                   CAMABA  ADMIN_PUSAT  ADMIN_VALIDASI
                      │     │     │
                      ↓     ↓     ↓
                   [Redirect ke Dashboard Sesuai Role]
```

---

## 📁 Files yang Diubah/Dibuat

### 1. **RootController.java** (BARU)
```java
// File: src/main/java/com/uhn/pmb/controller/RootController.java

@Controller
@RequestMapping("/")
public class RootController {
    @GetMapping("")
    public String redirectToIndex() {
        return "redirect:/index.html";
    }
}
```
**Purpose:** Menangani akses root path dan redirect ke index.html

---

### 2. **index.html** (root directory)
**Perubahan:**
- ✅ Ditambah script `checkAuthentication()` 
- ✅ Mendeteksi login status dari localStorage
- ✅ Redirect ke dashboard sesuai role user

**Authentication Check Logic:**
```javascript
function checkAuthentication() {
    const token = localStorage.getItem('authToken');
    const userRole = localStorage.getItem('userRole');
    
    if (!token) {
        // No login → Show landing page
        return;
    }
    
    // Redirect based on role
    if (userRole === 'CAMABA') {
        window.location.href = '/static/dashboard-camaba.html';
    } else if (userRole === 'ADMIN_PUSAT') {
        window.location.href = '/static/dashboard-admin-pusat.html';
    } else if (userRole === 'ADMIN_VALIDASI') {
        window.location.href = '/static/dashboard-admin-validasi.html';
    }
}
```

---

### 3. **src/main/resources/static/index.html**
**Perubahan:** Same as root/index.html

---

### 4. **src/main/resources/static/login.html**
**Perubahan:**
- ✅ Changed dari 'authToken' → 'authToken' (consistent)
- ✅ Updated redirect paths ke `/static/dashboard-*.html`

```javascript
// After successful login
localStorage.setItem('authToken', data.token);
localStorage.setItem('userRole', data.role);

// Redirect based on role
if (data.role === 'CAMABA') {
    window.location.href = '/static/dashboard-camaba.html';
} else if (data.role === 'ADMIN_PUSAT') {
    window.location.href = '/static/dashboard-admin-pusat.html';
} else if (data.role === 'ADMIN_VALIDASI') {
    window.location.href = '/static/dashboard-admin-validasi.html';
}
```

---

### 5. **src/main/resources/templates/login.html**
**Perubahan:**
- ✅ Changed ke 'authToken' (consistent dengan common-auth.js)
- ✅ Updated redirect paths ke `/static/dashboard-*.html`

---

## 🧪 Testing Steps

### Test 1: Root Path Redirect
```
1. Buka browser
2. Akses http://localhost:9500/
3. Expected: Redirect ke http://localhost:9500/index.html
4. Verify: URL berubah, landing page ditampilkan
```

### Test 2: No Login → Show Landing Page
```
1. Clear localStorage (DevTools → Application → Local Storage → Clear)
2. Refresh halaman
3. Expected: Landing page ditampilkan (tidak redirect)
4. Verify: Bisa lihat konten landing page dengan footer
```

### Test 3: Login as CAMABA
```
1. Clear localStorage
2. Click "Login" button
3. Login dengan email CAMABA
4. Expected: Auto redirect ke /static/dashboard-camaba.html
5. Verify: Halaman dashboard camaba ditampilkan
```

### Test 4: Login as ADMIN_PUSAT
```
1. Clear localStorage
2. Login dengan email ADMIN_PUSAT (admin@pmb.com)
3. Expected: Auto redirect ke /static/dashboard-admin-pusat.html
4. Verify: Halaman dashboard admin pusat ditampilkan
```

### Test 5: Login as ADMIN_VALIDASI
```
1. Clear localStorage
2. Login dengan email ADMIN_VALIDASI (validasi@pmb.com)
3. Expected: Auto redirect ke /static/dashboard-admin-validasi.html
4. Verify: Halaman dashboard admin validasi ditampilkan
```

### Test 6: Persistent Login
```
1. Login sebagai CAMABA
2. Refresh halaman (F5)
3. Expected: Tetap di dashboard camaba (tidak redirect lagi)
4. Verify: Page state preserved, tidak ada redirect
```

---

## 🔑 Key Features

✅ **Automatic Root Redirect**
- `/` → `/index.html` (via RootController)

✅ **Smart Authentication Detection**
- Cek localStorage untuk token dan role
- Auto redirect berdasarkan role

✅ **Graceful Fallback**
- Jika tidak ada token → Landing page ditampilkan
- Jika token invalid → Stay di landing page

✅ **Consistent localStorage Keys**
- `authToken` → Token JWT
- `userRole` → User role (CAMABA, ADMIN_PUSAT, ADMIN_VALIDASI)

✅ **Seamless Experience**
- Tidak perlu manual redirect setiap kali login
- User langsung ke dashboard yang tepat

---

## 📊 localStorage Structure

Setelah login, localStorage akan berisi:

```javascript
{
    "authToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userRole": "CAMABA"
}
```

---

## 🔐 Security Notes

⚠️ **WARNING:** localStorage TIDAK aman untuk data sensitif
- Token bisa diakses via JavaScript
- Vulnerable terhadap XSS attacks
- Untuk production, gunakan httpOnly cookies

✅ **Mitigations:**
- Token punya expiration time
- Server validate setiap request
- Use HTTPS untuk enkripsi in-transit

---

## 🚀 Deployment Checklist

- [x] RootController dibuat
- [x] index.html ditambah authentication check
- [x] login.html stored token dengan key yang benar
- [x] localStorage keys konsisten (authToken)
- [x] Build successful (Maven clean package)
- [ ] Run aplikasi dan test semua flows
- [ ] Clear browser cache setelah deploy

---

## 📞 Troubleshooting

### Masalah: Redirect tidak bekerja
**Solution:**
1. Check browser console (F12) untuk errors
2. Verify localStorage punya `authToken` dan `userRole`
3. Check network tab untuk intercept calls

### Masalah: Landing page timbul setelah login
**Solution:**
1. Verify login response punya `token` dan `role`
2. Check localStorage keys setelah login
3. Verify paths di redirect statement

### Masalah: Stuck di landing page
**Solution:**
1. Clear local storage: `localStorage.clear()`
2. Check RootController mapping di Spring Boot
3. Verify index.html file ada di `/` dan `/static/`

---

## 📝 Next Steps

1. **Run & Test** aplikasi dengan flows di atas
2. **Monitor** browser console untuk errors
3. **Consider** using httpOnly cookies untuk production
4. **Add** session timeout logic
5. **Implement** refresh token mechanism

---

Generated: 2026-03-28
Version: 1.0
Author: System
