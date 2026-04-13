-- ==========================================
-- SELECTION TYPE SETUP
-- ==========================================
-- This file must be run in H2 Console at http://localhost:8080/h2-console

-- Important: These are linked to PERIODS (Gelombang)
-- SelectionType represents different selection methods/formulas for each period

-- 1. DELETE old selection type entries (cleanup)
DELETE FROM selection_types;

-- ==========================================
-- GELOMBANG 1 (ID=1): Gelombang Awal (EARLY_NO_TEST)
-- Require testing: FALSE (tidak ada ujian)
-- ==========================================

-- Gelombang 1 - Selection Type 1: Non-Kedokteran (Bebas Testing)
INSERT INTO selection_types (
    period_id, name, description, 
    require_testing, require_ranking, 
    form_type, price, is_active, 
    created_at, updated_at
) VALUES (
    1,
    'Program Non-Kedokteran (Bebas Testing)',
    'Seleksi tanpa ujian tulis untuk program non-kedokteran. Berdasarkan dokumen akademik dan prestasi.',
    false,      -- require_testing = false (EARLY_NO_TEST)
    false,      -- require_ranking = false
    'NON_MEDICAL',
    750000,
    true,
    NOW(),
    NOW()
);

-- Gelombang 1 - Selection Type 2: Kedokteran (Bebas Testing dengan ranking)
INSERT INTO selection_types (
    period_id, name, description, 
    require_testing, require_ranking, 
    form_type, price, is_active, 
    created_at, updated_at
) VALUES (
    1,
    'Program Kedokteran (Bebas Testing)',
    'Seleksi tanpa ujian tulis untuk program kedokteran. Berdasarkan ranking dan prestasi akademik.',
    false,      -- require_testing = false (EARLY_NO_TEST)
    true,       -- require_ranking = true (value mesti top 10%)
    'MEDICAL',
    2500000,
    true,
    NOW(),
    NOW()
);

-- ==========================================
-- GELOMBANG 2 (ID=2): Gelombang Reguler (REGULAR_TEST)
-- Require testing: TRUE (ada ujian)
-- ==========================================

-- Gelombang 2 - Selection Type 3: Non-Kedokteran dengan Testing
INSERT INTO selection_types (
    period_id, name, description, 
    require_testing, require_ranking, 
    form_type, price, is_active, 
    created_at, updated_at
) VALUES (
    2,
    'Program Non-Kedokteran (Dengan Ujian)',
    'Seleksi dengan ujian tulis untuk program non-kedokteran.',
    true,       -- require_testing = true (REGULAR_TEST)
    false,      -- require_ranking = false
    'NON_MEDICAL',
    750000,
    true,
    NOW(),
    NOW()
);

-- ==========================================
-- GELOMBANG 3 (ID=3): Gelombang Ranking (RANKING_NO_TEST)
-- Require testing: FALSE
-- ==========================================

-- Gelombang 3 - Selection Type 4: Kedokteran Ranking
INSERT INTO selection_types (
    period_id, name, description, 
    require_testing, require_ranking, 
    form_type, price, is_active, 
    created_at, updated_at
) VALUES (
    3,
    'Program Kedokteran (Ranking)',
    'Seleksi berdasarkan ranking untuk program kedokteran.',
    false,      -- require_testing = false
    true,       -- require_ranking = true
    'MEDICAL',
    2500000,
    true,
    NOW(),
    NOW()
);

-- ==========================================
-- VERIFY (optional - check if inserted correctly)
-- ==========================================
-- SELECT id, period_id, name, form_type, require_testing, require_ranking, is_active 
-- FROM selection_types 
-- ORDER BY period_id, id;
