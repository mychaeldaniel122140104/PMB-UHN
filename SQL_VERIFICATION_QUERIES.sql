# SQL VERIFICATION QUERIES - DATABASE FIELD MAPPING

These SQL queries help verify that:
1. All required columns exist in the `admission_forms` table
2. Data is properly stored in dedicated columns (not in JSON)
3. All 50+ fields are populated correctly

## INSERT TEST DATA VERIFICATION

### Query 1: Check Latest Submitted Form (All Fields)
```sql
SELECT 
    id as 'Form ID',
    student_id as 'Student',
    full_name as 'Full Name',
    nik as 'NIK',
    email as 'Email',
    phone_number as 'Phone',
    birth_date as 'Birth Date',
    gender as 'Gender',
    address_medan as 'Address',
    religion as 'Religion',
    father_name as 'Father Name',
    father_nik as 'Father NIK',
    father_occupation as 'Father Job',
    mother_name as 'Mother Name',
    mother_nik as 'Mother NIK',
    mother_occupation as 'Mother Job',
    school_origin as 'School',
    nisn as 'NISN',
    school_major as 'Major',
    school_year as 'Year',
    photo_id_path as 'Photo Path',
    certificate_path as 'Certificate Path',
    transcript_path as 'Transcript Path',
    status as 'Status',
    additional_info as 'JSON (should be NULL)',
    submitted_at as 'Submitted At'
FROM admission_forms
WHERE status = 'VERIFIED'
ORDER BY submitted_at DESC
LIMIT 1;
```

### Query 2: Check All Column Exist
```sql
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_KEY
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'admission_forms'
ORDER BY ORDINAL_POSITION;
```

### Query 3: Detailed Column Verification (Personal Data)
```sql
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'admission_forms'
AND COLUMN_NAME IN (
    'full_name', 'nik', 'birth_date', 'birth_place', 'gender',
    'phone_number', 'email', 'address_medan', 'residence_info',
    'subdistrict', 'district', 'city', 'province', 'religion',
    'information_source'
)
ORDER BY ORDINAL_POSITION;
```

### Query 4: Detailed Column Verification (Father Data)
```sql
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'admission_forms'
AND COLUMN_NAME IN (
    'father_nik', 'father_name', 'father_birth_date',
    'father_education', 'father_occupation', 'father_income',
    'father_phone', 'father_status'
)
ORDER BY ORDINAL_POSITION;
```

### Query 5: Detailed Column Verification (Mother Data)
```sql
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'admission_forms'
AND COLUMN_NAME IN (
    'mother_nik', 'mother_name', 'mother_birth_date',
    'mother_education', 'mother_occupation', 'mother_income',
    'mother_phone', 'mother_status'
)
ORDER BY ORDINAL_POSITION;
```

### Query 6: Detailed Column Verification (School Data)
```sql
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'admission_forms'
AND COLUMN_NAME IN (
    'school_origin', 'school_major', 'school_year', 'nisn',
    'school_city', 'school_province'
)
ORDER BY ORDINAL_POSITION;
```

### Query 7: Detailed Column Verification (Files)
```sql
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'admission_forms'
AND COLUMN_NAME IN (
    'photo_id_path', 'certificate_path', 'transcript_path'
)
ORDER BY ORDINAL_POSITION;
```

### Query 8: Count Total Columns in admission_forms
```sql
SELECT 
    COUNT(*) as 'Total Columns',
    SUM(CASE WHEN IS_NULLABLE = 'YES' THEN 1 ELSE 0 END) as 'Nullable',
    SUM(CASE WHEN IS_NULLABLE = 'NO' THEN 1 ELSE 0 END) as 'Not Nullable'
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'admission_forms';
```

**Expected Result:**
```
Total Columns: 50+
Nullable: 45+
Not Nullable: 3-5 (id, student_id, selection_type_id, created_at, updated_at)
```

## DATA QUALITY CHECKS

### Query 9: Check if additionalInfo Contains JSON (RED FLAG!)
```sql
SELECT 
    id,
    full_name,
    additional_info,
    CHAR_LENGTH(additional_info) as 'JSON Size',
    submitted_at
FROM admission_forms
WHERE additional_info IS NOT NULL 
AND CHAR_LENGTH(additional_info) > 10
ORDER BY submitted_at DESC;
```

**Expected Result:**
- No rows returned (empty result)
- If rows appear, it means old data had JSON in additionalInfo

