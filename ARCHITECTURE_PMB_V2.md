# 🏛️ PMB System Architecture v2.0

## 📊 Alur Lengkap

```
1. CAMABA REGISTRATION & ACCOUNT
   Calon → Daftar & Buat Akun UHN → Mendapat akses login

2. GELOMBANG SELECTION
   Login → Pilih Gelombang (3 pilihan: BEBAS_SEBELUM_UTBK / BEBAS_JIKA_RANKING / TESTING_SESUDAH_UTBK)
   → Status: "Menunggu Verifikasi" (24 jam bisa edit)
   → Edit button tersedia (hingga 24 jam)

3. FORMULA SELECTION
   → Pilih Formula: Kedokteran (1jt) / Non-Kedokteran (250k)
   → Status: "Menunggu Verifikasi" (24 jam bisa edit)
   → Edit button tersedia

4. PAYMENT BRIVA
   → System generate kode BRIVA unik
   → Mahasiswa bayar via BRIVA
   → Webhook BRIVA → Status "SELESAI" (Payment confirmed)
   → Email ke mahasiswa: Kartu Ujian (Nomor Ujian auto-generated)

5. FORM SUBMISSION
   → Isi formulir elektronik
   → Submit → Status "SELESAI"
   → Auto-generate nomor ujian (jika belum ada dari payment)
   → Email ke mahasiswa: Kartu Ujian pdf

6. PSYCHO EXAM
   → Button "Ikuti Ujian" muncul
   → Input nomor ujian → Redirect ke GForm
   → GForm berisi soal psikologi (bisa di-add via koding)
   → Submit exam → Status "SELESAI"

7. DAFTAR ULANG (Admin-driven)
   → Admin lihat mahasiswa yang sudah ujian ✓
   → Admin pilih mahasiswa → buka form daftar ulang
   → Admin tentukan: Prodi + Cicilan method:
      - CICILAN: Admin input cicilan (e.g., 3 bulan) → Sistem auto-generate unique BRIVA code per cicilan
      - LANGSUNG: System generate 1 BRIVA code (full payment)
   → System kirim BRIVA code ke mahasiswa via SMS/email
   → Mahasiswa bayar sesuai cicilan schedule
   → If telat: Email reminder to mahasiswa + Email alert ke admin

8. DOCUMENT VERIFICATION
   → Mahasiswa upload dokumen (scan e-KTP, ijazah, nilai UTBK, dll)
   → Admin lihat & verify
   → Email ke admin: notification ada dokumen baru
   → Admin approve/reject
   → Jika approve: System auto-generate NPM → Email ke mahasiswa: "NPM: xxxxx"

9. FINAL STATUS
   → Mahasiswa can see: NPM, Status = "TERIMA" di dashboard
```

---

## 🗄️ Database Entities (NEW)

### 1. **RegistrationStatus** (UPDATED)
```java
Fields:
- id (Long)
- userId (User)
- stage (Enum):
  * GELOMBANG_SELECTION
  * FORMULA_SELECTION
  * PAYMENT_BRIVA
  * FORM_SUBMISSION
  * PSYCHO_EXAM
  * DAFTAR_ULANG
  * DOCUMENT_VERIFICATION
  * COMPLETED
  
- status (Enum):
  * MENUNGGU_VERIFIKASI (default, 24h editable)
  * SELESAI (passed stage)
  * REJECTED (admin reject)
  
- submissionDate (LocalDateTime)
- editDeadline (LocalDateTime) → submission + 24 hours
- canEdit (Boolean computed from editDeadline)
- editCount (Integer) → track how many times edited
- dataJson (String) → store stage-specific data (gelombangId, formulaId, etc)
- createdAt
- updatedAt
```

### 2. **StudentFormData** (NEW)
```java
Fields:
- id (Long)
- userId (User)
- formVersion (Integer) → 1, 2, 3... (for re-edits)
- gelombangId (RegistrationPeriod) 
- formulaId (SelectionType)
- personalData (JSON):
  * nama, email, noHP, tglLahir, alamat, etc
- parentData (JSON):
  * nama orangtua, pekerjaan, alamat
- educationData (JSON):
  * asal sekolah, nilai UN/UTBK
- examNumber (String) → auto-generated when form submitted
- submittedAt (LocalDateTime)
- modifiedAt (LocalDateTime)
```

