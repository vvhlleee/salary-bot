package com.example.salarymanager.controller;

import com.example.salarymanager.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    // 예: http://localhost:8080/api/budget/1 로 접속하면 최종 예산 결과가 JSON으로 나옴
    @GetMapping("/api/budget/{userId}")
    public BudgetService.BudgetSummary getBudgetSummary(@PathVariable Long userId) {
        return budgetService.calculateFinalBudget(userId);
    }
}