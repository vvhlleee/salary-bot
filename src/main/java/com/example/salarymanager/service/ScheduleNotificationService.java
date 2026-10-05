package com.example.salarymanager.service;

import com.example.salarymanager.domain.FixedExpense;
import com.example.salarymanager.domain.UserSetting;
import com.example.salarymanager.repository.FixedExpenseRepository;
import com.example.salarymanager.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleNotificationService {

    private final FixedExpenseRepository fixedExpenseRepository;
    private final UserSettingRepository userSettingRepository;
    private final TelegramService telegramService;

    // 매일 아침 9시 0분 0초에 자동 실행
    @Scheduled(cron = "0 0 9 * * *")
    public void checkScheduleAndNotify() {
        // 1. 유저 설정 확인 (알림 안 함 설정 여부 체크)
        UserSetting setting = userSettingRepository.findByUserId(1L).orElse(null);
        if (setting != null && setting.getAlertTime() != null) {
            String alertTime = setting.getAlertTime();
            // 유저가 알림을 받지 않도록 설정한 경우 스킵
            if (alertTime.contains("안 함") || alertTime.contains("없음") || alertTime.equalsIgnoreCase("OFF")) {
                return;
            }
        }

        LocalDate today = LocalDate.now();
        int currentDay = today.getDayOfMonth(); // 오늘 몇일인지 (예: 15, 27일 등)

        // 2. 고정지출 출금일 체크
        List<FixedExpense> fixedExpenses = fixedExpenseRepository.findByUserId(1L);
        for (FixedExpense fe : fixedExpenses) {
            if (fe.getPayDate() == currentDay) {
                String msg = "📌 [고정지출 안내]\n" +
                        "오늘은 **" + fe.getTitle() + "** (" + formatMoney(fe.getAmount()) + ")이 나가는 날입니다!\n" +
                        "통장 잔고를 확인해주세요!";
                telegramService.sendMessage(msg);
            }
        }

        // 3. 월급일 체크 (UserSetting에 저장된 월급일 기준)
        if (setting != null && setting.getPayday() != null && setting.getPayday() == currentDay) {
            String salaryMsg = "💰 [월급일 알림]\n" +
                    "오늘은 기쁜 월급날입니다! 통장 입금 내역을 확인하고 봇으로 잔액을 갱신해주세요! 🎉\n" +
                    "(예정 월급: " + formatMoney(setting.getSalary()) + ")";
            telegramService.sendMessage(salaryMsg);
        }
    }

    // 금액을 "450,000원" 형태로 예쁘게 변환해 주는 헬퍼 메서드
    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0원";
        DecimalFormat df = new DecimalFormat("#,###");
        return df.format(amount) + "원";
    }
}