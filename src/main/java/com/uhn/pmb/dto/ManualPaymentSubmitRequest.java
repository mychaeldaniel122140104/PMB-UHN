package com.uhn.pmb.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualPaymentSubmitRequest {
    private Long admissionFormId;
    private Long amount;
    private String bankName;
    private String accountHolder;
    private LocalDateTime transactionDate;
    // Note: proof_image akan di-handle via multipart file upload
}
