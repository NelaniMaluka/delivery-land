package com.deliveryland.backend.auth;

import com.deliveryland.backend.auth.dto.LoginResponse;
import com.deliveryland.backend.auth.dto.UserCreateDTO;
import com.deliveryland.backend.auth.dto.UserLoginDTO;
import com.deliveryland.backend.auth.dto.VerifyUserDto;
import com.deliveryland.backend.auth.model.UserRole;
import com.deliveryland.backend.auth.model.VerificationToken;
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
import java.util.UUID;

@Log4j2
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager, VerificationTokenRepository verificationTokenRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.verificationTokenRepository = verificationTokenRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void register(UserCreateDTO dto) {
        log.info("Registering new user with email: {}", dto.getEmail());

        // Check if the email is available
        if (userRepository.existsByEmail(dto.getEmail())) {
            log.warn("Registration failed: email '{}' already exists", dto.getEmail());
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
        VerificationToken verificationToken = VerificationToken.builder()
                .token(generateToken())
                .type(VerificationType.EMAIL_VERIFY)
                .user(newUser)
                .build();

        // Save and send the verification email
        userRepository.save(newUser);
        verificationTokenRepository.save(verificationToken);
        emailService.sendAccountVerificationEmail(dto.getEmail(), verificationToken.getToken());

        log.info("User '{}' registered successfully. Verification token generated.", dto.getEmail());
    }

    @Transactional
    public LoginResponse logIn(UserLoginDTO dto) {
        log.info("Login attempt for email: {}", dto.getEmail());

        // Check if the user exists
        User existingUser = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed: no account found for email '{}'", dto.getEmail());
                    return new ValidationException("No account is associated with that email.");
                });

        // Validate password
        if (!passwordEncoder.matches(dto.getPassword(), existingUser.getPassword())) {
            log.warn("Login failed: invalid credentials for email '{}'", dto.getEmail());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials provided.");
        }

        // checks if the account is verified
        if (!existingUser.isEnabled()) {
            log.warn("Login failed: account not verified for email '{}'", dto.getEmail());
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
        log.info("Verification attempt for email '{}'", dto.getEmail());

        // Check if a user exists with the provided email
        var user = userRepository
                .findByEmail(dto.getEmail())
                .orElseThrow(() -> {
                    log.warn("Verification failed: user not found for email '{}'", dto.getEmail());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });

        // Check if token exists
        var verificationToken = verificationTokenRepository
                .findByUserAndToken(user, dto.getToken())
                .orElseThrow(() -> {
                    log.warn("Verification failed: invalid token provided for email '{}'", dto.getEmail());
                    return new IllegalArgumentException(
                            "Verification failed: the token provided does not exist or is invalid.");
                });

        // Check if the token type matches
        if (verificationToken.getType() != VerificationType.EMAIL_VERIFY) {
            log.warn("Verification failed: incorrect token type '{}' for email '{}'",
                    verificationToken.getType(), dto.getEmail());
            throw new IllegalArgumentException(
                    "Verification failed: the token provided is not valid for email verification.");
        }

        // Check if token expired
        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.warn("Verification failed: expired token used for email '{}'", dto.getEmail());
            throw new IllegalArgumentException(
                    "Your verification token has expired. Please request a new token.");
        }

        // Check if token already used
        if (verificationToken.isUsed()) {
            log.warn("Verification failed: token reuse attempt for email '{}'", dto.getEmail());
            throw new IllegalArgumentException(
                    "This verification token has already been used.");
        }

        // Update and save the changes
        user.setEnabled(true);
        verificationToken.setUsed(true);
        verificationToken.setToken(null);

        userRepository.save(user);
        verificationTokenRepository.save(verificationToken);

        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName() + " " + user.getLastName());

        log.info("User '{}' successfully verified their account", dto.getEmail());

        // Generate a new user response
        return LoginResponse.buildResponse(jwtService.generateToken(user), 86400000, user);
    }

    @Transactional
    public void resendVerificationCode(String email) {
        log.info("Resend verification code requested for email '{}'", email);

        // Check if a user exists with the provided email
        var user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Resend verification failed: user not found for email '{}'", email);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });

        // Check if already verified
        if (user.isEnabled()) {
            log.warn("Resend verification failed: account already verified for email '{}'", email);
            throw new IllegalArgumentException(
                    "This account has already been verified. No further action is needed.");
        }

        // Check for active tokens
        List<VerificationToken> userVerificationList = verificationTokenRepository.findByUser(user);
        Optional<VerificationToken> optionalVerification = userVerificationList.stream()
                .filter(v -> v.getExpiryDate().isAfter(LocalDateTime.now()))
                .findFirst();

        if (optionalVerification.isPresent()) {
            log.warn("Resend verification failed: active token already exists for email '{}'", email);
            throw new IllegalArgumentException(
                    "You have an active verification token. Please use it or wait for it to expire.");
        }

        try {
            // Generate new verification entity
            VerificationToken verificationToken = VerificationToken.builder()
                    .token(generateToken())
                    .type(VerificationType.EMAIL_VERIFY)
                    .user(user)
                    .build();

            // Save and send email
            verificationTokenRepository.save(verificationToken);
            emailService.sendAccountVerificationEmail(user.getEmail(), verificationToken.getToken());

            log.info("Verification token resent successfully for email '{}'", email);

        } catch (Exception e) {
            log.error("Error while resending verification token for email '{}': {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to reset token");
        }
    }

    private static String generateToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
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
