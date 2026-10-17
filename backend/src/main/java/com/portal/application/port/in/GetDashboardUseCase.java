package com.portal.application.port.in;

import com.portal.domain.DashboardSnapshot;
import com.portal.domain.Role;

public interface GetDashboardUseCase {
    DashboardSnapshot execute(Role role);
}
