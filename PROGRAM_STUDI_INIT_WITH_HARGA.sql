-- =====================================================
-- PROGRAM_STUDI INITIALIZATION / UPDATE SCRIPT
-- Purpose: Initialize or update program studi with harga total per tahun
-- Created: 2026-04-10
-- =====================================================

-- ❌ BACKUP FIRST (if updating existing)
-- SELECT * FROM program_studi;

-- ============================================
-- OPTION 1: INSERT NEW (if table is empty)
-- ============================================
-- Note: Uncomment and run if table is empty

/*
INSERT INTO program_studi (kode, nama, deskripsi, is_medical, is_active, sort_order, harga_total_per_tahun, created_at, updated_at)
VALUES
-- Medical Programs
(1, 'pendidikan-dokter', 'Pendidikan Dokter', '', TRUE, 150000000, TRUE, 1, NOW(), NOW()),
(2, 'profesi-dokter', 'Profesi Dokter', '', TRUE, 100000000, TRUE, 2, NOW(), NOW()),

-- Engineering Programs
(3, 'teknik-sipil', 'Teknik Sipil', '', FALSE, 80000000, TRUE, 3, NOW(), NOW()),
(4, 'teknik-mesin', 'Teknik Mesin', '', FALSE, 80000000, TRUE, 4, NOW(), NOW()),
(5, 'teknik-elektro', 'Teknik Elektro', '', FALSE, 80000000, TRUE, 5, NOW(), NOW()),
(6, 'informatika', 'Informatika', '', FALSE, 80000000, TRUE, 6, NOW(), NOW()),

-- Education Programs
(7, 'pend-biologi-inggris', 'Pend. Biologi Inggris', '', FALSE, 50000000, TRUE, 7, NOW(), NOW()),
(8, 'pend-ekonomi', 'Pend. Ekonomi', '', FALSE, 50000000, TRUE, 8, NOW(), NOW()),
(9, 'pend-agama-kristen', 'Pend. Agama Kristen', '', FALSE, 50000000, TRUE, 9, NOW(), NOW()),
(10, 'pend-pancasila-kewarganegaraan', 'Pend. Pancasila & Kewarganegaraan', '', FALSE, 50000000, TRUE, 10, NOW(), NOW()),
(11, 'pend-bahasa-sastra-indonesia', 'Pend. Bahasa & Sastra Indonesia', '', FALSE, 50000000, TRUE, 11, NOW(), NOW()),
(12, 'pend-fisika', 'Pend. Fisika', '', FALSE, 50000000, TRUE, 12, NOW(), NOW()),
(13, 'pend-ipa', 'Pend. IPA', '', FALSE, 50000000, TRUE, 13, NOW(), NOW()),
(14, 'pend-profesi-guru', 'Pend. Profesi Guru', '', FALSE, 50000000, TRUE, 14, NOW(), NOW()),

-- Economics Programs
(15, 'ekonomi-pembangunan', 'Ekonomi Pembangunan', '', FALSE, 60000000, TRUE, 15, NOW(), NOW()),
(16, 'manajemen', 'Manajemen', '', FALSE, 60000000, TRUE, 16, NOW(), NOW()),
(17, 'akuntansi', 'Akuntansi', '', FALSE, 60000000, TRUE, 17, NOW(), NOW()),

-- Diploma Programs
(18, 'adm-perpajakan-d3', 'Administrasi Perpajakan (D3)', '', FALSE, 40000000, TRUE, 18, NOW(), NOW());
*/

-- ============================================
-- OPTION 2: UPDATE EXISTING (more common)
-- ============================================
-- This updates existing program studi with harga_total_per_tahun

-- Medical Programs (Pendidikan & Profesi Dokter)
UPDATE program_studi SET harga_total_per_tahun = 150000000, updated_at = NOW() WHERE kode = 'pendidikan-dokter';
UPDATE program_studi SET harga_total_per_tahun = 100000000, updated_at = NOW() WHERE kode = 'profesi-dokter';

-- Engineering Programs (Teknik)
UPDATE program_studi SET harga_total_per_tahun = 80000000, updated_at = NOW() WHERE kode = 'teknik-sipil';
UPDATE program_studi SET harga_total_per_tahun = 80000000, updated_at = NOW() WHERE kode = 'teknik-mesin';
UPDATE program_studi SET harga_total_per_tahun = 80000000, updated_at = NOW() WHERE kode = 'teknik-elektro';
UPDATE program_studi SET harga_total_per_tahun = 80000000, updated_at = NOW() WHERE kode = 'informatika';

-- Education Programs (Pend.)
UPDATE program_studi SET harga_total_per_tahun = 50000000, updated_at = NOW() WHERE kode = 'pend-biologi-inggris';
UPDATE program_studi SET harga_total_per_tahun = 50000000, updated_at = NOW() WHERE kode = 'pend-ekonomi';
UPDATE program_studi SET harga_total_per_tahun = 50000000, updated_at = NOW() WHERE kode = 'pend-agama-kristen';
UPDATE program_studi SET harga_total_per_tahun = 50000000, updated_at = NOW() WHERE kode = 'pend-pancasila-kewarganegaraan';
UPDATE program_studi SET harga_total_per_tahun = 50000000, updated_at = NOW() WHERE kode = 'pend-bahasa-sastra-indonesia';
UPDATE program_studi SET harga_total_per_tahun = 50000000, updated_at = NOW() WHERE kode = 'pend-fisika';
UPDATE program_studi SET harga_total_per_tahun = 50000000, updated_at = NOW() WHERE kode = 'pend-ipa';
UPDATE program_studi SET harga_total_per_tahun = 50000000, updated_at = NOW() WHERE kode = 'pend-profesi-guru';

-- Economics Programs (Ekonomi & Manajemen)
UPDATE program_studi SET harga_total_per_tahun = 60000000, updated_at = NOW() WHERE kode = 'ekonomi-pembangunan';
UPDATE program_studi SET harga_total_per_tahun = 60000000, updated_at = NOW() WHERE kode = 'manajemen';
UPDATE program_studi SET harga_total_per_tahun = 60000000, updated_at = NOW() WHERE kode = 'akuntansi';

-- Diploma Programs (D3)
UPDATE program_studi SET harga_total_per_tahun = 40000000, updated_at = NOW() WHERE kode = 'adm-perpajakan-d3';

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================
-- Verify data is correct
SELECT 'Program Studi Summary' as `Section`;
SELECT id, kode, nama, harga_total_per_tahun 
FROM program_studi 
ORDER BY id;

-- Check if any program studi missing harga_total_per_tahun
SELECT 'Program Studi dengan Harga Tidak Diisi' as `Check`;
SELECT id, kode, nama, harga_total_per_tahun 
FROM program_studi 
WHERE harga_total_per_tahun = 0 OR harga_total_per_tahun IS NULL
ORDER BY id;

-- Summary by category
SELECT 'Summary by Kategori' as `Summary`;
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
