package com.example.backend.repository;

import com.example.backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserId(Long userId);
    List<Notification> findByUserIdAndRead(Long userId, Boolean read);
    List<Notification> findByUserIdAndType(Long userId, String type);
    List<Notification> findByUserIdAndStatus(Long userId, String status);
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
} 