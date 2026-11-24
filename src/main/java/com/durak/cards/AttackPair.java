package com.durak.cards;

public class AttackPair {
    private final Card attackCard;  // Карта атаки (ніколи не null)
    private Card defenseCard;       // Карта захисту (може бути null, якщо ще не побита)

    // Конструктор створюється лише з картою атаки
    public AttackPair(Card attackCard) {
        this.attackCard = attackCard;
        this.defenseCard = null;
    }

    // Метод для встановлення карти захисту (коли хтось відбивається)
    public void setDefenseCard(Card defenseCard) {
        this.defenseCard = defenseCard;
    }

    // Геттер для карти атаки
    public Card getAttackCard() {
        return attackCard;
    }

    // Геттер для карти захисту
    public Card getDefenseCard() {
        return defenseCard;
    }

    // Перевіряє, чи вже побита ця карта
    public boolean isBeaten() {
        return defenseCard != null;
    }
}