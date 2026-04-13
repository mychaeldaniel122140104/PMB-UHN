package com.uhn.pmb.entity;

public enum QuestionDifficulty {
    EASY("Mudah"),
    MEDIUM("Sedang"),
    HARD("Sulit");

    private final String displayName;

    QuestionDifficulty(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
