package com.uhn.pmb.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CicilanRequestSubmitRequest {
    private Long programStudiId;
    private Integer jumlahCicilan; // 1-6
    private String paymentMethod; // SIMULATION or MANUAL
    
    // studentId will be obtained from authentication context
    // hargaCicilan1 dan hargaTotal akan di-fetch dari ProgramStudi
}
