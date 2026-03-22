package com.deliveryland.backend.auth;

import com.deliveryland.backend.auth.model.UserVerification;
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
public interface UserVerificationRepository extends JpaRepository<UserVerification, UUID> {
    Optional<UserVerification> findByUserAndToken(User user, String token);

    List<UserVerification> findByUser(User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserVerification uv WHERE uv.user = :user")
    int deleteByUser(@Param("user") User user);
}
