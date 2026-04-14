package com.deliveryland.backend.user.service;

import com.deliveryland.backend.auth.VerificationTokenRepository;
import com.deliveryland.backend.auth.dto.LoginResponse;
import com.deliveryland.backend.auth.model.VerificationToken;
import com.deliveryland.backend.auth.model.VerificationType;
import com.deliveryland.backend.common.notification.EmailService;
import com.deliveryland.backend.common.security.JwtService;
import com.deliveryland.backend.common.security.SecurityUtil;
import com.deliveryland.backend.user.UserRepository;
import com.deliveryland.backend.user.dto.UpdateUserDTO;
import com.deliveryland.backend.user.dto.UserResponse;
import com.deliveryland.backend.user.model.User;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import com.deliveryland.backend.common.util.TokenGenerator;

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
        User user = SecurityUtil.getCurrentUser(userRepository);

        return UserResponse.user(user);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "user", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()"),
            @CacheEvict(value = "users", allEntries = true)
    })
    public LoginResponse updateUserDetails(UpdateUserDTO dto) {
        User user = SecurityUtil.getCurrentUser(userRepository);
        log.info("User '{}' initiated updateUserDetails request", user.getEmail());

        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setContactNumber(dto.getContactNumber());

        userRepository.save(user);
        log.info("User '{}' details updated successfully", user.getEmail());

        // Generate a new user response
        LoginResponse response = LoginResponse.buildResponse(jwtService.generateToken(user), 86400000, user);

        // userSocket.sendUpdatedUser(response.user());
        log.info("Updated user '{}' broadcasted through WebSocket", user.getEmail());

        return response;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "user", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()"),
            @CacheEvict(value = "users", allEntries = true)
    })
    public void deleteUser() {
        User user = SecurityUtil.getCurrentUser(userRepository);
        log.info("Starting user deletion process for '{}'.", user.getEmail());

        // Delete everything associated with the user
        verificationTokenRepository.deleteByUser(user);
        userRepository.delete(user);
        log.info("User '{}' successfully deleted.", user.getEmail());
        emailService.sendAccountDeletionEmail(user.getEmail(), user.getFirstName() + " " + user.getLastName());
    }

    @Transactional
    public void changeEmailRequest(String newEmail) {
        User user = SecurityUtil.getCurrentUser(userRepository);
        String currentEmail = user.getEmail();
        log.info("User '{}' initiated email change request to '{}'", currentEmail, newEmail);

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
        String token = TokenGenerator.generateShortToken();
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
    @Caching(evict = {
            @CacheEvict(value = "user", allEntries = true),
            @CacheEvict(value = "users", allEntries = true)
    })
    public LoginResponse verifyChangeEmailRequest(String token) {
        User user = SecurityUtil.getCurrentUser(userRepository);
        String currentEmail = user.getEmail();
        log.info("User '{}' attempting to verify an email change using token: {}", currentEmail, token);

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

}
