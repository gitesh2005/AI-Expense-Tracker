package com.gitu.expense_tracker.repository;

import com.gitu.expense_tracker.entity.Expense;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ExpenseRepository extends MongoRepository<Expense, String> {

    Page<Expense> findAllByOwnerId(String ownerId, Pageable pageable);

    Optional<Expense> findByIdAndOwnerId(String id, String ownerId);

    void deleteByIdAndOwnerId(String id, String ownerId);

    void deleteAllByOwnerId(String ownerId);
}
