package com.example.backend.controller;

import com.example.backend.entity.GamePlayer;
import com.example.backend.service.GamePlayerService;
import com.example.backend.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/game-players")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Game Player", description = "Game player management APIs")
public class GamePlayerController {
    private final GamePlayerService gamePlayerService;

    public GamePlayerController(GamePlayerService gamePlayerService) {
        this.gamePlayerService = gamePlayerService;
    }

    @Data
    public static class GamePlayerRequest {
        @NotNull(message = "User ID is required")
        private Long userId;

        @NotNull(message = "Game ID is required")
        private Long gameId;

        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Rank is required")
        private String rank;

        @NotBlank(message = "Role is required")
        private String role;

        @NotBlank(message = "Server is required")
        private String server;

        @NotNull(message = "Price per hour is required")
        @DecimalMin(value = "0.0", message = "Price must be greater than 0")
        private BigDecimal pricePerHour;

        @Size(max = 500, message = "Description must be less than 500 characters")
        private String description;
    }

    @Data
    public static class HireRequest {
        @NotNull(message = "User ID is required")
        private Long userId;

        @NotNull(message = "Hours is required")
        @Min(value = 1, message = "Hours must be at least 1")
        private Integer hours;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('PLAYER')")
    @Operation(summary = "Create a new game player")
    public ResponseEntity<ApiResponse<GamePlayer>> createGamePlayer(
            @Valid @RequestBody GamePlayerRequest request) {
        GamePlayer gamePlayer = gamePlayerService.createGamePlayer(
            request.getUserId(),
            request.getGameId(),
            request.getUsername(),
            request.getRank(),
            request.getRole(),
            request.getServer(),
            request.getPricePerHour(),
            request.getDescription()
        );
        return ResponseEntity.ok(new ApiResponse<>(true, "Game player created successfully", gamePlayer));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PLAYER')")
    @Operation(summary = "Update a game player")
    public ResponseEntity<ApiResponse<GamePlayer>> updateGamePlayer(
            @PathVariable Long id,
            @Valid @RequestBody GamePlayerRequest request) {
        GamePlayer gamePlayer = gamePlayerService.updateGamePlayer(
            id,
            request.getUsername(),
            request.getRank(),
            request.getRole(),
            request.getServer(),
            request.getPricePerHour(),
            request.getDescription()
        );
        return ResponseEntity.ok(new ApiResponse<>(true, "Game player updated successfully", gamePlayer));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a game player")
    public ResponseEntity<ApiResponse<Void>> deleteGamePlayer(@PathVariable Long id) {
        gamePlayerService.deleteGamePlayer(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Game player deleted successfully", null));
    }

    @GetMapping("/game/{gameId}")
    @Operation(summary = "Get game players by game")
    public ResponseEntity<ApiResponse<List<GamePlayer>>> getGamePlayersByGame(@PathVariable Long gameId) {
        List<GamePlayer> gamePlayers = gamePlayerService.getGamePlayersByGame(gameId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Game players retrieved successfully", gamePlayers));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get game players by user")
    public ResponseEntity<ApiResponse<List<GamePlayer>>> getGamePlayersByUser(@PathVariable Long userId) {
        List<GamePlayer> gamePlayers = gamePlayerService.getGamePlayersByUser(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Game players retrieved successfully", gamePlayers));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get game players by status")
    public ResponseEntity<ApiResponse<List<GamePlayer>>> getGamePlayersByStatus(@PathVariable String status) {
        List<GamePlayer> gamePlayers = gamePlayerService.getGamePlayersByStatus(status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Game players retrieved successfully", gamePlayers));
    }

    @GetMapping("/rank/{rank}")
    @Operation(summary = "Get game players by rank")
    public ResponseEntity<ApiResponse<List<GamePlayer>>> getGamePlayersByRank(@PathVariable String rank) {
        List<GamePlayer> gamePlayers = gamePlayerService.getGamePlayersByRank(rank);
        return ResponseEntity.ok(new ApiResponse<>(true, "Game players retrieved successfully", gamePlayers));
    }

    @GetMapping("/role/{role}")
    @Operation(summary = "Get game players by role")
    public ResponseEntity<ApiResponse<List<GamePlayer>>> getGamePlayersByRole(@PathVariable String role) {
        List<GamePlayer> gamePlayers = gamePlayerService.getGamePlayersByRole(role);
        return ResponseEntity.ok(new ApiResponse<>(true, "Game players retrieved successfully", gamePlayers));
    }

    @GetMapping("/server/{server}")
    @Operation(summary = "Get game players by server")
    public ResponseEntity<ApiResponse<List<GamePlayer>>> getGamePlayersByServer(@PathVariable String server) {
        List<GamePlayer> gamePlayers = gamePlayerService.getGamePlayersByServer(server);
        return ResponseEntity.ok(new ApiResponse<>(true, "Game players retrieved successfully", gamePlayers));
    }

    @GetMapping("/available")
    @Operation(summary = "Get available game players")
    public ResponseEntity<ApiResponse<List<GamePlayer>>> getAvailableGamePlayers() {
        List<GamePlayer> gamePlayers = gamePlayerService.getAvailableGamePlayers();
        return ResponseEntity.ok(new ApiResponse<>(true, "Available game players retrieved successfully", gamePlayers));
    }

    @PostMapping("/{id}/hire")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Hire a game player")
    public ResponseEntity<ApiResponse<GamePlayer>> hireGamePlayer(
            @PathVariable Long id,
            @Valid @RequestBody HireRequest request) {
        GamePlayer gamePlayer = gamePlayerService.hireGamePlayer(id, request.getUserId(), request.getHours());
        return ResponseEntity.ok(new ApiResponse<>(true, "Game player hired successfully", gamePlayer));
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Return a game player")
    public ResponseEntity<ApiResponse<GamePlayer>> returnGamePlayer(@PathVariable Long id) {
        GamePlayer gamePlayer = gamePlayerService.returnGamePlayer(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Game player returned successfully", gamePlayer));
    }

    @PutMapping("/{id}/rating")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update game player rating")
    public ResponseEntity<ApiResponse<GamePlayer>> updateRating(
            @PathVariable Long id,
            @RequestParam @Min(0) @Max(5) Double rating) {
        GamePlayer gamePlayer = gamePlayerService.updateRating(id, rating);
        return ResponseEntity.ok(new ApiResponse<>(true, "Game player rating updated successfully", gamePlayer));
    }

    @PutMapping("/{id}/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PLAYER')")
    @Operation(summary = "Update game player stats")
    public ResponseEntity<ApiResponse<GamePlayer>> updateStats(
            @PathVariable Long id,
            @RequestParam @Min(0) Integer totalGames,
            @RequestParam @Min(0) @Max(100) Integer winRate) {
        GamePlayer gamePlayer = gamePlayerService.updateStats(id, totalGames, winRate);
        return ResponseEntity.ok(new ApiResponse<>(true, "Game player stats updated successfully", gamePlayer));
    }
} 