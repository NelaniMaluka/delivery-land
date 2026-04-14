package com.deliveryland.backend.user.controller;

import com.deliveryland.backend.auth.dto.LoginResponse;
import com.deliveryland.backend.common.security.CustomUserDetailsService;
import com.deliveryland.backend.common.security.JwtAuthenticationFilter;
import com.deliveryland.backend.common.security.JwtService;
import com.deliveryland.backend.user.dto.UserResponse;
import com.deliveryland.backend.user.model.AccountStatus;
import com.deliveryland.backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldReturnCurrentUser_whenGetMe() throws Exception {
        // Arrange
        UserResponse userResponse = UserResponse.builder()
                .id(UUID.randomUUID())
                .firstName("firstname")
                .lastName("lastname")
                .email("user@deliveryland.com")
                .contactNumber("+27821234567")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(userService.getCurrentUser()).thenReturn(userResponse);

        // Act & Assert
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Your user data"))
                .andExpect(jsonPath("$.data.email").value("user@deliveryland.com"))
                .andExpect(jsonPath("$.data.firstName").value("firstname"));
    }

    @Test
    void shouldReturnLoginResponse_whenUpdateUser_givenValidBody() throws Exception {
        // Arrange
        UserResponse userResponse = UserResponse.builder()
                .firstName("Updated")
                .lastName("Name")
                .email("user@deliveryland.com")
                .contactNumber("+27821234567")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        LoginResponse loginResponse = LoginResponse.builder()
                .token("jwt-token")
                .expiresIn(86400000)
                .user(userResponse)
                .build();

        when(userService.updateUserDetails(any())).thenReturn(loginResponse);

        // Act & Assert
        mockMvc.perform(put("/api/user/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Updated",
                                  "lastName": "Name",
                                  "contactNumber": "+27821234567"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully updated user details"))
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.expiresIn").value(86400000))
                .andExpect(jsonPath("$.data.user.firstName").value("Updated"));
    }

    @Test
    void shouldReturnNoContent_whenDeleteUser() throws Exception {
        // Arrange
        doNothing().when(userService).deleteUser();

        // Act & Assert
        mockMvc.perform(delete("/api/user/delete"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnSuccessMessage_whenEmailChangeRequest_givenNewEmail() throws Exception {
        // Arrange
        doNothing().when(userService).changeEmailRequest(eq("new@deliveryland.com"));

        // Act & Assert
        mockMvc.perform(post("/api/user/email/request")
                        .param("newEmail", "new@deliveryland.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verification email sent successfully"));
    }

    @Test
    void shouldReturnLoginData_whenVerifyEmailChange_givenToken() throws Exception {
        // Arrange
        UserResponse userResponse = UserResponse.builder()
                .firstName("firstname")
                .lastName("lastname")
                .email("verified@deliveryland.com")
                .contactNumber("+27821234567")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        LoginResponse loginResponse = LoginResponse.builder()
                .token("verify-token")
                .expiresIn(86400000)
                .user(userResponse)
                .build();

        when(userService.verifyChangeEmailRequest("abc123")).thenReturn(loginResponse);

        // Act & Assert
        mockMvc.perform(post("/api/user/email/verify")
                        .param("token", "abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email updated successfully"))
                .andExpect(jsonPath("$.data.token").value("verify-token"))
                .andExpect(jsonPath("$.data.user.email").value("verified@deliveryland.com"));
    }
}
