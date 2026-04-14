package com.deliveryland.backend.user.dto;

import com.deliveryland.backend.user.model.AccountStatus;
import com.deliveryland.backend.user.model.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Represents a user response returned by the system")
public record UserResponse(

        @Schema(description = "Unique identifier of the user", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "User's first name", example = "Nelani")
        String firstName,

        @Schema(description = "User's last name", example = "Maluka")
        String lastName,

        @Schema(description = "User's email address (only visible in full profile responses)", example = "nelani@example.com")
        String email,

        @Schema(description = "User's contact number", example = "+27831234567")
        String contactNumber,

        @Schema(description = "Current account status of the user", example = "ACTIVE")
        AccountStatus accountStatus
) {


     // Full user response (Admin/Customer/Driver view)
     // Includes all fields
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


     // Limited user response (Public view for orders)
     // Hides sensitive fields like email and account status
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