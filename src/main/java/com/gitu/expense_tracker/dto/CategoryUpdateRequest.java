package com.gitu.expense_tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(
        @NotBlank(message = "Category is required")
        @Size(max = 30, message = "Category is too long")
        String category
) {
}
