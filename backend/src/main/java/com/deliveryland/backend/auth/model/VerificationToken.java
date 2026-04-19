package com.deliveryland.backend.auth.model;

import com.deliveryland.backend.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "verification_tokens", indexes = {
                @Index(name = "idx_verification_token", columnList = "token"),
                @Index(name = "idx_verification_user", columnList = "user_id"),
                @Index(name = "idx_verification_type", columnList = "type")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerificationToken {

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        @Column(nullable = false, updatable = false)
        private UUID id;

        @NotNull
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "user_id", nullable = false, updatable = false)
        private User user;

        @NotNull
        @Column(nullable = false, unique = true, length = 128)
        private String token;

        @NotNull
        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 30)
        private VerificationType type;

        @Column(name = "target_email", length = 255, nullable = false, updatable = false)
        private String targetEmail;

        @NotNull
        @Column(nullable = false, updatable = false)
        @Builder.Default
        private LocalDateTime createdAt = LocalDateTime.now();

        @NotNull
        @Column(nullable = false)
        @Builder.Default
        private LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(15);
}