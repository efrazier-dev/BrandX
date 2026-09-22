package com.brandx.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Authentication and authorization.
 *
 * <p>Form login against the {@code app_users} table, with the roles on each account
 * driving what its screens offer and what its POSTs are allowed to do. This is the
 * deliberately simple option for the current stage: the front door is the only part
 * that would change to move to SSO later, because the role model, the
 * {@code @PreAuthorize} annotations on the services and the {@code sec:authorize}
 * guards in the templates all sit behind it.
 *
 * <p>Two things arrive for free with the starter and are load-bearing here:
 * <ul>
 *   <li><strong>CSRF protection</strong>, on by default. Every state-changing form in
 *       this app already posts through {@code th:action}, so Thymeleaf injects the token
 *       automatically and no template needed a hidden field added. The previously
 *       tokenless POSTs are now covered.</li>
 *   <li><strong>Session fixation protection</strong>, which rotates the session id on
 *       login.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * The H2 console, dev profile only.
     *
     * <p>A separate chain rather than a few {@code permitAll} lines in the main one: the
     * console needs CSRF disabled and framing allowed, and neither of those should exist
     * in a chain that also serves the real application. Scoping it to {@code @Profile("dev")}
     * means the relaxed rules cannot be switched on in another environment by setting a
     * property — the beans simply are not there.
     *
     * <p>{@code @Order(1)} puts it ahead of {@link #appSecurity}, whose matcher would
     * otherwise claim these requests first.
     */
    @Bean
    @Order(1)
    @Profile("dev")
    public SecurityFilterChain h2ConsoleSecurity(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/h2-console/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                // The console is a frameset; the default DENY would render it blank.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain appSecurity(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        // The login screen and the assets it needs to render.
                        .requestMatchers("/login", "/css/**", "/js/**", "/favicon.ico").permitAll()
                        // Everything else — including the read-only list and view screens,
                        // which were public until now — requires a signed-in user. Which
                        // *actions* each role may take is decided at the service layer, not
                        // here; see the @PreAuthorize annotations on ProductService,
                        // CustomerService and OrderService.
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        // No `true` second argument: on success Spring Security prefers the
                        // request that triggered the login, so a deep link survives it and
                        // only a direct visit to /login lands on the dashboard.
                        .defaultSuccessUrl("/")
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        // POST /logout, not GET: the default, and worth keeping — a GET
                        // logout can be triggered by any <img> tag on another site.
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                // A refused action is not a missing page, so it does not go through
                // GlobalExceptionHandler / not-found. It gets its own view, consistent with
                // this app's split between "no such thing" and "you may not do that".
                .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"))
                .build();
    }

    /**
     * Delegating encoder: hashes new passwords with BCrypt, and reads any hash whose
     * stored {id} prefix it recognises. That prefix is what makes a future move to a
     * different algorithm a data migration rather than a forced password reset for
     * everyone.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
