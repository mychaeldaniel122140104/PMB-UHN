## 📋 UPDATE PROGRAM STUDI - TAMBAH HARGA TOTAL

**Status**: ✅ **COMPLETE**  
**Date**: 2026-04-10  
**Changes**: Menambahkan field `hargaTotalPerTahun` untuk setiap program studi

---

## 🔄 HARGA TOTAL PER PROGRAM STUDI

| No | Program Studi | Kategori | Harga Per Tahun |
|----|---------------|----------|-----------------|
| 1 | **Pendidikan Dokter** | Medical | **Rp 150,000,000** |
| 2 | **Profesi Dokter** | Medical | **Rp 100,000,000** |
| 3 | Teknik Sipil | Engineering | Rp 80,000,000 |
| 4 | Teknik Mesin | Engineering | Rp 80,000,000 |
| 5 | Teknik Elektro | Engineering | Rp 80,000,000 |
| 6 | Informatika | Engineering | Rp 80,000,000 |
| 7 | Pend. Biologi Inggris | Education | Rp 50,000,000 |
| 8 | Pend. Ekonomi | Education | Rp 50,000,000 |
| 9 | Pend. Agama Kristen | Education | Rp 50,000,000 |
| 10 | Pend. Pancasila & Kewarganegaraan | Education | Rp 50,000,000 |
| 11 | Pend. Bahasa & Sastra Indonesia | Education | Rp 50,000,000 |
| 12 | Pend. Fisika | Education | Rp 50,000,000 |
| 13 | Pend. IPA | Education | Rp 50,000,000 |
| 14 | Pend. Profesi Guru | Education | Rp 50,000,000 |
| 15 | Ekonomi Pembangunan | Economics | Rp 60,000,000 |
| 16 | Manajemen | Economics | Rp 60,000,000 |
| 17 | Akuntansi | Economics | Rp 60,000,000 |
| 18 | Administrasi Perpajakan (D3) | Diploma D3 | Rp 40,000,000 |

---

## 📝 CHANGES MADE

### 1. **Entity Layer** ✅
**File**: `ProgramStudi.java`

```java
@Column(name = "harga_total_per_tahun")
private Long hargaTotalPerTahun = 0L; // Total program fee per year
```

**Type**: `BIGINT` (Long di Java)  
**Default**: 0  
**Purpose**: Menyimpan harga total program studi per tahun (bisa dibagi untuk cicilan)

---

### 2. **Database Migration** ✅
**File**: `MIGRATION_ADD_HARGA_TOTAL.sql`

**SQL Command**:
```sql
ALTER TABLE program_studi 
ADD COLUMN harga_total_per_tahun BIGINT DEFAULT 0;
```

**Kemudian**: Update harga untuk setiap program studi berdasarkan kategorinya.

---

### 3. **Initialization Script** ✅
**File**: `PROGRAM_STUDI_INIT_WITH_HARGA.sql`

- Script untuk **INSERT** jika tabel kosong
- Script untuk **UPDATE** jika sudah ada data (lebih umum)
- Verification queries untuk cek data

---

### 4. **Java Data Initializer** ✅
**File**: `DataInitializer.java`

Update method `initializeProgramStudi()`:
- Pass `hargaTotalPerTahun` saat membuat program studi baru
- Update method signature `createProgramStudi()` dengan parameter harga

---

### 5. **Admin Controller** ✅
**File**: `AdminController.java`

**Perubahan**:
- ✅ Update `ProgramStudiRequest` DTO - tambah field `hargaTotalPerTahun`
- ✅ Update POST method - handle harga total saat create
- ✅ Update PUT method - handle harga total saat update
- ✅ Update GET methods - return `hargaTotalPerTahun` di response

---

### 6. **Dashboard Admin UI** ✅
**File**: `dashboard-admin-pusat.html`

**Perubahan Form**:
```html
<div class="mb-3">
    <label class="form-label">Harga Total Per Tahun (Rp) <span class="text-danger">*</span></label>
    <input type="number" id="programStudiHargaTotalPerTahun" 
           class="form-control" placeholder="cth: 80000000" 
           min="0" step="1000000" required>
    <small class="text-muted">Harga total program per tahun (dapat dibagi untuk cicilan)</small>
</div>
```

**Perubahan Table Display**:
- Tambah kolom: `Harga Per Tahun` (dengan format Rupiah badge)
- Update colspan dari 5 menjadi 6
- Display menggunakan `formatRupiah()` helper function

**JavaScript Functions Updated**:
- `saveProgramStudi()` - handle hargaTotalPerTahun
- `editProgramStudi()` - populate hargaTotalPerTahun dari API
- `loadProgramStudiList()` - display hargaTotalPerTahun dengan format Rp

---

## 🚀 NEXT STEPS

### 1. **Run Migration SQL** (REQUIRED)
```bash
# Execute dalam database tools Anda
-- MIGRATION_ADD_HARGA_TOTAL.sql
```

### 2. **Update Data** (pilih salah satu):

**Option A: Fresh Database (baru)**
```sql
-- Jalankan: PROGRAM_STUDI_INIT_WITH_HARGA.sql (bagian INSERT)
-- Lalu rebuild sesuai database setup
```

