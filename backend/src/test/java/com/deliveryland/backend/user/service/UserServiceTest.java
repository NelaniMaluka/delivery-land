package com.deliveryland.backend.user.service;

import com.deliveryland.backend.auth.VerificationTokenRepository;
import com.deliveryland.backend.auth.model.VerificationToken;
import com.deliveryland.backend.auth.model.VerificationType;
import com.deliveryland.backend.common.notification.EmailService;
import com.deliveryland.backend.common.security.ApplicationUserRole;
import com.deliveryland.backend.common.security.JwtService;
import com.deliveryland.backend.user.UserRepository;
import com.deliveryland.backend.user.dto.UpdateUserDTO;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtService jwtService;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = createUser("user@deliveryland.com");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnUserResponse_whenGetCurrentUser_givenAuthenticatedUser() {
        // Arrange
        authenticate(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        // Act
        var result = userService.getCurrentUser();

        // Assert
        Assertions.assertThat(result.email()).isEqualTo(user.getEmail());
    }

    @Test
    void shouldUpdateAndReturnLoginResponse_whenUpdateUserDetails_givenValidDto() {
        // Arrange
        authenticate(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("new-jwt");

        UpdateUserDTO dto = UpdateUserDTO.builder()
                .firstName("Updated")
                .lastName("Name")
                .contactNumber("+27821234567")
                .build();

        // Act
        var result = userService.updateUserDetails(dto);

        // Assert
        Assertions.assertThat(result.token()).isEqualTo("new-jwt");
        Assertions.assertThat(result.expiresIn()).isEqualTo(86400000);
        Assertions.assertThat(result.user().firstName()).isEqualTo("Updated");
        Assertions.assertThat(result.user().lastName()).isEqualTo("Name");
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void shouldDeleteUserAndNotify_whenDeleteUser_givenAuthenticatedUser() {
        // Arrange
        authenticate(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        // Act
        userService.deleteUser();

        // Assert
        verify(verificationTokenRepository, times(1)).deleteByUser(user);
        verify(userRepository, times(1)).delete(user);
        verify(emailService, times(1))
                .sendAccountDeletionEmail(eq(user.getEmail()), eq("firstname lastname"));
    }

    @Test
    void shouldThrowBadRequest_whenChangeEmailRequest_givenSameEmail() {
        // Arrange
        authenticate(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        // Act & Assert
        Assertions.assertThatThrownBy(() -> userService.changeEmailRequest(user.getEmail()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> Assertions.assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void shouldThrowBadRequest_whenChangeEmailRequest_givenActiveVerificationExists() {
        // Arrange
        authenticate(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        VerificationToken active = VerificationToken.builder()
                .token("active-token")
                .user(user)
                .type(VerificationType.EMAIL_CHANGE)
                .targetEmail("new@deliveryland.com")
                .expiryDate(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        when(verificationTokenRepository.findByUser(user)).thenReturn(List.of(active));

        // Act & Assert
        Assertions.assertThatThrownBy(() -> userService.changeEmailRequest("new@deliveryland.com"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> Assertions.assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void shouldThrowTooManyRequests_whenChangeEmailRequest_givenExceededLimit() {
        // Arrange
        authenticate(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        LocalDateTime now = LocalDateTime.now();
        List<VerificationToken> expiredButRecent = IntStream.range(0, 3)
                .mapToObj(i -> VerificationToken.builder()
                        .token("tok-" + i)
                        .user(user)
                        .type(VerificationType.EMAIL_CHANGE)
                        .targetEmail("other" + i + "@deliveryland.com")
                        .expiryDate(now.minusDays(1))
                        .used(false)
                        .build())
                .toList();

        when(verificationTokenRepository.findByUser(user)).thenReturn(expiredButRecent);

        // Act & Assert
        Assertions.assertThatThrownBy(() -> userService.changeEmailRequest("brand-new@deliveryland.com"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> Assertions.assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    void shouldSaveTokenAndSendEmail_whenChangeEmailRequest_givenValidNewEmail() {
        // Arrange
        authenticate(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(verificationTokenRepository.findByUser(user)).thenReturn(List.of());

        // Act
        userService.changeEmailRequest("newemail@deliveryland.com");

        // Assert
        verify(verificationTokenRepository, times(1)).save(any(VerificationToken.class));
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(1))
                .sendEmailChangeVerificationEmail(eq("newemail@deliveryland.com"), tokenCaptor.capture());
        Assertions.assertThat(tokenCaptor.getValue()).isNotBlank();
    }

    @Test
    void shouldThrowNotFound_whenVerifyChangeEmailRequest_givenUnknownToken() {
        // Arrange
        authenticate(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(verificationTokenRepository.findByUserAndToken(user, "bad"))
                .thenReturn(Optional.empty());

        // Act & Assert
        Assertions.assertThatThrownBy(() -> userService.verifyChangeEmailRequest("bad"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> Assertions.assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void shouldThrowBadRequest_whenVerifyChangeEmailRequest_givenUsedToken() {
        // Arrange
        authenticate(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        VerificationToken vt = VerificationToken.builder()
                .token("t1")
                .user(user)
                .type(VerificationType.EMAIL_CHANGE)
                .targetEmail("new@deliveryland.com")
                .expiryDate(LocalDateTime.now().plusHours(1))
                .used(true)
                .build();

        when(verificationTokenRepository.findByUserAndToken(user, "t1"))
                .thenReturn(Optional.of(vt));

        // Act & Assert
        Assertions.assertThatThrownBy(() -> userService.verifyChangeEmailRequest("t1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> Assertions.assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void shouldThrowGone_whenVerifyChangeEmailRequest_givenExpiredToken() {
        // Arrange
        authenticate(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        VerificationToken vt = VerificationToken.builder()
                .token("t1")
                .user(user)
                .type(VerificationType.EMAIL_CHANGE)
                .targetEmail("new@deliveryland.com")
                .expiryDate(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .build();

        when(verificationTokenRepository.findByUserAndToken(user, "t1"))
                .thenReturn(Optional.of(vt));

        // Act & Assert
        Assertions.assertThatThrownBy(() -> userService.verifyChangeEmailRequest("t1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> Assertions.assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.GONE));
    }

    @Test
    void shouldUpdateEmailAndReturnResponse_whenVerifyChangeEmailRequest_givenValidToken() {
        // Arrange
        authenticate(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        VerificationToken vt = VerificationToken.builder()
                .token("verify-me")
                .user(user)
                .type(VerificationType.EMAIL_CHANGE)
                .targetEmail("verified@deliveryland.com")
                .expiryDate(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        when(verificationTokenRepository.findByUserAndToken(user, "verify-me"))
                .thenReturn(Optional.of(vt));

        // Act
        var result = userService.verifyChangeEmailRequest("verify-me");

        // Assert
        Assertions.assertThat(user.getEmail()).isEqualTo("verified@deliveryland.com");
        Assertions.assertThat(result.token()).isEqualTo("verify-me");
        Assertions.assertThat(result.user().email()).isEqualTo("verified@deliveryland.com");
        Assertions.assertThat(vt.isUsed()).isTrue();
        verify(userRepository, times(1)).save(user);
        verify(verificationTokenRepository, times(1)).save(vt);
    }

    private void authenticate(String email) {
        var auth = new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private User createUser(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .firstName("firstname")
                .lastName("lastname")
                .email(email)
                .contactNumber("+27821234567")
                .password("Password@123")
                .role(ApplicationUserRole.CUSTOMER)
                .accountStatus(AccountStatus.ACTIVE)
                .enabled(true)
                .build();
    }
}
