package com.example.backend.controller;

import com.example.backend.entity.Game;
import com.example.backend.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = "http://localhost:3000")
public class GameController {

    @Autowired
    private GameRepository gameRepository;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Game> createGame(@RequestBody Game game) {
        // Kiểm tra tên game đã tồn tại chưa
        if (gameRepository.findByName(game.getName()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(gameRepository.save(game));
    }

    @GetMapping
    public ResponseEntity<List<Game>> getAllGames() {
        return ResponseEntity.ok(gameRepository.findAll());
    }
} 