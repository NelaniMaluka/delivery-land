package com.deliveryland.backend.user.service;

import com.deliveryland.backend.auth.VerificationTokenRepository;
import com.deliveryland.backend.auth.dto.LoginResponse;
import com.deliveryland.backend.auth.model.VerificationToken;
import com.deliveryland.backend.auth.model.VerificationType;
import com.deliveryland.backend.common.notification.EmailService;
import com.deliveryland.backend.common.security.JwtService;
import com.deliveryland.backend.user.UserRepository;
import com.deliveryland.backend.user.dto.UpdateUserDTO;
import com.deliveryland.backend.user.dto.UserResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Log4j2
@Service
public class UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final VerificationTokenRepository verificationTokenRepository;

    public UserService(UserRepository userRepository, EmailService emailService, JwtService jwtService, VerificationTokenRepository verificationTokenRepository) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.verificationTokenRepository = verificationTokenRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "user", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Validate that a real (non-anonymous) user is authenticated
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please login or register");
        }

        // Extract username from authentication
        String username = authentication.getName();

        // get user by username or return 404 if not found
        var user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return UserResponse.user(user);
    }

    @Transactional
    @CacheEvict(value = "user", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
    public LoginResponse updateUserDetails(UpdateUserDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Validate that a real (non-anonymous) user is authenticated
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Unauthorized attempt to update user details");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please login or register");
        }

        // Extract email from authentication
        String email = authentication.getName();
        log.info("User '{}' initiated updateUserDetails request", email);

        // get user by email or return 404 if not found
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User '{}' not found while attempting to update details", email);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });

        log.debug("Updating firstname, lastname and contact-number for user '{}'", email);
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setContactNumber(dto.getContactNumber());

        userRepository.save(user);
        log.info("User '{}' details updated successfully", email);

        // Generate a new user response
        LoginResponse response = LoginResponse.buildResponse(jwtService.generateToken(user), 86400000, user);

        // userSocket.sendUpdatedUser(response.user());
        log.info("Updated user '{}' broadcasted through WebSocket", email);

        return response;
    }

    @Transactional
    @CacheEvict(value = "user", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
    public void deleteUser() {
        log.info("Starting user deletion process.");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Validate that a real (non-anonymous) user is authenticated
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Unauthorized deletion attempt detected.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please login or register");
        }

        // Extract email from authentication
        String email = authentication.getName();
        log.debug("Authenticated user: {}", email);

        // Delete user by email or return 404 if not found
        userRepository.findByEmail(email)
                .ifPresentOrElse(user -> {
                            verificationTokenRepository.deleteByUser(user);
                            userRepository.delete(user);
                            log.info("User '{}' successfully deleted.", email);
                            emailService.sendAccountDeletionEmail(email, user.getFirstName() + " " + user.getLastName());
                        },
                        () -> {
                            log.error("Deletion failed: user '{}' not found.", email);
                            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                        });
    }

    @Transactional
    public void changeEmailRequest(String newEmail) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Verify authentication (ensure real logged-in user)
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Unauthorized attempt to request email change to '{}'", newEmail);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please login or register");
        }

        // Get current authenticated email
        String currentEmail = authentication.getName();
        log.info("User '{}' initiated email change request to '{}'", currentEmail, newEmail);

        // Retrieve user record
        var user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> {
                    log.error("Authenticated user '{}' not found while attempting email change to '{}'", currentEmail,
                            newEmail);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });

        // Prevent changing to the same email
        if (currentEmail.equals(newEmail)) {
            log.warn("User '{}' attempted to change email to the same address '{}'", currentEmail, newEmail);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "New email must be different from the current email.");
        }

        LocalDateTime now = LocalDateTime.now();

        // Get active email verification requests
        List<VerificationToken> activeVerifications = verificationTokenRepository.findByUser(user).stream()
                .filter(v -> v.getExpiryDate().isAfter(now))
                .toList();

        if (!activeVerifications.isEmpty()) {
            log.warn("User '{}' attempted to create a new email change request while one is still valid", currentEmail);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A valid verification code already exists for this user.");
        }

        // Limit the number of changes per 7 days
        long recentResets = verificationTokenRepository.findByUser(user).stream()
                .filter(v -> v.getExpiryDate().isAfter(now.minusDays(7)) && !v.isUsed())
                .count();

        if (recentResets >= 3) {
            log.warn("User '{}' exceeded the maximum number of email change requests in the last 7 days", currentEmail);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "You can only request an email change 3 times every 7 days.");
        }

        // Generate and save a new email verification entry
        String token = generateToken();
        VerificationToken verificationToken = VerificationToken.builder()
                .type(VerificationType.EMAIL_CHANGE)
                .targetEmail(newEmail)
                .token(token)
                .user(user)
                .build();
        verificationTokenRepository.save(verificationToken);
        log.info("New email verification token '{}' generated for user '{}' for new email '{}'", token, currentEmail,
                newEmail);

        // Send verification email
        emailService.sendEmailChangeVerificationEmail(newEmail, token);
        log.info("Verification email sent to '{}' for user '{}'", newEmail, currentEmail);
    }

    @Transactional
    @CacheEvict(value = "user", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
    public LoginResponse verifyChangeEmailRequest(String token) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Ensure there is a valid-authenticated user
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Unauthorized attempt to change email using token: {}", token);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please login or register");
        }

        // Extract authenticated user's current email
        String currentEmail = authentication.getName();
        log.info("User '{}' attempting to verify an email change using token: {}", currentEmail, token);

        // Load user from repository (should exist if authenticated, but validated for safety)
        var user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> {
                    log.error("Authenticated user '{}' not found in the system during email change verification",
                            currentEmail);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });

        // Load token details from database
        var verificationToken = verificationTokenRepository.findByUserAndToken(user, token)
                .orElseThrow(() -> {
                    log.warn("Invalid or unknown token '{}' used by user '{}'", token, currentEmail);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid or unknown token");
                });

        // Ensure that the token is not used
        if (verificationToken.isUsed()) {
            log.warn("User '{}' attempted to reuse token '{}'", currentEmail, token);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token already used");
        }

        // Ensure the token is not expired
        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.warn("User '{}' attempted to use expired token '{}'", currentEmail, token);
            throw new ResponseStatusException(HttpStatus.GONE, "Verification token has expired");
        }

        // Apply email update and clean token entry
        String newEmail = verificationToken.getTargetEmail();
        user.setEmail(newEmail);
        userRepository.save(user);

        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);

        // Generate a new user response
        LoginResponse response = LoginResponse.buildResponse(token, 86400000, user);

        //userSocket.sendUpdatedUser(response.user());
        log.info("Updated user '{}' broadcasted through WebSocket", currentEmail);

        return response;
    }

    private static String generateToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

}
