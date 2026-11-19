package com.project.lexiquery.config;

import com.project.lexiquery.entity.AppUser;
import com.project.lexiquery.entity.Role;
import com.project.lexiquery.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/** Creates the first admin. No hard-coded password: supply APP_ADMIN_PASSWORD or a random one is logged once. */
@Component
public class BootstrapDataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(BootstrapDataInitializer.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String adminUser;
    private final String adminPassword;

    public BootstrapDataInitializer(UserRepository users, PasswordEncoder encoder,
                                    @Value("${app.bootstrap.admin-username}") String adminUser,
                                    @Value("${app.bootstrap.admin-password:}") String adminPassword) {
        this.users = users; this.encoder = encoder; this.adminUser = adminUser; this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (users.existsByUsername(adminUser)) return;
        String pwd = adminPassword;
        if (pwd == null || pwd.isBlank()) {
            byte[] b = new byte[12];
            new SecureRandom().nextBytes(b);
            pwd = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
            log.warn("==== Generated credentials (shown once) -> users: {}, analyst, viewer | password: {} ====", adminUser, pwd);
        }
        save(adminUser, pwd, Role.ADMIN);
        // demo accounts to try the role model; delete or change them in production
        save("analyst", pwd, Role.ANALYST);
        save("viewer", pwd, Role.VIEWER);
    }

    private void save(String name, String rawPassword, Role role) {
        if (users.existsByUsername(name)) return;
        AppUser u = new AppUser();
        u.setUsername(name);
        u.setPassword(encoder.encode(rawPassword));
        u.setRole(role);
        users.save(u);
    }
}
