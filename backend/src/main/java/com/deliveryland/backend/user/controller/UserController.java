package com.deliveryland.backend.user.controller;

import com.deliveryland.backend.auth.dto.LoginResponse;
import com.deliveryland.backend.common.dto.ApiResponseDTO;
import com.deliveryland.backend.user.dto.UpdateUserDTO;
import com.deliveryland.backend.user.dto.UserResponse;
import com.deliveryland.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@Validated
@Tag(name = "User Management Controller", description = "Endpoints for authenticated user profile management")
public class UserController {

        private final UserService userService;

        public UserController(UserService userService) {
                this.userService = userService;
        }

        @Operation(summary = "Get current authenticated user", description = "Retrieves the profile information of the currently authenticated user.")
        @ApiResponse(responseCode = "200", description = "Current user retrieved successfully")
        @GetMapping("/me")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponseDTO> getCurrentUser() {
                UserResponse response = userService.getCurrentUser();
                return ResponseEntity.ok(new ApiResponseDTO("Your user data", response));
        }

        @Operation(summary = "Update user details", description = "Updates the profile information of the authenticated user.")
        @ApiResponse(responseCode = "200", description = "User details updated successfully")
        @PutMapping("/update")
        @PreAuthorize("hasAuthority('user:write')")
        public ResponseEntity<ApiResponseDTO> updateUser(
                        @Parameter(description = "User update payload containing editable profile fields") @Valid @RequestBody UpdateUserDTO dto) {
                LoginResponse response = userService.updateUserDetails(dto);
                return ResponseEntity.ok(new ApiResponseDTO("Successfully updated user details", response));
        }

        @Operation(summary = "Delete current user", description = "Permanently deletes the authenticated user's account from the system.")
        @ApiResponse(responseCode = "204", description = "User deleted successfully")
        @DeleteMapping("/delete")
        @PreAuthorize("hasAuthority('user:delete')")
        public ResponseEntity<Void> deleteUser() {
                userService.deleteUser();
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Request email change", description = "Initiates an email change request by sending a verification token to the new email address.")
        @ApiResponse(responseCode = "200", description = "Verification email sent successfully")
        @PostMapping("/email/request")
        @PreAuthorize("hasAuthority('user:write')")
        public ResponseEntity<ApiResponseDTO> updateEmailRequest(
                        @Parameter(description = "New email address for verification", example = "newemail@example.com") @RequestParam String newEmail) {
                userService.changeEmailRequest(newEmail);
                return ResponseEntity.ok(new ApiResponseDTO("Verification email sent successfully"));
        }

        @Operation(summary = "Verify email change", description = "Verifies the provided token and updates the user's email address.")
        @ApiResponse(responseCode = "200", description = "Email updated successfully")
        @PostMapping("/email/verify")
        @PreAuthorize("hasAuthority('user:write')")
        public ResponseEntity<ApiResponseDTO> verifyEmailChangeRequest(
                        @Parameter(description = "Verification token sent to new email", example = "abc123-token") @RequestParam String token) {
                LoginResponse response = userService.verifyChangeEmailRequest(token);
                return ResponseEntity.ok(new ApiResponseDTO("Email updated successfully", response));
        }
}