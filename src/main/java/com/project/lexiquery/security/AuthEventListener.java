package com.project.lexiquery.security;

import com.project.lexiquery.repository.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/** Brute-force protection: lock the account for 15 minutes after 5 consecutive bad passwords. */
@Component
public class AuthEventListener {
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK = Duration.ofMinutes(15);
    private final UserRepository users;

    public AuthEventListener(UserRepository users) { this.users = users; }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent e) {
        users.findByUsername(e.getAuthentication().getName()).ifPresent(u -> {
            int n = u.getFailedAttempts() + 1;
            if (n >= MAX_ATTEMPTS) { u.setLockedUntil(Instant.now().plus(LOCK)); n = 0; }
            u.setFailedAttempts(n);
            users.save(u);
        });
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent e) {
        users.findByUsername(e.getAuthentication().getName()).ifPresent(u -> {
            if (u.getFailedAttempts() != 0 || u.getLockedUntil() != null) {
                u.setFailedAttempts(0); u.setLockedUntil(null); users.save(u);
            }
        });
    }
}
