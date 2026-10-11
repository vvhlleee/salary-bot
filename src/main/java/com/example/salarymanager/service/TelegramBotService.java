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

    public void processIncomingMessage(Long chatId, String text) {
        try {
            String trimmedText = text.trim();
            Long userId = chatId;

            UserSetting setting = userSettingRepository.findByUserId(userId)
                    .orElseGet(() -> {
                        UserSetting newSetting = new UserSetting();
                        newSetting.setUserId(userId);
                        newSetting.setSetupStep(UserSetting.SetupStep.DONE);
                        return userSettingRepository.save(newSetting);
                    });

            // 0. 초기화 명령어
            if (trimmedText.equals("/초기화")) {
                setting.setSetupStep(UserSetting.SetupStep.INIT_BALANCE);
                setting.setTempFixedTitle(null);
                setting.setTempFixedAmountStr(null);
                userSettingRepository.save(setting);
                telegramService.sendMessage(userId, "🔄 **초기화되었습니다.**\n\n현재 통장에 들어있는 **초기 잔액**을 입력해주세요! (예: 450000)");
                return;
            }

            // 1. 공통 명령어 처리 (/초기설정, /설정변경, /잔액, /입금, /출금, /지출)
            if (trimmedText.equals("/초기설정")) {
                setting.setSetupStep(UserSetting.SetupStep.INIT_BALANCE);
                userSettingRepository.save(setting);
                telegramService.sendMessage(userId, "⚙️ **[초기설정 시작]**\n\n현재 통장에 들어있는 **초기 잔액**을 입력해주세요! (예: 450000)");
                return;
            }

            if (trimmedText.equals("/설정변경")) {
                setting.setSetupStep(UserSetting.SetupStep.CHANGE_MENU);
                userSettingRepository.save(setting);
                sendChangeMenu(userId);
                return;
            }

            if (trimmedText.equals("/잔액") || trimmedText.equals("잔액")) {
                if (setting.getSetupStep() != UserSetting.SetupStep.DONE) {
                    telegramService.sendMessage(userId, "⚠️ 현재 설정 진행 중입니다. 입력을 완료해주세요!");
                    return;
                }
                sendBudgetReport(userId);
                return;
            }

            if (trimmedText.equals("/입금")) {
                if (setting.getSetupStep() != UserSetting.SetupStep.DONE) {
                    telegramService.sendMessage(userId, "⚠️ 현재 설정 진행 중입니다. 입력을 완료해주세요!");
                    return;
                }
                setting.setSetupStep(UserSetting.SetupStep.WAITING_FOR_DEPOSIT);
                userSettingRepository.save(setting);
                telegramService.sendMessage(userId, "💰 얼마를 입금하시겠습니까? 금액을 숫자로 입력해주세요.");
                return;
            }

            // /출금 또는 /지출 모두 지출 단계로 연결
            if (trimmedText.equals("/출금") || trimmedText.equals("/지출")) {
                if (setting.getSetupStep() != UserSetting.SetupStep.DONE) {
                    telegramService.sendMessage(userId, "⚠️ 현재 설정 진행 중입니다. 입력을 완료해주세요!");
                    return;
                }
                setting.setSetupStep(UserSetting.SetupStep.WAITING_FOR_EXPENSE);
                userSettingRepository.save(setting);
                telegramService.sendMessage(userId, "💸 얼마를 지출(출금)하시겠습니까? 금액을 숫자로 입력해주세요.");
                return;
            }

            if (trimmedText.equals("취소") || trimmedText.equals("/취소")) {
                if (setting.getSetupStep() != UserSetting.SetupStep.DONE) {
                    setting.setSetupStep(UserSetting.SetupStep.DONE);
                    userSettingRepository.save(setting);
                    telegramService.sendMessage(userId, "❌ 설정이 취소되었습니다.");
                    return;
                }
                if (setting.getSetupStep() == UserSetting.SetupStep.WAITING_FOR_DEPOSIT ||
                        setting.getSetupStep() == UserSetting.SetupStep.WAITING_FOR_EXPENSE) {
                    setting.setSetupStep(UserSetting.SetupStep.DONE);
                    userSettingRepository.save(setting);
                    telegramService.sendMessage(userId, "❌ 입출금 입력이 취소되었습니다.");
                    return;
                }
                handleCancel(userId);
                return;
            }

            // 2. 단계별 대화형 입력 처리
            if (setting.getSetupStep() != UserSetting.SetupStep.DONE) {
                handleWizardInput(trimmedText, setting);
                return;
            }

            // 3. 평상시 지출 입력 처리
            handleExpenseInput(trimmedText, userId);

        } catch (Exception e) {
            telegramService.sendMessage(chatId, "⚠️ 처리 중 오류가 발생했습니다. 형식에 맞게 입력해주세요.");
        }
    }

    private void handleWizardInput(String text, UserSetting setting) {
        Long userId = setting.getUserId();
        String cleanedText = text.replaceAll("[^0-9.]", "");

        switch (setting.getSetupStep()) {
            case WAITING_FOR_DEPOSIT:
                try {
                    BigDecimal amount = new BigDecimal(cleanedText);
                    Account account = accountRepository.findByUserId(userId).stream().findFirst().orElse(new Account());
                    BigDecimal currentBalance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
                    BigDecimal newBalance = currentBalance.add(amount);

                    updateAccountBalance(userId, newBalance);

                    setting.setSetupStep(UserSetting.SetupStep.DONE);
                    userSettingRepository.save(setting);

                    BudgetService.BudgetSummary summary = budgetService.calculateFinalBudget(userId);
                    telegramService.sendMessage(userId, String.format("✅ 입금이 완료되었습니다!\n입금액: %s\n✨ 현재 남은 가용 예산: %s",
                            formatMoney(amount), formatMoney(summary.getFinalAvailableBudget())));
                } catch (NumberFormatException e) {
                    telegramService.sendMessage(userId, "⚠️ 올바른 숫자 형태만 입력해주세요. 다시 입력해 주세요.");
                }
                break;

            case WAITING_FOR_EXPENSE:
                try {
                    BigDecimal amount = new BigDecimal(cleanedText);

                    Expense expense = new Expense();
                    expense.setUserId(userId);
                    expense.setDescription("기타 지출");
                    expense.setAmount(amount);
                    expense.setExpenseDate(LocalDateTime.now());
                    expenseRepository.save(expense);

                    setting.setSetupStep(UserSetting.SetupStep.DONE);
                    userSettingRepository.save(setting);

                    BudgetService.BudgetSummary summary = budgetService.calculateFinalBudget(userId);
                    telegramService.sendMessage(userId, String.format("✅ 지출이 반영되었습니다!\n지출액: %s\n✨ 현재 남은 가용 예산: %s",
                            formatMoney(amount), formatMoney(summary.getFinalAvailableBudget())));
                } catch (NumberFormatException e) {
                    telegramService.sendMessage(userId, "⚠️ 올바른 숫자 형태만 입력해주세요. 다시 입력해 주세요.");
                }
                break;

            case INIT_BALANCE:
                try {
                    BigDecimal balance = new BigDecimal(cleanedText);
                    setting.setBalance(balance);
                    updateAccountBalance(userId, balance);

                    setting.setSetupStep(UserSetting.SetupStep.INIT_SALARY);
                    userSettingRepository.save(setting);
                    telegramService.sendMessage(userId, "✅ 초기 잔액 저장 완료!\n\n💵 다음으로 매달 받는 **월급(급여)** 금액을 입력해주세요 (예: 2000000)");
                } catch (NumberFormatException e) {
                    telegramService.sendMessage(userId, "⚠️ 올바른 숫자를 입력해주세요. (예: 450000)");
                }
                break;

            case INIT_SALARY:
                try {
                    BigDecimal salary = new BigDecimal(cleanedText);
                    setting.setSalary(salary);

                    setting.setSetupStep(UserSetting.SetupStep.INIT_PAYDAY);
                    userSettingRepository.save(setting);
                    telegramService.sendMessage(userId, "✅ 월급 저장 완료!\n\n📅 다음으로 **월급일(일)**을 숫자만 입력해주세요 (예: 25)");
                } catch (NumberFormatException e) {
                    telegramService.sendMessage(userId, "⚠️ 올바른 월급 숫자를 입력해주세요. (예: 2000000)");
                }
                break;

            case INIT_PAYDAY:
                try {
                    int payday = Integer.parseInt(cleanedText);
                    setting.setPayday(payday);

                    setting.setSetupStep(UserSetting.SetupStep.INIT_FIXED_TITLE);
                    userSettingRepository.save(setting);
                    telegramService.sendMessage(userId, "✅ 월급일 저장 완료!\n\n📌 고정지출을 등록합니다.\n고정지출의 **출처(이름)**를 쉼표(,)로 구분해서 입력해주세요\n(예: 청년미래적금, 유튜브)");
                } catch (NumberFormatException e) {
                    telegramService.sendMessage(userId, "⚠️ 숫자로만 입력해주세요. (예: 25)");
                }
                break;

            case INIT_FIXED_TITLE:
                setting.setTempFixedTitle(text);
                setting.setSetupStep(UserSetting.SetupStep.INIT_FIXED_AMOUNT);
                userSettingRepository.save(setting);
                telegramService.sendMessage(userId, "💰 해당 고정지출들의 **금액**을 쉼표(,)로 구분해서 순서대로 입력해주세요\n(예: 500000, 10000)");
                break;

            case INIT_FIXED_AMOUNT:
                setting.setTempFixedAmountStr(text);
                setting.setSetupStep(UserSetting.SetupStep.INIT_FIXED_DATE);
                userSettingRepository.save(setting);
                telegramService.sendMessage(userId, "📅 해당 고정지출들이 빠져나가는 **날짜(일)**를 쉼표(,)로 구분해서 순서대로 입력해주세요\n(예: 27, 15)");
                break;

            case INIT_FIXED_DATE:
                try {
                    String[] titles = setting.getTempFixedTitle().split(",");
                    String[] amounts = setting.getTempFixedAmountStr().split(",");
                    String[] dates = text.split(",");

                    if (titles.length != amounts.length || titles.length != dates.length) {
                        telegramService.sendMessage(userId, "⚠️ 개수가 일치하지 않습니다! 출처(이름)부터 다시 입력해주세요.");
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
                    telegramService.sendMessage(userId, "✅ 고정지출 등록 완료!\n\n⏰ 마지막으로 알림 받을 시간을 입력해주세요 (예: 22:00 또는 알림 안 함)");
                } catch (Exception e) {
                    telegramService.sendMessage(userId, "⚠️ 형식 오류입니다. 고정지출 이름을 다시 입력해주세요.");
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
                telegramService.sendMessage(userId, "🎉 **설정이 모두 완료되었습니다!**");
                break;

            default:
                setting.setSetupStep(UserSetting.SetupStep.DONE);
                userSettingRepository.save(setting);
                break;
        }
    }

    private void sendChangeMenu(Long userId) {
        String menu = "⚙️ **[설정 변경 메뉴]**\n1️⃣ 초기 잔액 변경\n2️⃣ 월급 변경\n3️⃣ 월급일 변경\n4️⃣ 고정지출 추가\n5️⃣ 고정지출 삭제\n6️⃣ 알림 시간 변경";
        telegramService.sendMessage(userId, menu);
    }

    private void sendBudgetReport(Long userId) {
        BudgetService.BudgetSummary summary = budgetService.calculateFinalBudget(userId);
        String report = "📊 [내 지갑 현황]\n" +
                "💰 총 잔액: " + formatMoney(summary.getTotalBalance()) + "\n" +
                "📌 고정지출: " + formatMoney(summary.getTotalFixedExpense()) + "\n" +
                "💸 지출: " + formatMoney(summary.getTotalVariableExpense()) + "\n" +
                "✨ **가용 예산**: " + formatMoney(summary.getFinalAvailableBudget());
        telegramService.sendMessage(userId, report);
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
            telegramService.sendMessage(userId, "⚠️ 형식 오류! [편의점 1500] 또는 [1500] 형태로 입력하세요.");
            return;
        }

        Expense expense = new Expense();
        expense.setUserId(userId);
        expense.setDescription(description);
        expense.setAmount(amount);
        expense.setExpenseDate(LocalDateTime.now());
        expenseRepository.save(expense);

        BudgetService.BudgetSummary summary = budgetService.calculateFinalBudget(userId);
        telegramService.sendMessage(userId, "✅ 지출 반영 완료!\n- " + description + " " + formatMoney(amount) + "\n✨ 남은 예산: " + formatMoney(summary.getFinalAvailableBudget()));
    }

    private void handleCancel(Long userId) {
        Optional<Expense> latestExpense = expenseRepository.findTopByUserIdOrderByExpenseDateDesc(userId);
        if (latestExpense.isPresent()) {
            expenseRepository.delete(latestExpense.get());
            BudgetService.BudgetSummary summary = budgetService.calculateFinalBudget(userId);
            telegramService.sendMessage(userId, "❌ 직전 지출이 취소되었습니다.\n✨ 남은 예산: " + formatMoney(summary.getFinalAvailableBudget()));
        } else {
            telegramService.sendMessage(userId, "⚠️ 취소할 내역이 없습니다.");
        }
    }

    private void updateAccountBalance(Long userId, BigDecimal balance) {
        Account account = accountRepository.findByUserId(userId).stream().findFirst().orElse(new Account());
        account.setUserId(userId);
        account.setBankName("내 통장 잔액");
        account.setBalance(balance);
        accountRepository.save(account);
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0원";
        DecimalFormat df = new DecimalFormat("#,###");
        return df.format(amount) + "원";
    }
}