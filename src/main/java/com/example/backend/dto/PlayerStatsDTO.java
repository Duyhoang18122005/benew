package com.example.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PlayerStatsDTO {
    private Long playerId;
    private String playerName;
    private Double averageRating;
    private Integer totalReviews;
    private Integer totalHireHours;
    private Integer completedHires;
    private Integer totalHires;
    private Double completionRate; // Tỷ lệ hoàn thành các lượt thuê
    private BigDecimal totalEarnings;
    private List<ReviewDTO> recentReviews;
    private List<HireStatsDTO> hireStats; // Thống kê theo thời gian (ngày/tháng/năm)
}
