# 🧪 Testing Realtime Registration Status System

## ✅ What Was Fixed Today

### Dashboard Integration (`dashboard-camaba.html`)
- **Changed from:** Old API `/api/camaba/admission-status` (was broken with 401 errors)
- **Changed to:** New API `/api/camaba/registration-status/all` (realtime status tracking)
- **Now displays:** Dynamic status for all registration stages (GELOMBANG_SELECTION, FORMULA_SELECTION, etc)
- **Shows timer:** Time remaining to edit (within 24-hour window)

### Build Status
```
✅ Maven Build: SUCCESS
✅ Application Port: 9094
✅ All static files deployed to target/classes/static/
```

---

## 🎯 How to Test

### **Step 1: Start Application**
```bash
# Terminal shows application starting on port 9094
# (Should complete in ~10 seconds)

# Check if running:
http://localhost:9094/index.html
# Should show PMB login page
```

### **Step 2: Login with CAMABA Test Account** ⭐ IMPORTANT

**This account automatically created by system:**
```
Email: student@test.com
Password: student123
Role: CAMABA ✓ (Required for API access)
```

**Why this account?**
- Previous test users (admin@pmb.com, validasi@pmb.com) have ADMIN roles
- API endpoints require `ROLE_CAMABA` for security
- This account bypasses 401 authorization errors
- You can create more test accounts after this works

**Login Steps:**
1. Go to http://localhost:9094/login.html (or redirect)
2. Enter: `student@test.com` / `student123`
3. Click Login
4. Should redirect to Dashboard

---

### **Step 3: Check Console (F12)**

Open **Developer Tools (F12)** → **Console tab** and look for this sequence:

```javascript
common-auth.js:190 ✓ common-auth.js loaded
common-auth.js:58  🔍 Checking authentication with token...
common-auth.js:67  Response status: 200 
common-auth.js:107 ✓ Authentication verified - User: student@test.com
dashboard-camaba.html:599 📊 Registration API response: {success: true, data: []}
dashboard-camaba.html:573 ✓ Profile loaded from database: student@test.com
dashboard-camaba.html:605 No registration stages started yet
```

**What this means:**
- ✅ Authentication working
- ✅ API responding (200)
- ✅ Dashboard loading profile
- ✅ No stages completed yet (expected for new user)

---

### **Step 4: Test Stage Completion**

**On Dashboard, click:** "Mulai Pendaftaran" button

**This opens:** `gelombang-selection.html`

**In Console, you should see:**

```javascript
gelombang-selection.html:100 ✓ page loaded
gelombang-selection.html:200 Gelombang cards loaded (3 cards showing)
```

---

### **Step 5: Select Gelombang**

**Select Gelombang I (DIBUKA/Open)** and click **"Konfirmasi Pilihan"**

**Expected in Console:**

```javascript
gelombang-selection.html:450 📤 Calling API to mark GELOMBANG_SELECTION complete...

// If SUCCESS (200):
gelombang-selection.html:460 ✅ Status updated! Response: {
  success: true,
  data: {
    id: 1,
    stage: "GELOMBANG_SELECTION",
    status: "SELESAI",
    canEdit: true,
    editTimeRemainingHours: 24
  }
}

// If ERROR (401):
gelombang-selection.html:470 ❌ Authorization error! {status: 401}
// This means ROLE_CAMABA not assigned - check user role

// If ERROR (other):
gelombang-selection.html:475 ❌ API Error: {message: "..."}
```

---

### **Step 6: Verify Data in Database**

**Open H2 Console:**
```
http://localhost:9094/h2-console
```

**Login (default):**
```
JDBC URL: jdbc:h2:mem:pmb_uhn
User: sa
Password: (leave empty)
```

**Run Query:**
```sql
SELECT * FROM registration_status;
```

**Expected Result:**
```
ID  STAGE                  STATUS   SUBMISSION_DATE        EDIT_DEADLINE          CAN_EDIT  USER_ID
1   GELOMBANG_SELECTION    SELESAI  2026-03-25 14:45:30   2026-03-26 14:45:30    true      5
```

**Fields to verify:**
- ✅ `STAGE` = `GELOMBANG_SELECTION`
- ✅ `STATUS` = `SELESAI`
- ✅ `SUBMISSION_DATE` = timestamp when you clicked confirm
- ✅ `EDIT_DEADLINE` = submission + 24 hours
- ✅ `CAN_EDIT` = true (within deadline)
- ✅ `USER_ID` = ID of your student user

---

### **Step 7: Return to Dashboard & Verify Display**

**Go back to:** `http://localhost:9094/dashboard-camaba.html`

