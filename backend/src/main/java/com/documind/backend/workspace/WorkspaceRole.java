package com.documind.backend.workspace;

public enum WorkspaceRole {
    ADMIN(3),
    REVIEWER(2),
    VIEWER(1);

    private final int rank;

    WorkspaceRole(int rank) {
        this.rank = rank;
    }

    public boolean hasAtLeast(WorkspaceRole required) {
        return this.rank >= required.rank;
    }
}
