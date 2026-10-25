package com.gitu.expense_tracker;

import com.gitu.expense_tracker.dto.ExpenseRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpenseRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validator = null;
    }

    @Test
    void acceptsAValidExpenseRequest() {
        ExpenseRequest request = new ExpenseRequest("Lunch", new BigDecimal("350.00"), null);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsMissingDescriptionAndInvalidAmount() {
        ExpenseRequest request = new ExpenseRequest("", new BigDecimal("0.001"), null);

        var violations = validator.validate(request);

        assertTrue(violations.size() >= 2);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("description")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
    }
}
