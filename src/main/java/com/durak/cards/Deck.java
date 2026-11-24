package com.durak.cards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Deck {
    private final List<Card> cards = new ArrayList<>(); // кінець списку = верх колоди
    private Card trumpCard; // відкритий козир: лежить знизу колоди й береться останнім

    public Deck() {
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
    }

    public void shuffle() {
        shuffle(new Random());
    }

    /** Перемішування з власним генератором (потрібно для відтворюваних тестів). */
    public void shuffle(Random random) {
        Collections.shuffle(cards, random);
        if (!cards.isEmpty()) {
            // Нижня карта (індекс 0) — козир.
            this.trumpCard = cards.get(0);
        }
    }

    public Card drawCard() {
        if (cards.isEmpty()) {
            return null;
        }
        return cards.remove(cards.size() - 1);
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public int size() {
        return cards.size();
    }

    public Suit getTrumpSuit() {
        return trumpCard != null ? trumpCard.getSuit() : null;
    }

    public Card getTrumpCard() {
        return trumpCard;
    }

    /** Відкритий козир ще лежить під колодою (його ще ніхто не взяв). */
    public boolean isTrumpCardInDeck() {
        return !cards.isEmpty();
    }
}
