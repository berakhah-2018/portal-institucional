package com.portal.domain;

import java.util.List;

public record DashboardSnapshot(
        Role role,
        String title,
        String subtitle,
        List<Metric> metrics,
        boolean demoData
) {
    public record Metric(String label, String value, String note) { }
}
