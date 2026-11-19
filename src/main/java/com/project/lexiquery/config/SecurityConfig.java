package com.project.lexiquery.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                // JSP views are rendered via FORWARD dispatch; must be allowed or the login page is blocked
                .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                .requestMatchers("/login", "/css/**", "/error", "/actuator/health").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/query", true).permitAll())
            .logout(l -> l.logoutSuccessUrl("/login?logout").invalidateHttpSession(true).deleteCookies("JSESSIONID"))
            .sessionManagement(s -> s
                .sessionFixation(fix -> fix.migrateSession())
                .maximumSessions(2))
            .headers(h -> h
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER))
                .contentSecurityPolicy(c -> c.policyDirectives(
                    "default-src 'self'; style-src 'self'; script-src 'self'; img-src 'self' data:; form-action 'self'; frame-ancestors 'none'")));
        // CSRF protection stays ENABLED (default); every JSP form carries the token.
        return http.build();
    }
}
