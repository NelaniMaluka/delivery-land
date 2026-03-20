package com.deliveryland.backend.user.dto;

import com.deliveryland.backend.user.model.AccountStatus;
import com.deliveryland.backend.user.model.User;
import com.deliveryland.backend.user.model.UserRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        UUID id,
        String fullName,
        String email,
        String contactNumber,
        UserRole role,
        AccountStatus accountStatus
) {
    // Admin version (shows everything)
    public static UserResponse AdminUser(User user) {
        if (user == null) return null;
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getContactNumber(),
                user.getRole(),
                user.getAccountStatus()
        );
    }

    // Customer/Driver version (no account status)
    public static UserResponse user(User user) {
        if (user == null) return null;
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getContactNumber(),
                user.getRole(),
                null
        );
    }

    // Public version (no ID, no role – for public / list views)
    public static UserResponse publicUser(User user) {
        if (user == null) return null;
        return new UserResponse(
                null,
                user.getFullName(),
                user.getEmail(),
                user.getContactNumber(),
                null,
                null
        );
    }
}