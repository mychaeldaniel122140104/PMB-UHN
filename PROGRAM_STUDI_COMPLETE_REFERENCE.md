## 📊 PROGRAM STUDI - COMPREHENSIVE PRICE LIST (25 Programs)

**Date Updated**: 2026-04-10  
**Status**: ✅ Ready to Deploy  
**Total Programs**: 25  
**Total Annual Value**: Rp 197,445,250 (all programs combined)

---

## 📚 PROGRAM STUDI BY FACULTY

### **FKIP - Keguruan & Ilmu Pendidikan** (8 Programs)

| Kode | Nama | Harga Per Tahun |
|------|------|-----------------|
| pend-fisika | Pend. Fisika | **Rp 4,305,500** |
| pend-bahasa-sastra-indonesia | Pend. B. Indonesia | **Rp 8,999,000** |
| pend-biologi-inggris | Pend. B. Inggris | **Rp 8,980,000** |
| pend-pancasila-kewarganegaraan | Pend. PPKn | **Rp 7,875,000** |
| pend-ekonomi | Pend. Ekonomi | **Rp 8,057,000** |
| pend-matematika | Pend. Matematika | **Rp 7,968,000** |
| pend-agama-kristen | Pend. Agama Kristen | **Rp 7,966,000** |
| pend-ipa | Pend. IPA | **Rp 4,782,500** |

---

### **FISIPOL - Ilmu Sosial & Politik** (2 Programs)

| Kode | Nama | Harga Per Tahun |
|------|------|-----------------|
| adm-bisnis | Adm. Bisnis | **Rp 7,433,000** |
| adm-publik | Adm. Publik | **Rp 5,712,250** |

---

### **TEKNIK - Teknik (Engineering)** (4 Programs)

| Kode | Nama | Harga Per Tahun |
|------|------|-----------------|
| teknik-sipil | Teknik Sipil | **Rp 9,921,000** 🔝 |
| teknik-mesin | Teknik Mesin | **Rp 10,131,000** 🏆 TERTINGGI |
| teknik-elektro | Teknik Elektro | **Rp 5,438,000** |
| informatika | Informatika | **Rp 8,391,000** |

---

### **PETERNAKAN - Peternakan** (1 Program)

| Kode | Nama | Harga Per Tahun |
|------|------|-----------------|
| prod-ternak | Prod. Ternak | **Rp 6,438,250** |

---

### **EKONOMI & BISNIS** (4 Programs)

| Kode | Nama | Harga Per Tahun |
|------|------|-----------------|
| akuntansi | Akuntansi | **Rp 9,691,000** |
| manajemen | Manajemen | **Rp 9,991,000** |
| ekonomi-pembangunan | Ekonomi Pembangunan | **Rp 8,827,000** |
| adm-pajak | Adm. Pajak | **Rp 8,027,000** |

---

### **HUKUM - Ilmu Hukum** (1 Program)

| Kode | Nama | Harga Per Tahun |
|------|------|-----------------|
| ilmu-hukum | Ilmu Hukum | **Rp 8,355,000** |

---

### **PERTANIAN - Pertanian** (2 Programs)

| Kode | Nama | Harga Per Tahun |
|------|------|-----------------|
| agroteknologi | Agroteknologi | **Rp 6,475,750** |
| agribisnis | Agribisnis | **Rp 6,475,750** |

---

### **BAHASA & SENI - Bahasa & Seni** (2 Programs)

| Kode | Nama | Harga Per Tahun |
|------|------|-----------------|
| seni-musik | Seni Musik | **Rp 4,338,500** |
| sastra-inggris | Sastra Inggris | **Rp 6,558,250** |

---

### **PSIKOLOGI - Psikologi** (1 Program)

| Kode | Nama | Harga Per Tahun |
|------|------|-----------------|
| psikologi | Psikologi | **Rp 6,475,500** |

---

## 📊 PRICE STATISTICS

### **By Range:**
- **Terendah**: Seni Musik (Rp 4,338,500)
- **Tertinggi**: Teknik Mesin (Rp 10,131,000)
- **Range**: Rp 5,792,500

### **By Faculty:**

| Faculty | Count | Avg Price | Total |
|---------|-------|-----------|-------|
| FKIP | 8 | **Rp 6,671,313** | Rp 53,370,500 |
| FISIPOL | 2 | **Rp 6,572,625** | Rp 13,145,250 |
| TEKNIK | 4 | **Rp 8,470,250** | Rp 33,881,000 |
| PETERNAKAN | 1 | **Rp 6,438,250** | Rp 6,438,250 |
| EKONOMI & BISNIS | 4 | **Rp 9,136,000** | Rp 36,544,000 |
| HUKUM | 1 | **Rp 8,355,000** | Rp 8,355,000 |
| PERTANIAN | 2 | **Rp 6,475,750** | Rp 12,951,500 |
| BAHASA & SENI | 2 | **Rp 5,448,375** | Rp 10,896,750 |
| PSIKOLOGI | 1 | **Rp 6,475,500** | Rp 6,475,500 |
| **TOTAL** | **25** | **Rp 7,897,810** | **Rp 197,445,250** |

---

## 🔄 SQL MIGRATION SCRIPTS

### **1. For Fresh Database:**
Use `DataInitializer.java` - otomatis initialize ke 25 programs saat startup (jika table kosong)

