package com.gitu.expense_tracker.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.LocalDate;

@Document(collection = "monthly_archive")
@Data
public class MonthlyArchive {

    @Id
    private String id;

    private String monthLabel;

    private BigDecimal totalAmount;

    private String categoryBreakdown;

    private LocalDate archivedDate;
}