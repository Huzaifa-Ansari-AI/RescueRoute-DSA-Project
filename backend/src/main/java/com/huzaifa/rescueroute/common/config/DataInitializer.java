package com.huzaifa.rescueroute.common.config;

import com.huzaifa.rescueroute.entity.User;
import com.huzaifa.rescueroute.entity.UserRole;
import com.huzaifa.rescueroute.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Ensures a working Administrator account exists on application startup.
 * Automatically initializes or resets the default admin credentials for development and testing.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        initializeAdminUser("admin@rescueroute.com", "RescueRoute System Admin", "Admin@123456");
        initializeAdminUser("admin@rescueroute.local", "RescueRoute Local Admin", "Admin@123456");
    }

    private void initializeAdminUser(String email, String fullName, String rawPassword) {
        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> existingUser = userRepository.findByEmail(normalizedEmail);

        if (existingUser.isEmpty()) {
            User admin = User.builder()
                    .fullName(fullName)
                    .email(normalizedEmail)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .role(UserRole.ADMIN)
                    .isActive(true)
                    .build();
            userRepository.save(admin);
            log.info("[RescueRoute] Successfully created default ADMIN account: {} (password: {})", normalizedEmail, rawPassword);
        } else {
            User user = existingUser.get();
            boolean updated = false;

            if (user.getRole() != UserRole.ADMIN) {
                user.setRole(UserRole.ADMIN);
                updated = true;
            }
            if (!user.isActive()) {
                user.setActive(true);
                updated = true;
            }
            // If the user has a placeholder hash or needs a reset, encode valid password
            if (user.getPasswordHash() == null || user.getPasswordHash().contains("placeholder") || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
                user.setPasswordHash(passwordEncoder.encode(rawPassword));
                updated = true;
            }

            if (updated) {
                userRepository.save(user);
                log.info("[RescueRoute] Synchronized active ADMIN account: {} (password: {})", normalizedEmail, rawPassword);
            }
        }
    }
}