### 3. **PaymentBriva** (NEW)
```java
Fields:
- id (Long)
- userId (User)
- brivaCode (String) → unique BRIVA code
- amount (BigDecimal)
- purpose (Enum):
  * FORMULIR_REGISTRATION (payment awal formulir)
  * DAFTAR_ULANG_CICILAN_1 (cicilan 1 dari 3)
  * DAFTAR_ULANG_CICILAN_2
  * DAFTAR_ULANG_CICILAN_3
  
- status (Enum):
  * PENDING (waiting payment)
  * PAID (confirmed via webhook)
  * EXPIRED (24h+ unpaid)
  * FAILED (payment failed)
  
- dueDatetime (LocalDateTime)
- paidDatetime (LocalDateTime, nullable)
- createdAt
- brivaReference (String, nullable) → response dari BRIVA API
```

### 4. **ReEnrollmentData** (NEW)
```java
Fields:
- id (Long)
- userId (User)
- programId (SelectionType) → Pilihan prodi daftar ulang
- cicilationType (Enum):
  * FULL_PAYMENT (bayar 1x)
  * CICILAN_2 (bayar 2 kali)
  * CICILAN_3 (bayar 3 kali)
  * CICILAN_4 (bayar 4 kali)
  
- brivaCodeList (List<PaymentBriva>) → 1-4 payment records
- approvedByAdminId (User) → admin yang approve
- approvalDate (LocalDateTime)
- status (Enum):
  * PENDING (waiting admin input)
  * ASSIGNED (admin sudah tentukan cicilan & BRIVA generated)
  * PARTIAL_PAID (some cicilan paid)
  * FULLY_PAID (semua cicilan lunas)
  * VERIFIED (admin verify dokumen)
  * REJECTED (ditolak)
```

### 5. **DocumentVerification** (NEW)
```java
Fields:
- id (Long)
- userId (User)
- documentType (Enum):
  * KARTU_KELUARGA
  * KTP
  * IJAZAH_SMA
  * NILAI_UTBK
  * SURAT_SEHAT
  * SURAT_TIDAK_BERHENTI_SEKOLAH
  
- fileUrl (String) → S3/storage path
- uploadDate (LocalDateTime)
- status (Enum):
  * PENDING (uploaded, waiting verification)
  * VERIFIED (approved)
  * REJECTED (not accepted)
  
- rejectionReason (String, nullable)
- verifiedByAdminId (User, nullable)
- verifiedDate (LocalDateTime, nullable)
```

### 6. **StudentNPM** (NEW)
```java
Fields:
- id (Long)
- userId (User)
- npm (String) → unique NPM assigned
- programId (SelectionType)
- programmingStartDate (LocalDateTime)
- status (Enum):
  * ACTIVE
  * INACTIVE (drop out, etc)
  
- issuedDate (LocalDateTime)
```

### 7. **EmailLog** (NEW - for audit trail)
```java
Fields:
- id (Long)
- recipientEmail (String)
- subject (String)
- emailType (Enum):
  * KARTU_UJIAN_NEW_REGISTRATION
  * KARTU_UJIAN_FROM_PAYMENT
  * NPM_ASSIGNMENT
  * CICILAN_PAYMENT_REMINDER
  * CICILAN_PAYMENT_OVERDUE
  * ADMIN_NOTIFICATION_NEW_DOCUMENT
  * VERIFICATION_COMPLETE
  
- sentDate (LocalDateTime)
- successStatus (Boolean)
- errorMessage (String, nullable)
- attachmentUrl (String, nullable) → PDF path for kartu ujian, dll
```

---

## 🌐 3 Websites + Endpoints

