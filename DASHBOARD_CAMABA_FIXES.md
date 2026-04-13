# Dashboard Calon Mahasiswa (Camaba) - Perbaikan Logika

## Ringkasan Masalah dan Solusi

### ❌ MASALAH 1: Email Tidak Tampil di Dashboard
**Gejala:** Header dashboard menampilkan email sebagai "-"
**Penyebab:** Endpoint `/api/camaba/profile` hanya return entity `Student` tanpa object `user`
**✅ Solusi:** Endpoint sekarang return object lengkap dengan nested `user` yang include `email`

### ❌ MASALAH 2: Tombol Edit & View Tidak Muncul
**Gejala:** Tombol edit/view tidak pernah muncul meskipun form sudah disubmit
**Penyebab Root (Multiple):**
1. Backend tidak membuat RegistrationStatus saat form disubmit
2. Frontend menggunakan enum stage name yang salah (`FORM_VERIFICATION` bukan `FORM_SUBMISSION`)
3. Logic tombol tidak cek status pembayaran

**✅ Solusi:**
- Backend: Membuat RegistrationStatus dengan stage=`FORM_SUBMISSION` saat form submit
- Backend: Enhanced API response dengan field `editTimeRemainingHours`
- Frontend: Fix enum references ke `FORM_SUBMISSION` yang benar
- Frontend: Tombol hanya mucul ketika BOTH kondisi terpenuhi:
  - Form submission status = `SELESAI`
  - Payment status = `SELESAI`

### ❌ MASALAH 3: Status Masih "Menunggu" Padahal Form Sudah Disubmit
**Gejala:** Dashboard selalu tampil "Menunggu" meskipun data sudah di-database
**Penyebab:** RegistrationStatus tidak pernah dibuat saat form submission
**✅ Solusi:** Backend sekarang otomatis create RegistrationStatus record

---

## Detail Perubahan Teknis

### BACKEND CHANGES

#### 1. CamabaController.java
```java
// ADDED: Import RegistrationStatusService
import com.uhn.pmb.service.RegistrationStatusService;

// ADDED: Dependency injection
private final RegistrationStatusRepository registrationStatusRepository;
private final RegistrationStatusService registrationStatusService;

// MODIFIED: /api/camaba/profile endpoint
@GetMapping("/profile")
public ResponseEntity<?> getProfile() {
    // Sekarang return Map dengan nested user.email
    // Response: { id, fullName, email, user: { id, email, role } }
    return ResponseEntity.ok(response);
}

// MODIFIED: submitAdmissionForm() method
// ADDED after admissionFormRepository.save(form):
RegistrationStatus formStatus = registrationStatusService.markAsCompleted(
    user, 
    RegistrationStatus.RegistrationStage.FORM_SUBMISSION,
    "Form submitted at " + LocalDateTime.now()
);
```

#### 2. RegistrationStatusController.java
```java
// MODIFIED: /api/camaba/registration-status/all endpoint
// ENHANCED: Return object dengan computed field editTimeRemainingHours
// Response example:
{
  "success": true,
  "data": [
    {
      "id": 1,
      "stage": "FORM_SUBMISSION",
      "status": "SELESAI",
      "editDeadline": "2026-03-28T10:30:00",
      "editTimeRemainingHours": 22,  // <-- NEW computed field
      "canEdit": true,
      "submissionDate": "2026-03-27T10:30:00",
      ...
    }
  ]
}
```

### FRONTEND CHANGES

#### dashboard-camaba.html

**1. Fixed: Enum Reference**
```javascript
// BEFORE (WRONG):
const verifikasiStatus = statusMap['FORM_VERIFICATION'];

// AFTER (CORRECT):
const formSubmissionStatus = statusMap['FORM_SUBMISSION'];
```

**2. Enhanced: updateProgressItem() Logic**
```javascript
// BEFORE: Tombol muncul jika status = SELESAI
// AFTER: Tombol muncul HANYA jika:
//   - Form submission = SELESAI
//   - Payment = SELESAI
//   - Masih dalam 24-jam edit window
//   - Admin belum reject

function updateProgressItem(itemId, status, statusMap, badgeId) {
    if (status?.status === 'SELESAI') {
        if (itemId === 'formulir-item') {
            if (statusMap?.PAYMENT_BRIVA?.status === 'SELESAI') {
                // Show buttons - payment done
                actionsContainer.style.display = 'flex';
            } else {
                // Hide buttons - payment not done
                actionsContainer.style.display = 'none';
            }
        }
    }
}
```

**3. Fixed: Status Flow References**
```javascript
// Updated dari verifikasiStatus ke formSubmissionStatus
// di semua tempat yang check status untuk menampilkan button
```

---

## File yang Diubah

### Backend (Java):
- ✅ `src/main/java/com/uhn/pmb/controller/CamabaController.java`
- ✅ `src/main/java/com/uhn/pmb/controller/RegistrationStatusController.java`

### Frontend (HTML):
- ✅ `src/main/resources/static/dashboard-camaba.html`

---

## Enum Values (Referensi)

### RegistrationStage (stage column values):
```
GELOMBANG_SELECTION       - Pemilihan gelombang pendaftaran
FORMULA_SELECTION         - Pemilihan program studi
FORM_SUBMISSION          - Pengisian formulir (STAGE PENTING)
PAYMENT_BRIVA            - Pembayaran BRIVA
PSYCHO_EXAM              - Ujian psikologi
DAFTAR_ULANG             - Daftar ulang
DOCUMENT_VERIFICATION    - Verifikasi dokumen
COMPLETED                - Selesai
```

