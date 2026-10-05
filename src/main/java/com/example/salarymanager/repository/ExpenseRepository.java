package com.example.salarymanager.repository;

import com.example.salarymanager.domain.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // 유저 ID를 기준으로 가장 최근에 등록된 지출 1개를 찾아오는 쿼리
    Optional<Expense> findTopByUserIdOrderByExpenseDateDesc(Long userId);
}