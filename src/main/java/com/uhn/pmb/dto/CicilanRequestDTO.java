package com.uhn.pmb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CicilanRequestDTO {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private Long programStudiId;
    private String programStudiName;
    private Long admissionFormId;
    private Integer jumlahCicilan;
    private Long hargaCicilan1;
    private Long hargaTotal;
    private Long hargaPerCicilan;
    private String briva;
    private String paymentMethod;
    private String paymentMethodLabel;
    private String status;
    private String statusLabel;
    private String catatan;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
