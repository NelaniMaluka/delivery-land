package com.deliveryland.backend.user;

import com.deliveryland.backend.common.security.ApplicationUserRole;
import com.deliveryland.backend.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByRole(ApplicationUserRole role, Pageable pageable);

    @Query("""
                SELECT u FROM User u
                WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    Page<User> searchUsers(@Param("q") String query, Pageable pageable);

}
