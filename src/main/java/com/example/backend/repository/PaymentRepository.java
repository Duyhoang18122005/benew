package com.example.backend.repository;

import com.example.backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserId(Long userId);
    List<Payment> findByGamePlayerId(Long gamePlayerId);
    List<Payment> findByStatus(String status);
    List<Payment> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<Payment> findByUserIdAndStatus(Long userId, String status);
} 