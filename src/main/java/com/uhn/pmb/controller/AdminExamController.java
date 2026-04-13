package com.uhn.pmb.controller;

import com.uhn.pmb.dto.ApproveExamQuestionRequest;
import com.uhn.pmb.dto.GenerateExamQuestionRequest;
import com.uhn.pmb.entity.*;
import com.uhn.pmb.repository.ExamQuestionRepository;
import com.uhn.pmb.repository.UserRepository;
import com.uhn.pmb.service.GeminiAIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.io.PrintWriter;
import java.io.StringWriter;

@Slf4j
@RestController
@RequestMapping("/admin/api/exam-questions")
@CrossOrigin(origins = "*")
public class AdminExamController {

    @Autowired
    private ExamQuestionRepository examQuestionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GeminiAIService geminiAIService;

    /**
     * Generate exam question menggunakan Gemini AI
     * POST /admin/api/exam-questions/generate-ai
     */
    @PostMapping("/generate-ai")
    public ResponseEntity<?> generateExamQuestion(
            @RequestBody GenerateExamQuestionRequest request,
            Authentication authentication) {

        try {
            // Check authentication first
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("User not authenticated for generate-ai endpoint");
                return ResponseEntity.status(401).body(Map.of("error", "User belum login atau session sudah expired. Silakan login kembali."));
            }

            String email = authentication.getName();
            if (email == null || email.isEmpty()) {
                log.warn("Authentication exists but email is null/empty");
                return ResponseEntity.status(401).body(Map.of("error", "User email tidak ditemukan. Silakan login kembali."));
            }

            Optional<User> userOpt = userRepository.findByEmail(email);

            if (userOpt.isEmpty()) {
                log.warn("User email {} not found in database", email);
                return ResponseEntity.status(401).body(Map.of("error", "User not found in database"));
            }

            // Check if API key is configured
            if (!geminiAIService.isApiKeyConfigured()) {
                log.warn("Gemini API Key not configured");
                return ResponseEntity.status(500).body(Map.of(
                    "error", "Gemini API Key belum dikonfigurasi. Hubungi administrator untuk mengatur API key.",
                    "type", "API_KEY_NOT_CONFIGURED"
                ));
            }

            User user = userOpt.get();

            // Generate multiple questions menggunakan AI
            List<ExamQuestion> questions = geminiAIService.generateMultipleExamQuestions(request, user);

            if (questions.isEmpty()) {
                return ResponseEntity.status(500).body(Map.of(
                    "error", "Gagal generate soal. Quota Gemini API mungkin telah habis atau terjadi error pada API."
                ));
            }

            // Save semua ke database (status: PENDING untuk di-review)
            List<ExamQuestion> saved = examQuestionRepository.saveAll(questions);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Berhasil generate " + saved.size() + " soal. Silakan review di section 'Soal Menunggu Review'");
            response.put("count", saved.size());
            response.put("questions", saved);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating exam question: {}", e.getMessage(), e);
            
            // Log full stack trace for debugging
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            log.error("Full stack trace: {}", sw.toString());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage() != null ? e.getMessage() : "Unknown error");
            errorResponse.put("errorType", e.getClass().getSimpleName());
            
            // Provide specific error messages based on exception type
            String errorMsg = "Gagal generate soal: " + (e.getMessage() != null ? e.getMessage() : "Unknown error");
            if (e.getMessage() != null) {
                if (e.getMessage().contains("API key") || e.getMessage().contains("Configuration")) {
                    errorMsg = "Konfigurasi API key tidak valid atau belum diset. Hubungi administrator.";
                } else if (e.getMessage().contains("quota")) {
                    errorMsg = "Quota Gemini API telah habis.";
                } else if (e.getMessage().contains("rate_limit")) {
                    errorMsg = "Rate limit Gemini API tercapai. Mohon tunggu beberapa saat.";
                } else if (e.getMessage().contains("Unexpected response format")) {
                    errorMsg = "Format respons dari Gemini API tidak sesuai. Silakan coba lagi.";
                }
            }
            
