package com.portal.application.port.out;

import com.portal.domain.DashboardSnapshot;
import com.portal.domain.Role;

public interface DashboardContentPort {
    DashboardSnapshot loadFor(Role role);
}