**Console should show:**
```javascript
📊 Registration API response: {success: true, data: [{
  stage: "GELOMBANG_SELECTION",
  status: "SELESAI",
  canEdit: true,
  editTimeRemainingHours: 24
}]}
✓ Loaded 1 registration stages
  - GELOMBANG_SELECTION: SELESAI
```

**Dashboard UI should show:**
```
"Pemilihan Gelombang & Pengisian Formulir"
Status: ✓ Selesai (24h) ← Green badge with time

"Langkah Berikutnya"
Button: "Pilih Program Studi"
```

---

### **Step 8: Test Edit Window**

**Click "Edit" button (if available)** or go back to `gelombang-selection.html`

**You should be able to:**
- ✅ Select different gelombang
- ✅ Click confirm again
- ✅ Status updates to new selection
- ✅ Edit deadline remains same (24h from first submission)

**After 24 hours:** Edit button should be disabled, shows "Selesai" (no time)

---

### **Step 9: Test Next Stages**

**Click "Pilih Program Studi" button**

**Opens:** `formula-selection.html`

**Same flow:**
1. Select formula (Kedokteran or Non-Kedokteran)
2. Click confirm
3. API calls `/api/camaba/registration-status/FORMULA_SELECTION/complete`
4. Check console for success message
5. Verify database has new entry
6. Return to dashboard → should show both stages as SELESAI

---

## 🐛 Troubleshooting

### Problem 1: **401 Unauthorized on API call**
```
❌ Authorization error! {status: 401}
```

**Causes & Solutions:**
1. **Not logged in with CAMABA account**
   - Solution: Log out and login as `student@test.com / student123`

2. **Token expired**
   - Solution: Refresh page (F5) or logout/login again

3. **User role wrong**
   - Solution: Check H2 database → `SELECT * FROM user;`
   - Verify `ROLE` column = `CAMABA` for your user

---

### Problem 2: **"No registration stages started yet"**
```javascript
No registration stages started yet
```

**Causes & Solutions:**
1. **First login (expected!)**
   - Solution: Click "Mulai Pendaftaran" button

2. **API call failed silently**
   - Solution: Open F12 Console → check for errors
   - Check network tab for POST request responses

---

### Problem 3: **API response wrapping issue**
```
API returns: {success: true, data: [{...}]}
But app can't parse?
```

**Check dashboard code:**
- ✅ Already fixed: `const registrationStatuses = responseData.data || responseData;`
- F12 Console should show:
  ```
  📊 Registration API response: {success: true, data: [...]}
  ✓ Loaded X registration stages
  ```

---

### Problem 4: **Database shows 0 rows**
```sql
SELECT COUNT(*) FROM registration_status;
-- Returns: 0
```

**Causes:**
1. **API call not made** → Check console for errors
2. **API call made but returned 401** → Check authorization
3. **Data saved but H2 database reset** → H2 in-memory resets on app restart

**Solution:**
- Check browser console for exact error
- Verify user role is CAMABA
- Try test flow again step by step

---

## 📋 Success Checklist

- [ ] ✅ Login with `student@test.com / student123` succeeds
- [ ] ✅ Dashboard loads without 401 errors
- [ ] ✅ Console shows "Registration API response: {success: true, data: [...]}"
- [ ] ✅ Click "Mulai Pendaftaran" → opens gelombang selection
- [ ] ✅ Select gelombang → console shows ✅ Status updated
- [ ] ✅ H2 Query shows new RegistrationStatus entry
- [ ] ✅ Return to dashboard → shows "Gelombang Selesai (24h)"
- [ ] ✅ Dashboard button changed to "Pilih Program Studi"
- [ ] ✅ Repeat for formula selection → shows "Formula Selesai (24h)"

---

## 🔧 Current Configuration

```
Application Port: 9094
Database: H2 In-Memory (jdbc:h2:mem:pmb_uhn)
Test Account: student@test.com / student123 (ROLE_CAMABA)
API Base: /api/camaba/registration-status/
Dashboard: /dashboard-camaba.html
Console Logging: Verbose (shows all API calls)
```

---

## 📝 Notes

- **H2 Database resets on app restart** (in-memory database)
- **Registration status persists within same app session**
- **24-hour edit window starts on SELESAI timestamp**
- **Frontend automatically checks canEdit flag before allowing submissions**
- **Admin can verify/reject at any stage**

---

## ❓ Questions?

Check these files for implementation details:
- [RegistrationStatusController](src/main/java/com/uhn/pmb/controller/RegistrationStatusController.java) - API endpoints
- [RegistrationStatusService](src/main/java/com/uhn/pmb/service/RegistrationStatusService.java) - Business logic
- [dashboard-camaba.html](src/main/resources/static/dashboard-camaba.html) - UI & API integration
- [gelombang-selection.html](src/main/resources/static/gelombang-selection.html) - Stage submission example
