package com.example.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class HireStatsDTO {
    private String period; // Ngày/tháng/năm
    private Integer totalHires;
    private Integer completedHires;
    private Integer totalHours;
    private BigDecimal earnings;
} 