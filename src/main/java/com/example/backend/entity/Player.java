package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "players")
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer age;

    @Column(nullable = false)
    private String position;

    @Column(nullable = false)
    private String nationality;

    private String currentClub;
    private BigDecimal marketValue;
    private LocalDate contractExpiry;
    private String preferredFoot;
    private Integer height;
    private Integer weight;
    private String status; // AVAILABLE, HIRED, NOT_AVAILABLE

    @ManyToOne
    @JoinColumn(name = "hired_by")
    private User hiredBy;

    private LocalDate hireDate;
    private LocalDate returnDate;
} 