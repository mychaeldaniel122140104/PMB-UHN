-- ===== CREATE TABLE: Hasil Akhir (Final Results) =====
-- Stores final results shown to students after validation
-- Contains: BRIVA (Virtual Account), Registration Number
-- All fields UNIQUE to prevent duplicates
-- Date: April 2026

CREATE TABLE IF NOT EXISTS hasil_akhir (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Student reference (UNIQUE)
    student_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL UNIQUE,
    
    -- Virtual Account BRIVA (UNIQUE)
    briva_number VARCHAR(50) NOT NULL UNIQUE,
    briva_amount DECIMAL(15, 2),
    
    -- Registration Number / Nomor Registrasi (UNIQUE)
    nomor_registrasi VARCHAR(100) NOT NULL UNIQUE,
    
    -- Status and tracking
    status ENUM('PENDING', 'ACTIVE', 'EXPIRED', 'USED', 'CANCELLED') DEFAULT 'PENDING',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign keys
    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    
    -- Indexes for quick lookup
    INDEX idx_student (student_id),
    INDEX idx_briva (briva_number),
    INDEX idx_nomor_registrasi (nomor_registrasi),
    INDEX idx_status (status),
    
    -- Constraints
    CONSTRAINT uk_briva UNIQUE (briva_number),
    CONSTRAINT uk_nomor_registrasi UNIQUE (nomor_registrasi),
    CONSTRAINT uk_student_id UNIQUE (student_id)
);

-- Verify table created
-- SELECT COUNT(*) as total_hasil_akhir FROM hasil_akhir;
