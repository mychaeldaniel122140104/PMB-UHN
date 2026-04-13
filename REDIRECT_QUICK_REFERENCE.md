# 🎯 Quick Reference - Redirect & Auth Flow

## ✅ What Was Implemented

| Item | Status | File |
|------|--------|------|
| Root path redirect | ✅ | RootController.java (NEW) |
| Landing page auth check | ✅ | index.html |
| Static landing page auth check | ✅ | src/main/resources/static/index.html |
| Login token storage | ✅ | src/main/resources/static/login.html |
| Login redirect paths | ✅ | src/main/resources/templates/login.html |
| Build verification | ✅ | Maven (SUCCESS) |

---

## 🔗 URL Reminders

```
Application Port: 9500

First Access:
  http://localhost:9500/           → RootController → redirect to /index.html
  http://localhost:9500/index.html → Landing page with auth check

After Login (CAMABA):
  http://localhost:9500/static/dashboard-camaba.html

After Login (ADMIN_PUSAT):
  http://localhost:9500/static/dashboard-admin-pusat.html

After Login (ADMIN_VALIDASI):
  http://localhost:9500/static/dashboard-admin-validasi.html

Database Console:
  http://localhost:9500/h2-console

API Settings:
  http://localhost:9500/admin/api/settings
```

---

## 🧪 Test Credentials

| Role | Email | Password |
|------|-------|----------|
| ADMIN_PUSAT | admin@pmb.com | admin123 |
| ADMIN_VALIDASI | validasi@pmb.com | validasi123 |
| CAMABA | (register/any camaba) | - |

---

## 📝 Test Checklist

```
☐ Access http://localhost:9500/
  Expected: Redirect to http://localhost:9500/index.html

☐ Page loads with landing content
  Expected: No auto-redirect (no token)

☐ Click Login button
  Expected: Go to login page

☐ Login with ADMIN_PUSAT credentials
  Expected: Auto redirect to dashboard-admin-pusat.html

☐ Refresh page
  Expected: Stay on dashboard (token still in storage)

☐ Clear localStorage and refresh
  Expected: Auto redirect to login or landing page

☐ Check browser console
  Expected: No errors, only info logs
```

---

## 📂 Key Files Summary

```
src/main/java/com/uhn/pmb/controller/
  └── RootController.java (NEW)
      • Handles / → /index.html redirect

src/main/resources/
  ├── static/
  │   ├── index.html (UPDATED)
  │   │   • Added checkAuthentication()
  │   │   • Auto redirect based on role
  │   │
  │   └── login.html (UPDATED)
  │       • Store authToken after login
  │       • Updated redirect paths
  │
  └── templates/
      └── login.html (UPDATED)
          • Store authToken in localStorage
          • Redirect to /static/dashboards

root/
  └── index.html (UPDATED)
      • Added checkAuthentication()
      • Auto redirect based on role
```

---

## 🎨 localStorage Keys

After login:
```javascript
localStorage = {
    "authToken": "JWT_TOKEN_HERE",
    "userRole": "CAMABA|ADMIN_PUSAT|ADMIN_VALIDASI"
}
```

---

## 🔧 How to Test Locally

### Step 1: Start Application
```bash
cd "d:\all code\tugasakhir"
mvn spring-boot:run
```

### Step 2: Test Root Redirect
```
Browser: http://localhost:9500/
Expected: Auto redirect to http://localhost:9500/index.html
```

### Step 3: Test Auth Flow (No Login)
```
1. Open DevTools → Application → Local Storage
2. Clear all storage (localStorage.clear())
3. Refresh index.html
4. Expected: Landing page displays with footer
```

### Step 4: Test Login → Auto Redirect
```
1. Click Login button
2. Use admin@pmb.com / admin123
3. Expected: Auto redirect to /static/dashboard-admin-pusat.html
```

### Step 5: Test Persistence
```
1. Refresh dashboard page
2. Expected: Stay on dashboard (not redirected)
3. Check localStorage → authToken still there
```

---

## 🐛 Debugging Tips

### Check localStorage
```javascript
// In browser console
localStorage.getItem('authToken')   // Should return JWT token
localStorage.getItem('userRole')    // Should return role
```

### Check Redirect Logic
```javascript
// Break down the auth check
const token = localStorage.getItem('authToken');
const role = localStorage.getItem('userRole');
console.log('Token:', token);
console.log('Role:', role);
// Manually check redirect condition
```

### Monitor Network
```
F12 → Network tab → See all requests
Check if redirects are happening via HTTP 302 responses
```

### Check Application Logs
```
Watch server console for:
  - RootController being called
  - No auth errors
  - Successful login responses
```

---

## ✨ Success Indicators

✅ First access goes to /index.html  
✅ No login shows landing page  
✅ After login, auto redirect to dashboard  
✅ Refresh dashboard stays on dashboard  
✅ Clear storage and refresh redirects properly  
✅ Browser console has no errors  
✅ Footer with settings appears  

---

**Status: READY TO TEST** 🚀

For detailed documentation, see: `ROOT_REDIRECT_IMPLEMENTATION.md`
