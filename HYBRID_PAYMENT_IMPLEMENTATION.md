# 🔥 HYBRID PAYMENT SYSTEM - IMPLEMENTASI LENGKAP

**Status:** ✅ Backend READY | 🔄 Frontend (In Progress) | ⏳ Database (Pending SQL Execution)

---

## 📋 RINGKASAN IMPLEMENTASI

Sistem pembayaran hybrid dengan 2 mode:
- **🟦 SIMULATION** - Virtual Account (simulasi untuk demo)
- **🟩 MANUAL** - Transfer Bank + Upload Bukti (implementasi real)

---

## 🗄️ 1. DATABASE SCHEMA BARU

**File SQL:** `HYBRID_PAYMENT_SETUP.sql`

### Tabel Baru:

#### 1. `manual_payment` - Bukti Transfer Manual
```sql
CREATE TABLE manual_payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admission_form_id BIGINT NOT NULL,
    payment_method ENUM('MANUAL'),
    amount BIGINT NOT NULL,
    proof_image_path VARCHAR(500),
    bank_name VARCHAR(100),
    account_holder VARCHAR(200),
    transaction_date DATETIME,
    status ENUM('PENDING', 'VERIFIED', 'REJECTED'),
    rejection_reason VARCHAR(500),
    verified_by VARCHAR(200),
    verified_at DATETIME,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (admission_form_id) REFERENCES admission_form(id)
);
```

#### 2. `university_bank_account` - Rekening Tujuan
```sql
CREATE TABLE university_bank_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bank_name VARCHAR(100),
    account_number VARCHAR(50),
    account_holder VARCHAR(200),
    purpose VARCHAR(200),
    is_active BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### Kolom Baru di Tabel Existing:

#### `admission_form`
```sql
ALTER TABLE admission_form 
ADD COLUMN payment_method ENUM('SIMULATION', 'MANUAL') DEFAULT 'SIMULATION';
```

---

## 💻 2. JAVA BACKEND - ENTITIES & REPOSITORIES

### Entity: `ManualPayment.java` ✅
```
src/main/java/com/uhn/pmb/entity/ManualPayment.java
- payment_method: ENUM (MANUAL)
- amount: Long (Rp)
- proof_image_path: String
- bank_name, account_holder: String
- transaction_date: LocalDateTime
- status: ENUM (PENDING, VERIFIED, REJECTED)
- rejection_reason: String
- verified_by, verified_at: Admin verification tracking
```

### Entity: `UniversityBankAccount.java` ✅
```
src/main/java/com/uhn/pmb/entity/UniversityBankAccount.java
- bankName: String (BCA, Mandiri, BNI, dll)
- accountNumber: String
- accountHolder: String
- purpose: String (Pendaftaran, Cicilan 1, dll)
- isActive: Boolean
```

### Updated Entity: `AdmissionForm.java` ✅
```
Ditambahkan:
- paymentMethod: ENUM (SIMULATION, MANUAL) - default SIMULATION
- Enum: PaymentMethod dengan label untuk UI
```

### Repositories ✅
```
- ManualPaymentRepository
  * findByAdmissionFormId(Long)
  * findByStatus(PaymentStatus)
  * findByAdmissionFormIdIn(List<Long>)

- UniversityBankAccountRepository
  * findByIsActiveTrueOrderByBankName()
```

---

## 🔌 3. REST API ENDPOINTS

### User Payment API - `/api/payment`

#### 1. **GET /api/payment/bank-accounts**
Dapatkan daftar rekening universitas yang aktif
```
Response:
[{
    "id": 1,
    "bankName": "BCA",
    "accountNumber": "1234567890",
    "accountHolder": "HKBP Nommensen University",
    "purpose": "Pendaftaran PMB",
    "isActive": true
}]
```

#### 2. **POST /api/payment/manual/submit** ⭐ PENTING
Submit pembayaran manual dengan bukti transfer
```
Form Data:
- admissionFormId: Long
- amount: Long (Rp)
- bankName: String
- accountHolder: String
- transactionDate: String (ISO datetime)
- proofFile: MultipartFile (image)

