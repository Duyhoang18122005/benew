package com.example.backend.controller;

import com.example.backend.entity.Player;
import com.example.backend.entity.User;
import com.example.backend.service.PlayerService;
import com.example.backend.service.UserService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@CrossOrigin(origins = "http://localhost:3000")
public class PlayerController {

    private final PlayerService playerService;
    private final UserService userService;

    public PlayerController(PlayerService playerService, UserService userService) {
        this.playerService = playerService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<Player>> getAllPlayers() {
        return ResponseEntity.ok(playerService.findAll());
    }

    @GetMapping("/available")
    public ResponseEntity<List<Player>> getAvailablePlayers() {
        return ResponseEntity.ok(playerService.findByStatus("AVAILABLE"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Player> getPlayer(@PathVariable Long id) {
        return ResponseEntity.ok(playerService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Player> createPlayer(@RequestBody PlayerRequest playerRequest) {
        Player player = new Player();
        updatePlayerFromRequest(player, playerRequest);
        return ResponseEntity.ok(playerService.save(player));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Player> updatePlayer(@PathVariable Long id, @RequestBody PlayerRequest playerRequest) {
        Player player = playerService.findById(id);
        updatePlayerFromRequest(player, playerRequest);
        return ResponseEntity.ok(playerService.save(player));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlayer(@PathVariable Long id) {
        playerService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/hire")
    public ResponseEntity<Player> hirePlayer(@PathVariable Long id, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        return ResponseEntity.ok(playerService.hirePlayer(id, user));
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<Player> returnPlayer(@PathVariable Long id) {
        return ResponseEntity.ok(playerService.returnPlayer(id));
    }

    private void updatePlayerFromRequest(Player player, PlayerRequest request) {
        player.setName(request.getName());
        player.setAge(request.getAge());
        player.setPosition(request.getPosition());
        player.setNationality(request.getNationality());
        player.setCurrentClub(request.getCurrentClub());
        player.setMarketValue(request.getMarketValue());
        player.setContractExpiry(request.getContractExpiry());
        player.setPreferredFoot(request.getPreferredFoot());
        player.setHeight(request.getHeight());
        player.setWeight(request.getWeight());
        player.setStatus(request.getStatus());
    }
}

@Data
class PlayerRequest {
    private String name;
    private Integer age;
    private String position;
    private String nationality;
    private String currentClub;
    private java.math.BigDecimal marketValue;
    private java.time.LocalDate contractExpiry;
    private String preferredFoot;
    private Integer height;
    private Integer weight;
    private String status;
} 