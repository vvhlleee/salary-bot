package com.example.salarymanager.repository;

import com.example.salarymanager.domain.FixedExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FixedExpenseRepository extends JpaRepository<FixedExpense, Long> {
    List<FixedExpense> findByUserId(Long userId);
}