### RegistrationStatus_Enum (status column values):
```
MENUNGGU_VERIFIKASI      - Status baru, menunggu proses
SELESAI                  - Tahap selesai
REJECTED                 - Ditolak admin
```

---

## Data Flow Setelah Fix

### 1️⃣ User Submit Form
```
submit-form → Backend validates → Save to admission_forms table
           ↓
        Create RegistrationStatus row:
        - user_id = logged in user
        - stage = FORM_SUBMISSION
        - status = SELESAI
        - submission_date = now
        - edit_deadline = now + 24 hours
        - can_edit = true
```

### 2️⃣ Dashboard Loads
```
GET /api/camaba/registration-status/all
↓
Backend returns list of statuses dengan computed fields
↓
Frontend creates statusMap keyed by stage:
{
  GELOMBANG_SELECTION: {...},
  FORMULA_SELECTION: {...},
  FORM_SUBMISSION: { status: SELESAI, editTimeRemainingHours: 22 },
  PAYMENT_BRIVA: { status: SELESAI },
  ...
}
```

### 3️⃣ Frontend Renders Status
```
Check: statusMap.FORM_SUBMISSION.status === 'SELESAI'?
       statusMap.PAYMENT_BRIVA.status === 'SELESAI'?
       ↓
If BOTH true → Show Edit/View buttons
If either false → Hide buttons
```

### 4️⃣ Email Display
```
GET /api/camaba/profile
↓
Backend returns:
{
  id: 123,
  fullName: "John Doe",
  email: "john@example.com",
  user: {
    id: 1,
    email: "john@example.com",     // <-- Email sekarang available
    role: "CAMABA"
  }
}
↓
Frontend: student.user.email → "john@example.com" ✅
```

---

## Testing Checklist

- [ ] **Test 1:** Submit form → check database untuk RegistrationStatus record dengan stage=FORM_SUBMISSION
- [ ] **Test 2:** Reload dashboard → email muncul di header
- [ ] **Test 3:** Form submission SELESAI, pembayaran BELUM → buttons HIDDEN
- [ ] **Test 4:** Form submission SELESAI, pembayaran SELESAI → buttons VISIBLE
- [ ] **Test 5:** 24 jam kemudian → buttons disabled (edit deadline passed)
- [ ] **Test 6:** Reload dashboard beberapa kali → status tetap konsisten
- [ ] **Test 7:** Accept admin verification → data dapat diedit
- [ ] **Test 8:** Reject admin verification → buttons hidden, status REJECTED
- [ ] **Test 9:** Payment done → exam button appears
- [ ] **Test 10:** Try edit form → berhasil disimpan ke database

---

## API Response Examples

### ✅ SUCCESS: GET /api/camaba/profile
```json
{
  "id": 1,
  "fullName": "Adi Pratama",
  "nik": "1234567890123456",
  "birthDate": "2005-01-15",
  "birthPlace": "Jakarta",
  "gender": "MALE",
  "email": "adi@example.com",
  "phoneNumber": "08123456789",
  "user": {
    "id": 1,
    "email": "adi@example.com",
    "role": "CAMABA",
    "createdAt": "2026-03-25T10:00:00"
  },
  "createdAt": "2026-03-25T10:00:00",
  "updatedAt": "2026-03-27T10:30:00"
}
```

### ✅ SUCCESS: GET /api/camaba/registration-status/all
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "stage": "FORM_SUBMISSION",
      "status": "SELESAI",
      "submissionDate": "2026-03-27T10:30:00",
      "editDeadline": "2026-03-28T10:30:00",
      "editTimeRemainingHours": 22,
      "canEdit": true,
      "adminVerified": false,
      "editCount": 0,
      "createdAt": "2026-03-27T10:30:00",
      "updatedAt": "2026-03-27T10:30:00"
    },
    {
      "id": 2,
      "stage": "PAYMENT_BRIVA",
      "status": "SELESAI",
      "submissionDate": "2026-03-27T11:00:00",
      "editTimeRemainingHours": 0,
      "canEdit": false,
      "adminVerified": false,
      "editCount": 0,
      "createdAt": "2026-03-27T11:00:00",
      "updatedAt": "2026-03-27T11:00:00"
    }
  ]
}
```

---

## Catatan Penting

✅ **Backward Compatible:** Semua API changes tetap mendukung existing clients
✅ **No Database Migration:** Tidak perlu alter table, hanya logic fix
✅ **Edit Window Logic:** 24-jam edit window dimulai dari submission_date, deadline disimpan di edit_deadline
✅ **Admin Override:** Admin dapat override status dengan tidak mengijinkan edit (adminVerified=true)
✅ **Payment Integration:** Status Payment BRIVA di-track terpisah, digunakan untuk show/hide buttons

---

## Troubleshooting

### Email masih "-" setelah fix
- Check: Response dari `/api/camaba/profile` punya `user.email`?
- Check: Frontend path `student.user?.email` correct?
- Try: Clear localStorage, login ulang

### Buttons masih tidak muncul
- Check: Form status = SELESAI?
- Check: Payment status = SELESAI?
- Check: Edit deadline belum passed?
- Check: Browser console untuk error messages

### Status tidak update
- Check: RegistrationStatus record di database?
- Try: Refresh halaman atau clear cache
- Check: API response punya correct stage dan status value?