### Query 10: Check Data Completeness (Count Non-NULL Values)
```sql
SELECT 
    id,
    (
        (full_name IS NOT NULL)::INT +
        (nik IS NOT NULL)::INT +
        (email IS NOT NULL)::INT +
        (father_name IS NOT NULL)::INT +
        (mother_name IS NOT NULL)::INT +
        (school_origin IS NOT NULL)::INT +
        (nisn IS NOT NULL)::INT
    ) as 'Critical Fields Populated',
    submitted_at
FROM admission_forms
WHERE status = 'VERIFIED'
ORDER BY submitted_at DESC
LIMIT 5;
```

### Query 11: Find Forms with Missing Critical Data
```sql
SELECT 
    id,
    full_name,
    nik,
    email,
    father_name,
    mother_name,
    school_origin,
    nisn,
    submitted_at
FROM admission_forms
WHERE 
    (full_name IS NULL OR full_name = '') OR
    (nik IS NULL OR nik = '')  OR
    (email IS NULL OR email = '')
ORDER BY submitted_at DESC;
```

**What This Means:**
- If results show NULL values, those fields weren't saved properly
- Use the application logs to diagnose why

### Query 12: Verify No Corruption (Data Type Checks)
```sql
SELECT 
    id,
    full_name,
    CASE 
        WHEN full_name LIKE '{%' THEN 'JSON DETECTED!' 
        WHEN full_name LIKE '[%' THEN 'ARRAY DETECTED!'
        ELSE 'OK'
    END as 'Data Format Check',
    CHAR_LENGTH(full_name) as 'Length',
    submitted_at
FROM admission_forms
WHERE full_name IS NOT NULL
ORDER BY submitted_at DESC
LIMIT 5;
```

## FIELD POPULATION SUMMARY

### Query 13: Summary Report (Field Population %)
```sql
SELECT 
    COUNT(*) as 'Total Forms',
    SUM(CASE WHEN full_name IS NOT NULL THEN 1 ELSE 0 END) as 'full_name',
    SUM(CASE WHEN nik IS NOT NULL THEN 1 ELSE 0 END) as 'nik',
    SUM(CASE WHEN email IS NOT NULL THEN 1 ELSE 0 END) as 'email',
    SUM(CASE WHEN father_name IS NOT NULL THEN 1 ELSE 0 END) as 'father_name',
    SUM(CASE WHEN mother_name IS NOT NULL THEN 1 ELSE 0 END) as 'mother_name',
    SUM(CASE WHEN school_origin IS NOT NULL THEN 1 ELSE 0 END) as 'school_origin',
    SUM(CASE WHEN nisn IS NOT NULL THEN 1 ELSE 0 END) as 'nisn',
    ROUND(
        100.0 * SUM(CASE WHEN additional_info IS NULL THEN 1 ELSE 0 END) / COUNT(*),
        2
    ) as 'Forms Without JSON %'
FROM admission_forms;
```

### Query 14: Get Form by Student Email
```sql
SELECT 
    af.id,
    af.full_name,
    af.nik,
    af.email,
    af.father_name,
    af.mother_name,
    af.school_origin,
    af.nisn,
    af.status,
    af.submitted_at,
    af.additional_info
FROM admission_forms af
JOIN student s ON af.student_id = s.id
JOIN user u ON s.user_id = u.id
WHERE u.email = 'student@example.com'
ORDER BY af.submitted_at DESC
LIMIT 1;
```

## DEBUGGING QUERIES

### Query 15: Trace Specific Student's Forms
```sql
SELECT 
    af.id as 'Form ID',
    af.full_name,
    af.status,
    af.submitted_at,
    af.updated_at,
    COUNT(DISTINCT CASE WHEN af.full_name IS NOT NULL THEN 1 END) as 'Has Data'
FROM admission_forms af
JOIN student s ON af.student_id = s.id
JOIN user u ON s.user_id = u.id
WHERE u.email = 'student@example.com'
GROUP BY af.id
ORDER BY af.submitted_at DESC;
```

### Query 16: Find Recently Submitted Forms
```sql
SELECT 
    id,
    full_name,
    email,
    phone_number,
    status,
    submitted_at,
    additional_info IS NULL as 'No JSON'
FROM admission_forms
WHERE submitted_at > DATE_SUB(NOW(), INTERVAL 1 DAY)
ORDER BY submitted_at DESC;
```

