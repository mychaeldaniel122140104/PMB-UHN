-- =====================================================
-- COMPREHENSIVE PROGRAM STUDI PRICE UPDATE
-- 25 Programs with Actual Tuition Fees
-- Created: 2026-04-10
-- =====================================================

-- =====================================================
-- UPDATE EXISTING PROGRAMS WITH CORRECT PRICES
-- =====================================================

-- FKIP (Faculty of Teacher Training)
UPDATE program_studi SET harga_total_per_tahun = 4305500, is_active = TRUE, updated_at = NOW() WHERE kode = 'pend-fisika';
UPDATE program_studi SET harga_total_per_tahun = 8999000, is_active = TRUE, updated_at = NOW() WHERE kode = 'pend-bahasa-sastra-indonesia';
UPDATE program_studi SET harga_total_per_tahun = 8980000, is_active = TRUE, updated_at = NOW() WHERE kode = 'pend-biologi-inggris';
UPDATE program_studi SET harga_total_per_tahun = 7875000, is_active = TRUE, updated_at = NOW() WHERE kode = 'pend-pancasila-kewarganegaraan';
UPDATE program_studi SET harga_total_per_tahun = 8057000, is_active = TRUE, updated_at = NOW() WHERE kode = 'pend-ekonomi';
UPDATE program_studi SET harga_total_per_tahun = 7966000, is_active = TRUE, updated_at = NOW() WHERE kode = 'pend-agama-kristen';
UPDATE program_studi SET harga_total_per_tahun = 4782500, is_active = TRUE, updated_at = NOW() WHERE kode = 'pend-ipa';

-- TEKNIK (Engineering Faculty)
UPDATE program_studi SET harga_total_per_tahun = 9921000, is_active = TRUE, updated_at = NOW() WHERE kode = 'teknik-sipil';
UPDATE program_studi SET harga_total_per_tahun = 10131000, is_active = TRUE, updated_at = NOW() WHERE kode = 'teknik-mesin';
UPDATE program_studi SET harga_total_per_tahun = 5438000, is_active = TRUE, updated_at = NOW() WHERE kode = 'teknik-elektro';
UPDATE program_studi SET harga_total_per_tahun = 8391000, is_active = TRUE, updated_at = NOW() WHERE kode = 'informatika';

-- EKONOMI & BISNIS
UPDATE program_studi SET harga_total_per_tahun = 9691000, is_active = TRUE, updated_at = NOW() WHERE kode = 'akuntansi';
UPDATE program_studi SET harga_total_per_tahun = 9991000, is_active = TRUE, updated_at = NOW() WHERE kode = 'manajemen';
UPDATE program_studi SET harga_total_per_tahun = 8827000, is_active = TRUE, updated_at = NOW() WHERE kode = 'ekonomi-pembangunan';

-- Update D3 Perpajakan to new code & price
UPDATE program_studi SET kode = 'adm-pajak', harga_total_per_tahun = 8027000, is_active = TRUE, updated_at = NOW() WHERE kode = 'adm-perpajakan-d3';

-- FISIPOL (Faculty of Social & Political Sciences)
UPDATE program_studi SET harga_total_per_tahun = 7433000, is_active = TRUE, updated_at = NOW() WHERE kode = 'adm-bisnis' OR kode = 'adm-bisnis-fisipol';
UPDATE program_studi SET harga_total_per_tahun = 5712250, is_active = TRUE, updated_at = NOW() WHERE kode = 'adm-publik';

-- =====================================================
-- INSERT NEW PROGRAMS (if they don't exist)
-- =====================================================

-- FKIP - Pend. Matematika
INSERT INTO program_studi (kode, nama, deskripsi, is_medical, is_active, sort_order, harga_total_per_tahun, created_at, updated_at)
SELECT 'pend-matematika', 'Pend. Matematika', '', FALSE, TRUE, 15, 7968000, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM program_studi WHERE kode = 'pend-matematika');

