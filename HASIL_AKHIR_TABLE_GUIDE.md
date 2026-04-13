# Hasil Akhir Table - Final Results Storage

## Overview
The `hasil_akhir` table stores the final results shown to students after they complete all registration steps and receive validation approval. Contains uniquely constrained fields for BRIVA, Registration Number, and Student ID.

## Database Structure

### Table: `hasil_akhir`
```sql
CREATE TABLE hasil_akhir (
    id BIGINT PRIMARY KEY,
    student_id BIGINT NOT NULL UNIQUE,        -- One-to-one with students
    user_id BIGINT NOT NULL UNIQUE,           -- One-to-one with users
    briva_number VARCHAR(50) NOT NULL UNIQUE, -- Virtual Account BRIVA
    briva_amount DECIMAL(15, 2),               -- Amount
    nomor_registrasi VARCHAR(100) NOT NULL UNIQUE, -- Registration Number
    status ENUM(...) DEFAULT 'PENDING',
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### Key Constraints
- ✅ Each student has **ONE** HasilAkhir record
- ✅ Each BRIVA number is **UNIQUE** (no duplicates)
- ✅ Each registration number is **UNIQUE** (no duplicates)
- ✅ Foreign keys to students and users

---

## Entity Classes

### 1. HasilAkhir.java
JPA entity with:
- Lazy-loaded relationships to Student and User
- Unique constraints on BRIVA, nomor_registrasi, student_id
- Status enum: PENDING, ACTIVE, EXPIRED, USED, CANCELLED
- Auto-managed timestamps

### 2. HasilAkhirRepository.java
Spring Data JPA repository with query methods:
```java
findByStudentId(Long studentId)           // Get by student
findByUserId(Long userId)                 // Get by user
findByBrivaNumber(String brivaNumber)     // Get by BRIVA
findByNomorRegistrasi(String nomorRegistrasi)  // Get by registration number
existsByStudentId(Long studentId)         // Check if exists
```

### 3. HasilAkhirService.java
Business logic service with methods:
- `createHasilAkhir()` - Create/update hasil akhir
- `autoPopulateHasilAkhir()` - Auto-fill from cicilan + reenrollment data
- `getHasilAkhirByStudentId()` - Retrieve for display
- `updateStatus()` - Change status
- `studentHasHasilAkhir()` - Check existence

---

## Workflow: When to Populate

### 1. **After Validation Approval** (FormValidation DIVALIDASI)
When admin approves student's form (in AdminController):

```java
// In approval endpoint
if (validation.getStatus() == ValidationStatus.DISETUJUI) {
    hasilAkhirService.autoPopulateHasilAkhir(studentId);
}
```

### 2. **Data Sources**
- **BRIVA**: From `manual_cicilan_payments` table (virtual_account field)
- **Amount**: From `manual_cicilan_payments` (amount field)
- **Nomor Registrasi**: Generated format `REG-YYYYMMDD-STUDENTID` or from reenrollments
- **Student ID**: From admission context

### 3. **Status Lifecycle**
```
PENDING (created)
   ↓
ACTIVE (when validation approved)
   ↓
USED (when student accesses the info)
   ↓
EXPIRED (after deadline)
   ↓
CANCELLED (if application rejected)
```

---

## API Integration Points

### CamabaController Methods to Add

#### 1. Get Current Student's Hasil Akhir
```java
@GetMapping("/camaba/hasil-akhir")
public ResponseEntity<?> getHasilAkhir() {
    // Get authenticated user
    // Fetch from HasilAkhirRepository
    // Return BRIVA and nomor_registrasi
}
```

#### 2. Admin - Populate Hasil Akhir on Approval
```java
// In /api/validasi/formulir/{id}/approve endpoint:
hasilAkhirService.autoPopulateHasilAkhir(studentId);
```

---

## Database Migration

### Apply Migration
```bash
mysql -u [user] -p [database] < MIGRATION_HASIL_AKHIR.sql
```

### Verify
```sql
SELECT COUNT(*) as total_hasil_akhir FROM hasil_akhir;

-- Check unique constraints
SELECT student_id, COUNT(*) FROM hasil_akhir 
GROUP BY student_id HAVING COUNT(*) > 1;
```

---

## Testing Checklist

- [ ] Migration runs without errors
- [ ] Table created with all columns
- [ ] Unique constraints enforced
- [ ] Foreign keys work correctly
- [ ] HasilAkhir entity can be saved
- [ ] Can retrieve by student_id (one-to-one)
- [ ] BRIVA number is unique
- [ ] Nomor registrasi is unique
- [ ] Service methods work
- [ ] Admin approval triggers auto-population
- [ ] Student can view hasil akhir on dashboard

---

## Example Data
```sql
INSERT INTO hasil_akhir (student_id, user_id, briva_number, briva_amount, nomor_registrasi, status)
VALUES (1, 1, '123456789', 5000000.00, 'REG-20260411-000001', 'ACTIVE');
```

---

## Notes
- Table grows slowly (one row per approved student)
- Perfect for the "Hasil Akhir" dashboard section
- Immutable once created (only status can change)
- Easy to query for reporting
- Replaces complex JOIN logic in frontend
