package com.deliveryland.backend.user.controller;

import com.deliveryland.backend.common.dto.ApiResponseDTO;
import com.deliveryland.backend.common.security.ApplicationUserRole;
import com.deliveryland.backend.user.dto.UserResponse;
import com.deliveryland.backend.user.model.AccountStatus;
import com.deliveryland.backend.user.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin User Management", description = "Administrative operations for managing users (RBAC protected)")
public class AdminUserController {

        private final AdminUserService adminUserService;

        public AdminUserController(AdminUserService adminUserService) {
                this.adminUserService = adminUserService;
        }

        @Operation(summary = "Get all users (paginated)", description = "Retrieves a paginated list of all users in the system. Supports sorting and paging via Pageable.")
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
        @GetMapping
        public ResponseEntity<ApiResponseDTO> getAllUsers(
                        @Parameter(description = "Pagination and sorting parameters (page, size, sort)") Pageable pageable) {
                Page<UserResponse> users = adminUserService.getAllUsers(pageable);
                return ResponseEntity.ok(new ApiResponseDTO("Users retrieved successfully", users));
        }

        @Operation(summary = "Get users by role", description = "Fetches users filtered by their assigned system role.")
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
        @GetMapping("/role")
        public ResponseEntity<ApiResponseDTO> getByRole(
                        @Parameter(description = "User role filter (e.g. ADMIN, CUSTOMER, DRIVER)") @RequestParam ApplicationUserRole role,

                        @Parameter(description = "Pagination and sorting parameters") Pageable pageable) {
                Page<UserResponse> users = adminUserService.getUsersByRole(role, pageable);
                return ResponseEntity.ok(new ApiResponseDTO("Users retrieved successfully", users));
        }

        @Operation(summary = "Search users", description = "Search users by email, first name, or last name (case-insensitive partial match).")
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
        @GetMapping("/search")
        public ResponseEntity<ApiResponseDTO> searchUsers(

                        @Parameter(description = "Search keyword (email, first name, or last name)", example = "gmail or John") @RequestParam String query,

                        @Parameter(description = "Pagination and sorting parameters") Pageable pageable) {
                Page<UserResponse> users = adminUserService.searchUsers(query, pageable);
                return ResponseEntity.ok(new ApiResponseDTO("Search completed successfully", users));
        }

        @Operation(summary = "Get user by ID", description = "Retrieves detailed information about a specific user by UUID.")
        @ApiResponse(responseCode = "200", description = "User retrieved successfully")
        @GetMapping("/{userId}")
        public ResponseEntity<ApiResponseDTO> getUserById(
                        @Parameter(description = "Unique user UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID userId) {
                UserResponse user = adminUserService.getUserById(userId);
                return ResponseEntity.ok(new ApiResponseDTO("User retrieved successfully", user));
        }

        @Operation(summary = "Update user account status", description = "Allows an admin to activate, suspend, or deactivate a user account.")
        @ApiResponse(responseCode = "200", description = "Account status updated successfully")
        @PutMapping("/{userId}/status")
        public ResponseEntity<ApiResponseDTO> updateStatus(
                        @Parameter(description = "User UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID userId,

                        @Parameter(description = "New account status (ACTIVE, SUSPENDED, INACTIVE)") @RequestParam AccountStatus status) {
                UserResponse user = adminUserService.updateAccountStatus(userId, status);
                return ResponseEntity.ok(new ApiResponseDTO("Account status updated successfully", user));
        }

        @Operation(summary = "Force password reset", description = "Triggers a password reset email for the specified user.")
        @ApiResponse(responseCode = "200", description = "Password reset email sent successfully")
        @PostMapping("/{userId}/force-password-reset")
        public ResponseEntity<ApiResponseDTO> forcePasswordReset(
                        @Parameter(description = "User UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID userId) {
                adminUserService.forcePasswordReset(userId);
                return ResponseEntity.ok(new ApiResponseDTO("Password reset email sent successfully"));
        }

        @Operation(summary = "Delete user", description = "Permanently deletes a user from the system.")
        @ApiResponse(responseCode = "200", description = "User deleted successfully")
        @DeleteMapping("/{userId}")
        public ResponseEntity<ApiResponseDTO> deleteUser(
                        @Parameter(description = "User UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID userId) {
                adminUserService.deleteUser(userId);
                return ResponseEntity.ok(new ApiResponseDTO("User deleted successfully"));
        }
}