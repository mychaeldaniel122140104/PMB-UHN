-- ============================================================
-- MIGRATION: Add Wave Type, Selection Type, Program Studi to HASIL_AKHIR
-- Date: 2026-04-12
-- ============================================================

-- Add new columns to hasil_akhir table
ALTER TABLE hasil_akhir 
ADD COLUMN wave_type VARCHAR(50) COMMENT 'Tipe gelombang (REGULAR_TEST, EARLY_NO_TEST, etc)' AFTER nomor_registrasi;

ALTER TABLE hasil_akhir 
ADD COLUMN selection_type VARCHAR(100) COMMENT 'Tipe seleksi (KEDOKTERAN, NON_KEDOKTERAN, etc)' AFTER wave_type;

ALTER TABLE hasil_akhir 
ADD COLUMN program_studi_name VARCHAR(255) COMMENT 'Program studi yang dipilih/diterima' AFTER selection_type;

ALTER TABLE hasil_akhir 
ADD COLUMN selection_period_id BIGINT COMMENT 'Reference to RegistrationPeriod' AFTER program_studi_name;

-- Add foreign key constraint
ALTER TABLE hasil_akhir 
ADD CONSTRAINT fk_hasil_akhir_period 
FOREIGN KEY (selection_period_id) 
REFERENCES registration_period(id) 
ON DELETE SET NULL;

-- Add indexes untuk query performance
ALTER TABLE hasil_akhir ADD INDEX idx_wave_type (wave_type);
ALTER TABLE hasil_akhir ADD INDEX idx_selection_type (selection_type);
ALTER TABLE hasil_akhir ADD INDEX idx_program_studi (program_studi_name);
ALTER TABLE hasil_akhir ADD INDEX idx_selection_period (selection_period_id);

-- Verify updated schema
DESCRIBE hasil_akhir;

-- Show sample data dengan columns baru (should be empty at first)
-- SELECT id, student_id, briva_number, nomor_registrasi, wave_type, selection_type, program_studi_name, status 
-- FROM hasil_akhir;
