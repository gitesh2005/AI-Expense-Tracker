package com.gitu.expense_tracker.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.math.BigDecimal;

@Document(collection = "expenses")
@Data
public class Expense {

    @Id
    private String id;

    private String description;

    private BigDecimal amount;

    private String category;

    private LocalDate expenseDate;
}