**Option B: Existing Database (yang sudah ada)**
```sql
-- Jalankan: PROGRAM_STUDI_INIT_WITH_HARGA.sql (bagian UPDATE)
-- Atau jalankan MIGRATION_ADD_HARGA_TOTAL.sql yang sudah include UPDATE
```

### 3. **Rebuild Project**
```bash
mvn clean package -DskipTests
# atau
mvn clean install
```

### 4. **Test di Dashboard Admin**

1. Go to: **Admin Dashboard → Program Studi Management**
2. Click: **"Tambah Program Studi"**
3. Verify: Form memiliki field "Harga Total Per Tahun"
4. Fill field dengan harga (min 1juta - step 1 juta)
5. Save dan verify:
   - Harga tampil di table dengan format Rp
   - Edit program studi → harga terpopulate
   - GET API `/admin/program-studi` → return hargaTotalPerTahun

---

## 📊 VERIFICATION QUERIES

### Check Data Sudah Update:
```sql
SELECT id, kode, nama, harga_total_per_tahun 
FROM program_studi 
ORDER BY id;
```

### Check Summary by Kategori:
```sql
SELECT 
    CASE 
        WHEN kode IN ('pendidikan-dokter', 'profesi-dokter') THEN 'Medical'
        WHEN kode IN ('teknik-sipil', 'teknik-mesin', 'teknik-elektro', 'informatika') THEN 'Engineering'
        WHEN kode LIKE 'pend-%' THEN 'Education'
        WHEN kode LIKE 'adm-%' THEN 'Diploma (D3)'
        ELSE 'Economics'
    END as kategori,
    COUNT(*) as jumlah,
    SUM(harga_total_per_tahun) as total_harga,
    MIN(harga_total_per_tahun) as min_harga,
    MAX(harga_total_per_tahun) as max_harga
FROM program_studi
WHERE is_active = TRUE
GROUP BY kategori
ORDER BY total_harga DESC;
```

---

## 🔒 DATA TYPE SAFETY

**Type**: `BIGINT` (Long di Java)  
**Max Value**: 9,223,372,036,854,775,807  
**Harga Max Bisa Support**: 9.2 kuadriliun (lebih dari cukup 😄)  
**Untuk Entry**: Format Rupiah (tanpa simbol, hanya angka)

---

## 📱 API Endpoints

### GET - Fetch All Program Studi
```
GET /admin/program-studi
Response Include: id, kode, nama, deskripsi, isMedical, isActive, sortOrder, hargaTotalPerTahun
```

### GET - Fetch Single Program Studi
```
GET /admin/program-studi/{id}
Response Include: hargaTotalPerTahun
```

### POST - Create Program Studi
```
POST /admin/program-studi
Body: {
    "kode": "...",
    "nama": "...",
    "deskripsi": "...",
    "isMedical": false,
    "isActive": true,
    "sortOrder": 1,
    "hargaTotalPerTahun": 80000000
}
```

### PUT - Update Program Studi
```
PUT /admin/program-studi/{id}
Body: {
    "kode": "...",
    "nama": "...",
    "deskripsi": "...",
    "isMedical": false,
    "isActive": true,
    "sortOrder": 1,
    "hargaTotalPerTahun": 80000000
}
```

---

## 🎯 FEATURES YANG BISA DIKEMBANGKAN NANTI

1. **Installment Calculation** - Otomatis cara membagi harga ke cicilan
2. **Price History** - Track perubahan harga per tahun akademik
3. **Dynamic Pricing** - Harga berbeda per jenis seleksi/gelombang
4. **Price Range Validation** - Min/max range validation di form
5. **Bulk Price Update** - Update harga multiple program sekaligus

---

## ✅ TESTING CHECKLIST

- [ ] Database migration berhasil (column ada dengan data type BIGINT)
- [ ] Data sudah ter-update dengan harga untuk semua 18 program studi
- [ ] Form Add/Edit Program Studi ada field "Harga Total Per Tahun"
- [ ] Tidak bisa Submit form tanpa isi harga total
- [ ] Harga tampil di table dengan format Rp (contoh: Rp 80,000,000)
- [ ] Edit program studi → harga terpopulate dengan benar
- [ ] Get endpoint return hargaTotalPerTahun di response
- [ ] Harga bisa di-update tanpa error

---

## 📚 FILES YANG DIMODIFIKASI

```
✅ src/main/java/com/uhn/pmb/entity/ProgramStudi.java
✅ src/main/java/com/uhn/pmb/controller/AdminController.java
✅ src/main/java/com/uhn/pmb/component/DataInitializer.java
✅ src/main/resources/static/dashboard-admin-pusat.html

📄 MIGRATION_ADD_HARGA_TOTAL.sql (NEW)
📄 PROGRAM_STUDI_INIT_WITH_HARGA.sql (NEW)
```

---

## 🎉 SUMMARY

✅ **Harga total sudah terintegrasi di sistem!**

- Database schema updated dengan kolom `harga_total_per_tahun`
- Entity layer sudah support harga (type: Long)
- Controller API updated untuk create/update/read harga
- Dashboard admin punya form + display untuk manage harga
- Harga auto-initialize saat system startup (DataInitializer)
- Format display: **Rupiah currency** (Rp 80,000,000)

**Next task**: Run migration, update data, rebuild, dan test di dashboard! 🚀

