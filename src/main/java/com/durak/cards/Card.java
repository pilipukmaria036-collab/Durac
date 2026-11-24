package com.durak.cards;
import java.util.Objects;

public class Card {
    private final Suit suit; // масть
    private final Rank rank; //ранг
//консAтруктор
    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }
//гетер
    public Suit getSuit() {
        return suit;
    }
//гетер
    public Rank getRank() {
        return rank;
    }
//перевірка на козир, надходить обєкт класу карта і перевіряється на окзир
    public boolean isTrump(Suit trumpSuit) {
        return this.suit == trumpSuit;
    }
//поки що потрібно щоб мені в консоль виводилось
    @Override
    public String toString() {
        return rank + " of " + suit;
    }
    //для порівнняння карт
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Використовуємо instanceof — це те, що радить IntelliJ
        if (!(o instanceof Card)) return false;
        Card card = (Card) o;
        return suit == card.suit && rank == card.rank;
    }
    @Override
    public int hashCode() {
        return Objects.hash(suit, rank);
    }


}
