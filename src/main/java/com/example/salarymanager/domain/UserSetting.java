package com.example.salarymanager.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
public class UserSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private BigDecimal balance;   // 현재 초기 잔액
    private BigDecimal salary;    // 매달 받는 월급
    private Integer payday;       // 월급일
    private String alertTime;     // 알림 시간

    // 연속 입력 중 임시로 저장되는 고정지출 데이터
    private String tempFixedTitle;
    private BigDecimal tempFixedAmount;
    private String tempFixedAmountStr;

    @Enumerated(EnumType.STRING)
    private SetupStep setupStep = SetupStep.DONE;

    public enum SetupStep {
        DONE,
        // /초기설정 연속 단계
        INIT_BALANCE,
        INIT_SALARY,
        INIT_PAYDAY,
        INIT_FIXED_TITLE,
        INIT_FIXED_AMOUNT,
        INIT_FIXED_DATE,
        INIT_ALERT_TIME,

        // /설정변경 개별 선택 단계
        CHANGE_MENU,
        CHANGE_BALANCE,
        CHANGE_SALARY,
        CHANGE_PAYDAY,
        CHANGE_ALERT_TIME,
        ADD_FIXED_TITLE,
        ADD_FIXED_AMOUNT,
        ADD_FIXED_DATE,
        DELETE_FIXED_SELECT
    }
}