package com.example.backend.service;

import com.example.backend.entity.Player;
import com.example.backend.entity.User;
import com.example.backend.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerServiceImpl(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Override
    public List<Player> findAll() {
        return playerRepository.findAll();
    }

    @Override
    public List<Player> findByStatus(String status) {
        return playerRepository.findByStatus(status);
    }

    @Override
    public Player findById(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Player not found"));
    }

    @Override
    public Player save(Player player) {
        return playerRepository.save(player);
    }

    @Override
    public void deleteById(Long id) {
        playerRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Player hirePlayer(Long id, User user) {
        Player player = findById(id);
        if (!"AVAILABLE".equals(player.getStatus())) {
            throw new RuntimeException("Player is not available for hire");
        }

        player.setStatus("HIRED");
        player.setHiredBy(user);
        player.setHireDate(LocalDate.now());
        player.setReturnDate(LocalDate.now().plusMonths(1)); // Default 1-month hire period

        return playerRepository.save(player);
    }

    @Override
    @Transactional
    public Player returnPlayer(Long id) {
        Player player = findById(id);
        if (!"HIRED".equals(player.getStatus())) {
            throw new RuntimeException("Player is not currently hired");
        }

        player.setStatus("AVAILABLE");
        player.setHiredBy(null);
        player.setHireDate(null);
        player.setReturnDate(null);

        return playerRepository.save(player);
    }
} 