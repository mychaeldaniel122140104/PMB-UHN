package com.uhn.pmb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationPeriodRequest {
    private String name;
    private LocalDateTime regStartDate;
    private LocalDateTime regEndDate;
    private LocalDateTime examDate;
    private LocalDateTime examEndDate;
    private LocalDateTime announcementDate;
    private LocalDateTime reenrollmentStartDate;
    private LocalDateTime reenrollmentEndDate;
    private String description;
    private String requirements;
    private String waveType;
    private List<Long> jenisSeleksiIds;
}
