package com.uhn.pmb.service;

import com.uhn.pmb.dto.ExamTokenDTO;
import com.uhn.pmb.entity.*;
import com.uhn.pmb.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExamTokenService {

    @Autowired
    private ExamTokenRepository tokenRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ExamSubmissionRepository submissionRepository;

    @Autowired
    private AdmissionFormRepository formRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private WhatsAppService whatsAppService;

    @Value("${exam.gform.link:}")
    private String gformLink;

    /**
     * Generate token untuk mahasiswa
     * Dipanggil oleh admin saat approve formulir
     */
    @Transactional
    public ExamToken generateToken(Long studentId, Long approvedFormId, Integer expirationMinutes) {
        // Default 2 jam jika tidak spesifik
        if (expirationMinutes == null) {
            expirationMinutes = 120;
        }

        // Cari student
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student tidak ditemukan: " + studentId));

        // Revoke ALL old tokens if any exist (handle cases where multiple tokens exist)
        List<ExamToken> existingTokens = tokenRepository.findAllByStudentId(studentId);
        for (ExamToken oldToken : existingTokens) {
            if (!ExamToken.TokenStatus.REVOKED.equals(oldToken.getStatus())) {
                oldToken.setStatus(ExamToken.TokenStatus.REVOKED);
                oldToken.setRevokedAt(LocalDateTime.now());
                tokenRepository.save(oldToken);
                log.info("🔴 Token lama di-revoke untuk student: {} (Token: {})", studentId, oldToken.getTokenValue());
            }
        }

        // Generate token baru
        ExamToken token = ExamToken.builder()
                .student(student)
                .status(ExamToken.TokenStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(expirationMinutes))
                .approvedFormId(approvedFormId)
                .build();

        // Token value akan di-generate di @PrePersist
        token = tokenRepository.save(token);

        log.info("✅ Token baru di-generate untuk student {}: {}", studentId, token.getTokenValue());

        // ===== SEND EMAIL NOTIFIKASI =====
        try {
            String studentEmail = student.getUser().getEmail();
            String studentName = student.getFullName();
            String expiresAtFormatted = token.getExpiresAt().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            );

            // Send email
            emailService.sendFormApprovedEmail(
                    studentEmail,
                    studentName,
                    token.getTokenValue(),
                    expiresAtFormatted
            );
            log.info("📧 Email notifikasi dikirim ke: {}", studentEmail);

            // Send WhatsApp notification to admin
            whatsAppService.sendFormApprovedNotification(
                    studentName,
                    token.getTokenValue(),
                    expiresAtFormatted
            );
            log.info("📱 WA notification sent to admin");

        } catch (Exception e) {
            log.error("❌ Error sending notifications: {}", e.getMessage(), e);
            // Don't fail the token generation if email/WA fails
        }

        return token;
    }

    /**
     * Validate token mahasiswa saat akses ujian
     */
    @Transactional
    public ExamTokenDTO.ValidateTokenResponse validateToken(String tokenValue, Long studentId) {
        ExamToken token = tokenRepository.findByTokenValue(tokenValue)
                .orElseThrow(() -> new RuntimeException("Token tidak ditemukan: " + tokenValue));

        // Check: token untuk student ini?
        if (!token.getStudent().getId().equals(studentId)) {
            log.warn("❌ Token {} diakses oleh student yang berbeda!", tokenValue);
            throw new RuntimeException("Token bukan milik student ini");
        }

        // Check: token masih active?
        if (!token.isActive()) {
            String reason = token.getStatus() == ExamToken.TokenStatus.EXPIRED ? "Token sudah expired" :
                           token.getStatus() == ExamToken.TokenStatus.REVOKED ? "Token sudah di-revoke" :
                           "Token sudah digunakan";
            log.warn("❌ Token {} tidak active: {}", tokenValue, reason);
            throw new RuntimeException(reason);
        }

        // Check: token sudah expired?
        if (token.isExpired()) {
            token.setStatus(ExamToken.TokenStatus.EXPIRED);
            tokenRepository.save(token);
            log.warn("❌ Token {} sudah expired", tokenValue);
            throw new RuntimeException("Token sudah expired");
        }

        // Token valid!
        log.info("✅ Token {} valid untuk student {}", tokenValue, studentId);

        Student student = token.getStudent();
        LocalDateTime now = LocalDateTime.now();
        long minutesLeft = java.time.temporal.ChronoUnit.MINUTES.between(now, token.getExpiresAt());

        return ExamTokenDTO.ValidateTokenResponse.builder()
                .valid(true)
                .message("Token valid. Silakan mulai ujian.")
                .token(tokenValue)
                .gformLink(gformLink)
                .expiresAt(token.getExpiresAt())
                .expirationMinutes(minutesLeft)
                .studentInfo(ExamTokenDTO.ValidateTokenResponse.StudentInfo.builder()
                        .studentId(student.getId())
                        .fullName(student.getFullName())
                        .email(student.getUser().getEmail())
                        .build())
                .build();
    }

    /**
     * Submit hasil ujian dari mahasiswa
     */
    @Transactional
    public ExamSubmission submitExamResult(ExamTokenDTO.SubmitResultRequest request) {
        // Validate token dulu
        ExamToken token = tokenRepository.findByTokenValue(request.getToken())
                .orElseThrow(() -> new RuntimeException("Token tidak ditemukan"));

        if (!request.getStudentId().equals(token.getStudent().getId())) {
            throw new RuntimeException("Token bukan milik student ini");
        }

        if (!token.isActive()) {
            throw new RuntimeException("Token tidak active");
        }

        // Cari student
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student tidak ditemukan"));

        // Cek apakah sudah pernah submit
        Optional<ExamSubmission> existingSubmission = submissionRepository.findByStudentId(request.getStudentId());
        if (existingSubmission.isPresent()) {
            log.warn("⚠️ Student {} sudah pernah submit ujian sebelumnya", request.getStudentId());
            throw new RuntimeException("Anda sudah pernah submit ujian sebelumnya");
        }

        // Buat submission baru
        ExamSubmission submission = ExamSubmission.builder()
                .student(student)
                .examToken(token)
                .submissionData(request.getSubmissionData())
                .score(request.getScore())
                .passed(request.getPassed())
                .googleFormResponseId(request.getGoogleFormResponseId())
                .status(ExamSubmission.SubmissionStatus.PENDING)
                .submittedAt(LocalDateTime.now())
                .build();

        submission = submissionRepository.save(submission);

        // Mark token sebagai USED
        token.setStatus(ExamToken.TokenStatus.USED);
        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);

        log.info("✅ Hasil ujian di-submit untuk student {} dengan token {}", 
                request.getStudentId(), request.getToken());

        // ===== SEND NOTIFICATIONS =====
        try {
            String studentEmail = student.getUser().getEmail();
            String studentName = student.getFullName();
            int score = request.getScore();
            boolean passed = request.getPassed();

            // Send email
            emailService.sendExamCompletedEmail(
                    studentEmail,
                    studentName,
                    score,
                    passed
            );
            log.info("📧 Email hasil ujian dikirim ke: {}", studentEmail);

            // Send WA to admin
            whatsAppService.sendExamSubmittedNotification(
                    studentName,
                    score,
                    passed
            );
            log.info("📱 WA notification sent to admin");

        } catch (Exception e) {
            log.error("❌ Error sending notifications: {}", e.getMessage(), e);
            // Don't fail the submission if email/WA fails
        }

        return submission;
    }

    /**
     * Revoke token (admin endpoint)
     */
    @Transactional
    public void revokeToken(String tokenValue, String reason) {
        ExamToken token = tokenRepository.findByTokenValue(tokenValue)
                .orElseThrow(() -> new RuntimeException("Token tidak ditemukan"));

        token.setStatus(ExamToken.TokenStatus.REVOKED);
        token.setRevokedAt(LocalDateTime.now());
        tokenRepository.save(token);

        log.info("🔴 Token {} di-revoke. Alasan: {}", tokenValue, reason);
    }

    /**
     * Get token info untuk dashboard admin
     */
    public List<ExamTokenDTO.TokenInfoResponse> getValidatedStudentsWithTokens() {
        // Ambil semua token yang active
        List<ExamToken> tokens = tokenRepository.findByStatus(ExamToken.TokenStatus.ACTIVE);

        return tokens.stream().map(token -> {
            Student student = token.getStudent();
            Optional<ExamSubmission> submission = submissionRepository.findByStudentId(student.getId());

            return ExamTokenDTO.TokenInfoResponse.builder()
                    .tokenId(token.getId())
                    .token(token.getTokenValue())
                    .studentName(student.getFullName())
                    .email(student.getUser().getEmail())
                    .status(token.getStatus().toString())
                    .score(submission.map(ExamSubmission::getScore).orElse(null))
                    .submissionStatus(submission.map(s -> s.getStatus().toString()).orElse("Pending"))
                    .createdAt(token.getCreatedAt())
                    .expiresAt(token.getExpiresAt())
                    .usedAt(token.getUsedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Get statistics untuk dashboard
     */
    public Map<String, Object> getExamStatistics() {
        long totalTokens = tokenRepository.count();
        long activeTokens = tokenRepository.countByStatus(ExamToken.TokenStatus.ACTIVE);
        long usedTokens = tokenRepository.countByStatus(ExamToken.TokenStatus.USED);
        long expiredTokens = tokenRepository.countByStatus(ExamToken.TokenStatus.EXPIRED);
        long revokedTokens = tokenRepository.countByStatus(ExamToken.TokenStatus.REVOKED);

        long totalSubmissions = submissionRepository.count();
        long completedSubmissions = submissionRepository.countByStatus(ExamSubmission.SubmissionStatus.COMPLETED);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTokens", totalTokens);
        stats.put("activeTokens", activeTokens);
        stats.put("usedTokens", usedTokens);
        stats.put("expiredTokens", expiredTokens);
        stats.put("revokedTokens", revokedTokens);
        stats.put("totalSubmissions", totalSubmissions);
        stats.put("completedSubmissions", completedSubmissions);
        stats.put("responseRate", totalTokens > 0 ? (double) usedTokens / totalTokens * 100 : 0);

        return stats;
    }

    /**
     * Sync score dari Google Form (scheduled job - nanti)
     */
    @Transactional
    public void syncScoresFromGoogleForm() {
        log.info("🔄 Syncing scores dari Google Form...");
        // TODO: Implementasi nyambung ke Google Forms API
        // Untuk sekarang, score di-submit langsung dari mahasiswa via ujian.html
    }
}
