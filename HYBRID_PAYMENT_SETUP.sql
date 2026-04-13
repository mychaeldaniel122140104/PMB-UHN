-- =========================================================
-- HYBRID PAYMENT SYSTEM DATABASE SETUP
-- =========================================================
-- Menambahkan sistem pembayaran hybrid: SIMULATION (VA) + MANUAL (Transfer + Upload Bukti)
-- =========================================================

-- 1. Tambah kolom payment_method ke table admission_form
ALTER TABLE admission_form 
ADD COLUMN IF NOT EXISTS payment_method ENUM('SIMULATION', 'MANUAL') DEFAULT 'SIMULATION' COMMENT 'Mode pembayaran: SIMULATION (VA simulasi) atau MANUAL (transfer + upload bukti)',
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 2. Create table untuk manual payment (bukti transfer + upload)
CREATE TABLE IF NOT EXISTS manual_payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admission_form_id BIGINT NOT NULL,
    payment_method ENUM('MANUAL') DEFAULT 'MANUAL' COMMENT 'Jenis pembayaran manual',
    amount BIGINT NOT NULL COMMENT 'Jumlah pembayaran',
    proof_image_path VARCHAR(500) COMMENT 'Path ke file bukti transfer',
    bank_name VARCHAR(100) COMMENT 'Nama bank pengirim',
    account_holder VARCHAR(200) COMMENT 'Nama rekening pengguna',
    transaction_date DATETIME COMMENT 'Tanggal transfer actual',
    status ENUM('PENDING', 'VERIFIED', 'REJECTED') DEFAULT 'PENDING' COMMENT 'Status verifikasi: PENDING (menunggu), VERIFIED (sudah bayar), REJECTED (ditolak)',
    rejection_reason VARCHAR(500) COMMENT 'Alasan penolakan jika status REJECTED',
    verified_by VARCHAR(200) COMMENT 'Email admin yang verifikasi',
    verified_at DATETIME COMMENT 'Waktu verifikasi',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_admission_form (admission_form_id),
    KEY idx_status (status),
    FOREIGN KEY (admission_form_id) REFERENCES admission_form(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabel untuk menyimpan bukti transfer manual dan status verifikasi';

-- 3. Create table untuk rekening tujuan (bank account universitas)
CREATE TABLE IF NOT EXISTS university_bank_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bank_name VARCHAR(100) NOT NULL COMMENT 'Nama bank',
    account_number VARCHAR(50) NOT NULL COMMENT 'Nomor rekening',
    account_holder VARCHAR(200) NOT NULL COMMENT 'Nama pemilik rekening',
    purpose VARCHAR(200) NOT NULL COMMENT 'Tujuan rekening (cth: Pendaftaran, Cicilan)',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabel untuk menyimpan rekening tujuan pembayaran universitas';

-- 4. Insert sample bank account untuk demo
INSERT INTO university_bank_account (bank_name, account_number, account_holder, purpose, is_active) VALUES
('BCA', '1234567890', 'HKBP Nommensen University', 'Pendaftaran PMB', TRUE),
('Mandiri', '0987654321', 'HKBP Nommensen University', 'Pendaftaran PMB', TRUE)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 5. Update payment_status_tracker untuk support manual payment
ALTER TABLE payment_status_tracker
ADD COLUMN IF NOT EXISTS payment_method ENUM('SIMULATION', 'MANUAL') DEFAULT 'SIMULATION',
ADD COLUMN IF NOT EXISTS manual_payment_id BIGINT,
ADD KEY idx_payment_method (payment_method);

-- Verification queries
SELECT 'Column payment_method di admission_form:' AS 'Query';
DESCRIBE admission_form;

SELECT '\nTable manual_payment:' AS 'Query';
DESCRIBE manual_payment;

SELECT '\nTable university_bank_account:' AS 'Query';
DESCRIBE university_bank_account;

SELECT '\nUniversity Bank Accounts:' AS 'Query';
SELECT * FROM university_bank_account;