### 🎓 **Website 1: PMB Registration (Mahasiswa)**
```
URL: /
Pages:
- /login.html → Login/Register
- /dashboard-camaba.html → Home, lihat progress
- /gelombang-selection.html → Pilih gelombang
- /formula-selection.html → Pilih formula
- /payment-briva.html → Show BRIVA code, link untuk bayar
- /form-submission.html → Isi & submit formulir (auto kirim kartu ujian)
- /exam-preparation.html → Show kartu ujian, nomor ujian, link GForm
- /profile.html → Edit akun

API Endpoints (/api/camaba/):
- POST /registration/register → create account
- GET /dashboard/progress → get all stages status
- GET /stage/{stageName} → get specific stage status
- POST /gelombang/{id}/select → select gelombang (update status)
- POST /formula/{id}/select → select formula
- GET /payment/briva-code → get BRIVA code
- GET /payment/status → check payment status (polling)
- POST /form/submit → submit form data
- GET /exam/kartu → download kartu ujian pdf
- POST /exam/submit → submit exam result
- GET /enrollment/status → cek status daftar ulang (dari admin)
- GET /npm → get assigned NPM (jika sudah selesai)
```

---

### 👨‍💼 **Website 2: Admin Monitoring Dashboard**
```
URL: /admin/monitoring
Pages:
- /admin/monitoring/login.html
- /admin/monitoring/dashboard.html → Statistics, quick filters
- /admin/monitoring/students → Tabel semua calon & status mereka
  * Filter: Belum bayar, Belum isi formulir, Belum ujian, etc
- /admin/monitoring/forms → Lihat formulir yang sudah disubmit
- /admin/monitoring/exams → Lihat nilai exam, chart
- /admin/monitoring/documents → Lihat dokumen yang pending verify

API Endpoints (/api/admin/monitoring/):
- GET /students → list semua students dengan status
- GET /students/{userId}/timeline → lihat journey per student
- GET /forms/pending → form yg waiting admin review
- GET /exams/results → exam results dengan chart
- GET /documents/pending → dokumen waiting verification
- GET /payments/overdue → cicilan yg telat bayar
- GET /statistics → dashboard stats (total daftar, bayar, etc)
- PATCH /documents/{id}/verify → verify dokumen
- PATCH /documents/{id}/reject → reject dokumen + send email
```

---

### 🎯 **Website 3: Admin Daftar Ulang**
```
URL: /admin/reenrollment
Pages:
- /admin/reenrollment/login.html
- /admin/reenrollment/dashboard.html → Lihat students yang sudah lulus ujian
- /admin/reenrollment/students/{userId} → Form daftar ulang
  * Pilih Prodi dropdown
  * Pilih Cicilan: Radio [1x Bayar] [2 bulan] [3 bulan] [4 bulan]
  * Click "Generate BRIVA" → System generate unique BRIVA code per cicilan
  * Show preview: "Mahasiswa harus bayar: X bulan @ Y rupiah"
  * Button "Confirm & Send to Student"
  
- /admin/reenrollment/cicilan-tracking → Track semua cicilan, due dates
- /admin/reenrollment/npm-assignment → Assign NPM ke students

API Endpoints (/api/admin/reenrollment/):
- GET /students/exam-passed → list students yg passed exam
- POST /students/{userId}/assign-program → assign prodi + cicilan type
- GET /students/{userId}/briva-codes → get generated BRIVA codes
- POST /students/{userId}/send-briva → send BRIVA codes ke mahasiswa
- GET /cicilan/schedule → lihat semua cicilan schedule
- GET /cicilan/overdue → cicilan yang telat
- POST /npm/generate/{userId} → generate NPM + send email
- GET /cicilan/{brivaId}/payment-status → check payment via BRIVA webhook
```

---

## 📧 Email Notifications (Templates)

### 1. **Kartu Ujian - New Registration Format**
```
Subject: Kartu Ujian Pendaftaran - PMB UHN {{tahun}}
Body:
Yth. {{nama}},

Berikut adalah Kartu Ujian Pendaftaran Anda:

Nomor Ujian: {{examNumber}}
Nama: {{nama}}
Program: {{program}} 
Gelombang: {{gelombang}}
Tanggal Ujian: {{examDate}}
Waktu: {{examTime}}
Link Ujian: {{formLink}}

**Penting:** Screenshot kartu ini atau simpan nomor ujian untuk login ujian.

Salam,
PMB UHN

[Attachment: kartu_ujian_{{nomor}}.pdf]
```

### 2. **Cicilan Payment Reminder**
```
Subject: Pengingat: Cicilan Ke-{{n}} Jatuh Tempo {{dueDate}}
Body:
Yth. {{nama}},

Cicilan pendaftaran Anda yang ke-{{n}} akan jatuh tempo pada:
**Tanggal: {{dueDate}}**
**Jumlah: Rp {{amount}}**
**Kode BRIVA: {{brivaCode}}**

Silakan lakukan pembayaran sebelum jatuh tempo.

Terima kasih,
PMB UHN
```

