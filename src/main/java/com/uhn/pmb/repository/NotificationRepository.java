package com.uhn.pmb.repository;

import com.uhn.pmb.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUser_Id(Long userId);
    List<Notification> findByUser_IdAndStatus(Long userId, Notification.NotificationStatus status);
    List<Notification> findByStatus(Notification.NotificationStatus status);
}
