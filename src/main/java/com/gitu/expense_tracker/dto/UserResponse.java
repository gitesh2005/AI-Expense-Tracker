package com.gitu.expense_tracker.dto;

import com.gitu.expense_tracker.entity.User;

public record UserResponse(String id, String email) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail());
    }
}
