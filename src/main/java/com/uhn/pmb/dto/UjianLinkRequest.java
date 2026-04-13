package com.uhn.pmb.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UjianLinkRequest {
    private Long periodId;
    // Online Mode
    private String linkUjian;
    // Offline Mode
    private String examDate;
    private String examPlace;
    private String examTime;
}
