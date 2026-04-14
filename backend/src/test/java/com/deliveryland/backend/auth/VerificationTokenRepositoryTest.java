package com.deliveryland.backend.auth;

import com.deliveryland.backend.auth.model.VerificationToken;
import com.deliveryland.backend.auth.model.VerificationType;
import com.deliveryland.backend.user.UserRepository;
import com.deliveryland.backend.user.model.User;
import com.deliveryland.backend.common.security.ApplicationUserRole;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@ActiveProfiles("test")
class VerificationTokenRepositoryTest {

    @Autowired
    private VerificationTokenRepository verificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldReturnVerificationToken_whenFindByUserAndToken_givenValidToken() {
        // Arrange
        User user = createUser();
        VerificationToken token = createToken(user);

        userRepository.save(user);
        verificationRepository.save(token);

        // Act
        var result = verificationRepository.findByUserAndToken(user, "token-123");

        // Assert
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get().getType()).isEqualTo(VerificationType.EMAIL_VERIFY);
        Assertions.assertThat(result.get().getUser()).isEqualTo(user);
    }

    @Test
    void shouldReturnTokensList_whenFindByUser_givenExistingUser() {
        // Arrange
        User user = createUser();
        VerificationToken token = createToken(user);

        userRepository.save(user);
        verificationRepository.save(token);

        // Act
        var result = verificationRepository.findByUser(user);

        // Assert
        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.get(0).getUser()).isEqualTo(user);
        Assertions.assertThat(result.get(0).getType()).isEqualTo(VerificationType.EMAIL_VERIFY);
    }

    @Test
    void shouldDeleteTokens_whenDeleteByUser_givenExistingUser() {
        // Arrange
        User user = createUser();
        VerificationToken token = createToken(user);

        userRepository.save(user);
        verificationRepository.save(token);

        Assertions.assertThat(verificationRepository.findByUser(user)).hasSize(1);

        // Act
        int deleted = verificationRepository.deleteByUser(user);

        // Assert
        Assertions.assertThat(deleted).isEqualTo(1);
        Assertions.assertThat(verificationRepository.findByUser(user)).isEmpty();
    }

    // helper methods
    private User createUser() {
        return User.builder()
                .firstName("firstname")
                .lastName("lastname")
                .email("test-email@test.co.za")
                .contactNumber("0821234567")
                .role(ApplicationUserRole.CUSTOMER)
                .password("Password@123")
                .build();
    }

    private VerificationToken createToken(User user) {
        return VerificationToken.builder()
                .token("token-123")
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .type(VerificationType.EMAIL_VERIFY)
                .build();
    }
}