# 🔐 Authentication 401 Error - Diagnosis & Fix Guide

## Current Issue
Dashboard form viewing/editing returns **401 Unauthorized** errors despite having valid JWT tokens stored in localStorage.

### Error Details
- **Endpoint**: `GET /api/camaba/admission-form`
- **Status**: 401 Unauthorized
- **Message**: "Authentication required"
- **Frontend**: Token is being sent with `Authorization: Bearer {token}` header
- **Backend**: Requests are being rejected by Spring Security

---

## 🔍 Diagnosis Steps

### Step 1: Test Authentication Debug Endpoint
Open browser and go to: **`http://localhost:9500/debug-auth.html`**

> This page was added to help diagnose the authentication issues without requiring application rebuild.

**What to test:**
1. Click "Check Token in Storage" - Verify token exists in localStorage
2. Click "Check localhost:9500/api/camaba/debug-auth" - Test unauthenticated request
3. Click "Test Admission Form Endpoint" - See exact 401 error
4. Click "Analyze Token" - Decode and inspect JWT structure

### Step 2: Check Backend Logs
1. Look at application output for these log patterns:
   ```
   🔍 JWT token found in request for path: /api/camaba/admission-form
   ❌ JWT token validation failed for path: /api/camaba/admission-form
   ⚠️ Could not extract email from token
   ```

2. **Good logs** (token working):
   ```
   ✅ Token valid for email: student@test.com
   ✅ User loaded from DB: student@test.com with authorities: [CAMABA]
   ✅ Authentication set in SecurityContext for: student@test.com
   ```

---

## 🐛 Most Likely Causes (in order of probability)

### **Cause #1: JWT Secret Mismatch ⭐ MOST LIKELY**
**Symptom**: Token validation always fails even with correct format

**Check**:
```bash
# In application.properties:
jwt.secret=${JWT_SECRET:your-super-secret-key-change-this-in-production}
```

**Problem**: If `JWT_SECRET` environment variable exists on login but doesn't on request processing, or vice versa, tokens won't validate.

**Fix**:
Option A: Set environment variable persistently
```bash
# Windows - Set permanently
setx JWT_SECRET "your-super-secret-key-change-this-in-production"
# Then restart application
```

Option B: Update application.properties to use a fixed secret
```properties
jwt.secret=your-super-secret-key-12345
# Make sure it's the SAME value used when generating tokens
```

---

### **Cause #2: Role Not Assigned to User**
**Symptom**: Token validates, but 401 still occurs

**Check**:
1. In debug auth page, click "Test Login" with a known student account
2. After login, click "Analyze Token" 
3. Look for user role in the decoded JWT payload

**Expected**: Should see `ROLE_CAMABA` or `CAMABA` in the token

**Fix**:
Check `User` entity - ensure CAMABA role is assigned:
```java
// In AuthService.register() - line 57
user.setRole(User.UserRole.CAMABA);  // Must be set!
```

---

### **Cause #3: Token Expired**
**Symptom**: Token works briefly then stops working

**Check**:
1. Debug page > "Analyze Token"
2. Look at "Expires at" timestamp
3. Check if current time is past expiration

**Fix**:
Update in application.properties (currently 24 hours):
```properties
# 86400000 ms = 24 hours
# Increase to 7 days = 604800000
jwt.expiration=604800000
```

Then rebuild and restart.

---

### **Cause #4: CAMABA Role Not Recognized by Security Config**
**Symptom**: User is authenticated but endpoint still rejects

**Fix**:
Verify SecurityConfig.java line 126:
```java
.requestMatchers("/api/camaba/**").hasRole("CAMABA")
// Should match the role assigned in User entity
```

---

## 🔧 Immediate Action Plan

### Step 1: Check JWT Secret Configuration
```bash
# Check if JWT_SECRET environment variable is set
echo %JWT_SECRET%
```

