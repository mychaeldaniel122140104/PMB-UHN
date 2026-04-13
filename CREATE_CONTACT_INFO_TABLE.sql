-- Tabel Contact Info (Hubungi Kami)
CREATE TABLE IF NOT EXISTS contact_info (
    id INT PRIMARY KEY AUTO_INCREMENT,
    address VARCHAR(500) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    operating_hours VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default data
INSERT INTO contact_info (address, phone, email, operating_hours) 
VALUES ('Jl. Kimia No. 1 Paya Pasir, Medan, Sumatera Utara 20123', '(061) 4-15-555', 'pmb@uhn.ac.id', 'Senin-Jumat 08:00-16:30, Sabtu 09:00-12:00')
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;
