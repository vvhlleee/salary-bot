package com.example.salarymanager.service;

import com.example.salarymanager.domain.Account;
import com.example.salarymanager.domain.Expense;
import com.example.salarymanager.domain.FixedExpense;
import com.example.salarymanager.repository.AccountRepository;
import com.example.salarymanager.repository.ExpenseRepository;
import com.example.salarymanager.repository.FixedExpenseRepository;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class BudgetService {

    private final AccountRepository accountRepository;
    private final FixedExpenseRepository fixedExpenseRepository;
    private final ExpenseRepository expenseRepository;

    public BudgetService(AccountRepository accountRepository,
                         FixedExpenseRepository fixedExpenseRepository,
                         ExpenseRepository expenseRepository) {
        this.accountRepository = accountRepository;
        this.fixedExpenseRepository = fixedExpenseRepository;
        this.expenseRepository = expenseRepository;
    }

    public BudgetSummary calculateFinalBudget(Long userId) {
        // 1. 총 잔액 가져오기
        Account account = accountRepository.findByUserId(userId)
                .stream().findFirst().orElse(new Account());
        BigDecimal totalBalance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;

        // 2. 오늘 날짜의 '일(day)' 가져오기 (예: 30일)
        int currentDay = LocalDate.now().getDayOfMonth();

        // 3. 이번 달 남은 고정지출만 계산 (출금일이 오늘 이상인 것만 포함: payDate >= currentDay)
        // 지났거나 이미 처리된 고정지출은 자동으로 제외됩니다!
        List<FixedExpense> fixedExpenses = fixedExpenseRepository.findByUserId(userId);
        BigDecimal totalFixedExpense = BigDecimal.ZERO;
        for (FixedExpense fe : fixedExpenses) {
            if (fe.getPayDate() >= currentDay) {
                totalFixedExpense = totalFixedExpense.add(fe.getAmount());
            }
        }

        // 4. 짤짤이 쓴 돈 합산
        List<Expense> expenses = expenseRepository.findAll();
        BigDecimal totalVariableExpense = BigDecimal.ZERO;
        for (Expense exp : expenses) {
            if (exp.getUserId().equals(userId)) {
                totalVariableExpense = totalVariableExpense.add(exp.getAmount());
            }
        }

        // 5. 진짜 쓸 수 있는 돈 = 총 잔액 - 이번 달 남은 고정지출 - 짤짤이 쓴 돈
        BigDecimal finalAvailableBudget = totalBalance.subtract(totalFixedExpense).subtract(totalVariableExpense);

        return new BudgetSummary(totalBalance, totalFixedExpense, totalVariableExpense, finalAvailableBudget);
    }

    @Getter
    @Setter
    public static class BudgetSummary {
        private BigDecimal totalBalance;
        private BigDecimal totalFixedExpense;
        private BigDecimal totalVariableExpense;
        private BigDecimal finalAvailableBudget;

        public BudgetSummary(BigDecimal totalBalance, BigDecimal totalFixedExpense, BigDecimal totalVariableExpense, BigDecimal finalAvailableBudget) {
            this.totalBalance = totalBalance;
            this.totalFixedExpense = totalFixedExpense;
            this.totalVariableExpense = totalVariableExpense;
            this.finalAvailableBudget = finalAvailableBudget;
        }
    }
}