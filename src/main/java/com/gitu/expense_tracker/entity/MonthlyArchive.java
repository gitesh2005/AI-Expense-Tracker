package com.gitu.expense_tracker.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import java.math.BigDecimal;
import java.time.LocalDate;

@Document(collection = "monthly_archive")
@CompoundIndex(name = "owner_archived_date_idx", def = "{ 'ownerId': 1, 'archivedDate': -1 }")
@Data
public class MonthlyArchive {

    @Id
    private String id;

    private String ownerId;

    private String monthLabel;

    private BigDecimal totalAmount;

    private String categoryBreakdown;

    private LocalDate archivedDate;
}
