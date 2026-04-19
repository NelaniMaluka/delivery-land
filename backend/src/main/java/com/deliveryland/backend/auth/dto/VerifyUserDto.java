package com.deliveryland.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyUserDto {
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email should be valid")
        String email;

        @NotBlank(message = "Token must not be blank")
        @Pattern(regexp = "^[A-Za-z0-9]{8}$", message = "Token must be exactly 8 alphanumeric characters")
        String token;
}
