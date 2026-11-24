package com.durak.ai;

import com.durak.cards.Card;

/** Рішення комп'ютерного гравця. */
public final class BotMove {

    public enum Type { ATTACK, DEFEND, TAKE, PASS }

    private final Type type;
    private final Card card;
    private final Card target;

    private BotMove(Type type, Card card, Card target) {
        this.type = type;
        this.card = card;
        this.target = target;
    }

    /** Атакувати або підкинути card. */
    public static BotMove attack(Card card) { return new BotMove(Type.ATTACK, card, null); }

    /** Побити карту target картою card. */
    public static BotMove defend(Card card, Card target) { return new BotMove(Type.DEFEND, card, target); }

    public static BotMove take() { return new BotMove(Type.TAKE, null, null); }

    public static BotMove pass() { return new BotMove(Type.PASS, null, null); }

    public Type getType() { return type; }
    public Card getCard() { return card; }
    public Card getTarget() { return target; }

    @Override
    public String toString() {
        return type + (card != null ? " " + card : "") + (target != null ? " -> " + target : "");
    }
}
