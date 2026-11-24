package com.durak.ai;

/** Рівні складності комп'ютерного суперника. */
public enum Difficulty {
    EASY("Легкий"),
    HARD("Складний");

    private final String title;

    Difficulty(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public static Difficulty fromTitle(String title) {
        for (Difficulty d : values()) {
            if (d.title.equals(title)) {
                return d;
            }
        }
        return EASY;
    }
}
