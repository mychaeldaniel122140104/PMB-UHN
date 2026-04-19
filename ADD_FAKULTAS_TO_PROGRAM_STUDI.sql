-- =====================================================
-- ADD FAKULTAS COLUMN TO PROGRAM_STUDI
-- For F031: Klasifikasi data berdasarkan fakultas
-- =====================================================

ALTER TABLE program_studi ADD COLUMN IF NOT EXISTS fakultas VARCHAR(100);

-- =====================================================
-- POPULATE FAKULTAS BASED ON KNOWN GROUPINGS
-- =====================================================

-- FKIP (Fakultas Keguruan dan Ilmu Pendidikan)
UPDATE program_studi SET fakultas = 'FKIP' WHERE kode IN (
    'pend-fisika', 'pend-bahasa-sastra-indonesia', 'pend-biologi-inggris',
    'pend-pancasila-kewarganegaraan', 'pend-ekonomi', 'pend-agama-kristen',
    'pend-ipa', 'pend-matematika'
);

-- Teknik
UPDATE program_studi SET fakultas = 'Teknik' WHERE kode IN (
    'teknik-sipil', 'teknik-mesin', 'teknik-elektro', 'informatika'
);

-- Ekonomi & Bisnis
UPDATE program_studi SET fakultas = 'Ekonomi & Bisnis' WHERE kode IN (
    'akuntansi', 'manajemen', 'ekonomi-pembangunan', 'adm-pajak', 'adm-perpajakan-d3'
);

-- FISIPOL (Fakultas Ilmu Sosial dan Ilmu Politik)
UPDATE program_studi SET fakultas = 'FISIPOL' WHERE kode IN (
    'adm-bisnis', 'adm-bisnis-fisipol', 'adm-publik'
);

-- Peternakan
UPDATE program_studi SET fakultas = 'Peternakan' WHERE kode = 'prod-ternak';

-- Hukum
UPDATE program_studi SET fakultas = 'Hukum' WHERE kode = 'ilmu-hukum';

-- Pertanian
UPDATE program_studi SET fakultas = 'Pertanian' WHERE kode IN (
    'agroteknologi', 'agribisnis'
);

-- Bahasa & Seni
UPDATE program_studi SET fakultas = 'Bahasa & Seni' WHERE kode IN (
    'seni-musik', 'sastra-inggris'
);

-- Psikologi
UPDATE program_studi SET fakultas = 'Psikologi' WHERE kode = 'psikologi';

-- Kedokteran (for medical programs, even if inactive)
UPDATE program_studi SET fakultas = 'Kedokteran' WHERE kode IN (
    'pendidikan-dokter', 'profesi-dokter'
);

-- =====================================================
-- VERIFICATION
-- =====================================================
SELECT 'Fakultas column added and populated' AS status;
SELECT fakultas, COUNT(*) as jumlah_prodi
FROM program_studi
WHERE is_active = TRUE AND fakultas IS NOT NULL
GROUP BY fakultas
ORDER BY fakultas;
