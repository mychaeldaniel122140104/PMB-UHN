-- =====================================================
-- PROGRAM STUDI - QUICK EXECUTION SCRIPT
-- Copy & paste langsung ke database client
-- Tested: 2026-04-10
-- =====================================================

-- =====================================================
-- STEP 1: Backup existing data (OPTIONAL)
-- =====================================================
-- CREATE TABLE program_studi_backup_20260410 AS SELECT * FROM program_studi;


-- =====================================================
-- STEP 2: Deactivate old programs (no longer offered)
-- =====================================================
UPDATE program_studi SET is_active = FALSE, updated_at = NOW() WHERE kode = 'pendidikan-dokter';
UPDATE program_studi SET is_active = FALSE, updated_at = NOW() WHERE kode = 'profesi-dokter';
UPDATE program_studi SET is_active = FALSE, updated_at = NOW() WHERE kode = 'pend-profesi-guru';

-- =====================================================
-- STEP 3: Update prices for existing programs
-- =====================================================

-- FKIP
UPDATE program_studi SET harga_total_per_tahun = 4305500, is_active = TRUE, updated_at = NOW() WHERE kode = 'pend-fisika';
UPDATE program_studi SET harga_total_per_tahun = 8999000, is_active = TRUE, updated_at = NOW() WHERE kode = 'pend-bahasa-sastra-indonesia';
UPDATE program_studi SET harga_total_per_tahun = 8980000, is_active = TRUE, updated_at = NOW() WHERE kode = 'pend-biologi-inggris';
UPDATE program_studi SET harga_total_per_tahun = 7875000, is_active = TRUE, updated_at = NOW() WHERE kode = 'pend-pancasila-kewarganegaraan';
UPDATE program_studi SET harga_total_per_tahun = 8057000, is_active = TRUE, updated_at = NOW() WHERE kode = 'pend-ekonomi';
UPDATE program_studi SET harga_total_per_tahun = 7966000, is_active = TRUE, updated_at = NOW() WHERE kode = 'pend-agama-kristen';
UPDATE program_studi SET harga_total_per_tahun = 4782500, is_active = TRUE, updated_at = NOW() WHERE kode = 'pend-ipa';

-- TEKNIK
UPDATE program_studi SET harga_total_per_tahun = 9921000, is_active = TRUE, updated_at = NOW() WHERE kode = 'teknik-sipil';
UPDATE program_studi SET harga_total_per_tahun = 10131000, is_active = TRUE, updated_at = NOW() WHERE kode = 'teknik-mesin';
UPDATE program_studi SET harga_total_per_tahun = 5438000, is_active = TRUE, updated_at = NOW() WHERE kode = 'teknik-elektro';
UPDATE program_studi SET harga_total_per_tahun = 8391000, is_active = TRUE, updated_at = NOW() WHERE kode = 'informatika';

-- EKONOMI & BISNIS (juga update kode lama)
UPDATE program_studi SET harga_total_per_tahun = 9691000, is_active = TRUE, updated_at = NOW() WHERE kode = 'akuntansi';
UPDATE program_studi SET harga_total_per_tahun = 9991000, is_active = TRUE, updated_at = NOW() WHERE kode = 'manajemen';
UPDATE program_studi SET harga_total_per_tahun = 8827000, is_active = TRUE, updated_at = NOW() WHERE kode = 'ekonomi-pembangunan';
UPDATE program_studi SET kode = 'adm-pajak', harga_total_per_tahun = 8027000, is_active = TRUE, updated_at = NOW() WHERE kode = 'adm-perpajakan-d3';

-- =====================================================
-- STEP 4: Insert new programs (if they don't exist)
-- =====================================================

INSERT INTO program_studi (kode, nama, deskripsi, is_medical, is_active, sort_order, harga_total_per_tahun, created_at, updated_at)
SELECT 'pend-matematika', 'Pend. Matematika', '', FALSE, TRUE, 6, 7968000, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM program_studi WHERE kode = 'pend-matematika');

