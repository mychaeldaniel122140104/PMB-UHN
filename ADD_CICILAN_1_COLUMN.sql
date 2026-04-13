-- =====================================================
-- ADD CICILAN_1 COLUMN TO PROGRAM_STUDI
-- Purpose: Store first installment amount for each program
-- Created: 2026-04-10
-- =====================================================

-- Add new column
ALTER TABLE program_studi 
ADD COLUMN cicilan_1 BIGINT DEFAULT 0;

-- =====================================================
-- UPDATE ALL CICILAN_1 VALUES BY PROGRAM
-- =====================================================

-- FKIP (Faculty of Teacher Training)
UPDATE program_studi SET cicilan_1 = 1236583 WHERE kode = 'pend-fisika';
UPDATE program_studi SET cicilan_1 = 1596567 WHERE kode = 'pend-bahasa-sastra-indonesia';
UPDATE program_studi SET cicilan_1 = 1938867 WHERE kode = 'pend-biologi-inggris';
UPDATE program_studi SET cicilan_1 = 1821267 WHERE kode = 'pend-pancasila-kewarganegaraan';
UPDATE program_studi SET cicilan_1 = 1875867 WHERE kode = 'pend-ekonomi';
UPDATE program_studi SET cicilan_1 = 1875867 WHERE kode = 'pend-matematika';
UPDATE program_studi SET cicilan_1 = 1848567 WHERE kode = 'pend-agama-kristen';
UPDATE program_studi SET cicilan_1 = 1275083 WHERE kode = 'pend-ipa';

-- FISIPOL (Faculty of Social & Political Sciences)
UPDATE program_studi SET cicilan_1 = 1719967 WHERE kode = 'adm-bisnis';
UPDATE program_studi SET cicilan_1 = 1427475 WHERE kode = 'adm-publik';

-- TEKNIK (Engineering Faculty)
UPDATE program_studi SET cicilan_1 = 2280167 WHERE kode = 'teknik-sipil';
UPDATE program_studi SET cicilan_1 = 2352167 WHERE kode = 'teknik-mesin';
UPDATE program_studi SET cicilan_1 = 1455583 WHERE kode = 'teknik-elektro';
UPDATE program_studi SET cicilan_1 = 2091167 WHERE kode = 'informatika';

-- PETERNAKAN (Animal Science)
UPDATE program_studi SET cicilan_1 = 1615125 WHERE kode = 'prod-ternak';

-- EKONOMI & BISNIS (Economics & Business)
UPDATE program_studi SET cicilan_1 = 2111167 WHERE kode = 'akuntansi';
UPDATE program_studi SET cicilan_1 = 2111167 WHERE kode = 'manajemen';
UPDATE program_studi SET cicilan_1 = 1981567 WHERE kode = 'ekonomi-pembangunan';
UPDATE program_studi SET cicilan_1 = 1896567 WHERE kode = 'adm-pajak';

-- HUKUM (Law)
UPDATE program_studi SET cicilan_1 = 1857967 WHERE kode = 'ilmu-hukum';

-- PERTANIAN (Agriculture)
UPDATE program_studi SET cicilan_1 = 1933875 WHERE kode = 'agroteknologi';
UPDATE program_studi SET cicilan_1 = 1933875 WHERE kode = 'agribisnis';

-- BAHASA & SENI (Language & Arts)
UPDATE program_studi SET cicilan_1 = 1248283 WHERE kode = 'seni-musik';
UPDATE program_studi SET cicilan_1 = 1860625 WHERE kode = 'sastra-inggris';

-- PSIKOLOGI (Psychology)
UPDATE program_studi SET cicilan_1 = 1546200 WHERE kode = 'psikologi';

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================
SELECT 'Program Studi with Cicilan-1' as Check;
SELECT id, kode, nama, harga_total_per_tahun, cicilan_1 
FROM program_studi 
WHERE is_active = TRUE 
ORDER BY sort_order;

-- Check if any program missing cicilan_1
SELECT 'Programs with Missing Cicilan-1' as Check;
SELECT id, kode, nama, harga_total_per_tahun, cicilan_1 
FROM program_studi 
WHERE is_active = TRUE AND (cicilan_1 = 0 OR cicilan_1 IS NULL)
ORDER BY sort_order;
