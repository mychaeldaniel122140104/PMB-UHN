# Additional Improvements & Debugging Guide

## Recommended Future Improvements

### 1. Add Payment Status Tracking Route
Currently payment status is tracked in registration_status table. Consider creating dedicated endpoint:
```java
@GetMapping("/payment-status")
public ResponseEntity<?> getPaymentStatus() {
    // Returns: { stage: PAYMENT_BRIVA, status: SELESAI|MENUNGGU_VERIFIKASI }
}
```

### 2. Add Form Edit History
Track edit attempts and changes for audit trail:
```sql
ALTER TABLE registration_status ADD edit_history LONGTEXT;
-- Store JSON log of edits with timestamp and field changes
```

### 3. Implement Audit Logging
Log all status changes for admin review:
```java
@Service
public class AuditLogService {
    public void logStatusChange(User user, RegistrationStage stage, 
                                RegistrationStatus_Enum oldStatus, 
                                RegistrationStatus_Enum newStatus) {
        // Log change with timestamp and user info
    }
}
```

### 4. Add Email Notifications
Send email when status changes:
```java
// When form verified
emailService.send(user, "Formulir Anda Diverifikasi", "...");
// When edit deadline approaching
emailService.send(user, "Edit Window Closing Soon", "...");
```

### 5. Dashboard Analytics
Add statistics for admin dashboard:
- Total forms submitted today
- Pending verifications
- Payment completion rate
- Edit attempts per form

---

## Debugging Guide

### Issue: Email still showing "-"
**Debugging steps:**

1. Check API Response:
```bash
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/camaba/profile
# Look for: "user": { "email": "..." }
```

2. Check Frontend Console:
```javascript
// In browser console:
fetch('/api/camaba/profile', {
    headers: { 'Authorization': 'Bearer ' + localStorage.getItem('authToken') }
})
.then(r => r.json())
.then(data => {
    console.log('Full response:', data);
    console.log('Email path student.user?.email:', data.user?.email);
});
```

3. Check Database:
```sql
SELECT u.email, s.id FROM users u 
JOIN student s ON s.user_id = u.id
WHERE u.email = 'test@example.com';
```

### Issue: Buttons not showing
**Debugging steps:**

1. Check RegistrationStatus created:
```sql
SELECT * FROM registration_status 
WHERE user_id = <id> AND stage = 'FORM_SUBMISSION';
```

2. Check API returns correct data:
```javascript
// In browser console:
fetch('/api/camaba/registration-status/all', {
    headers: { 'Authorization': 'Bearer ' + localStorage.getItem('authToken') }
})
.then(r => r.json())
.then(data => {
    console.log('Status response:', data);
    // Check for FORM_SUBMISSION and PAYMENT_BRIVA stages
});
```

3. Check Frontend Logic:
```javascript
// In dashboard-camaba.html, add debug logs:
console.log('statusMap:', statusMap);
console.log('Form submission status:', statusMap.FORM_SUBMISSION);
console.log('Payment status:', statusMap.PAYMENT_BRIVA);
console.log('Show buttons?', 
    statusMap.FORM_SUBMISSION?.status === 'SELESAI' && 
    statusMap.PAYMENT_BRIVA?.status === 'SELESAI');
```

### Issue: Status stuck at "Menunggu"
**Debugging steps:**

1. Check if RegistrationStatus exists:
```sql
SELECT stage, status, created_at FROM registration_status 
WHERE user_id = <user_id>
ORDER BY created_at DESC;
```

2. If missing, check admission_form:
```sql
SELECT id, status, submitted_at FROM admission_forms 
WHERE student_id = <student_id>;
```

3. Check server logs for error on form submission:
```
[Looking for error stack trace in application logs]
- Is RegistrationStatusService being injected?
- Is enum value FORM_SUBMISSION correct?
- Is timestamp calculation working?
```

4. Manual create registration_status:
```sql
INSERT INTO registration_status 
(user_id, stage, status, submission_date, edit_deadline, can_edit, created_at, updated_at)
VALUES 
(<user_id>, 'FORM_SUBMISSION', 'SELESAI', NOW(), DATE_ADD(NOW(), INTERVAL 24 HOUR), true, NOW(), NOW());
```

