package com.example.backend.controller;

import com.example.backend.entity.Payment;
import com.example.backend.service.PaymentService;
import com.example.backend.service.UserService;
import com.example.backend.entity.User;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.PaymentException;
import com.example.backend.entity.PlayerReview;
import com.example.backend.repository.PlayerReviewRepository;
import com.example.backend.repository.PaymentRepository;
import com.example.backend.service.QRCodeService;
import com.example.backend.dto.ReviewRequest;
import com.example.backend.dto.PlayerStatsDTO;
import com.example.backend.entity.PlayerFollow;
import com.example.backend.repository.PlayerFollowRepository;
import com.example.backend.entity.UserBlock;
import com.example.backend.entity.Notification;
import com.example.backend.repository.UserBlockRepository;
import com.example.backend.repository.NotificationRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Payment", description = "Payment management APIs")
public class PaymentController {
    private final PaymentService paymentService;
    private final UserService userService;
    private final PaymentRepository paymentRepository;
    private final QRCodeService qrCodeService;
    private final PlayerReviewRepository playerReviewRepository;

    public PaymentController(PaymentService paymentService, UserService userService, 
                           PaymentRepository paymentRepository, QRCodeService qrCodeService,
                           PlayerReviewRepository playerReviewRepository) {
        this.paymentService = paymentService;
        this.userService = userService;
        this.paymentRepository = paymentRepository;
        this.qrCodeService = qrCodeService;
        this.playerReviewRepository = playerReviewRepository;
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
    public ResponseEntity<?> hirePlayer(@Valid @RequestBody HireRequest request, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        User player = userService.findById(request.getPlayerId());
        
        // Validate thời gian
        if (request.getStartTime() == null || request.getEndTime() == null) {
            return ResponseEntity.badRequest().body("Thời gian bắt đầu và kết thúc không được để trống");
        }
        if (request.getStartTime().isAfter(request.getEndTime())) {
            return ResponseEntity.badRequest().body("Thời gian bắt đầu phải trước thời gian kết thúc");
        }
        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Thời gian bắt đầu phải sau thời gian hiện tại");
        }

        // Kiểm tra xem player có đang được thuê không
        List<Payment> activeHires = paymentRepository.findByPlayerIdAndHireStatusAndEndTimeAfter(
            request.getPlayerId(), "ACTIVE", LocalDateTime.now());
        if (!activeHires.isEmpty()) {
            return ResponseEntity.badRequest().body("Player đang được thuê trong khoảng thời gian này");
        }

        BigDecimal amount = request.getAmount();
        if (user.getWalletBalance().compareTo(amount) < 0) {
            return ResponseEntity.badRequest().body("Số dư không đủ");
        }

        // Trừ tiền người thuê và cộng tiền cho player
        user.setWalletBalance(user.getWalletBalance().subtract(amount));
        player.setWalletBalance(player.getWalletBalance().add(amount));
        userService.save(user);
        userService.save(player);

        // Tạo bản ghi thanh toán
        Payment payment = new Payment();
        payment.setUser(user);
        payment.setPlayer(player);
        payment.setAmount(amount);
        payment.setCurrency("VND");
        payment.setStatus("COMPLETED");
        payment.setPaymentMethod("HIRE");
        payment.setType("HIRE");
        payment.setCreatedAt(LocalDateTime.now());
        payment.setStartTime(request.getStartTime());
        payment.setEndTime(request.getEndTime());
        payment.setHireStatus("ACTIVE");
        payment = paymentRepository.save(payment);

        return ResponseEntity.ok(Map.of(
            "message", "Thuê player thành công",
            "paymentId", payment.getId()
        ));
    }

