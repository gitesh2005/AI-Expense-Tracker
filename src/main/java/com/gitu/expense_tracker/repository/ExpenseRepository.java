package com.gitu.expense_tracker.repository;

import com.gitu.expense_tracker.entity.Expense;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExpenseRepository extends MongoRepository<Expense, String> {
}