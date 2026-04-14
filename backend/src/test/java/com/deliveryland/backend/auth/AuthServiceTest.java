package com.deliveryland.backend.auth;

import com.deliveryland.backend.auth.dto.UserCreateDTO;
import com.deliveryland.backend.auth.dto.UserLoginDTO;
import com.deliveryland.backend.auth.dto.VerifyUserDto;
import com.deliveryland.backend.auth.model.UserRole;
import com.deliveryland.backend.auth.model.VerificationToken;
import com.deliveryland.backend.auth.model.VerificationType;
import com.deliveryland.backend.common.notification.EmailService;
import com.deliveryland.backend.common.security.ApplicationUserRole;
import com.deliveryland.backend.common.security.JwtService;
import com.deliveryland.backend.user.UserRepository;
import com.deliveryland.backend.user.model.User;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .firstName("firstname")
                .lastName("lastname")
                .email("test-email@test.co.za")
                .contactNumber("0821234567")
                .role(ApplicationUserRole.CUSTOMER)
                .password("Password@123")
                .build();
    }

    @Test
    void shouldSendVerificationEmail_whenRegister_givenValidUser() {
        // Arrange
        UserCreateDTO dto = createRegisterDto();

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("encodedPassword");

        // Act
        authService.register(dto);

        // Assert
        verify(emailService, times(1))
                .sendAccountVerificationEmail(any(String.class), anyString());
    }

    @Test
    void shouldReturnToken_whenLogin_givenValidCredentials() {
        // Arrange
        UserLoginDTO dto = createLoginDto();

        user.setEnabled(true);

        when(userRepository.findByEmail(dto.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(dto.getPassword(), user.getPassword()))
                .thenReturn(true);

        when(jwtService.generateToken(user))
                .thenReturn("token");

        Authentication authMock = mock(Authentication.class);

        when(authenticationManager.authenticate(any()))
                .thenReturn(authMock);

        // Act
        var response = authService.logIn(dto);

        // Assert
        Assertions.assertThat(response.token()).isEqualTo("token");
        Assertions.assertThat(response.user().email()).isEqualTo(user.getEmail());
    }

    @Test
    void shouldReturnToken_whenVerifyUser_givenValidToken() {
        // Arrange
        VerifyUserDto dto = createVerifyDto();

        VerificationToken token = createVerificationToken();

        user.setEnabled(true);

        when(userRepository.findByEmail(dto.getEmail()))
                .thenReturn(Optional.of(user));

        when(verificationTokenRepository.findByUserAndToken(user, dto.getToken()))
                .thenReturn(Optional.of(token));

        when(jwtService.generateToken(user))
                .thenReturn("token");

        // Act
        var response = authService.verifyUser(dto);

        // Assert
        Assertions.assertThat(response.token()).isEqualTo("token");
        Assertions.assertThat(response.user().email()).isEqualTo(user.getEmail());
    }

    @Test
    void shouldSendVerificationEmail_whenResendVerification_givenNoActiveToken() {
        // Arrange
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(verificationTokenRepository.findByUser(user))
                .thenReturn(Collections.emptyList());

        // Act
        authService.resendVerificationCode(user.getEmail());

        // Assert
        verify(emailService, times(1))
                .sendAccountVerificationEmail(any(String.class), anyString());
    }

    // helper methods
    private UserCreateDTO createRegisterDto() {
        return UserCreateDTO.builder()
                .firstName("firstname")
                .lastName("lastname")
                .email("test-email@test.co.za")
                .password("password123")
                .role(UserRole.CUSTOMER)
                .build();
    }

    private UserLoginDTO createLoginDto() {
        return UserLoginDTO.builder()
                .email("test-email@test.co.za")
                .password("password123")
                .build();
    }

    private VerifyUserDto createVerifyDto() {
        return VerifyUserDto.builder()
                .email("test-email@test.co.za")
                .token("verification-token")
                .build();
    }

    private VerificationToken createVerificationToken() {
        return VerificationToken.builder()
                .token("verification-token")
                .user(user)
                .type(VerificationType.EMAIL_VERIFY)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .build();
    }
}