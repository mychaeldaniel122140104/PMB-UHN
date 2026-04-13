-- ============================================
-- FIX WAVE TYPE - Set NULL values to REGULAR_TEST
-- ============================================

-- 1. Update NULL wave_type to REGULAR_TEST for all existing records
UPDATE registration_periods 
SET wave_type = 'REGULAR_TEST' 
WHERE wave_type IS NULL;

-- 2. Verify the fix
SELECT id, name, wave_type, status 
FROM registration_periods 
ORDER BY id;

-- ============================================
-- Result: All gelombang should now have a valid waveType
-- ============================================
