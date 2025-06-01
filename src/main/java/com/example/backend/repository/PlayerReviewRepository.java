package com.example.backend.repository;

import com.example.backend.entity.PlayerReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PlayerReviewRepository extends JpaRepository<PlayerReview, Long> {
    List<PlayerReview> findByPlayerId(Long playerId);
    
    List<PlayerReview> findByPaymentId(Long paymentId);
    
    @Query("SELECT AVG(r.rating) FROM PlayerReview r WHERE r.player.id = ?1")
    Double getAverageRatingByPlayerId(Long playerId);
    
    boolean existsByPaymentId(Long paymentId);
} 