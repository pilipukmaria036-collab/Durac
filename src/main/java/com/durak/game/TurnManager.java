package com.durak.game;

import com.durak.players.Player;

import java.util.List;

/** Controls player order and attacker/defender roles. */
public class TurnManager {
    private Player currentPlayer;
    private Player nextPlayer;
    private Player attacker;
    private Player defender;
    private int currentPlayerID = -1;
    private int nextPlayerID = -1;

    public void initialize(List<Player> players, Player firstAttacker) {
        if (players == null || players.size() < 2 || firstAttacker == null) {
            throw new IllegalArgumentException("At least two players and a first attacker are required");
        }
        int index = players.indexOf(firstAttacker);
        if (index < 0) {
            throw new IllegalArgumentException("The attacker must belong to this game");
        }
        Player firstDefender = getPlayerAfter(firstAttacker, players, false);
        setRoles(firstAttacker, firstDefender, players);
    }

    public void setRoles(Player attacker, Player defender, List<Player> players) {
        if (attacker == null || defender == null || players == null
                || !players.contains(attacker) || !players.contains(defender)
                || attacker == defender) {
            throw new IllegalArgumentException("Attacker and defender must be different players in this game");
        }
        this.attacker = attacker;
        this.defender = defender;
        this.currentPlayer = attacker;
        this.currentPlayerID = players.indexOf(attacker);
        this.nextPlayer = defender;
        this.nextPlayerID = players.indexOf(defender);
    }

    public void setCurrentPlayer(Player currentPlayer, int currentPlayerID) {
        this.currentPlayer = currentPlayer;
        this.currentPlayerID = currentPlayerID;
    }

    public void setNextPlayer(Player nextPlayer) {
        this.nextPlayer = nextPlayer;
    }

    public void setAttacker(Player attacker) {
        this.attacker = attacker;
    }

    public void setDefender(Player defender) {
        this.defender = defender;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    /** Returns the next player cached by the last initialization/turn change. */
    public Player getNextPlayer() {
        return nextPlayer;
    }

    public Player getAttacker() {
        return attacker;
    }

    public Player getDefender() {
        return defender;
    }

    public int getCurrentPlayerID() {
        return currentPlayerID;
    }

    /** Finds the next player without changing the current turn. */
    public Player getNextPlayer(List<Player> players, boolean skipPlayersWithoutCards) {
        return getPlayerAfter(currentPlayer, players, skipPlayersWithoutCards);
    }

    /**
     * Finds the next player in list order. Empty hands are skipped only when
     * the caller says the deck is empty and such players have left the game.
     */
    public Player getPlayerAfter(Player player, List<Player> players, boolean skipPlayersWithoutCards) {
        if (players == null || players.size() < 2 || player == null) {
            return null;
        }
        int startIndex = players.indexOf(player);
        if (startIndex < 0) {
            return null;
        }
        for (int step = 1; step < players.size(); step++) {
            int candidateIndex = (startIndex + step) % players.size();
            Player candidate = players.get(candidateIndex);
            if (!skipPlayersWithoutCards || !candidate.getHand().isEmpty()) {
                return candidate;
            }
        }
        return null;
    }

    /** Advances to the next player; does not skip empty hands by default. */
    public void nextTurn(List<Player> players) {
        nextTurn(players, false);
    }

    public void nextTurn(List<Player> players, boolean skipPlayersWithoutCards) {
        Player candidate = getPlayerAfter(currentPlayer, players, skipPlayersWithoutCards);
        if (candidate == null) {
            return;
        }
        currentPlayer = candidate;
        currentPlayerID = players.indexOf(candidate);
        Player following = getPlayerAfter(currentPlayer, players, skipPlayersWithoutCards);
        nextPlayer = following;
        nextPlayerID = following == null ? -1 : players.indexOf(following);
    }
}
