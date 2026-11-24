
package com.durak.players;


import com.durak.cards.Card;
import com.durak.cards.Suit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Hand {

    private final List<Card> cards = new ArrayList<>();

    // Додавання карти
    public void addCard(Card card) {
        cards.add(Objects.requireNonNull(card, "Card cannot be null"));
    }

    // Видалення карти
    public boolean removeCard(Card card) {
        return cards.remove(card);
    }

    // Перевірка наявності карти
    public boolean contains(Card card) {
        return cards.contains(card);
    }

    // Отримання карти за індексом
    public Card getCard(int index) {
        return cards.get(index);
    }

    // Отримання всіх карт без можливості змінювати руку
    public List<Card> getCards() {
        return List.copyOf(cards);
    }

    // Кількість карт
    public int size() {
        return cards.size();
    }

    // Перевірка порожньої руки
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    // Сортування карт за мастю та рангом
    // Сортування: некозирні зліва, козирні справа
    public void sortCards(Suit trumpSuit) {
        cards.sort(
                Comparator
                        .comparing((Card card) ->
                                card.getSuit() == trumpSuit
                        )
                        .thenComparingInt(card -> card.getSuit().ordinal())
                        .thenComparingInt(card -> card.getRank().ordinal())
        );
    }
}