If empty, either:
- Keep using default: `your-super-secret-key-change-this-in-production`
- OR set the variable: `setx JWT_SECRET "your-secret-key"`

### Step 2: Verify Token Validity
1. Go to `http://localhost:9500/debug-auth.html`
2. Register a new test account OR login with existing account
3. Check debug output for:
   - ✅ Token in localStorage
   - ✅ Token validates with debug endpoint
   - ✅ JWT payload shows CAMABA role

### Step 3: Test Form Endpoint
From debug page, click "Call /api/camaba/admission-form"
- If 200 OK: Authentication works ✅
- If 401: See error details and logs for specific reason

---

## 📋 Code Changes Made This Session

### 1. Added Debug Endpoint (CamabaController.java)
```java
@GetMapping("/debug-auth")
@org.springframework.security.access.prepost.PermitAll()
public ResponseEntity<?> debugAuth(@RequestHeader(value = "Authorization") String authHeader)
```
- Doesn't require authentication
- Shows what the backend sees about current request
- Helps identify if token is received and SecurityContext is set

### 2. Enhanced Logging (JwtAuthenticationFilter.java)
- Detailed logs for token extraction, validation, and user loading
- Helps trace exact point of failure

### 3. Enhanced Logging (JwtTokenProvider.java)
- Checks if JWT_SECRET is null/empty
- Logs JWT validation failures with details

---

## 🚀 Next Steps After Diagnosis

### If JWT Validation Fails:
1. Check JWT_SECRET environment variable
2. Ensure it's the same during token generation and validation
3. Rebuild application: `mvn clean package -DskipTests`
4. Restart application
5. Re-test from debug page

### If Token Validates But Still 401:
1. Check user role in decoded JWT
2. Verify CAMABA role is assigned in User entity
3. Check SecurityConfig has correct role matching

### If All Tests Pass:
1. Update dashboard-camaba.html to use endpoints
2. Test form viewing/editing on dashboard
3. Test file uploads during form edit

---

## 📊 Application Architecture for Auth

```
Login Request
    ↓
AuthController.login() → AuthService.login()
    ↓
AuthenticationManager.authenticate(credentials)
    ↓
✅ Credentials valid
    ↓
JwtTokenProvider.generateToken(authentication)
    ↓
Returns JWT token to frontend
    ↓ (Token stored in localStorage)
    
User Request to /api/camaba/admission-form
    ↓
JwtAuthenticationFilter.doFilterInternal()
    ↓
Extract token from Authorization header
    ↓
JwtTokenProvider.validateToken(jwt) [Uses JWT_SECRET to verify]
    ↓
✅ Token valid → Extract email → Load UserDetails
    ↓
Set SecurityContext with UsernamePasswordAuthenticationToken
    ↓
❌ Token invalid → SecurityContext NOT set
    ↓
Request continues to endpoint
    ↓
@PreAuthorize("hasRole('CAMABA')") checks SecurityContext
    ↓
✅ Authenticated → Return form data
✅ Authorized → Return form data  
❌ Not authenticated → 401 Unauthorized
❌ Wrong role → 403 Forbidden
```

---

## 💡 Pro Tips

1. **Always check logs first** - Detailed debug logging is in place
2. **JWT secrets must match** - This is the #1 cause of 401 errors
3. **Token format matters** - Must be `Bearer {token}` (with space)
4. **Environment variables** - Be careful with configuration
5. **Rebuild affects JWT** - After config changes, rebuild and restart

---

## 📞 Quick Reference

| Issue | Log Pattern | Likely Cause |
|-------|-----------|--------------|
| Token not found | "❌ No JWT token found" | Header not sent or malformed |
| Token validation fails | "⚠️ JWT token validation failed" | Secret mismatch or expired token |
| Email not extracted | "⚠️ Could not extract email" | Token corrupted or invalid signature |
| User not loaded | "❌ JWT authentication error" | User doesn't exist in database |
| 401 after validation | Endpoint called but 401 returned | Role not in SecurityContext |

