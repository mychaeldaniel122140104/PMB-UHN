package com.uhn.pmb.task;

import com.uhn.pmb.entity.PublicationSchedule;
import com.uhn.pmb.repository.PublicationScheduleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task to auto-publish results when the scheduled time arrives.
 * Runs every 1 minute to check for pending publications.
 */
@Component
@EnableScheduling
@Slf4j
public class PublicationScheduleTask {

    @Autowired
    private PublicationScheduleRepository publicationScheduleRepository;

    @Scheduled(fixedRate = 60000) // every 1 minute
    public void checkAndPublishScheduledResults() {
        List<PublicationSchedule> pendingPublications =
                publicationScheduleRepository.findByIsPublishedFalseAndPublishDateTimeBefore(LocalDateTime.now());

        for (PublicationSchedule schedule : pendingPublications) {
            schedule.setIsPublished(true);
            schedule.setPublishedAt(LocalDateTime.now());
            publicationScheduleRepository.save(schedule);
            log.info("📢 [AUTO-PUBLISH] Results auto-published for period: {} at scheduled time: {}",
                    schedule.getPeriod().getName(), schedule.getPublishDateTime());
        }
    }
}
