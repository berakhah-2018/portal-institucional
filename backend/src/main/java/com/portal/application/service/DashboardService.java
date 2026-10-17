package com.portal.application.service;

import com.portal.application.port.in.GetDashboardUseCase;
import com.portal.application.port.out.DashboardContentPort;
import com.portal.domain.DashboardSnapshot;
import com.portal.domain.Role;
import org.springframework.stereotype.Service;

@Service
public class DashboardService implements GetDashboardUseCase {
    private final DashboardContentPort dashboardContent;

    public DashboardService(DashboardContentPort dashboardContent) {
        this.dashboardContent = dashboardContent;
    }

    @Override
    public DashboardSnapshot execute(Role role) {
        return dashboardContent.loadFor(role);
    }
}