            errorResponse.put("userMessage", errorMsg);
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get semua soal yang pending untuk di-review
     * GET /admin/api/exam-questions/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingQuestions(Authentication authentication) {
        try {
            List<ExamQuestion> pendingQuestions = examQuestionRepository.findAllPending();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", pendingQuestions.size(),
                    "questions", pendingQuestions
            ));
        } catch (Exception e) {
            log.error("Error fetching pending questions: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Gagal mengambil soal pending"));
        }
    }

    /**
     * Get soal berdasarkan kategori (hanya yang sudah approved)
     * GET /admin/api/exam-questions/by-category/{category}
     */
    @GetMapping("/by-category/{category}")
    public ResponseEntity<?> getQuestionsByCategory(@PathVariable String category) {
        try {
            QuestionCategory cat = QuestionCategory.valueOf(category.toUpperCase());
            List<ExamQuestion> questions = examQuestionRepository.findByCategoryApproved(cat);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "category", category,
                    "count", questions.size(),
                    "questions", questions
            ));
        } catch (Exception e) {
            log.error("Error fetching questions by category: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Gagal mengambil soal berdasarkan kategori"));
        }
    }

    /**
     * Approve atau Reject soal ujian
     * PUT /admin/api/exam-questions/{id}/approve
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveQuestion(
            @PathVariable Long id,
            @RequestBody ApproveExamQuestionRequest request,
            Authentication authentication) {

        try {
            Optional<ExamQuestion> questionOpt = examQuestionRepository.findById(id);
            if (questionOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Soal tidak ditemukan"));
            }

            String email = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("error", "User not found"));
            }

            ExamQuestion question = questionOpt.get();
            User approver = userOpt.get();

            if (request.isApproved()) {
                question.setApprovalStatus(ApprovalStatus.APPROVED);
                question.setApprovedAt(LocalDateTime.now());
                question.setApprovedBy(approver);
            } else {
                question.setApprovalStatus(ApprovalStatus.REJECTED);
                question.setRejectionReason(request.getRejectionReason());
            }

            ExamQuestion updated = examQuestionRepository.save(question);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", request.isApproved() ? "Soal disetujui" : "Soal ditolak",
                    "question", updated
            ));
        } catch (Exception e) {
            log.error("Error approving question: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Gagal meng-approve soal"));
        }
    }

    /**
     * Delete soal ujian
     * DELETE /admin/api/exam-questions/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long id, Authentication authentication) {
        try {
            Optional<ExamQuestion> questionOpt = examQuestionRepository.findById(id);
            if (questionOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Soal tidak ditemukan"));
            }

            examQuestionRepository.deleteById(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Soal berhasil dihapus"
            ));
        } catch (Exception e) {
            log.error("Error deleting question: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Gagal menghapus soal"));
        }
    }

    /**
     * Get detail soal
     * GET /admin/api/exam-questions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getQuestion(@PathVariable Long id) {
        try {
            Optional<ExamQuestion> questionOpt = examQuestionRepository.findById(id);
            if (questionOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Soal tidak ditemukan"));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "question", questionOpt.get()
            ));
        } catch (Exception e) {
            log.error("Error fetching question: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Gagal mengambil soal"));
        }
    }

    /**
     * Get semua soal berdasarkan approval status
     * GET /admin/api/exam-questions?status=APPROVED
     */
    @GetMapping
    public ResponseEntity<?> getAllQuestions(
            @RequestParam(required = false) String status,
            Authentication authentication) {
        try {
            List<ExamQuestion> questions;

            if (status != null && !status.isEmpty()) {
                ApprovalStatus approvalStatus = ApprovalStatus.valueOf(status.toUpperCase());
                questions = examQuestionRepository.findByApprovalStatus(approvalStatus);
            } else {
                questions = examQuestionRepository.findAll();
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", questions.size(),
                    "questions", questions
            ));
        } catch (Exception e) {
            log.error("Error fetching all questions: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Gagal mengambil daftar soal"));
        }
    }

    /**
     * Get statistics
     * GET /admin/api/exam-questions/stats/summary
     */
    @GetMapping("/stats/summary")
    public ResponseEntity<?> getStatistics(Authentication authentication) {
        try {
            Long pendingCount = examQuestionRepository.countPendingQuestions();
            List<ExamQuestion> approved = examQuestionRepository.findByApprovalStatus(ApprovalStatus.APPROVED);
            List<ExamQuestion> rejected = examQuestionRepository.findByApprovalStatus(ApprovalStatus.REJECTED);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "pending", pendingCount,
                    "approved", approved.size(),
                    "rejected", rejected.size(),
                    "total", approved.size() + rejected.size() + pendingCount
            ));
        } catch (Exception e) {
            log.error("Error fetching statistics: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Gagal mengambil statistik"));
        }
    }
}
