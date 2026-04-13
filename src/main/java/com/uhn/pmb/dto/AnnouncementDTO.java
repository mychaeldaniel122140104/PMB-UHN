package com.uhn.pmb.dto;

import com.uhn.pmb.entity.Announcement;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementDTO {
    private Long id;
    private String title;
    private String content;
    private String description;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private Boolean isActive;
    private Integer priority;
    private String announcementType;

    public static AnnouncementDTO fromEntity(Announcement announcement) {
        return AnnouncementDTO.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .description(announcement.getDescription())
                .createdByName(announcement.getCreatedByName())
                .createdAt(announcement.getCreatedAt())
                .updatedAt(announcement.getUpdatedAt())
                .publishedAt(announcement.getPublishedAt())
                .isActive(announcement.getIsActive())
                .priority(announcement.getPriority())
                .announcementType(announcement.getAnnouncementType().name())
                .build();
    }
}

