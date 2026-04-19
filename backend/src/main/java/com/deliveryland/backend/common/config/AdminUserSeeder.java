package com.deliveryland.backend.common.config;

import com.deliveryland.backend.common.security.ApplicationUserRole;
import com.deliveryland.backend.user.UserRepository;
import com.deliveryland.backend.user.model.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserSeeder(UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        String adminEmail = "admin@deliveryland.com";

        boolean adminExists = userRepository.existsByEmail(adminEmail);

        if (adminExists) {
            return;
        }

        User admin = User.builder()
                .firstName("System")
                .lastName("Admin")
                .email(adminEmail)
                .contactNumber("+27000000000")
                .password(passwordEncoder.encode("Admin@123"))
                .role(ApplicationUserRole.ADMIN)
                .enabled(true)
                .build();

        userRepository.save(admin);

        System.out.println("Default admin user created");
    }
}
