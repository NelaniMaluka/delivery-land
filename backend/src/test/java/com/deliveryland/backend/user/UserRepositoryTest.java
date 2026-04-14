package com.deliveryland.backend.user;

import com.deliveryland.backend.common.security.ApplicationUserRole;
import com.deliveryland.backend.user.model.User;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

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
    void shouldReturnUser_whenFindById_givenValidId() {
        // Arrange
        User savedUser = userRepository.save(user);

        // Act
        var result = userRepository.findById(savedUser.getId());

        // Assert
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get().getEmail()).isEqualTo(savedUser.getEmail());
    }

    @Test
    void shouldReturnUser_whenFindByEmail_givenExistingEmail() {
        // Arrange
        User savedUser = userRepository.save(user);

        // Act
        var result = userRepository.findByEmail(savedUser.getEmail());

        // Assert
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get().getEmail()).isEqualTo(savedUser.getEmail());
    }

    @Test
    void shouldReturnEmpty_whenFindByEmail_givenNonExistingEmail() {
        // Arrange
        var result = userRepository.findByEmail("missing@email.com");

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnTrue_whenExistsByEmail_givenExistingEmail() {
        // Arrange
        User savedUser = userRepository.save(user);

        // Act
        boolean exists = userRepository.existsByEmail(savedUser.getEmail());

        // Assert
        Assertions.assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalse_whenExistsByEmail_givenNonExistingEmail() {
        // Arrange
        boolean exists = userRepository.existsByEmail("random@email.com");

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    void shouldDeleteUser_whenDelete_givenExistingUser() {
        // Arrange
        User savedUser = userRepository.save(user);

        // Act
        userRepository.delete(savedUser);

        var result = userRepository.findById(savedUser.getId());

        // Assert
        Assertions.assertThat(result).isEmpty();
    }
}