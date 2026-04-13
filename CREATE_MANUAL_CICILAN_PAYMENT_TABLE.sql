-- ===== CREATE MANUAL_CICILAN_PAYMENT TABLE =====
-- Table untuk tracking manual payment submissions (pembayaran manual cicilan)

CREATE TABLE IF NOT EXISTS manual_cicilan_payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Foreign Keys
    cicilan_request_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    
    -- Payment Info
    cicilan_ke INT NOT NULL,              -- Which installment (1-6)
    nominal BIGINT NOT NULL,              -- Amount to pay
    payment_proof_path TEXT,              -- Path to uploaded proof image
    
    -- Status & Verification
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, VERIFIED, REJECTED
    keterangan LONGTEXT,                  -- Rejection reason or admin notes
    
    -- Admin Verification
    verified_by VARCHAR(255),             -- Admin username
    verified_at TIMESTAMP NULL,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes
    KEY idx_cicilan_request (cicilan_request_id),
    KEY idx_student (student_id),
    KEY idx_status (status),
    KEY idx_created_at (created_at),
    KEY idx_cicilan_ke (cicilan_ke),
    UNIQUE KEY uk_cicilan_ke (cicilan_request_id, cicilan_ke),
    
    -- Foreign Key Constraints
    CONSTRAINT fk_manual_cicilan_request FOREIGN KEY (cicilan_request_id) 
        REFERENCES cicilan_request(id) ON DELETE CASCADE,
    CONSTRAINT fk_manual_student FOREIGN KEY (student_id) 
        REFERENCES student(id) ON DELETE CASCADE
);

-- ===== CREATE INDEXES =====
CREATE INDEX idx_manual_payment_student_status ON manual_cicilan_payment(student_id, status);
CREATE INDEX idx_manual_payment_status_created ON manual_cicilan_payment(status, created_at);

-- ===== SAMPLE DATA (OPTIONAL) =====
-- INSERT INTO manual_cicilan_payment 
-- (cicilan_request_id, student_id, cicilan_ke, nominal, status, created_at)
-- VALUES 
-- (1, 1, 1, 1236583, 'PENDING', NOW());
