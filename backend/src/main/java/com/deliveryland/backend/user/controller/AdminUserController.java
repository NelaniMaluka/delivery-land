package com.deliveryland.backend.user.controller;

import com.deliveryland.backend.common.dto.ApiResponseDTO;
import com.deliveryland.backend.common.security.ApplicationUserRole;
import com.deliveryland.backend.user.dto.UserResponse;
import com.deliveryland.backend.user.model.AccountStatus;
import com.deliveryland.backend.user.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Log4j2
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin User Management", description = "Administrative user operations (RBAC protected)")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @Operation(summary = "Get all users", description = "Returns paginated list of all users")
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    @GetMapping
    public ResponseEntity<ApiResponseDTO> getAllUsers(Pageable pageable) {
        Page<UserResponse> users = adminUserService.getAllUsers(pageable);
        return ResponseEntity.ok(new ApiResponseDTO("Users retrieved successfully", users));
    }

    @Operation(summary = "Get users by role", description = "Fetch users filtered by role")
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    @GetMapping("/role")
    public ResponseEntity<ApiResponseDTO> getByRole(
            @Parameter(description = "Role filter")
            @RequestParam ApplicationUserRole role,
            Pageable pageable
    ) {
        Page<UserResponse> users = adminUserService.getUsersByRole(role, pageable);
        return ResponseEntity.ok(new ApiResponseDTO("Users retrieved successfully", users));
    }

    @Operation(summary = "Search users by email")
    @ApiResponse(responseCode = "200", description = "Search completed successfully")
    @GetMapping("/search/email")
    public ResponseEntity<ApiResponseDTO> searchByEmail(
            @Parameter(description = "Email keyword")
            @RequestParam String email,
            Pageable pageable
    ) {
        Page<UserResponse> users = adminUserService.searchByEmail(email, pageable);
        return ResponseEntity.ok(new ApiResponseDTO("Search completed successfully", users));
    }

    @Operation(summary = "Search users by name")
    @ApiResponse(responseCode = "200", description = "Search completed successfully")
    @GetMapping("/search/name")
    public ResponseEntity<ApiResponseDTO> searchByName(
            @Parameter(description = "Name keyword")
            @RequestParam String query,
            Pageable pageable
    ) {
        Page<UserResponse> users = adminUserService.searchByName(query, pageable);
        return ResponseEntity.ok(new ApiResponseDTO("Search completed successfully", users));
    }

    @Operation(summary = "Get user by ID")
    @ApiResponse(responseCode = "200", description = "User retrieved successfully")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponseDTO> getUserById(
            @Parameter(description = "User UUID")
            @PathVariable UUID userId
    ) {
        UserResponse user = adminUserService.getUserById(userId);
        return ResponseEntity.ok(new ApiResponseDTO("User retrieved successfully", user));
    }

    @Operation(summary = "Update account status")
    @ApiResponse(responseCode = "200", description = "Account status updated successfully")
    @PutMapping("/{userId}/status")
    public ResponseEntity<ApiResponseDTO> updateStatus(
            @PathVariable UUID userId,
            @RequestParam AccountStatus status
    ) {
        UserResponse user = adminUserService.updateAccountStatus(userId, status);
        return ResponseEntity.ok(new ApiResponseDTO("Account status updated successfully", user));
    }

    @Operation(summary = "Force password reset")
    @ApiResponse(responseCode = "200", description = "Password reset email sent")
    @PostMapping("/{userId}/force-password-reset")
    public ResponseEntity<ApiResponseDTO> forcePasswordReset(@PathVariable UUID userId) {
        adminUserService.forcePasswordReset(userId);
        return ResponseEntity.ok(new ApiResponseDTO("Password reset email sent successfully"));
    }

    @Operation(summary = "Delete user")
    @ApiResponse(responseCode = "200", description = "User deleted successfully")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponseDTO> deleteUser(@PathVariable UUID userId) {
        adminUserService.deleteUser(userId);
        return ResponseEntity.ok(new ApiResponseDTO("User deleted successfully"));
    }
}