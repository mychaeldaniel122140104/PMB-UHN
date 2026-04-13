package com.uhn.pmb.dto;

import lombok.*;

import java.time.LocalDateTime;

public class ExamTokenDTO {

    /**
     * Request untuk generate token (admin endpoint)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GenerateTokenRequest {
        private Long studentId;
        private Long approvedFormId;
        private Integer expirationMinutes; // Optional, default 120 (2 jam)
    }

    /**
     * Request untuk validate token (mahasiswa endpoint)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidateTokenRequest {
        private String token;
        private Long studentId; // Optional, untuk double-check
    }

    /**
     * Response untuk validate token
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidateTokenResponse {
        private boolean valid;
        private String message;
        private String token;
        private String gformLink;
        private LocalDateTime expiresAt;
        private Long expirationMinutes;
        private StudentInfo studentInfo;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class StudentInfo {
            private Long studentId;
            private String fullName;
            private String email;
        }
    }

    /**
     * Request untuk submit hasil ujian
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmitResultRequest {
        private String token;
        private Long studentId;
        private String submissionData; // JSON string dari GForm submission
        private Integer score;
        private Boolean passed;
        private String googleFormResponseId;
    }

    /**
     * Response untuk token info (di dashboard)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TokenInfoResponse {
        private Long tokenId;
        private String token;
        private String studentName;
        private String email;
        private String status;
        private Integer score;
        private String submissionStatus;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
        private LocalDateTime usedAt;
    }

    /**
     * Request untuk revoke token (admin endpoint)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevokeTokenRequest {
        private String token;
        private String reason;
    }
}
