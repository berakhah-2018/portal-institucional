package com.portal.adapter.in.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfiguration {
        @Value("${app.frontend-url:http://localhost:5173}")
        private String frontendUrl;

        @Value("${app.frontend-origin:http://localhost:5173}")
        private String frontendOrigin;

        @Value("${server.servlet.session.cookie.same-site:lax}")
        private String cookieSameSite;

        @Value("${server.servlet.session.cookie.secure:false}")
        private boolean cookieSecure;

        @Bean
        CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(List.of(frontendOrigin));
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
                configuration.setAllowCredentials(true);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<ClientRegistrationRepository> registrationRepository,
            RoleAuthoritiesMapper roleAuthoritiesMapper
    ) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookieCustomizer(cookie -> cookie
                .sameSite(cookieSameSite)
                .secure(cookieSecure)
                .path("/"));

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/status", "/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/editor/**").hasAnyRole("ADMIN", "EDITOR")
                        .requestMatchers(HttpMethod.POST, "/api/images").hasAnyRole("ADMIN", "EDITOR")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(logout -> logout.logoutSuccessUrl(frontendUrl));

        ClientRegistrationRepository registrations = registrationRepository.getIfAvailable();
        if (registrations != null) {
            DefaultOAuth2AuthorizationRequestResolver requestResolver =
                    new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
            requestResolver.setAuthorizationRequestCustomizer(builder -> builder.additionalParameters(parameters -> {
                parameters.put("access_type", "offline");
                parameters.put("prompt", "consent");
            }));
            http.oauth2Login(oauth -> oauth
                    .defaultSuccessUrl(frontendUrl, true)
                    .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(requestResolver))
                    .userInfoEndpoint(userInfo -> userInfo.userAuthoritiesMapper(roleAuthoritiesMapper)));
        }
        return http.build();
    }
}
