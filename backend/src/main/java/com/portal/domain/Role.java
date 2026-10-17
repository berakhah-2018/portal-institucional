package com.portal.domain;

public enum Role {
    ADMIN(3),
    EDITOR(2),
    LECTOR(1);

    private final int visibilityLevel;

    Role(int visibilityLevel) {
        this.visibilityLevel = visibilityLevel;
    }

    public boolean canView(Role minimumRole) {
        return visibilityLevel >= minimumRole.visibilityLevel;
    }
}
