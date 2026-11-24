package com.durak.players;

public class Player {

    private final String name;
    private final Hand hand;
    private final PlayerType type;

    public Player(String name, PlayerType type) {
        this.name = name;
        this.type = type;
        this.hand = new Hand();
    }

    public String getName() {
        return name;
    }

    public Hand getHand() {
        return hand;
    }

    public PlayerType getType() {
        return type;
    }

    public boolean isBot() {
        return type == PlayerType.BOT;
    }

    public boolean hasCards() {
        return !hand.isEmpty();
    }

    public int getCardCount() {
        return hand.size();
    }

    @Override
    public String toString() {
        return name;
    }
}
