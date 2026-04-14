package com.deliveryland.backend.user.controller;

import com.deliveryland.backend.common.security.ApplicationUserRole;
import com.deliveryland.backend.common.security.CustomUserDetailsService;
import com.deliveryland.backend.common.security.JwtAuthenticationFilter;
import com.deliveryland.backend.common.security.JwtService;
import com.deliveryland.backend.user.dto.UserResponse;
import com.deliveryland.backend.user.model.AccountStatus;
import com.deliveryland.backend.user.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldReturnUsers_whenGetAllUsers() throws Exception {
        // Arrange
        UserResponse user = createUserResponse();
        when(adminUserService.getAllUsers(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        // Act & Assert
        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Users retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].email").value(user.email()));
    }

    @Test
    void shouldReturnUsers_whenGetUsersByRole_givenRole() throws Exception {
        // Arrange
        UserResponse user = createUserResponse();
        when(adminUserService.getUsersByRole(any(ApplicationUserRole.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        // Act & Assert
        mockMvc.perform(get("/api/admin/users/role")
                        .param("role", "CUSTOMER")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Users retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].email").value(user.email()));
    }

    @Test
    void shouldReturnUsers_whenSearchByEmail_givenQuery() throws Exception {
        // Arrange
        UserResponse user = createUserResponse();
        when(adminUserService.searchByEmail(any(String.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        // Act & Assert
        mockMvc.perform(get("/api/admin/users/search/email")
                        .param("email", "user")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Search completed successfully"))
                .andExpect(jsonPath("$.data.content[0].email").value(user.email()));
    }

    @Test
    void shouldReturnUsers_whenSearchByName_givenQuery() throws Exception {
        // Arrange
        UserResponse user = createUserResponse();
        when(adminUserService.searchByName(any(String.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        // Act & Assert
        mockMvc.perform(get("/api/admin/users/search/name")
                        .param("query", "first")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Search completed successfully"))
                .andExpect(jsonPath("$.data.content[0].email").value(user.email()));
    }

    @Test
    void shouldReturnUser_whenGetUserById_givenValidId() throws Exception {
        // Arrange
        UserResponse user = createUserResponse();
        when(adminUserService.getUserById(user.id()))
                .thenReturn(user);

        // Act & Assert
        mockMvc.perform(get("/api/admin/users/{userId}", user.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(user.id().toString()))
                .andExpect(jsonPath("$.data.email").value(user.email()));
    }

    @Test
    void shouldReturnUpdatedUser_whenUpdateStatus_givenValidRequest() throws Exception {
        // Arrange
        UserResponse user = createUserResponse();
        when(adminUserService.updateAccountStatus(user.id(), AccountStatus.SUSPENDED))
                .thenReturn(user);

        // Act & Assert
        mockMvc.perform(put("/api/admin/users/{userId}/status", user.id())
                        .param("status", "SUSPENDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account status updated successfully"))
                .andExpect(jsonPath("$.data.email").value(user.email()));
    }

    @Test
    void shouldReturnSuccessMessage_whenForcePasswordReset_givenValidId() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        doNothing().when(adminUserService).forcePasswordReset(userId);

        // Act & Assert
        mockMvc.perform(post("/api/admin/users/{userId}/force-password-reset", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset email sent successfully"));
    }

    @Test
    void shouldReturnSuccessMessage_whenDeleteUser_givenValidId() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        doNothing().when(adminUserService).deleteUser(userId);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted successfully"));
    }

    private UserResponse createUserResponse() {
        return UserResponse.builder()
                .id(UUID.randomUUID())
                .firstName("firstname")
                .lastName("lastname")
                .email("test-email@test.co.za")
                .contactNumber("+27821234567")
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }
}
