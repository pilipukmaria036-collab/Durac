package com.durak.game;

import com.durak.cards.Card;
import com.durak.players.Player;

import java.util.List;

/**
 * Подія, яку генерує рушій. Інтерфейс користувача відтворює їх по черзі
 * (з анімацією), тому рушій сам нічого не знає про графіку.
 */
public final class GameEvent {

    public enum Type {
        /** Початкова роздача: player отримує card з колоди. */
        DEAL,
        /** player поклав атакуючу (або підкинуту) карту на стіл. */
        ATTACK,
        /** player побив attackCard картою card. */
        DEFEND,
        /** Захисник оголосив, що бере карти. */
        TAKE_DECLARED,
        /** Захисник забрав усі карти зі столу (cards). */
        TAKE,
        /** Карти зі столу пішли у відбій (cards). */
        DISCARD,
        /** Добір: player бере card з колоди. */
        DRAW,
        /** Гравець сказав «бито» / «досить». */
        PASS,
        /** Гру завершено. */
        GAME_OVER
    }

    private final Type type;
    private final Player player;
    private final Card card;
    private final Card attackCard;
    private final List<Card> cards;

    private GameEvent(Type type, Player player, Card card, Card attackCard, List<Card> cards) {
        this.type = type;
        this.player = player;
        this.card = card;
        this.attackCard = attackCard;
        this.cards = cards == null ? List.of() : List.copyOf(cards);
    }

    public static GameEvent deal(Player p, Card c) { return new GameEvent(Type.DEAL, p, c, null, null); }
    public static GameEvent attack(Player p, Card c) { return new GameEvent(Type.ATTACK, p, c, null, null); }
    public static GameEvent defend(Player p, Card attack, Card c) { return new GameEvent(Type.DEFEND, p, c, attack, null); }
    public static GameEvent takeDeclared(Player p) { return new GameEvent(Type.TAKE_DECLARED, p, null, null, null); }
    public static GameEvent take(Player p, List<Card> cards) { return new GameEvent(Type.TAKE, p, null, null, cards); }
    public static GameEvent discard(List<Card> cards) { return new GameEvent(Type.DISCARD, null, null, null, cards); }
    public static GameEvent draw(Player p, Card c) { return new GameEvent(Type.DRAW, p, c, null, null); }
    public static GameEvent pass(Player p) { return new GameEvent(Type.PASS, p, null, null, null); }
    public static GameEvent gameOver() { return new GameEvent(Type.GAME_OVER, null, null, null, null); }

    public Type getType() { return type; }
    public Player getPlayer() { return player; }
    public Card getCard() { return card; }
    public Card getAttackCard() { return attackCard; }
    public List<Card> getCards() { return cards; }

    @Override
    public String toString() {
        return type + (player != null ? " " + player.getName() : "")
                + (card != null ? " " + card : "")
                + (attackCard != null ? " on " + attackCard : "")
                + (cards.isEmpty() ? "" : " " + cards);
    }
}
