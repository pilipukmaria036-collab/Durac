
package com.durak.players;

import com.durak.cards.Card;
import com.durak.cards.Rank;
import com.durak.cards.Suit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HandTest {

    // Перевірка додавання карти
    @Test
    void addCardShouldIncreaseHandSize() {
        Hand hand = new Hand();
        Card card = new Card(Suit.HEARTS, Rank.SIX);

        hand.addCard(card);

        assertEquals(1, hand.size());
        assertTrue(hand.contains(card));
    }

    // Перевірка видалення карти
    @Test
    void removeCardShouldDecreaseHandSize() {
        Hand hand = new Hand();
        Card card = new Card(Suit.HEARTS, Rank.SIX);
        hand.addCard(card);

        boolean removed = hand.removeCard(card);

        assertTrue(removed);
        assertEquals(0, hand.size());
        assertFalse(hand.contains(card));
    }

    // Перевірка видалення карти, якої немає
    @Test
    void removeMissingCardShouldReturnFalse() {
        Hand hand = new Hand();
        Card card = new Card(Suit.HEARTS, Rank.SIX);

        assertFalse(hand.removeCard(card));
        assertEquals(0, hand.size());
    }

    // Перевірка contains()
    @Test
    void containsShouldFindExistingCard() {
        Hand hand = new Hand();
        Card card = new Card(Suit.HEARTS, Rank.SIX);
        hand.addCard(card);

        assertTrue(hand.contains(card));
    }

    // Перевірка отримання карти за індексом
    @Test
    void getCardShouldReturnCorrectCard() {
        Hand hand = new Hand();
        Card card = new Card(Suit.HEARTS, Rank.SIX);
        hand.addCard(card);

        assertEquals(card, hand.getCard(0));
    }

    // Перевірка кількості карт
    @Test
    void sizeShouldReturnCorrectNumber() {
        Hand hand = new Hand();

        hand.addCard(new Card(Suit.HEARTS, Rank.SIX));
        hand.addCard(new Card(Suit.SPADES, Rank.SEVEN));

        assertEquals(2, hand.size());
    }

    // Перевірка порожньої руки
    @Test
    void isEmptyShouldReturnCorrectResult() {
        Hand hand = new Hand();

        assertTrue(hand.isEmpty());

        hand.addCard(new Card(Suit.HEARTS, Rank.SIX));

        assertFalse(hand.isEmpty());
    }

    // Перевірка незмінності списку getCards()
    @Test
    void getCardsShouldReturnUnmodifiableList() {
        Hand hand = new Hand();
        hand.addCard(new Card(Suit.HEARTS, Rank.SIX));

        List<Card> cards = hand.getCards();

        assertEquals(1, cards.size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> cards.add(new Card(Suit.SPADES, Rank.SEVEN))
        );
    }

    // Перевірка сортування козирної карти в кінець
    @Test
    void sortCardsShouldPlaceTrumpCardsLast() {
        Hand hand = new Hand();

        Card trumpCard = new Card(Suit.HEARTS, Rank.SIX);
        Card normalCard = new Card(Suit.SPADES, Rank.SEVEN);

        hand.addCard(trumpCard);
        hand.addCard(normalCard);

        hand.sortCards(Suit.HEARTS);

        assertEquals(trumpCard, hand.getCard(hand.size() - 1));
    }
}
