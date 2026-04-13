-- ===== CREATE CICILAN_REQUEST TABLE =====
CREATE TABLE IF NOT EXISTS cicilan_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Foreign Keys
    student_id BIGINT NOT NULL,
    program_studi_id BIGINT NOT NULL,
    admission_form_id BIGINT NOT NULL,
    
    -- Cicilan Info
    jumlah_cicilan INT NOT NULL CHECK (jumlah_cicilan >= 1 AND jumlah_cicilan <= 6),
    harga_cicilan_1 BIGINT NOT NULL,           -- Harga cicilan pertama (dari program_studi.cicilan_1)
    harga_total BIGINT NOT NULL,                -- Total harga (dari program_studi.harga_total_per_tahun)
    harga_per_cicilan BIGINT,                   -- Calculated: harga_total / jumlah_cicilan
    
    -- Status & Approval
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, APPROVED, REJECTED
    catatan LONGTEXT,                           -- For rejection reason or notes
    
    -- Admin Approval Info
    approved_by VARCHAR(255),                   -- Admin username
    approved_at TIMESTAMP NULL,
    briva VARCHAR(50),                          -- BRI Virtual Account number
    payment_method VARCHAR(20) DEFAULT 'SIMULATION',  -- SIMULATION or MANUAL
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes
    KEY idx_student (student_id),
    KEY idx_program_studi (program_studi_id),
    KEY idx_admission_form (admission_form_id),
    KEY idx_status (status),
    KEY idx_created_at (created_at),
    
    -- Foreign Key Constraints
    CONSTRAINT fk_cicilan_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE,
    CONSTRAINT fk_cicilan_program_studi FOREIGN KEY (program_studi_id) REFERENCES program_studi(id) ON DELETE RESTRICT,
    CONSTRAINT fk_cicilan_admission_form FOREIGN KEY (admission_form_id) REFERENCES admission_form(id) ON DELETE CASCADE
);

-- ===== CREATE INDEXES =====
CREATE INDEX idx_cicilan_student_status ON cicilan_request(student_id, status);
CREATE INDEX idx_cicilan_program_studi_status ON cicilan_request(program_studi_id, status);

-- ===== CREATE TRIGGER FOR HARGA_PER_CICILAN =====
DELIMITER //
CREATE TRIGGER cicilan_request_calc_before_insert
BEFORE INSERT ON cicilan_request
FOR EACH ROW
BEGIN
    IF NEW.jumlah_cicilan > 0 THEN
        SET NEW.harga_per_cicilan = CEIL(NEW.harga_total / NEW.jumlah_cicilan);
    ELSE
        SET NEW.harga_per_cicilan = NEW.harga_total;
    END IF;
END//

CREATE TRIGGER cicilan_request_calc_before_update
BEFORE UPDATE ON cicilan_request
FOR EACH ROW
BEGIN
    IF NEW.jumlah_cicilan > 0 THEN
        SET NEW.harga_per_cicilan = CEIL(NEW.harga_total / NEW.jumlah_cicilan);
    ELSE
        SET NEW.harga_per_cicilan = NEW.harga_total;
    END IF;
END//
DELIMITER ;

-- ===== SAMPLE DATA (OPTIONAL) =====
-- INSERT INTO cicilan_request 
-- (student_id, program_studi_id, admission_form_id, jumlah_cicilan, harga_cicilan_1, harga_total, status, created_at)
-- VALUES 
-- (1, 1, 1, 3, 1236583, 3709749, 'PENDING', NOW()),
-- (2, 2, 2, 2, 1500000, 4500000, 'PENDING', NOW());

-- ===== VIEW: CICILAN_REQUEST_DETAILS (for easier querying) =====
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
