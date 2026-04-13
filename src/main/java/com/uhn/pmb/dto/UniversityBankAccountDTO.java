package com.uhn.pmb.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UniversityBankAccountDTO {
    private Long id;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private String purpose;
    private Boolean isActive;
}
