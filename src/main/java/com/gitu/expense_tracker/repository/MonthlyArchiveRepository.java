package com.gitu.expense_tracker.repository;

import com.gitu.expense_tracker.entity.MonthlyArchive;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MonthlyArchiveRepository extends MongoRepository<MonthlyArchive, String> {

    Page<MonthlyArchive> findAllByOwnerId(String ownerId, Pageable pageable);

    void deleteByIdAndOwnerId(String id, String ownerId);
}