### Query 17: Field Population Distribution
```sql
SELECT 
    'Personal Data' as 'Category',
    15 as 'Total Fields',
    SUM(CASE WHEN full_name IS NOT NULL THEN 1 ELSE 0 END) +
    SUM(CASE WHEN nik IS NOT NULL THEN 1 ELSE 0 END) +
    SUM(CASE WHEN email IS NOT NULL THEN 1 ELSE 0 END) +
    SUM(CASE WHEN phone_number IS NOT NULL THEN 1 ELSE 0 END) +
    SUM(CASE WHEN gender IS NOT NULL THEN 1 ELSE 0 END) +
    SUM(CASE WHEN address_medan IS NOT NULL THEN 1 ELSE 0 END) +
    SUM(CASE WHEN religion IS NOT NULL THEN 1 ELSE 0 END)
    as 'Avg Populated'
FROM admission_forms
UNION ALL
SELECT 
    'Father Data',
    8,
    SUM(CASE WHEN father_name IS NOT NULL THEN 1 ELSE 0 END) +
    SUM(CASE WHEN father_occupation IS NOT NULL THEN 1 ELSE 0 END) +
    SUM(CASE WHEN father_income IS NOT NULL THEN 1 ELSE 0 END)
FROM admission_forms
UNION ALL
SELECT 
    'School Data',
    6,
    SUM(CASE WHEN school_origin IS NOT NULL THEN 1 ELSE 0 END) +
    SUM(CASE WHEN nisn IS NOT NULL THEN 1 ELSE 0 END) +
    SUM(CASE WHEN school_year IS NOT NULL THEN 1 ELSE 0 END)
FROM admission_forms;
```

## PERFORMANCE CHECKS

### Query 18: Check for Slow Queries on admission_forms
```sql
SELECT 
    COUNT(*) as 'Total Forms',
    AVG(CHAR_LENGTH(additional_info)) as 'Avg JSON Size (bytes)',
    MAX(CHAR_LENGTH(additional_info)) as 'Max JSON Size (bytes)'
FROM admission_forms;
```

**Good Result:**
- Avg JSON Size < 100 bytes (mostly NULL or very small)
- Max JSON Size < 500 bytes

**Bad Result:**
- Avg JSON Size > 1000 bytes (data being stored as JSON!)
- Max JSON Size > 10000 bytes (big JSON blobs!)

---

## QUICK COMMANDS

### Run All Verification Queries (MySQL)
```bash
mysql -h localhost -u root -p pmb_database < verification_queries.sql
```

### Connect to Database
```bash
# MySQL
mysql -h localhost -u root -p -D pmb_database

# OR if using H2 (development)
# Go to http://localhost:9500/h2-console
# JDBC URL: jdbc:h2:mem:pmb_uhn;MODE=MySQL
```

---

## INTERPRETATION GUIDE

### ✅ GOOD RESULTS
- All columns exist (50+columns)
- No JSON in additional_info column
- All form fields have values (not NULL)
- Forms recently submitted have proper data
- No data corruption detected

### ⚠️ RED FLAGS
- Some columns missing (means old schema)
- additional_info contains JSON data (old autosave!)
- Critical fields are NULL (mapping issue)
- Data looks like corrupted JSON string
- Recent forms have weird/truncated data

### 🔧 TROUBLESHOOTING

**If columns are missing:**
```sql
ALTER TABLE admission_forms 
ADD COLUMN father_occupation VARCHAR(100);
```

**If additional_info has JSON:**
```sql
UPDATE admission_forms 
SET additional_info = NULL 
WHERE additional_info IS NOT NULL;
```

**If specific fields are NULL:**
- Check application logs for error during submission
- Check DTO and Entity mapping
- Verify frontend is sending that field

---

## MONITORING QUERIES  

Use these in an ongoing basis to monitor form submissions:

### Daily Summary
```sql
SELECT 
    DATE(submitted_at) as 'Date',
    COUNT(*) as 'Forms Submitted',
    SUM(CASE WHEN status='VERIFIED' THEN 1 ELSE 0 END) as 'Verified',
    SUM(CASE WHEN additional_info IS NULL THEN 1 ELSE 0 END) as 'No JSON'
FROM admission_forms
WHERE submitted_at > DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY DATE(submitted_at)
ORDER BY DATE(submitted_at) DESC;
```

### Check Last 10 Submissions
```sql
SELECT 
    id,
    full_name,
    email,
    status,
    submitted_at,
    CASE WHEN full_name IS NOT NULL THEN '✓' ELSE '✗' END as 'Data OK'
FROM admission_forms
ORDER BY submitted_at DESC
LIMIT 10;
```

---

**Last Updated:** 27 March 2026  
**Database:** MySQL / H2  
**Version:** 1.0