-- FISIPOL - Adm. Publik
INSERT INTO program_studi (kode, nama, deskripsi, is_medical, is_active, sort_order, harga_total_per_tahun, created_at, updated_at)
SELECT 'adm-publik', 'Adm. Publik', '', FALSE, TRUE, 16, 5712250, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM program_studi WHERE kode = 'adm-publik');

-- PETERNAKAN - Prod. Ternak
INSERT INTO program_studi (kode, nama, deskripsi, is_medical, is_active, sort_order, harga_total_per_tahun, created_at, updated_at)
SELECT 'prod-ternak', 'Prod. Ternak', '', FALSE, TRUE, 17, 6438250, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM program_studi WHERE kode = 'prod-ternak');

-- HUKUM - Ilmu Hukum
INSERT INTO program_studi (kode, nama, deskripsi, is_medical, is_active, sort_order, harga_total_per_tahun, created_at, updated_at)
SELECT 'ilmu-hukum', 'Ilmu Hukum', '', FALSE, TRUE, 18, 8355000, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM program_studi WHERE kode = 'ilmu-hukum');

-- PERTANIAN - Agroteknologi
INSERT INTO program_studi (kode, nama, deskripsi, is_medical, is_active, sort_order, harga_total_per_tahun, created_at, updated_at)
SELECT 'agroteknologi', 'Agroteknologi', '', FALSE, TRUE, 19, 6475750, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM program_studi WHERE kode = 'agroteknologi');

-- PERTANIAN - Agribisnis
INSERT INTO program_studi (kode, nama, deskripsi, is_medical, is_active, sort_order, harga_total_per_tahun, created_at, updated_at)
SELECT 'agribisnis', 'Agribisnis', '', FALSE, TRUE, 20, 6475750, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM program_studi WHERE kode = 'agribisnis');

-- BAHASA & SENI - Seni Musik
INSERT INTO program_studi (kode, nama, deskripsi, is_medical, is_active, sort_order, harga_total_per_tahun, created_at, updated_at)
SELECT 'seni-musik', 'Seni Musik', '', FALSE, TRUE, 21, 4338500, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM program_studi WHERE kode = 'seni-musik');

-- BAHASA & SENI - Sastra Inggris
INSERT INTO program_studi (kode, nama, deskripsi, is_medical, is_active, sort_order, harga_total_per_tahun, created_at, updated_at)
SELECT 'sastra-inggris', 'Sastra Inggris', '', FALSE, TRUE, 22, 6558250, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM program_studi WHERE kode = 'sastra-inggris');

-- PSIKOLOGI - Psikologi
INSERT INTO program_studi (kode, nama, deskripsi, is_medical, is_active, sort_order, harga_total_per_tahun, created_at, updated_at)
SELECT 'psikologi', 'Psikologi', '', FALSE, TRUE, 23, 6475500, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM program_studi WHERE kode = 'psikologi');

-- =====================================================
-- DEACTIVATE PROGRAMS THAT NO LONGER EXIST
-- =====================================================
UPDATE program_studi SET is_active = FALSE, updated_at = NOW() WHERE kode = 'pendidikan-dokter';
UPDATE program_studi SET is_active = FALSE, updated_at = NOW() WHERE kode = 'profesi-dokter';
UPDATE program_studi SET is_active = FALSE, updated_at = NOW() WHERE kode = 'pend-profesi-guru';

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================
-- Check all active programs with prices
SELECT 'FINAL LIST - All Active Programs' as `Status`;
SELECT id, kode, nama, harga_total_per_tahun, is_active
FROM program_studi
WHERE is_active = TRUE
ORDER BY sort_order;

-- Count active programs
SELECT 'TOTAL ACTIVE PROGRAMS' as `Metric`;
SELECT COUNT(*) as active_count, SUM(harga_total_per_tahun) as total_value FROM program_studi WHERE is_active = TRUE;

-- Check data integrity
SELECT 'Programs with Missing Prices' as `Check`;
SELECT id, kode, nama, harga_total_per_tahun FROM program_studi WHERE is_active = TRUE AND (harga_total_per_tahun = 0 OR harga_total_per_tahun IS NULL);
