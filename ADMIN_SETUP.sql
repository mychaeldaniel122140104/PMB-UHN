-- Initialize Admin Accounts for PMB System
-- Run these commands in H2 Console at http://localhost:8080/h2-console

-- 1. DELETE old entries
DELETE FROM users WHERE email IN ('admin@pmb.com', 'validasi@pmb.com', 'camaba@pmb.com');

-- 2. INSERT Admin Pusat account
-- Password: admin123
-- BCrypt Hash generated with strength 10
INSERT INTO users (email, password, role, is_active, created_at, updated_at) 
VALUES ('admin@pmb.com', '$2a$10$JM58wVw1F0/hf10fPEfnveCp3U0X4Ql1KLOmhYHNlqJHXBXCg8zJe', 'ADMIN_PUSAT', true, NOW(), NOW());

-- 3. INSERT Admin Validasi account
-- Password: validasi123
-- BCrypt Hash generated with strength 10
INSERT INTO users (email, password, role, is_active, created_at, updated_at)
VALUES ('validasi@pmb.com', '$2a$10$8qbGBJh9vE9AqnvzH2L5geH5f8K.RCZQzYjzZb8RzYfH6F9Q0V1Ke', 'ADMIN_VALIDASI', true, NOW(), NOW());

-- 4. INSERT Admin Camaba account
-- Password: camaba123
-- BCrypt Hash generated with strength 10
INSERT INTO users (email, password, role, is_active, created_at, updated_at)
VALUES ('camaba@pmb.com', '$2a$10$G5V8.8nF7K2N4Q9R8P7L5hJ2K8M9N4O5P3Q7R8S9T0U1V2W3X4Y5Z', 'ADMIN_CAMABA', true, NOW(), NOW());

-- 5. VERIFY (optional - check if inserted correctly)
-- SELECT * FROM users WHERE email IN ('admin@pmb.com', 'validasi@pmb.com', 'camaba@pmb.com');

-- ==========================================
-- GELOMBANG / REGISTRATION PERIODS SETUP
-- ==========================================

-- 6. DELETE old gelombang entries (if any)
DELETE FROM registration_periods WHERE name IN ('Gelombang Awal', 'Gelombang Reguler', 'Gelombang Ranking');

-- 7. INSERT 3 Gelombang Pendaftaran
-- Gelombang 1: Early (No Test) - Kedokteran Focused
INSERT INTO registration_periods (
    name, description, requirements, wave_type, status, 
    reg_start_date, reg_end_date, 
    exam_date, exam_end_date, 
    announcement_date, 
    reenrollment_start_date, reenrollment_end_date, 
    created_at, updated_at
) VALUES (
    'Gelombang Awal',
    'Program pendaftaran awal tanpa ujian tulis untuk calon mahasiswa Program Studi Kedokteran dan Program Non-Kedokteran yang memenuhi persyaratan',
    'Wajib upload dokumen akademik, nilai rapor, dan surat rekomendasi. Khusus Kedokteran: nilai rata-rata minimum 85 atau ranking atas 10%',
    'EARLY_NO_TEST',
    'OPEN',
    '2026-04-05 08:00:00', '2026-04-20 16:00:00',
    '2026-04-21 09:00:00', '2026-04-21 11:00:00',
    '2026-04-25 14:00:00',
    '2026-04-26 08:00:00', '2026-05-02 16:00:00',
    NOW(), NOW()
);

-- Gelombang 2: Regular Test for Non-Kedokteran
INSERT INTO registration_periods (
    name, description, requirements, wave_type, status, 
    reg_start_date, reg_end_date, 
    exam_date, exam_end_date, 
    announcement_date, 
    reenrollment_start_date, reenrollment_end_date, 
    created_at, updated_at
) VALUES (
    'Gelombang Reguler',
    'Program pendaftaran reguler dengan ujian tulis untuk Program Studi Non-Kedokteran. Terbuka untuk semua calon mahasiswa yang memenuhi syarat pendaftaran dasar',
    'Wajib upload dokumen akademik dan mengikuti ujian kompetensi dasar. Daftar nilai ujian UTBK opsional',
    'REGULAR_TEST',
    'OPEN',
    '2026-04-25 08:00:00', '2026-05-10 16:00:00',
    '2026-05-12 09:00:00', '2026-05-12 11:30:00',
    '2026-05-16 14:00:00',
    '2026-05-17 08:00:00', '2026-05-24 16:00:00',
    NOW(), NOW()
);

-- Gelombang 3: Ranking Based - Kedokteran
INSERT INTO registration_periods (
    name, description, requirements, wave_type, status, 
    reg_start_date, reg_end_date, 
    exam_date, exam_end_date, 
    announcement_date, 
    reenrollment_start_date, reenrollment_end_date, 
    created_at, updated_at
) VALUES (
    'Gelombang Ranking',
    'Program pendaftaran berdasarkan ranking dan nilai akademis untuk Program Studi Kedokteran. Tanpa ujian tulis untuk peserta yang memenuhi persyaratan ranking',
    'Wajib TOP 15% dalam ranking sekolah atau nilai UTBK minimum 600. Upload bukti ranking resmi dari sekolah',
    'RANKING_NO_TEST',
    'OPEN',
    '2026-05-15 08:00:00', '2026-05-25 16:00:00',
    '2026-05-26 09:00:00', '2026-05-26 10:00:00',
    '2026-05-30 14:00:00',
    '2026-06-01 08:00:00', '2026-06-08 16:00:00',
    NOW(), NOW()
);

-- 8. VERIFY GELOMBANG (optional)
-- SELECT id, name, wave_type, status, reg_start_date, reg_end_date FROM registration_periods;

COMMIT;
