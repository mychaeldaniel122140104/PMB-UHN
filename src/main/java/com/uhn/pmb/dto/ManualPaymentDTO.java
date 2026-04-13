package com.uhn.pmb.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualPaymentDTO {
    private Long id;
    private Long admissionFormId;
    private Long amount;
    private String bankName;
    private String accountHolder;
    private LocalDateTime transactionDate;
    private String status;
    private String proofImageUrl;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime verifiedAt;
    private String verifiedBy;
}
