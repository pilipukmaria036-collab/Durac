package com.durak.cards;

public enum Rank {
    SIX(6),     // шістка
    SEVEN(7),   // сімка
    EIGHT(8),   // вісімка
    NINE(9),    // дев'ятка
    TEN(10),    // десятка
    JACK(11),   // валет
    QUEEN(12),  // дама
    KING(13),   // король
    ACE(14);    // туз

    private final int value; // щоб випадково нічого не змінити

    // Конструктор для збереження числового значення
    Rank(int value) {
        this.value = value;
    }

    // Метод, який повертає числове значення (від 6 до 14)
    public int getValue() {
        return value;
    }
}