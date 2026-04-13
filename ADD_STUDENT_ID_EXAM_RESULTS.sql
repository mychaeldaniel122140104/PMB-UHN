-- ✅ ADD STUDENT_ID COLUMN TO EXAM_RESULTS TABLE

-- Check if column already exists
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = 'exam_results' AND COLUMN_NAME = 'student_id'
)
BEGIN
    ALTER TABLE exam_results
    ADD student_id BIGINT;
    
    -- Add foreign key constraint
    ALTER TABLE exam_results
    ADD CONSTRAINT fk_exam_results_student 
    FOREIGN KEY (student_id) REFERENCES student(id);
    
    PRINT '✅ Column student_id added to exam_results table';
    PRINT '✅ Foreign key constraint added: fk_exam_results_student';
END
ELSE
BEGIN
    PRINT '⚠️ Column student_id already exists in exam_results table';
END;

-- ✅ UPDATE existing exam_results with student data from exam table
-- This finds the student who took the exam by joining through Exam table
UPDATE exam_results
SET student_id = e.student_id
FROM exam_results er
INNER JOIN exam e ON er.exam_id = e.id
WHERE er.student_id IS NULL;

PRINT '✅ Existing exam_results records updated with student_id from exam table';
