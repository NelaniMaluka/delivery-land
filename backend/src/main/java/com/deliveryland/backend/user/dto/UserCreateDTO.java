package com.deliveryland.backend.user.dto;

import com.deliveryland.backend.user.model.UserRole;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateDTO {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "\\+?[0-9]{10,15}", message = "Contact number must be valid")
    private String contactNumber;

    private UserRole role;

}