-- Migration: Add cicilan_2 through cicilan_6 columns to program_studi and cicilan_request tables
-- For flexible per-installment pricing (Feedback #4 & #5)

-- ===== PROGRAM STUDI: Add cicilan 2-6 price columns =====
ALTER TABLE program_studi ADD COLUMN IF NOT EXISTS cicilan_2 BIGINT DEFAULT 0;
ALTER TABLE program_studi ADD COLUMN IF NOT EXISTS cicilan_3 BIGINT DEFAULT 0;
ALTER TABLE program_studi ADD COLUMN IF NOT EXISTS cicilan_4 BIGINT DEFAULT 0;
ALTER TABLE program_studi ADD COLUMN IF NOT EXISTS cicilan_5 BIGINT DEFAULT 0;
ALTER TABLE program_studi ADD COLUMN IF NOT EXISTS cicilan_6 BIGINT DEFAULT 0;

-- ===== CICILAN REQUEST: Add per-installment price columns =====
ALTER TABLE cicilan_request ADD COLUMN IF NOT EXISTS harga_cicilan_2 BIGINT DEFAULT 0;
ALTER TABLE cicilan_request ADD COLUMN IF NOT EXISTS harga_cicilan_3 BIGINT DEFAULT 0;
ALTER TABLE cicilan_request ADD COLUMN IF NOT EXISTS harga_cicilan_4 BIGINT DEFAULT 0;
ALTER TABLE cicilan_request ADD COLUMN IF NOT EXISTS harga_cicilan_5 BIGINT DEFAULT 0;
ALTER TABLE cicilan_request ADD COLUMN IF NOT EXISTS harga_cicilan_6 BIGINT DEFAULT 0;

-- ===== Set sample prices for existing program studi (adjust as needed) =====
-- Example: Informatika with 6 installments
-- UPDATE program_studi SET cicilan_2 = 2000000, cicilan_3 = 2000000, cicilan_4 = 2000000, cicilan_5 = 2000000, cicilan_6 = 2000000 WHERE kode = 'TI';
