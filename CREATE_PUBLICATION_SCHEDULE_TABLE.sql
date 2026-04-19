-- =====================================================
-- CREATE PUBLICATION SCHEDULE TABLE
-- For F009: Penjadwalan Publikasi Hasil Kelulusan
-- For F013: Tampilan Hasil Kelulusan Terjadwal
-- =====================================================

CREATE TABLE IF NOT EXISTS publication_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period_id BIGINT NOT NULL,
    publish_date_time DATETIME NOT NULL,
    is_published BOOLEAN DEFAULT FALSE,
    published_at DATETIME NULL,
    created_by VARCHAR(100),
    notes TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pub_schedule_period FOREIGN KEY (period_id) REFERENCES registration_periods(id),
    CONSTRAINT uk_pub_schedule_period UNIQUE (period_id)
);

-- =====================================================
-- VERIFICATION
-- =====================================================
SELECT 'publication_schedule table created' AS status;
DESC publication_schedule;
