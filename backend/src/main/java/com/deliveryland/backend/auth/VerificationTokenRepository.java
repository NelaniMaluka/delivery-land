package com.deliveryland.backend.auth;

import com.deliveryland.backend.auth.model.VerificationToken;
import com.deliveryland.backend.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    Optional<VerificationToken> findByUserAndToken(User user, String token);

    List<VerificationToken> findByUser(User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationToken uv WHERE uv.user = :user")
    int deleteByUser(@Param("user") User user);
}
