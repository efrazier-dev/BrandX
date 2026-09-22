package com.brandx.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The two screens that belong to authentication itself.
 *
 * <p>No POST handlers: Spring Security's filters process the login and logout
 * submissions before they ever reach a controller. This only renders the views.
 */
@Controller
public class AuthController {

    /**
     * Both outcomes are query-string flags set by the security config's
     * {@code failureUrl} / {@code logoutSuccessUrl}, read directly by the template.
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Mapped with no HTTP method restriction, which matters more than it looks.
     *
     * <p>{@code accessDeniedPage} is reached by a servlet <em>forward</em>, and a forward
     * keeps the original request's method. Almost every refusal here comes from a
     * blocked POST — a delete, a create — so a {@code @GetMapping} would answer those
     * with 405 Method Not Allowed instead of 403, turning "you may not do that" into
     * what looks like a broken route.
     */
    @RequestMapping("/access-denied")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String accessDenied() {
        return "access-denied";
    }
}
