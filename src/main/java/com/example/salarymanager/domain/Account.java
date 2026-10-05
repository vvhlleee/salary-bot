package com.example.salarymanager.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Getter @Setter
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // 어떤 유저의 계좌인지 (나중에 연관관계로 고도화 가능)

    private String bankName; // 은행 이름 (예: 토스뱅크, 카카오뱅크)

    private BigDecimal balance; // 계좌 잔액
}