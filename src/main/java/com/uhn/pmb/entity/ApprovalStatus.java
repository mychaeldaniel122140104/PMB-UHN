package com.uhn.pmb.entity;

public enum ApprovalStatus {
    PENDING("Menunggu Approval"),
    APPROVED("Disetujui"),
    REJECTED("Ditolak");

    private final String displayName;

    ApprovalStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
