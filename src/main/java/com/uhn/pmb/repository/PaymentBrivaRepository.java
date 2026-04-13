package com.uhn.pmb.repository;

import com.uhn.pmb.entity.PaymentBriva;
import com.uhn.pmb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentBrivaRepository extends JpaRepository<PaymentBriva, Long> {
    Optional<PaymentBriva> findByBrivaCode(String brivaCode);
    List<PaymentBriva> findByUser(User user);
    List<PaymentBriva> findByUserAndStatus(User user, PaymentBriva.PaymentStatus status);
    List<PaymentBriva> findByStatusAndDueDatetimeBefore(PaymentBriva.PaymentStatus status, LocalDateTime dateTime);
}
