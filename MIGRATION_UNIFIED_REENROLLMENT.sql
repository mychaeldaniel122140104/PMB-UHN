-- ===== MIGRATION: Unified ReEnrollment Table =====
-- This migration consolidates document file paths into the reenrollments table
-- Date: April 2026

-- Step 1: Add new columns to reenrollments table for unified document storage
ALTER TABLE reenrollments 
ADD COLUMN IF NOT EXISTS parent_name VARCHAR(255) AFTER parent_phone,
ADD COLUMN IF NOT EXISTS current_address TEXT AFTER permanent_address,
ADD COLUMN IF NOT EXISTS alumni_relation VARCHAR(255) AFTER alumni_name,
ADD COLUMN IF NOT EXISTS pakta_integritas_file TEXT AFTER alumni_relation,
ADD COLUMN IF NOT EXISTS ijazah_file TEXT AFTER pakta_integritas_file,
ADD COLUMN IF NOT EXISTS pasphoto_file TEXT AFTER ijazah_file,
ADD COLUMN IF NOT EXISTS kartu_keluarga_file TEXT AFTER pasphoto_file,
ADD COLUMN IF NOT EXISTS ktp_file TEXT AFTER kartu_keluarga_file,
ADD COLUMN IF NOT EXISTS surat_bebas_narkoba_file TEXT AFTER ktp_file,
ADD COLUMN IF NOT EXISTS skck_file TEXT AFTER surat_bebas_narkoba_file;

-- Step 2: Make exam_result_id nullable for students without exams
ALTER TABLE reenrollments MODIFY COLUMN exam_result_id BIGINT NULL;

-- Step 3: Make parent_phone, parent_email, permanent_address NOT NULL (these are required)
ALTER TABLE reenrollments MODIFY COLUMN parent_phone VARCHAR(20) NOT NULL,
                          MODIFY COLUMN parent_email VARCHAR(255) NOT NULL,
                          MODIFY COLUMN permanent_address TEXT NOT NULL;

-- Step 4: Migrate data from reenrollment_documents to reenrollments table
-- This maps document types to their corresponding file columns
UPDATE reenrollments r
SET 
    pakta_integritas_file = (
        SELECT file_path FROM reenrollment_documents rd 
        WHERE rd.reenrollment_id = r.id 
        AND rd.document_type = 'PAKTA_INTEGRITAS' 
        LIMIT 1
    ),
    ijazah_file = (
        SELECT file_path FROM reenrollment_documents rd 
        WHERE rd.reenrollment_id = r.id 
        AND rd.document_type = 'IJAZAH' 
        LIMIT 1
    ),
    pasphoto_file = (
        SELECT file_path FROM reenrollment_documents rd 
        WHERE rd.reenrollment_id = r.id 
        AND rd.document_type = 'PASPHOTO' 
        LIMIT 1
    ),
    kartu_keluarga_file = (
        SELECT file_path FROM reenrollment_documents rd 
        WHERE rd.reenrollment_id = r.id 
        AND rd.document_type = 'KARTU_KELUARGA' 
        LIMIT 1
    ),
    ktp_file = (
        SELECT file_path FROM reenrollment_documents rd 
        WHERE rd.reenrollment_id = r.id 
        AND rd.document_type = 'KARTU_TANDA_PENDUDUK' 
        LIMIT 1
    ),
    surat_bebas_narkoba_file = (
        SELECT file_path FROM reenrollment_documents rd 
        WHERE rd.reenrollment_id = r.id 
        AND rd.document_type = 'KETERANGAN_BEBAS_NARKOBA' 
        LIMIT 1
    ),
    skck_file = (
        SELECT file_path FROM reenrollment_documents rd 
        WHERE rd.reenrollment_id = r.id 
        AND rd.document_type = 'SKCK' 
        LIMIT 1
    )
WHERE EXISTS (
    SELECT 1 FROM reenrollment_documents rd 
    WHERE rd.reenrollment_id = r.id
);

-- Step 5: Verify migration (check if documents were migrated)
-- SELECT COUNT(*) as documents_with_files FROM reenrollments 
-- WHERE pakta_integritas_file IS NOT NULL 
--    OR ijazah_file IS NOT NULL 
--    OR pasphoto_file IS NOT NULL 
--    OR kartu_keluarga_file IS NOT NULL 
--    OR ktp_file IS NOT NULL 
--    OR surat_bebas_narkoba_file IS NOT NULL 
--    OR skck_file IS NOT NULL;

-- Step 6: Optional - Keep reenrollment_documents table for historical purposes
-- Or uncomment to delete: DROP TABLE reenrollment_documents;

COMMIT;
