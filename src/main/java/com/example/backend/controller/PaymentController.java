package com.example.backend.controller;

import com.example.backend.entity.Payment;
import com.example.backend.service.PaymentService;
import com.example.backend.service.UserService;
import com.example.backend.entity.User;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.PaymentException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.example.backend.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Payment", description = "Payment management APIs")
public class PaymentController {
    private final PaymentService paymentService;
    private final UserService userService;
    private final PaymentRepository paymentRepository;

    public PaymentController(PaymentService paymentService, UserService userService, PaymentRepository paymentRepository) {
        this.paymentService = paymentService;
        this.userService = userService;
        this.paymentRepository = paymentRepository;
    }

    @Operation(summary = "Create a new payment")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Payment> createPayment(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Payment payment = paymentService.createPayment(
                    request.getGamePlayerId(),
                    user.getId(),
                    request.getAmount(),
                    request.getCurrency(),
                    request.getPaymentMethod()
            );
            return ResponseEntity.ok(payment);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Process a payment")
    @PostMapping("/{id}/process")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Payment> processPayment(
            @Parameter(description = "Payment ID") @PathVariable Long id,
            @Valid @RequestBody ProcessPaymentRequest request) {
        try {
            Payment payment = paymentService.processPayment(id, request.getTransactionId());
            return ResponseEntity.ok(payment);
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Refund a payment")
    @PostMapping("/{id}/refund")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Payment> refundPayment(
            @Parameter(description = "Payment ID") @PathVariable Long id,
            @Valid @RequestBody RefundRequest request) {
        try {
            Payment payment = paymentService.refundPayment(id, request.getReason());
            return ResponseEntity.ok(payment);
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Get user payments")
    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Payment>> getUserPayments(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        return ResponseEntity.ok(paymentService.getUserPayments(user.getId()));
    }

    @Operation(summary = "Get game player payments")
    @GetMapping("/game-player/{gamePlayerId}")
    public ResponseEntity<List<Payment>> getGamePlayerPayments(
            @Parameter(description = "Game player ID") @PathVariable Long gamePlayerId) {
        return ResponseEntity.ok(paymentService.getGamePlayerPayments(gamePlayerId));
    }

    @Operation(summary = "Get payments by status")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Payment>> getPaymentsByStatus(
            @Parameter(description = "Payment status") @PathVariable String status) {
        return ResponseEntity.ok(paymentService.getPaymentsByStatus(status));
    }

    @Operation(summary = "Get payments by date range")
    @GetMapping("/date-range")
    public ResponseEntity<List<Payment>> getPaymentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(paymentService.getPaymentsByDateRange(start, end));
    }

    @PostMapping("/topup")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> topUp(@RequestBody TopUpRequest request, Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            // Validate amount
            if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body("Amount must be greater than 0");
            }

            // Update wallet balance
            user.setWalletBalance(user.getWalletBalance().add(request.getAmount()));
            user = userService.save(user);

            // Create payment record
            Payment payment = new Payment();
            payment.setUser(user);
            payment.setAmount(request.getAmount());
            payment.setCurrency("VND");
            payment.setStatus("COMPLETED");
            payment.setPaymentMethod("TOPUP");
            payment.setType("TOPUP");
            payment.setCreatedAt(java.time.LocalDateTime.now());
            paymentRepository.save(payment);

            return ResponseEntity.ok("Nạp tiền thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi nạp tiền: " + e.getMessage());
        }
    }

    @PostMapping("/hire")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> hirePlayer(@RequestBody HireRequest request, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        User player = userService.findById(request.getPlayerId());
        BigDecimal amount = request.getAmount();
        if (user.getWalletBalance().compareTo(amount) < 0) {
            return ResponseEntity.badRequest().body("Số dư không đủ");
        }
        user.setWalletBalance(user.getWalletBalance().subtract(amount));
        player.setWalletBalance(player.getWalletBalance().add(amount));
        userService.save(user);
        userService.save(player);
        Payment payment = new Payment();
        payment.setUser(user);
        payment.setAmount(amount);
        payment.setCurrency("VND");
        payment.setStatus("COMPLETED");
        payment.setPaymentMethod("HIRE");
        payment.setType("HIRE");
        payment.setCreatedAt(java.time.LocalDateTime.now());
        payment.setStartTime(request.getStartTime());
        payment.setEndTime(request.getEndTime());
        payment.setHireStatus(request.getHireStatus() != null ? request.getHireStatus() : "ACTIVE");
        paymentRepository.save(payment);
        return ResponseEntity.ok("Thuê player thành công");
    }

    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('PLAYER')")
    @Transactional
    public ResponseEntity<?> withdraw(@RequestBody WithdrawRequest request, Authentication authentication) {
        User player = userService.findByUsername(authentication.getName());
        BigDecimal amount = request.getAmount();
        if (player.getWalletBalance().compareTo(amount) < 0) {
            return ResponseEntity.badRequest().body("Số dư không đủ");
        }
        player.setWalletBalance(player.getWalletBalance().subtract(amount));
        userService.save(player);
        Payment payment = new Payment();
        payment.setUser(player);
        payment.setAmount(amount);
        payment.setCurrency("VND");
        payment.setStatus("COMPLETED");
        payment.setPaymentMethod("WITHDRAW");
        payment.setType("WITHDRAW");
        payment.setCreatedAt(java.time.LocalDateTime.now());
        paymentRepository.save(payment);
        return ResponseEntity.ok("Rút tiền thành công");
    }

    @Operation(summary = "Get user wallet balance")
    @GetMapping("/wallet-balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BigDecimal> getWalletBalance(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        return ResponseEntity.ok(user.getWalletBalance());
    }
}

@Data
class PaymentRequest {
    @NotNull(message = "Game player ID is required")
    private Long gamePlayerId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;
}

@Data
class ProcessPaymentRequest {
    @NotBlank(message = "Transaction ID is required")
    private String transactionId;
}

@Data
class RefundRequest {
    @NotBlank(message = "Reason is required")
    private String reason;
}

@Data
class TopUpRequest {
    @NotNull
    @Positive
    private BigDecimal amount;
}

@Data
class HireRequest {
    @NotNull
    private Long playerId;
    @NotNull
    @Positive
    private BigDecimal amount;
    private java.time.LocalDateTime startTime;
    private java.time.LocalDateTime endTime;
    private String hireStatus; // ACTIVE, COMPLETED, CANCELED, ...
}

@Data
class WithdrawRequest {
    @NotNull
    @Positive
    private BigDecimal amount;
} 