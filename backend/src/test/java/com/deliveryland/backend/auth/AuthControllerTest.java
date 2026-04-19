package com.deliveryland.backend.auth;

import com.deliveryland.backend.auth.dto.LoginResponse;
import com.deliveryland.backend.auth.dto.UserCreateDTO;
import com.deliveryland.backend.auth.dto.UserLoginDTO;
import com.deliveryland.backend.auth.dto.VerifyUserDto;
import com.deliveryland.backend.common.security.CustomUserDetailsService;
import com.deliveryland.backend.common.security.JwtAuthenticationFilter;
import com.deliveryland.backend.common.security.JwtService;
import com.deliveryland.backend.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuthService authenticationService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @MockitoBean
  private CustomUserDetailsService customUserDetailsService;

  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @Test
  void shouldReturnCreatedResponse_whenRegister_givenValidUser() throws Exception {
    // Arrange
    doNothing().when(authenticationService).register(any(UserCreateDTO.class));

    // Act and Assert
    mockMvc.perform(post("/api/public/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "firstName": "firstname",
              "lastName": "lastname",
              "email": "test-email@test.co.za",
              "contactNumber": "0812345678",
              "password": "Password@123"
            }
            """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.message")
            .value("We have sent a verification email. Please verify your email."));
  }

  @Test
  void shouldReturnLoginResponse_whenLogin_givenValidCredentials() throws Exception {
    // Arrange
    UserResponse userResponse = UserResponse.builder()
        .firstName("firstname")
        .lastName("lastname")
        .build();

    LoginResponse loginResponse = LoginResponse.builder()
        .jwtToken("token")
        .expiresIn(1000)
        .user(userResponse)
        .build();

    when(authenticationService.logIn(any(UserLoginDTO.class)))
        .thenReturn(loginResponse);

    // Act and Assert
    mockMvc.perform(post("/api/public/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "test-email@test.co.za",
              "password": "Password@123"
            }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Login successful"))
        .andExpect(jsonPath("$.data.jwtToken").value("token"))
        .andExpect(jsonPath("$.data.expiresIn").value(1000))
        .andExpect(jsonPath("$.data.user.firstName").value("firstname"))
        .andExpect(jsonPath("$.data.user.lastName").value("lastname"));
  }

  @Test
  void shouldReturnVerificationResponse_whenVerify_givenValidToken() throws Exception {
    // Arrange
    UserResponse userResponse = UserResponse.builder()
        .firstName("firstname")
        .lastName("lastname")
        .build();

    LoginResponse loginResponse = LoginResponse.builder()
        .jwtToken("token")
        .expiresIn(1000)
        .user(userResponse)
        .build();

    when(authenticationService.verifyUser(any(VerifyUserDto.class)))
        .thenReturn(loginResponse);

    // Act and Assert
    mockMvc.perform(post("/api/public/auth/verify")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "token": "A1B2C3D4",
              "email": "test-email@test.co.za"
            }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Account successfully verified"))
        .andExpect(jsonPath("$.data.jwtToken").value("token"))
        .andExpect(jsonPath("$.data.expiresIn").value(1000))
        .andExpect(jsonPath("$.data.user.firstName").value("firstname"))
        .andExpect(jsonPath("$.data.user.lastName").value("lastname"));
  }

  @Test
  void shouldReturnSuccessMessage_whenResendVerification_givenValidEmail() throws Exception {
    // Arrange
    doNothing().when(authenticationService)
        .resendVerificationCode(any(String.class));

    // Act and Assert
    mockMvc.perform(post("/api/public/auth/resend-verification")
        .param("email", "malukanelani@gmail.com"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message")
            .value("Successfully sent a new verification code."))
        .andExpect(jsonPath("$.data")
            .value("malukanelani@gmail.com"));
  }
}