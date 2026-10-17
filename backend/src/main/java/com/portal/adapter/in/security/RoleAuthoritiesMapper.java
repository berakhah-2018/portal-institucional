package com.portal.adapter.in.security;

import com.portal.domain.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class RoleAuthoritiesMapper implements GrantedAuthoritiesMapper {
    private final Map<String, Role> roleByEmail = new HashMap<>();

    public RoleAuthoritiesMapper(@Value("${app.auth.role-mappings:}") String mappings) {
        for (String mapping : mappings.split(",")) {
            String[] pair = mapping.strip().split("=", 2);
            if (pair.length == 2 && !pair[0].isBlank()) {
                try {
                    roleByEmail.put(pair[0].strip().toLowerCase(Locale.ROOT), Role.valueOf(pair[1].strip().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ignored) { }
            }
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Set<GrantedAuthority> mapped = new LinkedHashSet<>(authorities);
        String email = authorities.stream()
                .filter(OAuth2UserAuthority.class::isInstance)
                .map(OAuth2UserAuthority.class::cast)
                .filter(authority -> Boolean.TRUE.equals(authority.getAttributes().get("email_verified")))
                .map(authority -> authority.getAttributes().get("email"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst()
                .orElse("");
        Role role = roleByEmail.getOrDefault(email.toLowerCase(Locale.ROOT), Role.LECTOR);
        mapped.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        return mapped;
    }
}
