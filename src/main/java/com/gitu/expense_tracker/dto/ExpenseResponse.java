package com.gitu.expense_tracker.dto;

import com.gitu.expense_tracker.entity.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(
        String id,
        String description,
        BigDecimal amount,
        String category,
        LocalDate expenseDate
) {
    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(expense.getId(), expense.getDescription(), expense.getAmount(),
                expense.getCategory(), expense.getExpenseDate());
    }
}
