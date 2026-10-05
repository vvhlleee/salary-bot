package com.example.salarymanager.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Getter @Setter
@NoArgsConstructor
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // 유저 ID

    private String description; // 사용처 (예: 편의점, 치킨)

    private BigDecimal amount; // 지출 금액

    private LocalDateTime expenseDate; // 지출 시간
}