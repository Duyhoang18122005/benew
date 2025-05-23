package com.example.backend.service;

import com.example.backend.entity.Player;
import com.example.backend.entity.User;
import java.util.List;

public interface PlayerService {
    List<Player> findAll();
    List<Player> findByStatus(String status);
    Player findById(Long id);
    Player save(Player player);
    void deleteById(Long id);
    Player hirePlayer(Long id, User user);
    Player returnPlayer(Long id);
} 