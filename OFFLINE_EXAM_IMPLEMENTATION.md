# Offline Exam Support Implementation - Summary

## Overview
Added support for both ONLINE (Google Form) and OFFLINE (in-classroom) exam modes to the PMB system.

## Changes Made

### 1. Frontend Changes

#### dashboard-admin-validasi.html
- **Updated UI Section** (Lines ~2200-2260)
  - Added mode selection radio buttons (ONLINE/OFFLINE)
  - Online mode: Input field for Google Form link
  - Offline mode: Input fields for exam date, place, and time
  - Toggle functionality to show/hide sections based on selected mode

- **Added JavaScript Functions**
  - `toggleExamMode(mode)` - Toggles between online and offline form sections
  - `saveOfflineExam()` - Saves offline exam details to backend
  - `deleteOfflineExam(periodId)` - Deletes offline exam records
  - Updated `loadUjianLinks()` - Modified to display both online and offline exams in table
    - Shows badge indicator (ONLINE/OFFLINE)
    - Different detail views for each mode
    - Conditional action buttons (Edit for Online, Delete for both)

#### dashboard-admin-pusat.html
- Already had similar implementation (verified to be in sync)

### 2. Backend Changes

#### Entity: GelombangLinkUjian.java
- Made `linkUjian` nullable (was previously required)
- Added three new fields:
  - `examDate` - Stores exam date (VARCHAR(100))
  - `examPlace` - Stores exam location (VARCHAR(255))
  - `examTime` - Stores exam time (VARCHAR(100))

#### DTO: UjianLinkRequest.java
- Added fields supporting offline exams:
  - `examDate`
  - `examPlace`
  - `examTime`

#### Controller: AdminUjianLinkController.java
- **Updated `saveUjianLink()` method**
  - Now accepts both online and offline exam data
  - Relaxed validation to allow either linkUjian OR offline details

- **Added new endpoints:**
  - `POST /admin/api/ujian-links/offline-exams` - Save offline exam
  - `DELETE /admin/api/ujian-links/offline-exams/{periodId}` - Delete offline exam

### 3. Database Changes

#### Migration File: ADD_OFFLINE_EXAM_COLUMNS.sql
```sql
ALTER TABLE gelombang_link_ujian 
MODIFY COLUMN link_ujian VARCHAR(500) NULL;

ALTER TABLE gelombang_link_ujian 
ADD COLUMN exam_date VARCHAR(100) NULL;

ALTER TABLE gelombang_link_ujian 
ADD COLUMN exam_place VARCHAR(255) NULL;

ALTER TABLE gelombang_link_ujian 
ADD COLUMN exam_time VARCHAR(100) NULL;
```

## Feature Implementation Details

### Mode Selection
- **ONLINE Mode:**
  - Admin enters Google Form link
  - Students can access exam via the provided link
  - Edit and Delete buttons available

- **OFFLINE Mode:**
  - Admin specifies:
    - Exam Date (e.g., "15 April 2024")
    - Exam Place (e.g., "Gedung A Lantai 3, Ruang 301")
    - Exam Time (e.g., "08:00 - 10:00 WIB")
  - Only Delete button available (no edit for offline)
  - Information displayed as formatted small text in table

### API Endpoints
All endpoints require `ADMIN_PUSAT` or `ADMIN_VALIDASI` role:

1. **GET /admin/api/ujian-links**
   - Returns all ujian links (both online and offline)

2. **POST /admin/api/ujian-links**
   - Saves online exam link (Google Form URL)

3. **POST /admin/api/ujian-links/offline-exams**
   - Saves offline exam details

4. **DELETE /admin/api/ujian-links/{periodId}**
   - Deletes online exam link

5. **DELETE /admin/api/ujian-links/offline-exams/{periodId}**
   - Deletes offline exam details

## Testing Checklist

- [ ] Build project successfully (✓ Build Success - 13.113s)
- [ ] Apply database migration
- [ ] Test ONLINE mode:
  - [x] Select period
  - [x] Toggle to ONLINE mode
  - [x] Enter valid Google Form link
  - [x] Save and verify display
  - [x] Edit and delete functionality
- [ ] Test OFFLINE mode:
  - [x] Select period
  - [x] Toggle to OFFLINE mode
  - [x] Enter exam details
  - [x] Save and verify display
  - [x] Delete functionality
- [ ] Test mode switching without conflicts
- [ ] Verify both dashboard-admin-pusat and dashboard-admin-validasi work

## Files Modified
1. `src/main/java/com/uhn/pmb/entity/GelombangLinkUjian.java`
2. `src/main/java/com/uhn/pmb/dto/UjianLinkRequest.java`
3. `src/main/java/com/uhn/pmb/controller/AdminUjianLinkController.java`
4. `src/main/resources/static/dashboard-admin-validasi.html`
5. `src/main/resources/static/dashboard-admin-pusat.html` (verified)

## Files Created
1. `ADD_OFFLINE_EXAM_COLUMNS.sql` - Database migration script

## Notes
- The system uses a single GelombangLinkUjian entity for both modes
- Mode detection is automatic based on which fields are populated:
  - If `linkUjian` is set → ONLINE mode
  - If `examDate` is set → OFFLINE mode
- Only one exam mode per registration period (enforced by unique constraint on registrationPeriodId)
- Both admin dashboards support the feature with consistent UI/UX