Response:
{
    "success": true,
    "message": "Bukti pembayaran berhasil diunggah, menunggu verifikasi admin",
    "paymentId": 123,
    "status": "Menunggu Verifikasi"
}
```

#### 3. **GET /api/payment/manual/{id}**
Cek status pembayaran manual
```
Response:
{
    "id": 1,
    "admissionFormId": 100,
    "amount": 2500000,
    "bankName": "BCA",
    "accountHolder": "John Doe",
    "transactionDate": "2026-04-10T15:30:00",
    "status": "Menunggu Verifikasi",
    "proofImageUrl": "/uploads/payment-proof/payment_100_xxxxx.jpg",
    "rejectionReason": null,
    "createdAt": "2026-04-10T10:00:00",
    "verifiedAt": null,
    "verifiedBy": null
}
```

#### 4. **GET /api/payment/manual/admission-form/{admissionFormId}**
Cek status manual payment by admission form ID
```
Response: (sama dengan endpoint #3)
```

### Admin Payment API - `/admin/payment`

#### 1. **GET /admin/payment/manual/pending?page=0&size=10**
Admin lihat pembayaran yang pending verifikasi
```
Response: List of ManualPaymentDTO
```

#### 2. **PUT /admin/payment/manual/{id}/verify**
Admin verifikasi pembayaran manual sebagai VERIFIED
```
Query Params:
- verifiedBy: String (email admin)

Response:
{
    "success": true,
    "message": "Pembayaran berhasil diverifikasi",
    "status": "Terverifikasi"
}
```

#### 3. **PUT /admin/payment/manual/{id}/reject**
Admin tolak pembayaran manual dengan alasan
```
Query Params:
- reason: String (alasan penolakan)
- rejectedBy: String (email admin)

Response:
{
    "success": true,
    "message": "Pembayaran berhasil ditolak",
    "status": "Ditolak"
}
```

---

## 🎨 4. FRONTEND - IMPLEMENTASI YANG DIBUTUHKAN

### A. Payment Page (`payment-briva.html`)

#### Sebelum tampilkan VA atau form upload, tambahkan pilihan metode:

```html
<div class="payment-method-selection">
    <h3>Pilih Metode Pembayaran</h3>
    <div class="button-group">
        <button class="method-btn" onclick="selectPaymentMethod('SIMULATION')">
            <i class="bi bi-credit-card"></i>
            <span>Virtual Account (Simulasi)</span>
            <small>Demo sistem otomatis, instant</small>
        </button>
        <button class="method-btn" onclick="selectPaymentMethod('MANUAL')">
            <i class="bi bi-upload"></i>
            <span>Transfer Manual (Upload Bukti)</span>
            <small>Realistis, verifikasi admin</small>
        </button>
    </div>
</div>

<!-- SIMULATION MODE -->
<div id="simulationMode" style="display: none;">
    <!-- Tambahin label: "Mode Simulasi" -->
    <!-- Gunakan flow VA yang sudah ada -->
</div>

<!-- MANUAL MODE -->
<div id="manualMode" style="display: none;">
    <div class="bank-account-info">
        <!-- Tampilkan rekening tujuan dari API -->
        <!-- User transfer ke rekening ini -->
    </div>
    <form id="manualPaymentForm">
        <input type="file" name="proofFile" accept="image/*" required>
        <input type="text" name="bankName" placeholder="Nama bank pengirim" required>
        <input type="text" name="accountHolder" placeholder="Nama pemilik rekening" required>
        <input type="datetime-local" name="transactionDate" required>
        <button type="submit">Upload Bukti Pembayaran</button>
    </form>
</div>
```

### B. Admin Dashboard Cicilan (`dashboard-admin-validasi.html`)

#### Tambahkan tab/section untuk manage manual payments:

```html
<div class="tab-pane fade" id="tab-cicilan" role="tabpanel">
    <div class="card">
        <div class="card-header">
            <span>Pembayaran Manual - Pending Verifikasi</span>
            <button onclick="loadPendingManualPayments()" class="btn btn-sm btn-primary">
                <i class="bi bi-arrow-clockwise"></i> Refresh
            </button>
        </div>
        <div class="card-body">
            <div class="table-responsive">
                <table class="table table-hover" id="manualPaymentTable">
                    <thead>
                        <tr>
                            <th>No.</th>
                            <th>Nama Calon Mahasiswa</th>
                            <th>Jumlah (Rp)</th>
                            <th>Bank Pengirim</th>
                            <th>Tanggal Transfer</th>
                            <th>Bukti</th>
                            <th>Status</th>
                            <th>Aksi</th>
                        </tr>
                    </thead>
                    <tbody id="manualPaymentList"></tbody>
                </table>
            </div>
        </div>
    </div>
</div>
```

#### JavaScript Functions yang Diperlukan:

```javascript
// 1. Load bank accounts saat payment page dibuka
async function loadBankAccounts() {
    const response = await fetch('/api/payment/bank-accounts');
    const accounts = await response.json();
    // Tampilkan rekening di manual mode
}

// 2. Select payment method
function selectPaymentMethod(method) {
    if (method === 'SIMULATION') {
        document.getElementById('simulationMode').style.display = 'block';
        document.getElementById('manualMode').style.display = 'none';
        selectedPaymentMethod = 'SIMULATION';
    } else {
        document.getElementById('simulationMode').style.display = 'none';
        document.getElementById('manualMode').style.display = 'block';
        loadBankAccounts();
        selectedPaymentMethod = 'MANUAL';
    }
}

// 3. Submit manual payment dengan file
async function submitManualPayment(e) {
    e.preventDefault();
    const formData = new FormData(document.getElementById('manualPaymentForm'));
    formData.append('admissionFormId', currentAdmissionFormId);
    formData.append('amount', totalBayar);
    
    const response = await fetch('/api/payment/manual/submit', {
        method: 'POST',
        body: formData
    });
    
    const result = await response.json();
    if (result.success) {
        alert('Bukti pembayaran berhasil diunggah!');
        // Redirect atau update status
    }
}

// 4. Admin: Load pending manual payments
async function loadPendingManualPayments() {
    const response = await fetch('/admin/payment/manual/pending?page=0&size=10', {
        headers: { 'Authorization': 'Bearer ' + authToken }
    });
    
    const payments = await response.json();
    // Render table dengan payment list
}

// 5. Admin: Verify payment
async function verifyPayment(paymentId) {
    const response = await fetch(`/admin/payment/manual/${paymentId}/verify?verifiedBy=${adminEmail}`, {
        method: 'PUT',
        headers: { 'Authorization': 'Bearer ' + authToken }
    });
    
    const result = await response.json();
    if (result.success) {
        alert('Payment terverifikasi!');
        loadPendingManualPayments();
    }
}

// 6. Admin: Reject payment with reason
async function rejectPayment(paymentId) {
    const reason = prompt('Alasan penolakan:');
    if (!reason) return;
    
    const response = await fetch(`/admin/payment/manual/${paymentId}/reject?reason=${encodeURIComponent(reason)}&rejectedBy=${adminEmail}`, {
        method: 'PUT',
        headers: { 'Authorization': 'Bearer ' + authToken }
    });
    
    const result = await response.json();
    if (result.success) {
        alert('Payment ditolak!');
        loadPendingManualPayments();
    }
}
```

---

## 📁 5. FILE STRUCTURE

```
src/main/java/com/uhn/pmb/
├── entity/
│   ├── AdmissionForm.java (UPDATED)
│   ├── ManualPayment.java (NEW)
│   └── UniversityBankAccount.java (NEW)
├── repository/
│   ├── ManualPaymentRepository.java (NEW)
│   └── UniversityBankAccountRepository.java (NEW)
├── dto/
│   ├── ManualPaymentDTO.java (NEW)
│   ├── ManualPaymentSubmitRequest.java (NEW)
│   └── UniversityBankAccountDTO.java (NEW)
└── controller/
    ├── PaymentController.java (NEW)
    └── AdminPaymentController.java (NEW)

src/main/resources/static/
├── payment-briva.html (EDIT NEEDED)
└── dashboard-admin-validasi.html (EDIT NEEDED)

uploads/payment-proof/ (auto-created untuk bukti transfer)
```

---

## 🚀 6. EXECUTION STEPS

### Step 1: Execute SQL Migration
```sql
-- Run HYBRID_PAYMENT_SETUP.sql di MySQL database
-- Membuat table baru: manual_payment, university_bank_account
-- Tambah kolom payment_method ke admission_form
```

### Step 2: Backend Build
```bash
mvn clean package -DskipTests
# ✅ BUILD SUCCESS sudah terbukti
```

### Step 3: Frontend Integration
- Update `payment-briva.html` dengan payment method selection
- Update `dashboard-admin-validasi.html` dengan cicilan management tab
- Integrate JavaScript functions untuk API calls

### Step 4: Test
- **Simulasi Mode:** Pilih VA → flow otomatis
- **Manual Mode:** Pilih Transfer → upload gambar dummy
- **Admin Verify:** Cek payment PENDING → Verify/Reject

---

## 📊 7. DATABASE RECORD EXAMPLES

### Bank Account Initialization
```sql
INSERT INTO university_bank_account VALUES
(1, 'BCA', '1234567890', 'HKBP Nommensen University', 'Pendaftaran PMB', true, NOW(), NOW()),
(2, 'Mandiri', '0987654321', 'HKBP Nommensen University', 'Pendaftaran PMB', true, NOW(), NOW());
```

### Manual Payment Record
```
id: 1
admission_form_id: 100
amount: 2500000
bank_name: BCA
account_holder: John Doe
transaction_date: 2026-04-10 15:30:00
status: PENDING (atau VERIFIED / REJECTED)
proof_image_path: payment_100_1681234567890.jpg
created_at: 2026-04-10 10:00:00
```

---

## 🎓 8. PENJELASAN UNTUK DOSEN

**Judul:** Hybrid Payment System dengan Mode Simulasi dan Manual

**Deskripsi:**
"Sistem pembayaran dirancang menggunakan pendekatan hybrid yang inovatif:

1. **Mode Simulasi (Virtual Account)** - menggambarkan integrasi dengan payment gateway di masa depan
   - Demonstrasi sistem pembayaran otomatis yang canggih
   - Flow realistic: generate VA → polling status → auto success
   - Jelas diberi label "Mode Simulasi" agar tidak keliru

2. **Mode Manual (Transfer + Upload Bukti)** - implementasi realistis
   - User melakukan transfer ke rekening universitas
   - Upload bukti pembayaran (foto/screenshot)
   - Admin verifikasi bukti secara manual
   - Status: PENDING → VERIFIED / REJECTED

**Keuntungan:**
- ✔ Sistem canggih (VA simulasi) untuk showcase kemampuan
- ✔ Implementasi realistis (manual) yang bisa langsung dipakai
- ✔ Tidak perlu kerjasama dengan bank
- ✔ Fleksibel untuk future integration dengan payment gateway
- ✔ Admin control penuh atas verifikasi pembayaran"

---

## ✅ CHECKLIST

- [x] Database schema (SQL)
- [x] Java entities & repositories
- [x] DTOs & request objects
- [x] PaymentController API
- [x] AdminPaymentController API
- [x] Build success (9.5s)
- [ ] Payment page UI (payment-briva.html)
- [ ] Admin dashboard UI (dashboard-admin-validasi.html)
- [ ] JavaScript functions
-[ ] Database migration execution
- [ ] Testing

---

**Next:** Run `HYBRID_PAYMENT_SETUP.sql` di database, kemudian integrate frontend components!