### **2. For Existing Database:**
Jalankan: `UPDATE_PROGRAM_STUDI_COMPLETE_PRICES.sql`
- Updates existing programs dengan harga actual
- Inserts programs baru jika belum ada
- Deactivates programs yang sudah tidak berlaku (Pendidikan Dokter, Profesi Dokter, Pend. Profesi Guru)

---

## ✅ CHECKLISt DEPLOYMENT

- [ ] Database migration berhasil (ADD COLUMN jika diperlukan)
- [ ] `UPDATE_PROGRAM_STUDI_COMPLETE_PRICES.sql` executed
- [ ] Verify 25 programs semuanya ada dengan harga correct:
  ```sql
  SELECT COUNT(*), SUM(harga_total_per_tahun) 
  FROM program_studi 
  WHERE is_active = TRUE;
  -- Should return: 25 programs, Rp 197,445,250
  ```
- [ ] Rebuild project: `mvn clean package -DskipTests`
- [ ] Test di Dashboard Admin:
  - List menampilkan 25 programs
  - Harga tampil dengan format Rp
  - Add/Edit program bekerja dengan baik
  - GET `/admin/program-studi` return complete list

---

## 🗂️ FILE REFERENCE

| File | Purpose | Action |
|------|---------|--------|
| `DataInitializer.java` | Auto-init 25 programs | ✅ Updated (25 programs + real prices) |
| `UPDATE_PROGRAM_STUDI_COMPLETE_PRICES.sql` | Complete migration script | ✅ Created (new) |
| `ProgramStudi.java` | Entity model | ✅ Has `hargaTotalPerTahun` field |
| `AdminController.java` | API endpoints | ✅ Updated (handles harga) |
| `dashboard-admin-pusat.html` | Admin UI | ✅ Updated (form + display harga) |

---

## 🎯 ENROLLMENT FEE BREAKDOWN

Harga yang ada adalah **TOTAL ANNUAL FEE per tahun akademik**. Biasanya dibagi:

**Example - Teknik Mesin (Rp 10,131,000 / tahun):**
- Semester 1: Rp 5,065,500
- Semester 2: Rp 5,065,500
- Total: Rp 10,131,000

Atau bisa dibagi menjadi **4-6 cicilan** sesuai kebijakan institusi.

---

## 📋 PROGRAM CODES MAPPING (untuk referensi API/DB)

```
FKIP:
- pend-fisika → Pend. Fisika
- pend-bahasa-sastra-indonesia → Pend. B. Indonesia  
- pend-biologi-inggris → Pend. B. Inggris
- pend-pancasila-kewarganegaraan → Pend. PPKn
- pend-ekonomi → Pend. Ekonomi
- pend-matematika → Pend. Matematika (NEW)
- pend-agama-kristen → Pend. Agama Kristen
- pend-ipa → Pend. IPA

FISIPOL:
- adm-bisnis → Adm. Bisnis
- adm-publik → Adm. Publik (NEW)

TEKNIK:
- teknik-sipil → Teknik Sipil
- teknik-mesin → Teknik Mesin
- teknik-elektro → Teknik Elektro
- informatika → Informatika

PETERNAKAN:
- prod-ternak → Prod. Ternak (NEW)

EKONOMI & BISNIS:
- akuntansi → Akuntansi
- manajemen → Manajemen
- ekonomi-pembangunan → Ekonomi Pembangunan
- adm-pajak → Adm. Pajak (UPDATED from adm-perpajakan-d3)

HUKUM:
- ilmu-hukum → Ilmu Hukum (NEW)

PERTANIAN:
- agroteknologi → Agroteknologi (NEW)
- agribisnis → Agribisnis (NEW)

BAHASA & SENI:
- seni-musik → Seni Musik (NEW)
- sastra-inggris → Sastra Inggris (NEW)

PSIKOLOGI:
- psikologi → Psikologi (NEW)
```

---

## 🚀 DEPLOYMENT STEPS

1. **Backup Database** (opsional tapi recommended):
   ```sql
   -- Create backup of program_studi
   CREATE TABLE program_studi_backup AS SELECT * FROM program_studi;
   ```

2. **Run Migration**:
   ```bash
   # Execute in database client
   source UPDATE_PROGRAM_STUDI_COMPLETE_PRICES.sql;
   ```

3. **Verify**:
   ```sql
   SELECT COUNT(*) as total_active, SUM(harga_total_per_tahun) as total_value
   FROM program_studi 
   WHERE is_active = TRUE;
   -- Expected: 25, 197445250
   ```

4. **Rebuild Project**:
   ```bash
   cd d:\all code\tugasakhir
   mvn clean package -DskipTests
   ```

5. **Test**:
   - Restart application
   - Go to Admin Dashboard → Program Studi Management
   - Verify 25 programs dengan harga correct (format Rp)

---

## 💡 NOTES

- Semua harga sudah **actual** dari institusi
- Program yang di-deactivate (Pendidikan Dokter, Profesi Dokter): Tidak ada di enrollment tapi tetap ada di DB (soft delete via `is_active = FALSE`)
- Harga bisa di-edit di dashboard kapan saja
- Proses installment split: Dikerjakan di module Jenis Seleksi (akan link ke program studi untuk ambil hargaTotalPerTahun)

---

**Status**: ✅ **READY TO DEPLOY**

