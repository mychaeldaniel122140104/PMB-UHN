-- Migration: Create form_repair_status table (separate from form_validations)
-- Purpose: Track repair/revision status independently from validation status
-- Date: 2026-04-12

CREATE TABLE IF NOT EXISTS form_repair_status (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    form_validation_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'BELUM_PERBAIKAN' COMMENT 'BELUM_PERBAIKAN or SUDAH_PERBAIKAN',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_form_repair_status_validation FOREIGN KEY (form_validation_id) 
        REFERENCES form_validations(id) ON DELETE CASCADE,
    CONSTRAINT uk_form_validation_repair_status UNIQUE KEY (form_validation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Tracks form repair/revision status - separate from form_validations'
;

-- Index for faster lookups
CREATE INDEX idx_form_repair_status_validation ON form_repair_status(form_validation_id);

-- Optional: Migrate existing repair_status data from form_validations if column exists
-- This will be handled automatically by Hibernate on startup
-- The column will be dropped from form_validations table when the application starts
