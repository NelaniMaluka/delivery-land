package com.deliveryland.backend.auth;

import com.deliveryland.backend.auth.dto.LoginResponse;
import com.deliveryland.backend.auth.dto.UserCreateDTO;
import com.deliveryland.backend.auth.dto.UserLoginDTO;
import com.deliveryland.backend.auth.dto.VerifyUserDto;
import com.deliveryland.backend.common.dto.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api")
@Tag(name = "Authentication Controller", description = "Endpoints for user registration and login")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register a new user account", description = "Creates a user and sends a verification email.")
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @PostMapping("/public/auth/register")
    public ResponseEntity<ApiResponseDTO> register(@RequestBody @Valid UserCreateDTO dto) {
        authService.register(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponseDTO("We have sent a verification email. Please verify your email."));
    }

    @Operation(summary = "Authenticate user and generate token", description = "Validates the provided credentials and returns a JWT token upon successful login.")
    @ApiResponse(responseCode = "200", description = "Login successful, token returned")
    @PostMapping("/public/auth/login")
    public ResponseEntity<ApiResponseDTO> login(@RequestBody @Valid UserLoginDTO dto) {
        LoginResponse response = authService.logIn(dto);

        return ResponseEntity.ok(new ApiResponseDTO("Login successful", response));
    }

    @Operation(summary = "Log out the authenticated user", description = "Logs out the currently authenticated user by clearing the security context "
            +
            "and invalidating the session. Returns a success message upon completion.")
    @ApiResponse(responseCode = "200", description = "User successfully logged out")
    @PostMapping("/user/auth/log-out")
    public ResponseEntity<ApiResponseDTO> logOut(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("No Authorization header provided or token is missing.");
        }

        return ResponseEntity.ok(new ApiResponseDTO("Logged out successfully"));
    }

    @Operation(summary = "Verify user account", description = "Verifies a user's account using the provided verification code.")
    @ApiResponse(responseCode = "200", description = "Account successfully verified")
    @PostMapping("/public/auth/verify")
    public ResponseEntity<ApiResponseDTO> verifyUser(@Valid @RequestBody VerifyUserDto dto) {
        LoginResponse response = authService.verifyUser(dto);
        return ResponseEntity.ok(new ApiResponseDTO("Account successfully verified", response));
    }

    @Operation(summary = "Resend verification code", description = "Sends a new verification code to the specified email address.")
    @ApiResponse(responseCode = "200", description = "Verification code resent successfully")
    @PostMapping("/public/auth/resend-verification")
    public ResponseEntity<ApiResponseDTO> resetVerification(
            @RequestParam @NotBlank(message = "Email must not be blank")
            @Email(message = "Email should be valid") String email) {
        authService.resendVerificationCode(email);
        return ResponseEntity.ok(new ApiResponseDTO("Successfully sent a new verification code.", email));
    }
}
