package com.gitu.expense_tracker.repository;

import com.gitu.expense_tracker.entity.MonthlyArchive;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyArchiveRepository extends JpaRepository<MonthlyArchive, Integer> {
}