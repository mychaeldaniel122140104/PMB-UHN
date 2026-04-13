-- Create table untuk menyimpan foto bukti ujian offline
-- Tabel ini digunakan untuk menyimpan foto hasil ujian offline yang diunggah oleh calon mahasiswa
CREATE TABLE FOTO (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    exam_token VARCHAR(50),
    photo_path VARCHAR(500),
    photo_data LONGBLOB,
    file_name VARCHAR(255),
    file_size BIGINT,
    photo_type VARCHAR(50) DEFAULT 'image/jpeg' COMMENT 'MIME type: image/jpeg, image/png, etc',
    status VARCHAR(50) DEFAULT 'UPLOADED' COMMENT 'UPLOADED, VERIFIED, REJECTED',
    verified_by INT COMMENT 'User ID of admin who verified',
    verified_at TIMESTAMP NULL,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES USER(id) ON DELETE CASCADE,
    FOREIGN KEY (verified_by) REFERENCES USER(id) ON DELETE SET NULL,
    
    UNIQUE KEY uk_user_token (user_id, exam_token),
    INDEX idx_user_id (user_id),
    INDEX idx_exam_token (exam_token),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- Create table untuk riwayat pencarian dan verifikasi foto
CREATE TABLE FOTO_VERIFICATION_LOG (
    id INT PRIMARY KEY AUTO_INCREMENT,
    foto_id INT NOT NULL,
    verified_by INT,
    action VARCHAR(50) COMMENT 'VERIFY, REJECT, UPDATE',
    notes VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (foto_id) REFERENCES FOTO(id) ON DELETE CASCADE,
    FOREIGN KEY (verified_by) REFERENCES USER(id) ON DELETE SET NULL,
    
    INDEX idx_foto_id (foto_id),
    INDEX idx_created_at (created_at)
);

-- Alternatif: Jika ingin menyimpan foto di file system dan hanya path di database (lebih efisien)
-- PHOTO_PATH akan berisi path seperti: /uploads/exam-photos/2024/04/user_123_exam_token_xyz.jpg
-- PHOTO_DATA tetap NULL untuk menghemat ruang database
