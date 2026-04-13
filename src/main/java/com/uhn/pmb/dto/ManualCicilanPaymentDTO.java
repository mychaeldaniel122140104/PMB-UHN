package com.uhn.pmb.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualCicilanPaymentDTO {
    private Long id;
    private Long cicilanRequestId;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private Integer cicilanKe;
    private Long nominal;
    private String paymentProofPath;
    private String status;
    private String statusLabel;
    private String keterangan;
    private String verifiedBy;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
