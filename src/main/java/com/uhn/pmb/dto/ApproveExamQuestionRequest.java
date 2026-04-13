package com.uhn.pmb.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApproveExamQuestionRequest {
    private Long questionId;
    private boolean approved; // true = approve, false = reject
    private String rejectionReason; // Alasan jika di-reject
}
