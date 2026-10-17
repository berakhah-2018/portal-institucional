package com.portal.adapter.in.rest;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {
    private final boolean googleConfigured;

    public AuthController(ObjectProvider<ClientRegistrationRepository> registrationRepository) {
        this.googleConfigured = registrationRepository.getIfAvailable() != null;
    }

    @GetMapping("/api/auth/status")
    public Map<String, Object> status(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return Map.of("authenticated", false, "googleConfigured", googleConfigured);
        }
        Object principal = authentication.getPrincipal();
        String email = principal instanceof OAuth2User user ? user.getAttribute("email") : authentication.getName();
        String role = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .findFirst()
                .orElse("LECTOR");
        return Map.of("authenticated", true, "googleConfigured", googleConfigured, "email", email, "role", role);
    }
}
