package com.portal.adapter.in.rest;

import com.portal.application.port.in.GetDashboardUseCase;
import com.portal.domain.DashboardSnapshot;
import com.portal.domain.Role;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final GetDashboardUseCase getDashboard;

    public DashboardController(GetDashboardUseCase getDashboard) {
        this.getDashboard = getDashboard;
    }

    @GetMapping
    public DashboardSnapshot dashboard(Authentication authentication) {
        Role role = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> Role.valueOf(authority.substring(5)))
                .findFirst()
                .orElse(Role.LECTOR);
        return getDashboard.execute(role);
    }
}
