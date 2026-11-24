package com.durak.mechanics;

import com.durak.cards.Card;
import com.durak.cards.Suit;

/** Чисті правила «Підкидного дурня» без жодного стану гри. */
public class GameRules {

    /**
     * Чи може defenseCard побити attackCard.
     * Та сама масть і старший ранг, або козир проти некозирної карти.
     */
    public boolean canBeat(Card defenseCard, Card attackCard, Suit trumpSuit) {
        if (defenseCard == null || attackCard == null) {
            return false;
        }
        boolean attackIsTrump = attackCard.isTrump(trumpSuit);
        boolean defenseIsTrump = defenseCard.isTrump(trumpSuit);

        if (defenseCard.getSuit() == attackCard.getSuit()) {
            return defenseCard.getRank().getValue() > attackCard.getRank().getValue();
        }
        return defenseIsTrump && !attackIsTrump;
    }

    /** Сила карти для порівнянь у ШІ: козирі завжди сильніші за будь-яку некозирну. */
    public int strength(Card card, Suit trumpSuit) {
        return card.getRank().getValue() + (card.isTrump(trumpSuit) ? 20 : 0);
    }
}