INSERT INTO program_studi (kode, nama, deskripsi, is_medical, is_active, sort_order, harga_total_per_tahun, created_at, updated_at)
SELECT 'adm-bisnis', 'Adm. Bisnis', '', FALSE, TRUE, 9, 7433000, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM program_studi WHERE kode = 'adm-bisnis');

INSERT INTO program_studi (kode, nama, deskripsi, is_medical, is_active, sort_order, harga_total_per_tahun, created_at, updated_at)
SELECT 'adm-publik', 'Adm. Publik', '', FALSE, TRUE, 10, 5712250, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM program_studi WHERE kode = 'adm-publik');

INSERT INTO program_studi (kode, nama, deskripsi, is_medical, is_active, sort_order, harga_total_per_tahun, created_at, updated_at)
SELECT 'prod-ternak', 'Prod. Ternak', '', FALSE, TRUE, 15, 6438250, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM program_studi WHERE kode = 'prod-ternak');

INSERT INTO program_studi (kode, nama, deskripsi, is_medical, is_active, sort_order, harga_total_per_tahun, created_at, updated_at)
SELECT 'ilmu-hukum', 'Ilmu Hukum', '', FALSE, TRUE, 16, 8355000, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM program_studi WHERE kode = 'ilmu-hukum');

INSERT INTO program_studi (kode, nama, deskripsi, is_medical, is_active, sort_order, harga_total_per_tahun, created_at, updated_at)
SELECT 'agroteknologi', 'Agroteknologi', '', FALSE, TRUE, 17, 6475750, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM program_studi WHERE kode = 'agroteknologi');

INSERT INTO program_studi (kode, nama, deskripsi, is_medical, is_active, sort_order, harga_total_per_tahun, created_at, updated_at)
SELECT 'agribisnis', 'Agribisnis', '', FALSE, TRUE, 18, 6475750, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM program_studi WHERE kode = 'agribisnis');

INSERT INTO program_studi (kode, nama, deskripsi, is_medical, is_active, sort_order, harga_total_per_tahun, created_at, updated_at)
SELECT 'seni-musik', 'Seni Musik', '', FALSE, TRUE, 19, 4338500, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM program_studi WHERE kode = 'seni-musik');

INSERT INTO program_studi (kode, nama, deskripsi, is_medical, is_active, sort_order, harga_total_per_tahun, created_at, updated_at)
SELECT 'sastra-inggris', 'Sastra Inggris', '', FALSE, TRUE, 20, 6558250, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM program_studi WHERE kode = 'sastra-inggris');

INSERT INTO program_studi (kode, nama, deskripsi, is_medical, is_active, sort_order, harga_total_per_tahun, created_at, updated_at)
SELECT 'psikologi', 'Psikologi', '', FALSE, TRUE, 21, 6475500, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM program_studi WHERE kode = 'psikologi');

-- =====================================================
-- STEP 5: VERIFICATION (Run these to check)
-- =====================================================

-- Check total: should be 25 programs, Rp 197,445,250
SELECT 'FINAL VERIFICATION' as Check;
SELECT COUNT(*) as total_active_programs, SUM(harga_total_per_tahun) as total_value_rp
FROM program_studi
WHERE is_active = TRUE;

-- List all programs with prices
SELECT 'COMPLETE LIST OF 25 PROGRAMS' as List;
SELECT id, kode, nama, harga_total_per_tahun, is_active, sort_order
FROM program_studi
WHERE is_active = TRUE
ORDER BY sort_order;

-- Check if any programs have missing data
SELECT 'CHECK FOR ISSUES' as Status;
SELECT id, kode, nama, harga_total_per_tahun
FROM program_studi
WHERE is_active = TRUE AND (harga_total_per_tahun = 0 OR harga_total_per_tahun IS NULL);

-- If above query returns 0 rows, everything is OK!
-- If returns rows, check those programs.

-- =====================================================
-- DONE! Ready for rebuild & test
-- =====================================================
