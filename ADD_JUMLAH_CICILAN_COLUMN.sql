-- Add jumlah_cicilan column to HASIL_AKHIR table
-- This stores the number of installments from CICILAN_REQUEST

ALTER TABLE hasil_akhir 
ADD COLUMN jumlah_cicilan INTEGER DEFAULT 1;

-- Update existing records with default value if needed
UPDATE hasil_akhir 
SET jumlah_cicilan = 1 
WHERE jumlah_cicilan IS NULL;

-- Add comment
COMMENT ON COLUMN hasil_akhir.jumlah_cicilan IS 'Jumlah cicilan (number of installments) from CICILAN_REQUEST';
