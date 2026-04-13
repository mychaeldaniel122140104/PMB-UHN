package com.uhn.pmb.repository;

import com.uhn.pmb.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    // Get all active announcements for public viewing (sorted by priority and date)
    @Query("SELECT a FROM Announcement a WHERE a.isActive = true ORDER BY a.priority DESC, a.publishedAt DESC")
    List<Announcement> findAllActive();

    // Get active announcements with pagination
    @Query("SELECT a FROM Announcement a WHERE a.isActive = true ORDER BY a.priority DESC, a.publishedAt DESC")
    Page<Announcement> findAllActive(Pageable pageable);

    // Get announcements by type
    @Query("SELECT a FROM Announcement a WHERE a.isActive = true AND a.announcementType = :type ORDER BY a.priority DESC, a.publishedAt DESC")
    List<Announcement> findByType(@Param("type") Announcement.AnnouncementType type);

    // Get recent announcements (last 5)
    @Query(value = "SELECT a FROM Announcement a WHERE a.isActive = true ORDER BY a.publishedAt DESC LIMIT 5")
    List<Announcement> findRecent();

    // Find announcements created after specific date
    @Query("SELECT a FROM Announcement a WHERE a.isActive = true AND a.publishedAt >= :dateTime ORDER BY a.publishedAt DESC")
    List<Announcement> findAfterDate(@Param("dateTime") LocalDateTime dateTime);

    // Count active announcements
    @Query("SELECT COUNT(a) FROM Announcement a WHERE a.isActive = true")
    Long countActive();

    // Find by title (for search)
    @Query("SELECT a FROM Announcement a WHERE a.isActive = true AND LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY a.publishedAt DESC")
    List<Announcement> searchByTitle(@Param("keyword") String keyword);

    // Get urgent announcements
    @Query("SELECT a FROM Announcement a WHERE a.isActive = true AND (a.priority >= 1) ORDER BY a.priority DESC, a.publishedAt DESC")
    List<Announcement> findUrgent();
}
