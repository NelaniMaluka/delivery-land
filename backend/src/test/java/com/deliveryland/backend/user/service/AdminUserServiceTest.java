package com.deliveryland.backend.user.service;

import com.deliveryland.backend.auth.VerificationTokenRepository;
import com.deliveryland.backend.common.notification.EmailService;
import com.deliveryland.backend.common.security.ApplicationUserRole;
import com.deliveryland.backend.user.UserRepository;
import com.deliveryland.backend.user.model.AccountStatus;
import com.deliveryland.backend.user.model.User;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    private User admin;
    private User user;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        admin = createUser("admin@deliveryland.com", ApplicationUserRole.ADMIN, AccountStatus.ACTIVE);
        user = createUser("user@deliveryland.com", ApplicationUserRole.CUSTOMER, AccountStatus.ACTIVE);
        pageable = PageRequest.of(0, 10);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnUsers_whenGetAllUsers_givenPageable() {
        // Arrange
        when(userRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(user), pageable, 1));

        // Act
        var result = adminUserService.getAllUsers(pageable);

        // Assert
        Assertions.assertThat(result.getTotalElements()).isEqualTo(1);
        Assertions.assertThat(result.getContent().get(0).email()).isEqualTo(user.getEmail());
    }

    @Test
    void shouldReturnUsers_whenGetUsersByRole_givenRole() {
        // Arrange
        when(userRepository.findByRole(ApplicationUserRole.CUSTOMER, pageable))
                .thenReturn(new PageImpl<>(List.of(user), pageable, 1));

        // Act
        var result = adminUserService.getUsersByRole(ApplicationUserRole.CUSTOMER, pageable);

        // Assert
        Assertions.assertThat(result.getTotalElements()).isEqualTo(1);
        Assertions.assertThat(result.getContent().get(0).email()).isEqualTo(user.getEmail());
    }

    @Test
    void shouldReturnUser_whenGetUserById_givenExistingId() {
        // Arrange
        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        // Act
        var result = adminUserService.getUserById(user.getId());

        // Assert
        Assertions.assertThat(result.email()).isEqualTo(user.getEmail());
    }

    @Test
    void shouldThrowNotFound_whenGetUserById_givenMissingUser() {
        // Arrange
        UUID missingId = UUID.randomUUID();
        when(userRepository.findById(missingId))
                .thenReturn(Optional.empty());

        // Act & Assert
        Assertions.assertThatThrownBy(() -> adminUserService.getUserById(missingId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    @Test
    void shouldUpdateStatus_whenUpdateAccountStatus_givenDifferentStatus() {
        // Arrange
        authenticateAsAdmin(admin.getEmail());

        when(userRepository.findByEmail(admin.getEmail()))
                .thenReturn(Optional.of(admin));
        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        // Act
        var result = adminUserService.updateAccountStatus(user.getId(), AccountStatus.SUSPENDED);

        // Assert
        Assertions.assertThat(result.accountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void shouldSkipSave_whenUpdateAccountStatus_givenSameStatus() {
        // Arrange
        authenticateAsAdmin(admin.getEmail());

        when(userRepository.findByEmail(admin.getEmail()))
                .thenReturn(Optional.of(admin));
        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        // Act
        var result = adminUserService.updateAccountStatus(user.getId(), AccountStatus.ACTIVE);

        // Assert
        Assertions.assertThat(result.accountStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldSendResetEmail_whenForcePasswordReset_givenExistingUser() {
        // Arrange
        authenticateAsAdmin(admin.getEmail());

        when(userRepository.findByEmail(admin.getEmail()))
                .thenReturn(Optional.of(admin));
        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        // Act
        adminUserService.forcePasswordReset(user.getId());

        // Assert
        verify(verificationTokenRepository, times(1)).deleteByUser(user);
        verify(verificationTokenRepository, times(1)).save(any());

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(1))
                .sendPasswordResetEmail(eq(user.getEmail()), tokenCaptor.capture());
        Assertions.assertThat(tokenCaptor.getValue()).isNotBlank();
    }

    @Test
    void shouldDeleteUser_whenDeleteUser_givenExistingUser() {
        // Arrange
        authenticateAsAdmin(admin.getEmail());

        when(userRepository.findByEmail(admin.getEmail()))
                .thenReturn(Optional.of(admin));
        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        // Act
        adminUserService.deleteUser(user.getId());

        // Assert
        verify(userRepository, times(1)).delete(user);
    }

    private void authenticateAsAdmin(String email) {
        var auth = new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private User createUser(String email, ApplicationUserRole role, AccountStatus status) {
        return User.builder()
                .id(UUID.randomUUID())
                .firstName("firstname")
                .lastName("lastname")
                .password("password123")
                .email(email)
                .contactNumber("+27821234567")
                .role(role)
                .accountStatus(status)
                .enabled(true)
                .build();
    }
}
