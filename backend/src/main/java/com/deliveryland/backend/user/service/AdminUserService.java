package com.deliveryland.backend.user.service;

import com.deliveryland.backend.auth.VerificationTokenRepository;
import com.deliveryland.backend.auth.model.VerificationToken;
import com.deliveryland.backend.auth.model.VerificationType;
import com.deliveryland.backend.common.notification.EmailService;
import com.deliveryland.backend.common.security.SecurityUtil;
import com.deliveryland.backend.user.UserRepository;
import com.deliveryland.backend.user.dto.UserResponse;
import com.deliveryland.backend.user.model.AccountStatus;
import com.deliveryland.backend.user.model.User;
import com.deliveryland.backend.common.security.ApplicationUserRole;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

import com.deliveryland.backend.common.util.TokenGenerator;

@Log4j2
@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final VerificationTokenRepository verificationTokenRepository;

    public AdminUserService(UserRepository userRepository, EmailService emailService, VerificationTokenRepository verificationTokenRepository) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.verificationTokenRepository = verificationTokenRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'all-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        log.debug("Admin fetched users page={}", pageable.getPageNumber());

        return userRepository.findAll(pageable)
                .map(UserResponse::user);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'role-' + #role + '-' + #pageable.pageNumber")
    public Page<UserResponse> getUsersByRole(ApplicationUserRole role, Pageable pageable) {
        log.debug("Admin fetching users by role: {}", role);

        return userRepository.findByRole(role, pageable)
                .map(UserResponse::user);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'email-' + #email + '-' + #pageable.pageNumber")
    public Page<UserResponse> searchByEmail(String email, Pageable pageable) {
        log.debug("Admin search email={}", email);

        return userRepository.findByEmailContainingIgnoreCase(email, pageable)
                .map(UserResponse::user);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'name-' + #query + '-' + #pageable.pageNumber")
    public Page<UserResponse> searchByName(String query, Pageable pageable) {
        log.debug("Admin search name={}", query);

        return userRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(query, query, pageable)
                .map(UserResponse::user);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "user", key = "#userId")
    public UserResponse getUserById(UUID userId) {
        log.info("Admin fetching user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found: {}", userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });

        return UserResponse.user(user);
    }

    @Transactional
    @CacheEvict(value = {"users", "user"}, allEntries = true)
    public UserResponse updateAccountStatus(UUID userId, AccountStatus status) {
        User admin = SecurityUtil.getCurrentUser(userRepository);

        log.info("ADMIN_ACTION: update_account_status | adminId={} | adminEmail={} | targetUserId={} | newStatus={}",
                admin.getId(),
                admin.getEmail(),
                userId,
                status
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("ADMIN_ACTION_FAILED: update_account_status | adminId={} | targetUserId={} | reason=USER_NOT_FOUND",
                            admin.getId(), userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });

        AccountStatus oldStatus = user.getAccountStatus();

        if (oldStatus == status) {
            log.warn("ADMIN_ACTION_SKIPPED: update_account_status | adminId={} | targetUserId={} | reason=STATUS_UNCHANGED",
                    admin.getId(), userId);
            return UserResponse.user(user);
        }

        user.setAccountStatus(status);
        userRepository.save(user);

        log.info("ADMIN_ACTION_SUCCESS: update_account_status | adminId={} | targetUserId={} | oldStatus={} | newStatus={}",
                admin.getId(),
                userId,
                oldStatus,
                status
        );

        return UserResponse.user(user);
    }

    @Transactional
    @CacheEvict(value = {"users", "user"}, allEntries = true)
    public void forcePasswordReset(UUID userId) {
        User admin = SecurityUtil.getCurrentUser(userRepository);

        log.info("ADMIN_ACTION: force_password_reset | adminId={} | adminEmail={} | targetUserId={}",
                admin.getId(),
                admin.getEmail(),
                userId
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("ADMIN_ACTION_FAILED: force_password_reset | adminId={} | targetUserId={} | reason=USER_NOT_FOUND",
                            admin.getId(), userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });

        // Delete old tokens (clean state)
        verificationTokenRepository.deleteByUser(user);
        log.debug("Old verification tokens cleared | targetUserId={}", userId);

        // Generate token
        String token = TokenGenerator.generateShortToken();

        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(user)
                .type(VerificationType.PASSWORD_RESET)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .build();

        verificationTokenRepository.save(verificationToken);

        emailService.sendPasswordResetEmail(user.getEmail(), token);

        log.info("ADMIN_ACTION_SUCCESS: force_password_reset | adminId={} | targetUserId={} | targetEmail={}",
                admin.getId(),
                userId,
                user.getEmail()
        );
    }

    @Transactional
    @CacheEvict(value = {"users", "user"}, allEntries = true)
    public void deleteUser(UUID userId) {
        User admin = SecurityUtil.getCurrentUser(userRepository);

        log.info("ADMIN_ACTION: delete_user | adminId={} | adminEmail={} | targetUserId={}",
                admin.getId(),
                admin.getEmail(),
                userId
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("ADMIN_ACTION_FAILED: delete_user | adminId={} | targetUserId={} | reason=USER_NOT_FOUND",
                            admin.getId(), userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });

        userRepository.delete(user);

        log.info("ADMIN_ACTION_SUCCESS: delete_user | adminId={} | targetUserId={} | targetEmail={}",
                admin.getId(),
                userId,
                user.getEmail()
        );
    }
}