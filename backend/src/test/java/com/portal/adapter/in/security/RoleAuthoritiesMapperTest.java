package com.portal.adapter.in.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleAuthoritiesMapperTest {
    @Test
    void mapsVerifiedConfiguredEmailToAdministrator() {
        RoleAuthoritiesMapper mapper = new RoleAuthoritiesMapper("admin@example.com=ADMIN");
        OAuth2UserAuthority user = new OAuth2UserAuthority("OAUTH2_USER", Map.of(
                "email", "admin@example.com",
                "email_verified", true
        ));

        List<? extends GrantedAuthority> authorities = List.copyOf(mapper.mapAuthorities(List.of(user)));

        assertTrue(authorities.stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
    }

    @Test
    void givesUnverifiedEmailOnlyReaderRole() {
        RoleAuthoritiesMapper mapper = new RoleAuthoritiesMapper("admin@example.com=ADMIN");
        OAuth2UserAuthority user = new OAuth2UserAuthority("OAUTH2_USER", Map.of(
                "email", "admin@example.com",
                "email_verified", false
        ));

        List<? extends GrantedAuthority> authorities = List.copyOf(mapper.mapAuthorities(List.of(user)));

        assertTrue(authorities.stream().anyMatch(authority -> "ROLE_LECTOR".equals(authority.getAuthority())));
    }
}
