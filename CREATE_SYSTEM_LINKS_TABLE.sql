-- Tabel System Links (Video Tutorial, Alur Diagram, Biaya UKT, Google Drive, dll)
CREATE TABLE IF NOT EXISTS system_links (
    id INT PRIMARY KEY AUTO_INCREMENT,
    link_name VARCHAR(100) NOT NULL UNIQUE,
    link_type VARCHAR(50) NOT NULL COMMENT 'YOUTUBE, GOOGLE_FORM, GOOGLE_DRIVE, EXTERNAL_URL',
    link_url VARCHAR(500) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_link_name (link_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default data
INSERT INTO system_links (link_name, link_type, link_url, description, is_active) VALUES
('VIDEO_TUTORIAL', 'YOUTUBE', 'https://www.youtube.com/embed/dQw4w9WgXcQ', 'Video Tutorial PMB', TRUE),
('ALUR_DIAGRAM', 'GOOGLE_DRIVE', '1xct68wCnsRST0SFy4tJX5NXNBgHOfSJ8', 'Diagram Alur Pendaftaran', TRUE),
('BIAYA_UKT', 'GOOGLE_DRIVE', '1BsgxBsQxOHmQxNKYttBmrfTC4yUlje0Y', 'Detail Biaya UKT', TRUE)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;
