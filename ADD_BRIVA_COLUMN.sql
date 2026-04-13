-- ===== ADD BRIVA COLUMN TO CICILAN_REQUEST TABLE =====
-- Migration: Add BRIVA (BRI Virtual Account) column to cicilan_request table

-- Add BRIVA column if it doesn't exist
ALTER TABLE cicilan_request 
ADD COLUMN briva VARCHAR(50) NULL AFTER approved_by;

-- Update VIEW to include BRIVA column
CREATE OR REPLACE VIEW cicilan_request_details AS
SELECT
    cr.id,
    cr.student_id,
    s.full_name AS student_name,
    s.email AS student_email,
    cr.program_studi_id,
    ps.nama_program AS program_studi_name,
    cr.admission_form_id,
    cr.jumlah_cicilan,
    cr.harga_cicilan_1,
    cr.harga_total,
    cr.harga_per_cicilan,
    cr.status,
    cr.catatan,
    cr.approved_by,
    cr.briva,
    cr.approved_at,
    cr.created_at,
    cr.updated_at
FROM cicilan_request cr
LEFT JOIN student s ON cr.student_id = s.id
LEFT JOIN program_studi ps ON cr.program_studi_id = ps.id;
