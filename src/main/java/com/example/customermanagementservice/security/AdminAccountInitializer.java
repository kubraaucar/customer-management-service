package com.example.customermanagementservice.security;

import com.example.customermanagementservice.model.AppUser;
import com.example.customermanagementservice.model.Role;
import com.example.customermanagementservice.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * At startup, makes sure the administrator from app.security.admin.* exists and has that password.
 * To change the admin password, change ADMIN_PASSWORD and restart the app.
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppSecurityProperties.Admin admin;

    public AdminAccountInitializer(AppUserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   AppSecurityProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.admin = properties.admin();
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureAdminAccount();
    }

    void ensureAdminAccount() {
        AppUser user = userRepository.findByUsername(admin.username()).orElse(null);
        if (user == null) {
            userRepository.save(new AppUser(admin.username(), passwordEncoder.encode(admin.password()), Role.ADMIN));
            log.info("Created administrator account '{}'", admin.username());
            return;
        }

        boolean changed = false;
        if (!passwordEncoder.matches(admin.password(), user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(admin.password()));
            log.info("Updated the password of administrator account '{}'", admin.username());
            changed = true;
        }
        if (user.getRole() != Role.ADMIN) {
            user.setRole(Role.ADMIN);
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
        }
    }
}