    @PostMapping("/hire/{paymentId}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> cancelHire(@PathVariable Long paymentId, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (!payment.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Không có quyền hủy hợp đồng này");
        }

        if (!"ACTIVE".equals(payment.getHireStatus())) {
            return ResponseEntity.badRequest().body("Hợp đồng không ở trạng thái active");
        }

        if (payment.getStartTime().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Không thể hủy hợp đồng đã bắt đầu");
        }

        // Hoàn tiền cho người thuê
        User hirer = payment.getUser();
        hirer.setWalletBalance(hirer.getWalletBalance().add(payment.getAmount()));
        userService.save(hirer);

        // Trừ tiền của player
        User hiredPlayer = payment.getPlayer();
        hiredPlayer.setWalletBalance(hiredPlayer.getWalletBalance().subtract(payment.getAmount()));
        userService.save(hiredPlayer);

        // Cập nhật trạng thái
        payment.setHireStatus("CANCELED");
        paymentRepository.save(payment);

        return ResponseEntity.ok("Hủy hợp đồng thành công");
    }

    @GetMapping("/hire/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getHireHistory(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        List<Payment> hires = paymentRepository.findByUserIdAndTypeOrderByCreatedAtDesc(user.getId(), "HIRE");
        return ResponseEntity.ok(hires);
    }

    @GetMapping("/hire/player/{playerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPlayerHireHistory(@PathVariable Long playerId) {
        List<Payment> hires = paymentRepository.findByPlayerIdAndTypeOrderByCreatedAtDesc(playerId, "HIRE");
        return ResponseEntity.ok(hires);
    }

    @PostMapping("/hire/{paymentId}/review")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> reviewPlayer(
            @PathVariable Long paymentId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        User reviewer = userService.findByUsername(authentication.getName());
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // Kiểm tra quyền đánh giá
        if (!payment.getUser().getId().equals(reviewer.getId())) {
            return ResponseEntity.status(403).body("Không có quyền đánh giá");
        }

        // Kiểm tra thời gian đánh giá
        if (payment.getEndTime().isAfter(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Chưa thể đánh giá, hợp đồng chưa kết thúc");
        }

        // Kiểm tra đã đánh giá chưa
        if (playerReviewRepository.existsByPaymentId(paymentId)) {
            return ResponseEntity.badRequest().body("Đã đánh giá cho hợp đồng này");
        }

        // Tạo đánh giá
        PlayerReview review = new PlayerReview();
        review.setPayment(payment);
        review.setPlayer(payment.getPlayer());
        review.setReviewer(reviewer);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        playerReviewRepository.save(review);

        return ResponseEntity.ok("Đánh giá thành công");
    }

    @GetMapping("/hire/player/{playerId}/reviews")
    public ResponseEntity<?> getPlayerReviews(@PathVariable Long playerId) {
        List<PlayerReview> reviews = playerReviewRepository.findByPlayerId(playerId);
        Double averageRating = playerReviewRepository.getAverageRatingByPlayerId(playerId);
        int reviewCount = reviews.size();
        return ResponseEntity.ok(Map.of(
            "reviews", reviews,
            "averageRating", averageRating != null ? averageRating : 0.0,
            "reviewCount", reviewCount
        ));
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

    @PostMapping("/deposit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deposit(@RequestBody DepositRequest request, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        DepositResponse response = new DepositResponse();

        // Validate số tiền
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.valueOf(10000)) < 0) {
            response.setMessage("Số tiền nạp tối thiểu là 10.000đ");
            return ResponseEntity.badRequest().body(response);
        }

        String method = request.getMethod().toUpperCase();
        String transactionId = "TXN_" + System.currentTimeMillis();
        
        try {
            switch (method) {
                case "MOMO":
                case "VNPAY":
                case "ZALOPAY":
                    String qrCode = qrCodeService.generatePaymentQRCode(
                        method,
                        request.getAmount().toString(),
                        user.getId().toString(),
                        transactionId
                    );
                    response.setQrCode(qrCode);
                    response.setMessage("Quét mã QR bằng ứng dụng " + method + " để thanh toán");
                    break;
                case "BANK_TRANSFER":
                    response.setBankAccount("123456789");
                    response.setBankName("Ngân hàng ABC");
                    response.setBankOwner("CTY TNHH PLAYERDUO");
                    String transferContent = "NAPTIEN_" + user.getId() + "_" + transactionId;
                    response.setTransferContent(transferContent);
                    response.setMessage("Vui lòng chuyển khoản đúng nội dung để được cộng tiền tự động.");
                    break;
                default:
                    response.setMessage("Phương thức thanh toán không hợp lệ!");
                    return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setMessage("Lỗi khi tạo mã QR: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
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
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String hireStatus; // ACTIVE, COMPLETED, CANCELED, ...
}

@Data
class WithdrawRequest {
    @NotNull
    @Positive
    private BigDecimal amount;
}

@Data
class DepositRequest {
    @NotNull
    @Positive
    private BigDecimal amount;
    @NotBlank
    private String method;
}

@Data
class DepositResponse {
    private String qrCode;  // Base64 encoded QR code image
    private String message;
    private String bankAccount;
    private String bankName;
    private String bankOwner;
    private String transferContent;
}