---

## Common Issues & Solutions

### Database Inconsistency
**Problem:** Form exists but no RegistrationStatus
**Solution:** Implement synchronization:
```java
@Scheduled(fixedDelay = 3600000) // Run hourly
public void syncRegistrationStatuses() {
    List<AdmissionForm> forms = admissionFormRepository.findAll();
    for (AdmissionForm form : forms) {
        RegistrationStatus status = registrationStatusRepository
            .findByUserAndStage(form.getStudent().getUser(), 
                               RegistrationStatus.RegistrationStage.FORM_SUBMISSION)
            .orElse(null);
        
        if (status == null && form.getSubmittedAt() != null) {
            // Create missing status
            registrationStatusService.markAsCompleted(
                form.getStudent().getUser(),
                RegistrationStatus.RegistrationStage.FORM_SUBMISSION,
                "Auto-synced from form"
            );
        }
    }
}
```

### Edit Deadline Calculation
**Problem:** Deadline not correctly calculated
**Solution:** Verify in entity:
```java
@PrePersist
protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (editDeadline == null && submissionDate != null) {
        editDeadline = submissionDate.plusHours(24);
    }
}
```

### Payment Status Not Syncing
**Problem:** Payment shows SELESAI but edit buttons hidden
**Solution:** Check Payment stage enum:
```java
// Verify this exists in RegistrationStatus.RegistrationStage:
PAYMENT_BRIVA  // Must be exact name

// Check API response has correct stage name
.then(data => {
    const paymentStage = data.data.find(s => s.stage === 'PAYMENT_BRIVA');
    if (!paymentStage) console.error('PAYMENT_BRIVA stage not found!');
});
```

---

## Performance Optimization

### Query Optimization
```java
// Instead of:
List<RegistrationStatus> statuses = service.getUserStatuses(user);

// Use optimized query:
@Query("SELECT rs FROM RegistrationStatus rs WHERE rs.user = ?1 AND rs.stage IN (?2)")
List<RegistrationStatus> findByUserAndStages(User user, List<RegistrationStage> stages);
```

### Caching
```java
@Service
@CacheConfig(cacheNames = "registrationStatus")
public class RegistrationStatusService {
    @Cacheable(key = "#user.id")
    public List<RegistrationStatus> getUserStatuses(User user) {
        // Will be cached for 5 minutes
    }
}
```

---

## Security Considerations

### Validate Edit Window
```java
public void validateAndUpdate(User user, RegistrationStage stage) {
    RegistrationStatus status = getOrThrow(user, stage);
    
    if (!status.getCanEdit()) {
        throw new SecurityException("Not allowed to edit");
    }
    
    if (LocalDateTime.now().isAfter(status.getEditDeadline())) {
        throw new SecurityException("Edit deadline passed");
    }
    
    // Proceed with update
}
```

### Prevent Double Submission
```java
@PostMapping("/submit-form")
public ResponseEntity<?> submitForm(@Valid @RequestBody AdmissionFormSubmitRequest req) {
    // Check if already submitted
    if (existingForm != null && existingForm.getSubmittedAt() != null) {
        if (!canUserEdit(...)) {
            return ResponseEntity.forbidden().build();
        }
    }
    // Process submission
}
```

---

## Monitoring & Alerts

### Key Metrics to Monitor
1. **Form Submission Rate**
   ```sql
   SELECT COUNT(*) as submitted_today FROM admission_forms 
   WHERE DATE(submitted_at) = CURDATE();
   ```

2. **Edit Window Usage**
   ```sql
   SELECT COUNT(*) as edited_within_window FROM registration_status 
   WHERE stage = 'FORM_SUBMISSION' 
   AND edit_count > 1 
   AND edited_at > DATE_SUB(NOW(), INTERVAL 24 HOUR);
   ```