### 3. **Cicilan Overdue Alert - to Admin**
```
Subject: ALERT: Mahasiswa {{nama}} Telat Bayar Cicilan
To: admin@pmb.com, validasi@pmb.com
Body:
Ada mahasiswa yang telat membayar cicilan:

Nama: {{nama}}
Email: {{email}}
Cicilan Ke: {{n}}
Status: OVERDUE sejak {{overdueDate}}
Jumlah: Rp {{amount}}
Kode BRIVA: {{brivaCode}}

Silakan hubungi mahasiswa untuk pengumpulan tunggakan.
```

### 4. **Document Uploaded - Notification to Admin**
```
Subject: Ada Dokumen Baru untuk Diverifikasi
To: admin@monitoring@pmb.com
Body:
Mahasiswa {{nama}} telah mengupload dokumen:

Tipe: {{documentType}}
Email: {{email}}
Upload Time: {{uploadTime}}

Link untuk verifikasi: {{verificationLink}}
```

### 5. **NPM Assignment**
```
Subject: Nomor Pokok Mahasiswa (NPM) Anda - UHN {{tahun}}
Body:
Yth. {{nama}},

Selamat! Anda telah resmi menjadi mahasiswa UHN.

**Nomor Pokok Mahasiswa (NPM): {{npm}}**
Program Studi: {{program}}
Tahun Akademik: {{tahunAkademik}}

Gunakan NPM ini untuk pendaftaran akademik lebih lanjut.

Selamat datang di UHN!
```

---

## 🔐 Roles & Permissions

```
ROLE_CAMABA (Student)
- Access: /dashboard-camaba.html, /gelombang-selection.html, etc
- Can: Select gelombang/formula, submit form, check status, upload dokumen
- Cannot: Lihat student lain, approve/verify

ROLE_ADMIN_PUSAT
- Access: /admin/monitoring/** 
- Can: View all student progress, verify dokumen, lihat exam results, statistics
- Cannot: Assign prodi/cicilan (itu di website 3)

ROLE_ADMIN_VALIDASI
- Access: /admin/monitoring/** (same as ADMIN_PUSAT)
- Can: Verify dokumen, lihat progress

ROLE_ADMIN_REENROLLMENT (NEW)
- Access: /admin/reenrollment/**
- Can: Assign prodi, tentukan cicilan, generate BRIVA, assign NPM
- Cannot: Approve dokumen (website 2 job)
```

---

## 📞 BRIVA Integration

```
API Call Saat Payment Created:
POST https://api.briva.id/payment/request
{
  "amount": 250000,
  "description": "Formulir Pendaftaran Kedokteran - {{nama}}",
  "notificationUrl": "/webhook/briva/payment-notification",
  "customerId": "{{userId}}",
  "invoiceId": "{{paymentId}}"
}

Response:
{
  "brivaNo": "8864501000004521",
  "amount": 250000,
  "status": "PENDING"
}

Webhook (Payment Confirmed):
POST /webhook/briva/payment-notification
{
  "brivaNo": "8864501000004521",
  "amount": 250000,
  "status": "PAID",
  "paidDatetime": "2024-03-25 14:30:00"
}

Action: Update PaymentBriva.status = PAID → Trigger email kartu ujian
```

---

## 🎯 Implementation Priority

1. ✅ Update **RegistrationStatus** entity
2. ✅ Create **StudentFormData** entity
3. ✅ Create **PaymentBriva** entity
4. ✅ Create **ReEnrollmentData** entity
5. ✅ Create **DocumentVerification** entity
6. ✅ Create **StudentNPM** entity
7. ✅ Create **EmailLog** entity
8. ✅ Update controllers for new endpoints
9. ✅ Create email service (send kartu ujian, NPM, cicilan reminder)
10. ✅ Create website 1 pages (payment-briva.html, form-submission.html, exam.html)
11. ✅ Create website 2 (admin monitoring dashboard)
12. ✅ Create website 3 (admin reenrollment)
13. ✅ Integrate BRIVA webhook
14. ✅ Test complete flow
