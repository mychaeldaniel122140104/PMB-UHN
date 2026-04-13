-- Create table untuk menyimpan link ujian per gelombang (1-to-1 relationship)
-- Setiap gelombang hanya punya 1 link ujian, dan setiap link ujian hanya untuk 1 gelombang
CREATE TABLE GELOMBANG_LINK_UJIAN (
    id INT PRIMARY KEY AUTO_INCREMENT,
    registration_period_id INT NOT NULL UNIQUE,
    link_ujian VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (registration_period_id) REFERENCES REGISTRATION_PERIODS(id) ON DELETE CASCADE,
    UNIQUE KEY uk_period_link (registration_period_id)
);

-- Insert contoh data (opsional, bisa dihapus)
-- INSERT INTO GELOMBANG_LINK_UJIAN (registration_period_id, link_ujian) 
-- VALUES (1, 'https://docs.google.com/forms/d/e/...');
