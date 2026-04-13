-- =====================================================
-- Migration: Add HARGA_TOTAL_PER_TAHUN to PROGRAM_STUDI
-- Created: 2026-04-10
-- Purpose: Add total program fee per year field
-- =====================================================

-- Add new column to PROGRAM_STUDI table
ALTER TABLE program_studi 
ADD COLUMN harga_total_per_tahun BIGINT DEFAULT 0;

-- Update harga for each program studi based on category
-- Medical Programs (Pendidikan & Profesi Dokter)
UPDATE program_studi SET harga_total_per_tahun = 150000000 WHERE kode = 'pendidikan-dokter';
UPDATE program_studi SET harga_total_per_tahun = 100000000 WHERE kode = 'profesi-dokter';

-- Engineering Programs (Teknik)
UPDATE program_studi SET harga_total_per_tahun = 80000000 WHERE kode IN ('teknik-sipil', 'teknik-mesin', 'teknik-elektro', 'informatika');

-- Education Programs (Pend.)
UPDATE program_studi SET harga_total_per_tahun = 50000000 
WHERE kode IN ('pend-biologi-inggris', 'pend-ekonomi', 'pend-agama-kristen', 
               'pend-pancasila-kewarganegaraan', 'pend-bahasa-sastra-indonesia', 
               'pend-fisika', 'pend-ipa', 'pend-profesi-guru');

-- Economics Programs
UPDATE program_studi SET harga_total_per_tahun = 60000000 
WHERE kode IN ('ekonomi-pembangunan', 'manajemen', 'akuntansi');

-- Diploma Programs (D3)
UPDATE program_studi SET harga_total_per_tahun = 40000000 WHERE kode = 'adm-perpajakan-d3';

-- =====================================================
-- Verification Query
-- =====================================================
-- SELECT id, kode, nama, harga_total_per_tahun FROM program_studi ORDER BY id;
