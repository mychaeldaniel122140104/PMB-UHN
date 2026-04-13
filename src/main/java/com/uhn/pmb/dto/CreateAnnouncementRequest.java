package com.uhn.pmb.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAnnouncementRequest {
    private String title;
    private String content;
    private String description;
    private Integer priority; // 0 = normal, 1 = high, 2 = urgent
    private String announcementType; // GENERAL, UPCOMING, DEADLINE, MAINTENANCE, IMPORTANT, EVENT
    private Boolean isActive;
}
