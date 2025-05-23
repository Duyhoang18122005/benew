package com.example.backend.controller;

import com.example.backend.entity.Payment;
import com.example.backend.service.PaymentService;
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
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Payment", description = "Payment management APIs")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Create a new payment")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Payment> createPayment(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        try {
            Payment payment = paymentService.createPayment(
                    request.getGamePlayerId(),
                    Long.parseLong(authentication.getName()),
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
        return ResponseEntity.ok(paymentService.getUserPayments(
                Long.parseLong(authentication.getName())));
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