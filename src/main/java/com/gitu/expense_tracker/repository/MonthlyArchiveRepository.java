package com.gitu.expense_tracker.repository;

import com.gitu.expense_tracker.entity.MonthlyArchive;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MonthlyArchiveRepository extends MongoRepository<MonthlyArchive, String> {
}