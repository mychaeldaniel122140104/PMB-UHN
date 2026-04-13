package com.uhn.pmb.entity;

public enum QuestionCategory {
    IPA("IPA - Sains"),
    IPS("IPS - Sosial"),
    PSIKOTES("Psikotes"),
    BAHASA("Bahasa Indonesia & Inggris");

    private final String displayName;

    QuestionCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
