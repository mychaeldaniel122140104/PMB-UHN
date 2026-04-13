-- Add offline exam columns to gelombang_link_ujian table
-- Migration for supporting both Online (Google Form) and Offline exams

ALTER TABLE gelombang_link_ujian 
MODIFY COLUMN link_ujian VARCHAR(500) NULL;

ALTER TABLE gelombang_link_ujian 
ADD COLUMN exam_date VARCHAR(100) NULL COMMENT 'Exam date (e.g., 15 April 2024)' AFTER link_ujian;

ALTER TABLE gelombang_link_ujian 
ADD COLUMN exam_place VARCHAR(255) NULL COMMENT 'Exam location (e.g., Gedung A Lantai 3, Ruang 301)' AFTER exam_date;

ALTER TABLE gelombang_link_ujian 
ADD COLUMN exam_time VARCHAR(100) NULL COMMENT 'Exam time (e.g., 08:00 - 10:00 WIB)' AFTER exam_place;

-- Verify the table structure
DESC gelombang_link_ujian;
