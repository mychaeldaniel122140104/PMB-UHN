-- ===== UPDATE HASIL_AKHIR TABLE - ADD WAVE AND SELECTION INFO =====

-- Add new columns to HASIL_AKHIR table
ALTER TABLE hasil_akhir 
ADD COLUMN wave_type VARCHAR(50) COMMENT 'Tipe gelombang (REGULAR_TEST, EARLY_NO_TEST, etc)';

ALTER TABLE hasil_akhir 
ADD COLUMN selection_type VARCHAR(100) COMMENT 'Tipe seleksi (KEDOKTERAN, NON_KEDOKTERAN, etc)';

ALTER TABLE hasil_akhir 
ADD COLUMN program_studi_name VARCHAR(255) COMMENT 'Program studi yang dipilih/diterima';

-- Optional: Add selection period info
ALTER TABLE hasil_akhir 
ADD COLUMN selection_period_id BIGINT COMMENT 'Reference to RegistrationPeriod';

-- Add index untuk queries lebih cepat
ALTER TABLE hasil_akhir ADD INDEX idx_wave_type (wave_type);
ALTER TABLE hasil_akhir ADD INDEX idx_selection_type (selection_type);
ALTER TABLE hasil_akhir ADD INDEX idx_program_studi (program_studi_name);

-- Show updated schema
DESCRIBE hasil_akhir;
