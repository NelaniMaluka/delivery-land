package com.deliveryland.backend.user.dto;

import com.deliveryland.backend.user.model.AccountStatus;
import com.deliveryland.backend.user.model.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String contactNumber,
        AccountStatus accountStatus
) {
    // Admin/Customer/Driver version (shows everything)
    public static UserResponse user(User user) {
        if (user == null) return null;
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getContactNumber(),
                user.getAccountStatus()
        );
    }

    // Public Customer/Driver version (no email, no account status)
    public static UserResponse orderUser(User user) {
        if (user == null) return null;
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                null,
                user.getContactNumber(),
                null
        );
    }
}