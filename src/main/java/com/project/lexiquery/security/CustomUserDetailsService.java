package com.project.lexiquery.security;

import com.project.lexiquery.entity.AppUser;
import com.project.lexiquery.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public CustomUserDetailsService(UserRepository users) { this.users = users; }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser u = users.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("not found"));
        boolean locked = u.getLockedUntil() != null && u.getLockedUntil().isAfter(Instant.now());
        return User.withUsername(u.getUsername())
                .password(u.getPassword())
                .roles(u.getRole().name())
                .accountLocked(locked)
                .disabled(!u.isEnabled())
                .build();
    }
}
