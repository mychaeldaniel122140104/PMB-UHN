package com.uhn.pmb.repository;

import com.uhn.pmb.entity.AdminMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminMessageRepository extends JpaRepository<AdminMessage, Long> {

    @Query("SELECT am FROM AdminMessage am WHERE " +
           "(am.sender.id = :userId OR am.recipient.id = :userId) " +
           "ORDER BY am.createdAt DESC")
    List<AdminMessage> findConversationWith(@Param("userId") Long userId);

    @Query("SELECT am FROM AdminMessage am WHERE am.recipient.id = :userId AND am.status = 'UNREAD' ORDER BY am.createdAt DESC")
    List<AdminMessage> findUnreadMessages(@Param("userId") Long userId);

    @Query("SELECT COUNT(am) FROM AdminMessage am WHERE am.recipient.id = :userId AND am.status = 'UNREAD'")
    Long countUnreadMessages(@Param("userId") Long userId);

    @Query("SELECT am FROM AdminMessage am WHERE " +
           "((am.sender.id = :senderId AND am.recipient.id = :recipientId) OR " +
           "(am.sender.id = :recipientId AND am.recipient.id = :senderId)) " +
           "ORDER BY am.createdAt ASC")
    List<AdminMessage> findConversationBetween(@Param("senderId") Long senderId, @Param("recipientId") Long recipientId);

    @Query("SELECT am FROM AdminMessage am WHERE am.recipient.id = :userId ORDER BY am.createdAt DESC")
    List<AdminMessage> findReceivedMessages(@Param("userId") Long userId);
}
