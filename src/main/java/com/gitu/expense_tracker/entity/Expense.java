package com.gitu.expense_tracker.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import java.time.LocalDate;
import java.math.BigDecimal;

@Document(collection = "expenses")
@CompoundIndex(name = "owner_expense_date_idx", def = "{ 'ownerId': 1, 'expenseDate': -1 }")
@Data
public class Expense {

    @Id
    private String id;

    private String ownerId;

    private String description;

    private BigDecimal amount;

    private String category;

    private LocalDate expenseDate;
}
