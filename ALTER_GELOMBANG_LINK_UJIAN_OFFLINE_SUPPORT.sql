-- Alter GELOMBANG_LINK_UJIAN table untuk mendukung ujian offline
-- Tambah kolom untuk menyimpan detail ujian offline (tanggal, tempat, jam)

ALTER TABLE GELOMBANG_LINK_UJIAN ADD COLUMN (
    exam_mode VARCHAR(50) DEFAULT 'ONLINE' COMMENT 'ONLINE atau OFFLINE',
    exam_date VARCHAR(100) COMMENT 'Tanggal ujian offline (e.g., 15 April 2024)',
    exam_place VARCHAR(500) COMMENT 'Lokasi/gedung ujian offline (e.g., Gedung A Lantai 3, Ruang 301)',
    exam_time VARCHAR(100) COMMENT 'Jam ujian offline (e.g., 08:00 - 10:00 WIB)'
) IF NOT EXISTS;

-- Update index jika diperlukan
ALTER TABLE GELOMBANG_LINK_UJIAN ADD INDEX idx_exam_mode (exam_mode);

-- Menambahkan constraint untuk memastikan link_ujian wajib ada jika mode ONLINE
-- (Note: Constraint check ini mungkin tidak semua database support, bisa di-handle di aplikasi)
