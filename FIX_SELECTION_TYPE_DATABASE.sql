-- ============================================
-- FIX:  Selection Types - Verify & Correct Mapping
-- ============================================
-- Issue: Selection Type labels tidak sesuai dengan formType
-- User memilih "Program Non-Kedokteran" (ID 2) tapi sistem mengirim formType "MEDICAL"
-- Mengakibatkan program filter hanya menampilkan KEDOKTERAN programs

-- ============================================
-- STEP 1: VIEW CURRENT STATE (untuk debugging)
-- ============================================
SELECT 
    id, 
    name, 
    form_type, 
    price, 
    require_testing, 
    require_ranking,
    is_active
FROM selection_types 
ORDER BY id;

-- ============================================
-- STEP 2: FIX DATA - ENSURE CORRECT MAPPING
-- ============================================
-- Based on DataInitializer.java configuration:
-- ID 1 = Kedokteran - Bebas Testing (MEDICAL) - Price: 1500000
-- ID 2 = Kedokteran - Testing (MEDICAL) - Price: 2000000
-- ID 3 = Non-Kedokteran - Bebas Testing (NON_MEDICAL) - Price: 500000
-- ID 4 = Non-Kedokteran - Testing (NON_MEDICAL) - Price: 750000

-- Find and fix any MEDICAL selection types that have wrong labels
UPDATE selection_types 
SET form_type = 'NON_MEDICAL'
WHERE id = 2 
  AND form_type = 'MEDICAL'
  AND (name LIKE '%Non-Kedokteran%' OR name LIKE '%non%kedok%');

-- Alternative: If ID 2 is truly Non-Kedokteran Testing, update it
UPDATE selection_types 
SET 
    form_type = 'NON_MEDICAL',
    name = 'Non-Kedokteran - Testing',
    price = 750000,
    require_testing = true
WHERE id = 2;

-- ============================================
-- STEP 3: VERIFY FIX
-- ============================================
SELECT 
    id, 
    name, 
    form_type, 
    price, 
    require_testing,
    require_ranking,
    is_active
FROM selection_types 
ORDER BY id;

-- ============================================
-- STEP 4: IF STILL WRONG, CHECK ALL PERIODS
-- ============================================
-- Sometimes each period has separate selection types
SELECT 
    st.id,
    st.period_id,
    p.name AS period_name,
    st.name,
    st.form_type,
    st.price,
    st.require_testing
FROM selection_types st
JOIN registration_periods p ON st.period_id = p.id
ORDER BY st.period_id, st.id;

COMMIT;

