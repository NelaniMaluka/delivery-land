package com.deliveryland.backend.auth;

import com.deliveryland.backend.auth.dto.UserCreateDTO;
import com.deliveryland.backend.auth.dto.UserLoginDTO;
import com.deliveryland.backend.auth.dto.VerifyUserDto;
import com.deliveryland.backend.auth.model.UserRole;
import com.deliveryland.backend.auth.model.UserVerification;
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
import org.mockito.Mockito;
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
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserVerificationRepository userVerificationRepository;

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
    public void init() {
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
    public void UserService_Register_SendEmail() {
        // Arrange
        UserCreateDTO dto = UserCreateDTO.builder()
                .firstName("firstname")
                .lastName("lastname")
                .email("test-email@test.co.za")
                .password("password123")
                .role(UserRole.CUSTOMER)
                .build();

        when(userRepository.existsByEmail(dto.getEmail()))
                .thenReturn(false);
        when(passwordEncoder.encode(dto.getPassword()))
                .thenReturn("encodedPassword");

        // Act
        authService.register(dto);

        // Assert
        verify(emailService, Mockito.times(1))
                .sendAccountVerificationEmail(any(String.class), Mockito.anyString());
    }

    @Test
    public void UserService_LogIn_ReturnToken() {
        // Arrange
        UserLoginDTO dto = UserLoginDTO.builder()
                .email("test-email@test.co.za")
                .password("password123")
                .build();

        user.setEnabled(true);

        when(userRepository.findByEmail(dto.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(dto.getPassword(), user.getPassword()))
                .thenReturn(true);

        when(jwtService.generateToken(user))
                .thenReturn("token");

        Authentication authenticationMock = mock(Authentication.class);

        when(authenticationManager.authenticate(
                argThat(token -> token.getPrincipal().equals(dto.getEmail()) &&
                        token.getCredentials().equals(dto.getPassword()))))
                .thenReturn(authenticationMock);

        // Act
        var response = authService.logIn(dto);

        // Assert
        Assertions.assertThat(response.token()).isEqualTo("token");
        Assertions.assertThat(response.user().email()).isEqualTo("test-email@test.co.za");
        Assertions.assertThat(response.user().firstName()).isEqualTo("firstname");
        Assertions.assertThat(response.user().lastName()).isEqualTo("lastname");
    }

    @Test
    public void UserService_VerifyUser_ReturnToken() {
        // Arrange
        VerifyUserDto dto = VerifyUserDto.builder()
                .token("verification-token")
                .email("test-email@test.co.za")
                .build();

        UserVerification userVerification = UserVerification.builder()
                .token("verification-token")
                .user(user)
                .type(VerificationType.EMAIL)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .build();

        user.setEnabled(true);
        when(userRepository.findByEmail(dto.getEmail()))
                .thenReturn(Optional.of(user));
        when(userVerificationRepository.findByUserAndToken(user, dto.getToken()))
                .thenReturn(Optional.of(userVerification));
        when(jwtService.generateToken(user))
                .thenReturn("token");

        var response = authService.verifyUser(dto);

        // Assert
        Assertions.assertThat(response.token()).isEqualTo("token");
        Assertions.assertThat(response.user().email()).isEqualTo("test-email@test.co.za");
        Assertions.assertThat(response.user().firstName()).isEqualTo("firstname");
        Assertions.assertThat(response.user().lastName()).isEqualTo("lastname");
    }

    @Test
    public void UserService_ResendVerification_SendEmail() {

        when(userRepository.findByEmail("test-email@test.co.za"))
                .thenReturn(Optional.of(user));
        when(userVerificationRepository.findByUser(user))
                .thenReturn(Collections.emptyList());

        // Act
        authService.resendVerificationCode("test-email@test.co.za");

        // Assert
        verify(emailService, Mockito.times(1))
                .sendAccountVerificationEmail(any(String.class), Mockito.anyString());
    }

}