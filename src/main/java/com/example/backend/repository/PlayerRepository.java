package com.example.backend.repository;

import com.example.backend.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> findByStatus(String status);
    List<Player> findByHiredBy_Id(Long userId);
    List<Player> findByPositionAndStatus(String position, String status);
    List<Player> findByNationalityAndStatus(String nationality, String status);
} 