3. **Payment Completion Rate**
   ```sql
   SELECT 
       COUNT(DISTINCT CASE WHEN stage = 'FORM_SUBMISSION' THEN user_id END) as total_submitted,
       COUNT(DISTINCT CASE WHEN stage = 'PAYMENT_BRIVA' AND status = 'SELESAI' THEN user_id END) as paid
   FROM registration_status;
   ```

---

## Testing Scenarios

### Test Case 1: Complete Flow
```
1. User submits form
   → Check: RegistrationStatus created with FORM_SUBMISSION/SELESAI
   
2. Dashboard reloads
   → Check: Email displays
   → Check: Buttons hidden (payment pending)
   
3. Admin verifies form
   → Check: Status updates to VERIFIED
   
4. Payment completed
   → Check: Payment stage shows SELESAI
   → Check: Edit/View buttons appear
   
5. After 24 hours
   → Check: Edit deadline passed
   → Check: Buttons disabled
   
6. Click exam button
   → Check: Redirects to exam page
```

### Test Case 2: Edit Window
```
1. Form submitted at 10:00 AM
   → Deadline = 10:00 AM + 24 hours = next day 10:00 AM
   
2. Edit at 11:00 AM (within 24h)
   → Should succeed
   
3. Edit next day at 11:00 AM (past deadline)
   → Should fail with "Edit deadline passed"
```

### Test Case 3: Admin Rejection
```
1. Form submitted and verified by admin with REJECTED status
   
2. Dashboard reloads
   → Check: Status shows "Ditolak"
   → Check: Buttons hidden
   → Check: Can mention why in admin notes
```

---

## Useful SQL Queries for Inspection

### Check User Registration State
```sql
SELECT 
    u.email,
    COUNT(rs.id) as stage_count,
    GROUP_CONCAT(CONCAT(rs.stage, ':', rs.status)) as stages,
    MAX(rs.updated_at) as last_update
FROM users u
LEFT JOIN registration_status rs ON rs.user_id = u.id
GROUP BY u.id;
```

### Find Forms Without RegistrationStatus
```sql
SELECT 
    s.id as student_id,
    u.email,
    af.id as form_id,
    af.submitted_at,
    CASE WHEN rs.id IS NULL THEN 'MISSING' ELSE 'OK' END as status
FROM admission_forms af
JOIN student s ON af.student_id = s.id
JOIN users u ON s.user_id = u.id
LEFT JOIN registration_status rs ON rs.user_id = u.id 
    AND rs.stage = 'FORM_SUBMISSION'
WHERE af.submitted_at IS NOT NULL;
```

### Check Edit Window Status
```sql
SELECT 
    u.email,
    rs.stage,
    rs.status,
    rs.submission_date,
    rs.edit_deadline,
    CASE WHEN NOW() < rs.edit_deadline THEN 
        CONCAT(FLOOR(HOUR(TIMEDIFF(rs.edit_deadline, NOW()))), 'h left')
    ELSE 'EXPIRED' END as edit_window
FROM registration_status rs
JOIN users u ON rs.user_id = u.id
WHERE rs.stage = 'FORM_SUBMISSION'
ORDER BY rs.edit_deadline ASC;
```

---

## Rollback Plan (if needed)

If something goes wrong:

1. **Revert Backend Changes:**
   ```bash
   git checkout HEAD~1 src/main/java/com/uhn/pmb/controller/CamabaController.java
   git checkout HEAD~1 src/main/java/com/uhn/pmb/controller/RegistrationStatusController.java
   mvn clean package
   ```

2. **Revert Frontend Changes:**
   ```bash
   git checkout HEAD~1 src/main/resources/static/dashboard-camaba.html
   ```

3. **Clear Cache:**
   - Browser: Clear localStorage and cache
   - Server: Restart application
   - Database: No schema changes, data intact

---

## Support Commands

### Check Application Health
```bash
# Check if running
curl http://localhost:8080/api/health

# Test authentication
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'

# Check profile endpoint
curl -H "Authorization: Bearer <token>" \
  http://localhost:8080/api/camaba/profile
```

### View Logs
```bash
# Recent logs
tail -50 logs/application.log

# Error logs only
grep ERROR logs/application.log | tail -20

# Specific feature
grep -i "camaba\|registration" logs/application.log
```
