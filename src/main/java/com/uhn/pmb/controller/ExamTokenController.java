package com.uhn.pmb.controller;

import com.uhn.pmb.dto.ExamTokenDTO;
import com.uhn.pmb.entity.ExamToken;
import com.uhn.pmb.entity.ExamSubmission;
import com.uhn.pmb.service.ExamTokenService;
import com.uhn.pmb.repository.ExamTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
public class ExamTokenController {

    @Autowired
    private ExamTokenService tokenService;

    @Autowired
    private ExamTokenRepository tokenRepository;

    @Value("${exam.gform.link:}")
    private String gformLink;

    /**
     * ===== ADMIN ENDPOINTS =====
     */

    /**
     * 1️⃣ ENDPOINT: Admin approve formulir dan generate token
     * POST /admin/api/exam/generate-token
     * 
     * Request: { studentId, approvedFormId, expirationMinutes }
     * Response: { tokenId, token, expiresAt, studentInfo }
     */
    @PostMapping("/admin/api/exam/generate-token")
    public ResponseEntity<?> generateToken(
            @RequestBody ExamTokenDTO.GenerateTokenRequest request,
            Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("❌ Tidak ada authentikasi untuk generate token");
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Silakan login sebagai admin"
                ));
            }

            log.info("📝 Admin generate token untuk student: {}", request.getStudentId());

            // Generate token menggunakan service
            ExamToken token = tokenService.generateToken(
                    request.getStudentId(),
                    request.getApprovedFormId(),
                    request.getExpirationMinutes()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Token berhasil di-generate");
            response.put("tokenId", token.getId());
            response.put("token", token.getTokenValue());
            response.put("expiresAt", token.getExpiresAt());
            response.put("studentInfo", Map.of(
                    "id", token.getStudent().getId(),
                    "name", token.getStudent().getFullName(),
                    "email", token.getStudent().getUser().getEmail()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error generate token", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Gagal generate token: " + e.getMessage()
            ));
        }
    }

    /**
     * Get validated students dengan token mereka (untuk dashboard)
     * GET /admin/api/exam/get-validated-students
     */
    @GetMapping("/admin/api/exam/get-validated-students")
    public ResponseEntity<?> getValidatedStudents(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Silakan login"
                ));
            }

            List<ExamTokenDTO.TokenInfoResponse> students = tokenService.getValidatedStudentsWithTokens();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", students,
                    "total", students.size()
            ));
        } catch (Exception e) {
            log.error("❌ Error get validated students", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get exam statistics (untuk dashboard)
     * GET /admin/api/exam/statistics
     */
    @GetMapping("/admin/api/exam/statistics")
    public ResponseEntity<?> getStatistics(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Silakan login"
                ));
            }

            Map<String, Object> stats = tokenService.getExamStatistics();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", stats
            ));
        } catch (Exception e) {
            log.error("❌ Error get statistics", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Revoke token (admin endpoint)
     * POST /admin/api/exam/revoke-token
     */
    @PostMapping("/admin/api/exam/revoke-token")
    public ResponseEntity<?> revokeToken(
            @RequestBody ExamTokenDTO.RevokeTokenRequest request,
            Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Silakan login"
                ));
            }

            log.info("🔴 Revoke token: {}", request.getToken());
            tokenService.revokeToken(request.getToken(), request.getReason());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Token berhasil di-revoke"
            ));
        } catch (Exception e) {
            log.error("❌ Error revoke token", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * ===== MAHASISWA ENDPOINTS =====
     */

    /**
     * 2️⃣ ENDPOINT: Mahasiswa validate token mereka
     * POST /api/exam/validate-token
     * 
     * Request: { token, studentId }
     * Response: { valid, gformLink, expiresAt, studentInfo }
     */
    @PostMapping("/api/exam/validate-token")
    public ResponseEntity<?> validateToken(@RequestBody ExamTokenDTO.ValidateTokenRequest request) {
        try {
            log.info("🔐 Validating token: {} untuk student: {}", request.getToken(), request.getStudentId());

            ExamTokenDTO.ValidateTokenResponse response = tokenService.validateToken(
                    request.getToken(),
                    request.getStudentId()
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", response
            ));
        } catch (Exception e) {
            log.error("❌ Error validate token", e);
            return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "valid", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get Google Form link (sebelum iframe)
     * GET /api/exam/get-gform-link
     */
    @GetMapping("/api/exam/get-gform-link")
    public ResponseEntity<?> getGFormLink() {
        try {
            if (gformLink == null || gformLink.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "Admin belum setup link Google Form"
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "gformLink", gformLink
            ));
        } catch (Exception e) {
            log.error("❌ Error get gform link", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 3️⃣ ENDPOINT: Mahasiswa submit hasil ujian
     * POST /api/exam/submit-results
     * 
     * Request: { token, studentId, score, submissionData }
     * Response: { submissionId, message }
     */
    @PostMapping("/api/exam/submit-results")
    public ResponseEntity<?> submitResults(@RequestBody ExamTokenDTO.SubmitResultRequest request) {
        try {
            log.info("📊 Submitting exam results untuk student: {} dengan token: {}", 
                    request.getStudentId(), request.getToken());

            ExamSubmission submission = tokenService.submitExamResult(request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Hasil ujian berhasil di-submit",
                    "submissionId", submission.getId(),
                    "submittedAt", submission.getSubmittedAt(),
                    "score", submission.getScore()
            ));
        } catch (Exception e) {
            log.error("❌ Error submit results", e);
            return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get submission status
     * GET /api/exam/submission-status/{studentId}
     */
    @GetMapping("/api/exam/submission-status/{studentId}")
    public ResponseEntity<?> getSubmissionStatus(@PathVariable Long studentId) {
        try {
            // TODO: Implement submission status retrieval
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Status endpoint"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
