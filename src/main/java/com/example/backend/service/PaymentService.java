package com.example.backend.service;

import com.example.backend.entity.Payment;
import com.example.backend.entity.GamePlayer;
import com.example.backend.entity.User;
import com.example.backend.repository.PaymentRepository;
import com.example.backend.repository.GamePlayerRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.PaymentException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final UserRepository userRepository;

    public PaymentService(PaymentRepository paymentRepository,
                         GamePlayerRepository gamePlayerRepository,
                         UserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.gamePlayerRepository = gamePlayerRepository;
        this.userRepository = userRepository;
    }

    public Payment createPayment(Long gamePlayerId, Long userId, BigDecimal amount,
                               String currency, String paymentMethod) {
        GamePlayer gamePlayer = gamePlayerRepository.findById(gamePlayerId)
                .orElseThrow(() -> new ResourceNotFoundException("Game player not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validateAmount(amount);

        Payment payment = new Payment();
        payment.setGamePlayer(gamePlayer);
        payment.setUser(user);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus("PENDING");
        payment.setCreatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    public Payment processPayment(Long paymentId, String transactionId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (!"PENDING".equals(payment.getStatus())) {
            throw new PaymentException("Payment is not in pending status");
        }

        payment.setTransactionId(transactionId);
        payment.setStatus("COMPLETED");
        payment.setCompletedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    public Payment refundPayment(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (!"COMPLETED".equals(payment.getStatus())) {
            throw new PaymentException("Payment is not completed");
        }

        payment.setStatus("REFUNDED");
        payment.setDescription(reason);

        return paymentRepository.save(payment);
    }

    public List<Payment> getUserPayments(Long userId) {
        return paymentRepository.findByUserId(userId);
    }

    public List<Payment> getGamePlayerPayments(Long gamePlayerId) {
        return paymentRepository.findByGamePlayerId(gamePlayerId);
    }

    public List<Payment> getPaymentsByStatus(String status) {
        validateStatus(status);
        return paymentRepository.findByStatus(status);
    }

    public List<Payment> getPaymentsByDateRange(LocalDateTime start, LocalDateTime end) {
        return paymentRepository.findByCreatedAtBetween(start, end);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("Amount must be greater than 0");
        }
    }

    private void validateStatus(String status) {
        if (!Arrays.asList("PENDING", "COMPLETED", "FAILED", "REFUNDED").contains(status)) {
            throw new IllegalArgumentException("Invalid payment status");
        }
    }
} 