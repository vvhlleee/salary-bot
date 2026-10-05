package com.example.salarymanager.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "fixed_expenses")
@Getter @Setter
@NoArgsConstructor
public class FixedExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // 어떤 유저의 고정지출인지

    private String title; // 항목 이름 (예: 유튜브 프리미엄, 적금, 교통비)

    private BigDecimal amount; // 나가는 금액

    private int payDate; // 출금 날짜 (예: 매달 5일이면 5)
}