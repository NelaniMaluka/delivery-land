package com.deliveryland.backend.auth;

import com.deliveryland.backend.auth.dto.LoginResponse;
import com.deliveryland.backend.auth.dto.UserCreateDTO;
import com.deliveryland.backend.auth.dto.UserLoginDTO;
import com.deliveryland.backend.auth.dto.VerifyUserDto;
import com.deliveryland.backend.auth.model.UserRole;
import com.deliveryland.backend.auth.model.UserVerification;
import com.deliveryland.backend.common.notification.EmailService;
import com.deliveryland.backend.common.security.ApplicationUserRole;
import com.deliveryland.backend.common.security.JwtService;
import com.deliveryland.backend.user.UserRepository;
import com.deliveryland.backend.user.model.User;
import com.deliveryland.backend.auth.model.VerificationType;
import jakarta.validation.ValidationException;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Log4j2
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserVerificationRepository userVerificationRepository;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager, UserVerificationRepository userVerificationRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userVerificationRepository = userVerificationRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void register(UserCreateDTO dto) {
        log.info("Registering new user with email: {}", dto.getEmail());

        // Check if the email is available
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User already exists with the provided email.");
        }

        ApplicationUserRole appRole = toApplicationRole(dto.getRole());

        // Create the user object
        User newUser = User.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .password(passwordEncoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .contactNumber(dto.getContactNumber())
                .role(appRole)
                .build();

        // Generate new verification entity
        UserVerification verification = UserVerification.builder()
                .token(generateVerificationCode())
                .type(VerificationType.EMAIL)
                .user(newUser)
                .build();

        // Save and send the verification email
        userRepository.save(newUser);
        userVerificationRepository.save(verification);
        emailService.sendAccountVerificationEmail(dto.getEmail(), verification.getToken());
    }

    @Transactional
    public LoginResponse logIn(UserLoginDTO dto) {
        log.info("Login attempt for email: {}", dto.getEmail());

        // Check if the user exists
        User existingUser = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ValidationException(
                        "No account is associated with that email."
                ));

        // Validate password
        if (!passwordEncoder.matches(dto.getPassword(), existingUser.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials provided.");
        }

        // checks if the account is verified
        if (!existingUser.isEnabled()) {
            throw new IllegalArgumentException("Account not verified. Please verify your account.");
        }

        // Authenticates the user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        dto.getEmail(),
                        dto.getPassword()));

        log.info("User logged in successfully: {}", existingUser.getEmail());

        // Generate a new user response
        return LoginResponse.buildResponse(jwtService.generateToken(existingUser), 86400000, existingUser);
    }

    @Transactional
    public LoginResponse verifyUser(VerifyUserDto dto) {
        // Check if a user exists with the provided email
        var user = userRepository
                .findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Check if token exists
        var verification = userVerificationRepository
                .findByUserAndToken(user, dto.getToken())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Verification failed: the token provided does not exist or is invalid."));

        // Check if the token type matches
        if (verification.getType() != VerificationType.EMAIL) {
            throw new IllegalArgumentException(
                    "Verification failed: the token provided is not valid for email verification.");
        }

        // Check if token expired
        if (verification.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Your verification token has expired. Please request a new token.");
        }

        // Check if token already used
        if (verification.isUsed()) {
            throw new IllegalArgumentException("This verification token has already been used.");
        }

        // Update and save the changes
        user.setEnabled(true);
        verification.setUsed(true);
        verification.setToken(null);

        userRepository.save(user);
        userVerificationRepository.save(verification);

        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName() + " " + user.getLastName());

        // Generate a new user response
        return LoginResponse.buildResponse(jwtService.generateToken(user), 86400000, user);
    }

    @Transactional
    public void resendVerificationCode(String email) {
        // Check if a user exists with the provided email
        var user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.isEnabled()) {
            throw new IllegalArgumentException("This account has already been verified. No further action is needed.");
        }

        // Checks if there isn't any active tokens?
        List<UserVerification> userVerificationList = userVerificationRepository.findByUser(user);
        Optional<UserVerification> optionalVerification = userVerificationList.stream()
                .filter(v -> v.getExpiryDate().isAfter(LocalDateTime.now()))
                .findFirst();
        if (optionalVerification.isPresent()) {
            throw new IllegalArgumentException(
                    "You have an active verification token. Please use it or wait for it to expire.");
        }

        try {
            // Generate new verification entity
            UserVerification verification = UserVerification.builder()
                    .token(generateVerificationCode())
                    .type(VerificationType.EMAIL)
                    .user(user)
                    .build();

            // Save the changes
            userVerificationRepository.save(verification);
            emailService.sendAccountVerificationEmail(user.getEmail(), verification.getToken());
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset token");
        }
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(9000000) + 100000;
        return String.valueOf(code);
    }

    private ApplicationUserRole toApplicationRole(UserRole simpleRole) {
        return switch (simpleRole) {
            case CUSTOMER      -> ApplicationUserRole.CUSTOMER;
            case DRIVER        -> ApplicationUserRole.DRIVER;
            case STORE_OWNER   -> ApplicationUserRole.STORE_OWNER;
            case ADMIN         -> ApplicationUserRole.ADMIN;
        };
    }
}
