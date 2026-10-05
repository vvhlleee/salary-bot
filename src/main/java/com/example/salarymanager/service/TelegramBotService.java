package com.example.salarymanager.service;

import com.example.salarymanager.domain.Account;
import com.example.salarymanager.domain.Expense;
import com.example.salarymanager.domain.FixedExpense;
import com.example.salarymanager.domain.UserSetting;
import com.example.salarymanager.repository.AccountRepository;
import com.example.salarymanager.repository.ExpenseRepository;
import com.example.salarymanager.repository.FixedExpenseRepository;
import com.example.salarymanager.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TelegramBotService {

    private final ExpenseRepository expenseRepository;
    private final AccountRepository accountRepository;
    private final FixedExpenseRepository fixedExpenseRepository;
    private final UserSettingRepository userSettingRepository;
    private final TelegramService telegramService;
    private final BudgetService budgetService;

    public void processIncomingMessage(String text) {
        try {
            String trimmedText = text.trim();
            Long userId = 1L;

            UserSetting setting = userSettingRepository.findByUserId(userId)
                    .orElseGet(() -> {
                        UserSetting newSetting = new UserSetting();
                        newSetting.setUserId(userId);
                        newSetting.setSetupStep(UserSetting.SetupStep.DONE);
                        return userSettingRepository.save(newSetting);
                    });

            // 0. 어느 단계에서든 "/초기화"를 치면 즉시 리셋
            if (trimmedText.equals("/초기화")) {
                setting.setSetupStep(UserSetting.SetupStep.INIT_BALANCE);
                setting.setTempFixedTitle(null);
                setting.setTempFixedAmountStr(null);
                userSettingRepository.save(setting);
                telegramService.sendMessage("🔄 **초기화되었습니다.**\n\n현재 통장에 들어있는 **초기 잔액**을 입력해주세요! (예: 450,000 또는 450000)");
                return;
            }

            // 1. 공통 명령어 처리 (/초기설정, /설정변경, /잔액, 취소)
            if (trimmedText.equals("/초기설정")) {
                setting.setSetupStep(UserSetting.SetupStep.INIT_BALANCE);
                userSettingRepository.save(setting);
                telegramService.sendMessage("⚙️ **[초기설정 시작]**\n\n현재 통장에 들어있는 **초기 잔액**을 입력해주세요! (예: 450,000)");
                return;
            }

            if (trimmedText.equals("/설정변경")) {
                setting.setSetupStep(UserSetting.SetupStep.CHANGE_MENU);
                userSettingRepository.save(setting);
                sendChangeMenu();
                return;
            }

            if (trimmedText.equals("/잔액") || trimmedText.equals("잔액")) {
                if (setting.getSetupStep() != UserSetting.SetupStep.DONE) {
                    telegramService.sendMessage("⚠️ 현재 설정 진행 중입니다. 입력을 완료해주세요!");
                    return;
                }
                sendBudgetReport(userId);
                return;
            }

            if (trimmedText.equals("취소") || trimmedText.equals("/취소")) {
                if (setting.getSetupStep() != UserSetting.SetupStep.DONE) {
                    setting.setSetupStep(UserSetting.SetupStep.DONE);
                    userSettingRepository.save(setting);
                    telegramService.sendMessage("❌ 설정이 취소되었습니다.");
                    return;
                }
                handleCancel(userId);
                return;
            }

            // 2. 단계별 대화형 입력 처리 (초기설정 또는 설정변경 중일 때)
            if (setting.getSetupStep() != UserSetting.SetupStep.DONE) {
                handleWizardInput(trimmedText, setting);
                return;
            }

            // 3. 평상시 지출 입력 처리
            handleExpenseInput(trimmedText, userId);

        } catch (Exception e) {
            telegramService.sendMessage("⚠️ 처리 중 오류가 발생했습니다. 형식에 맞게 입력해주세요.");
        }
    }

    // 마법사(위저드) 형태의 대화형 입력 분기 처리
    private void handleWizardInput(String text, UserSetting setting) {
        Long userId = setting.getUserId();
        String cleanedText = text.replaceAll("[^0-9.]", "");

        switch (setting.getSetupStep()) {
            // --- [ /초기설정 연속 단계 ] ---
            case INIT_BALANCE:
                try {
                    BigDecimal balance = new BigDecimal(cleanedText);
                    setting.setBalance(balance);
                    updateAccountBalance(userId, balance);

                    setting.setSetupStep(UserSetting.SetupStep.INIT_SALARY);
                    userSettingRepository.save(setting);
                    telegramService.sendMessage("✅ 초기 잔액 저장 완료!\n\n💵 다음으로 매달 받는 **월급(급여)** 금액을 입력해주세요 (예: 2,000,000)");
                } catch (NumberFormatException e) {
                    telegramService.sendMessage("⚠️ 올바른 숫자를 입력해주세요. (예: 450000)");
                }
                break;

            case INIT_SALARY:
                try {
                    BigDecimal salary = new BigDecimal(cleanedText);
                    setting.setSalary(salary);

                    setting.setSetupStep(UserSetting.SetupStep.INIT_PAYDAY);
                    userSettingRepository.save(setting);
                    telegramService.sendMessage("✅ 월급 저장 완료!\n\n📅 다음으로 **월급일(일)**을 숫자만 입력해주세요 (예: 25)");
                } catch (NumberFormatException e) {
                    telegramService.sendMessage("⚠️ 올바른 월급 숫자를 입력해주세요. (예: 2000000)");
                }
                break;

            case INIT_PAYDAY:
                try {
                    int payday = Integer.parseInt(cleanedText);
                    setting.setPayday(payday);

                    setting.setSetupStep(UserSetting.SetupStep.INIT_FIXED_TITLE);
                    userSettingRepository.save(setting);
                    telegramService.sendMessage("✅ 월급일 저장 완료!\n\n📌 고정지출을 등록합니다.\n고정지출의 **출처(이름)**를 쉼표(,)로 구분해서 입력해주세요\n(예: 청년미래적금, 유튜브 프리미엄)");
                } catch (NumberFormatException e) {
                    telegramService.sendMessage("⚠️ 숫자로만 입력해주세요. (예: 25)");
                }
                break;

            case INIT_FIXED_TITLE:
                setting.setTempFixedTitle(text);
                setting.setSetupStep(UserSetting.SetupStep.INIT_FIXED_AMOUNT);
                userSettingRepository.save(setting);
                telegramService.sendMessage("💰 해당 고정지출들의 **금액**을 쉼표(,)로 구분해서 순서대로 입력해주세요\n(예: 500,000, 10,000)");
                break;

            case INIT_FIXED_AMOUNT:
                setting.setTempFixedAmountStr(text);
                setting.setSetupStep(UserSetting.SetupStep.INIT_FIXED_DATE);
                userSettingRepository.save(setting);
                telegramService.sendMessage("📅 해당 고정지출들이 빠져나가는 **날짜(일)**를 쉼표(,)로 구분해서 순서대로 입력해주세요\n(예: 27, 15)");
                break;

            case INIT_FIXED_DATE:
                try {
                    String[] titles = setting.getTempFixedTitle().split(",");
                    String[] amounts = setting.getTempFixedAmountStr().split(",");
                    String[] dates = text.split(",");

                    if (titles.length != amounts.length || titles.length != dates.length) {
                        telegramService.sendMessage("⚠️ 항목 개수, 금액 개수, 날짜 개수가 서로 일치하지 않습니다!\n다시 입력해주세요. 고정지출 **출처(이름)**부터 다시 입력합니다.");
                        setting.setSetupStep(UserSetting.SetupStep.INIT_FIXED_TITLE);
                        userSettingRepository.save(setting);
                        return;
                    }

                    for (int i = 0; i < titles.length; i++) {
                        String title = titles[i].trim();
                        BigDecimal amount = new BigDecimal(amounts[i].trim().replaceAll("[^0-9.]", ""));
                        int payDate = Integer.parseInt(dates[i].trim().replaceAll("[^0-9]", ""));

                        FixedExpense fe = new FixedExpense();
                        fe.setUserId(userId);
                        fe.setTitle(title);
                        fe.setAmount(amount);
                        fe.setPayDate(payDate);
                        fixedExpenseRepository.save(fe);
                    }

                    setting.setSetupStep(UserSetting.SetupStep.INIT_ALERT_TIME);
                    userSettingRepository.save(setting);
                    telegramService.sendMessage("✅ 모든 고정지출 일괄 등록 완료!\n\n⏰ 마지막으로 매일 하루 쓴 돈을 체크받을 **알림 시간**을 입력해주세요 (예: 22:00 또는 알림 안 함)");
                } catch (Exception e) {
                    telegramService.sendMessage("⚠️ 형식 변환 중 오류가 발생했습니다. 고정지출 출처부터 다시 시작합니다. 이름을 입력해주세요.");
                    setting.setSetupStep(UserSetting.SetupStep.INIT_FIXED_TITLE);
                    userSettingRepository.save(setting);
                }
                break;

            case INIT_ALERT_TIME:
            case CHANGE_ALERT_TIME:
                String alertInput = text.trim();
                setting.setAlertTime(alertInput);
                setting.setSetupStep(UserSetting.SetupStep.DONE);
                userSettingRepository.save(setting);

                if (alertInput.contains("안 함") || alertInput.contains("없음") || alertInput.equalsIgnoreCase("OFF")) {
                    telegramService.sendMessage("🔕 **알림을 받지 않도록 설정되었습니다.**\n모든 설정이 완료되었습니다!");
                } else {
                    telegramService.sendMessage("🎉 **설정이 모두 완료되었습니다!**\n⏰ 매일 " + alertInput + "에 일일 정산 알림이 발송됩니다.");
                }
                break;

            // --- [ /설정변경 개별 메뉴 선택 단계 ] ---
            case CHANGE_MENU:
                if (text.equals("1")) {
                    setting.setSetupStep(UserSetting.SetupStep.CHANGE_BALANCE);
                    userSettingRepository.save(setting);
                    telegramService.sendMessage("💰 변경할 **새로운 초기 잔액**을 입력해주세요.");
                } else if (text.equals("2")) {
                    setting.setSetupStep(UserSetting.SetupStep.CHANGE_SALARY);
                    userSettingRepository.save(setting);
                    telegramService.sendMessage("💵 변경할 **새로운 월급 금액**을 입력해주세요.");
                } else if (text.equals("3")) {
                    setting.setSetupStep(UserSetting.SetupStep.CHANGE_PAYDAY);
                    userSettingRepository.save(setting);
                    telegramService.sendMessage("📅 변경할 **새로운 월급일**을 입력해주세요.");
                } else if (text.equals("4")) {
                    setting.setSetupStep(UserSetting.SetupStep.ADD_FIXED_TITLE);
                    userSettingRepository.save(setting);
                    telegramService.sendMessage("📌 추가할 고정지출의 **출처(이름)**를 입력해주세요.");
                } else if (text.equals("5")) {
                    setting.setSetupStep(UserSetting.SetupStep.DELETE_FIXED_SELECT);
                    userSettingRepository.save(setting);
                    sendFixedExpenseListForDelete(userId);
                } else if (text.equals("6")) {
                    setting.setSetupStep(UserSetting.SetupStep.CHANGE_ALERT_TIME);
                    userSettingRepository.save(setting);
                    telegramService.sendMessage("⏰ 변경할 **알림 시간**을 입력해주세요 (예: 21:30 또는 알림 안 함)");
                } else {
                    telegramService.sendMessage("⚠️ 올바른 번호(1~6)를 선택해주세요.\n취소하려면 '취소'를 입력하세요.");
                }
                break;

            case CHANGE_BALANCE:
                try {
                    BigDecimal newBal = new BigDecimal(cleanedText);
                    setting.setBalance(newBal);
                    updateAccountBalance(userId, newBal);
                    setting.setSetupStep(UserSetting.SetupStep.DONE);
                    userSettingRepository.save(setting);
                    telegramService.sendMessage("✅ 초기 잔액이 성공적으로 변경되었습니다!");
                } catch (NumberFormatException e) {
                    telegramService.sendMessage("⚠️ 올바른 숫자를 입력해주세요.");
                }
                break;

            case CHANGE_SALARY:
                try {
                    BigDecimal newSal = new BigDecimal(cleanedText);
                    setting.setSalary(newSal);
                    setting.setSetupStep(UserSetting.SetupStep.DONE);
                    userSettingRepository.save(setting);
                    telegramService.sendMessage("✅ 월급 금액이 성공적으로 변경되었습니다!");
                } catch (NumberFormatException e) {
                    telegramService.sendMessage("⚠️ 올바른 숫자를 입력해주세요.");
                }
                break;

            case CHANGE_PAYDAY:
                try {
                    int newDay = Integer.parseInt(cleanedText);
                    setting.setPayday(newDay);
                    setting.setSetupStep(UserSetting.SetupStep.DONE);
                    userSettingRepository.save(setting);
                    telegramService.sendMessage("✅ 월급일이 성공적으로 변경되었습니다!");
                } catch (NumberFormatException e) {
                    telegramService.sendMessage("⚠️ 숫자로 입력해주세요.");
                }
                break;

            case ADD_FIXED_TITLE:
                setting.setTempFixedTitle(text);
                setting.setSetupStep(UserSetting.SetupStep.ADD_FIXED_AMOUNT);
                userSettingRepository.save(setting);
                telegramService.sendMessage("💰 추가할 고정지출의 **금액**을 입력해주세요.");
                break;

            case ADD_FIXED_AMOUNT:
                try {
                    BigDecimal amount = new BigDecimal(cleanedText);
                    setting.setTempFixedAmount(amount);
                    setting.setSetupStep(UserSetting.SetupStep.INIT_FIXED_DATE);
                    userSettingRepository.save(setting);
                    telegramService.sendMessage("📅 추가할 고정지출의 **출금일(일)**을 입력해주세요.");
                } catch (NumberFormatException e) {
                    telegramService.sendMessage("⚠️ 올바른 금액 숫자를 입력해주세요.");
                }
                break;

            case DELETE_FIXED_SELECT:
                try {
                    Long targetId = Long.parseLong(cleanedText);
                    Optional<FixedExpense> targetFe = fixedExpenseRepository.findById(targetId);
                    if (targetFe.isPresent() && targetFe.get().getUserId().equals(userId)) {
                        fixedExpenseRepository.delete(targetFe.get());
                        setting.setSetupStep(UserSetting.SetupStep.DONE);
                        userSettingRepository.save(setting);
                        telegramService.sendMessage("🗑️ 선택하신 고정지출이 **완전히 삭제**되었습니다!");
                    } else {
                        telegramService.sendMessage("⚠️ 존재하지 않거나 잘못된 번호입니다.");
                    }
                } catch (NumberFormatException e) {
                    telegramService.sendMessage("⚠️ 삭제할 항목의 번호를 숫자로 입력해주세요.");
                }
                break;

            default:
                break;
        }
    }

    private void sendChangeMenu() {
        String menu = "⚙️ **[설정 변경 메뉴]**\n" +
                "원하시는 항목의 번호를 입력해주세요:\n\n" +
                "1️⃣ 초기 잔액 변경\n" +
                "2️⃣ 월급 금액 변경\n" +
                "3️⃣ 월급일 변경\n" +
                "4️⃣ 새로운 고정지출 추가\n" +
                "5️⃣ 고정지출 삭제\n" +
                "6️⃣ 알림 시간 변경\n\n" +
                "*(도중에 그만두려면 '취소'를 입력하세요)*";
        telegramService.sendMessage(menu);
    }

    private void sendFixedExpenseListForDelete(Long userId) {
        List<FixedExpense> list = fixedExpenseRepository.findByUserId(userId);
        if (list.isEmpty()) {
            UserSetting setting = userSettingRepository.findByUserId(userId).get();
            setting.setSetupStep(UserSetting.SetupStep.DONE);
            userSettingRepository.save(setting);
            telegramService.sendMessage("⚠️ 삭제할 고정지출 내역이 없습니다.");
            return;
        }

        StringBuilder sb = new StringBuilder("🗑️ **[삭제할 고정지출 선택]**\n삭제할 항목의 **번호**를 입력해주세요:\n\n");
        for (FixedExpense fe : list) {
            sb.append("🆔 **").append(fe.getId()).append("** | ")
                    .append(fe.getTitle()).append(" / ")
                    .append(formatMoney(fe.getAmount())).append(" / 매월 ")
                    .append(fe.getPayDate()).append("일\n");
        }
        telegramService.sendMessage(sb.toString());
    }

    private void updateAccountBalance(Long userId, BigDecimal balance) {
        Account account = accountRepository.findByUserId(userId).stream().findFirst().orElse(new Account());
        account.setUserId(userId);
        account.setBankName("내 통장");
        account.setBalance(balance);
        accountRepository.save(account);
    }

    private void handleExpenseInput(String text, Long userId) {
        String[] parts = text.split("\\s+");
        String description = "기타 지출";
        BigDecimal amount;

        if (parts.length == 2) {
            description = parts[0];
            amount = new BigDecimal(parts[1].replaceAll("[^0-9.]", ""));
        } else if (parts.length == 1) {
            amount = new BigDecimal(parts[0].replaceAll("[^0-9.]", ""));
        } else {
            telegramService.sendMessage("⚠️ 입력 형식 오류!\n- 지출 입력: [편의점 1500] 또는 [20000]\n- 잔액 확인: [/잔액]\n- 설정 변경: [/설정변경]");
            return;
        }

        Expense expense = new Expense();
        expense.setUserId(userId);
        expense.setDescription(description);
        expense.setAmount(amount);
        expense.setExpenseDate(LocalDateTime.now());
        expenseRepository.save(expense);

        BudgetService.BudgetSummary summary = budgetService.calculateFinalBudget(userId);
        telegramService.sendMessage("✅ 지출 반영 완료!\n" +
                "- " + description + " " + formatMoney(amount) + " 지출 처리됨.\n" +
                "✨ 남은 가용 예산: " + formatMoney(summary.getFinalAvailableBudget()));
    }

    private void handleCancel(Long userId) {
        Optional<Expense> latestExpense = expenseRepository.findTopByUserIdOrderByExpenseDateDesc(userId);
        if (latestExpense.isPresent()) {
            Expense expense = latestExpense.get();
            expenseRepository.delete(expense);

            BudgetService.BudgetSummary summary = budgetService.calculateFinalBudget(userId);
            telegramService.sendMessage("❌ **최근 지출 취소 완료!**\n" +
                    "- 삭제된 내역: " + expense.getDescription() + " (" + formatMoney(expense.getAmount()) + ")\n" +
                    "✨ 복구된 남은 가용 예산: " + formatMoney(summary.getFinalAvailableBudget()));
        } else {
            telegramService.sendMessage("⚠️ 취소할 지출 내역이 없습니다.");
        }
    }

    private void sendBudgetReport(Long userId) {
        BudgetService.BudgetSummary summary = budgetService.calculateFinalBudget(userId);

        String report = "📊 [내 지갑 현황 보고]\n" +
                "────────────────\n" +
                "💰 총 잔액: " + formatMoney(summary.getTotalBalance()) + "\n" +
                "📌 고정지출 빠질 돈: " + formatMoney(summary.getTotalFixedExpense()) + "\n" +
                "💸 지출한돈: " + formatMoney(summary.getTotalVariableExpense()) + "\n" +
                "────────────────\n" +
                "✨ **지금 진짜 쓸 수 있는 돈**: " + formatMoney(summary.getFinalAvailableBudget());

        telegramService.sendMessage(report);
    }

    // 숫자를 "450,000원" 형태로 예쁘게 변환해 주는 헬퍼 메서드
    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0원";
        DecimalFormat df = new DecimalFormat("#,###");
        return df.format(amount) + "원";
    }
}