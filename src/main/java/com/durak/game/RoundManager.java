package com.durak.game;

import com.durak.players.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Controls a round's lifecycle and prepares the ordered card-draw queue. */
public class RoundManager {
    private Game game;
    private boolean roundFinished = true;
    private int roundNumber;
    private final List<Player> cardDrawOrder = new ArrayList<>();

    public RoundManager() {
    }

    public RoundManager(Game game) {
        this.game = game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    /** Starts the first round after GameManager has initialized roles. */
    public void startRound() {
        requireGame();
        if (game.getGameState() == GameState.GAME_FINISHED) {
            throw new IllegalStateException("Cannot start a round after the game has finished");
        }
        if (game.getPlayerList().size() < 2) {
            throw new IllegalStateException("At least two players are required to start a round");
        }
        game.clearAttackPairs();
        cardDrawOrder.clear();
        roundFinished = false;
        roundNumber++;
        game.setGameState(GameState.PLAYING);
    }

    /** Ends the round and prepares, but does not itself perform, card drawing. */
    public void endRound() {
        requireGame();
        if (roundFinished) {
            throw new IllegalStateException("The current round has already finished");
        }
        roundFinished = true;
        game.setGameState(GameState.ROUND_FINISHED);
        prepareCardDraw();
    }

    /** Starts the next round after GameManager has performed the card draw. */
    public void nextRound() {
        requireGame();
        if (game.getGameState() == GameState.GAME_FINISHED) {
            return;
        }
        if (!roundFinished) {
            throw new IllegalStateException("The current round must finish before the next round starts");
        }
        startRound();
    }

    /**
     * Order: attacker first, then the remaining players in circular order,
     * with the defender moved to the end. GameManager performs the actual draw.
     */
    public void prepareCardDraw() {
        requireGame();
        cardDrawOrder.clear();
        List<Player> players = game.getPlayerList();
        if (players.isEmpty()) {
            return;
        }

        Player attacker = game.getTurnManager().getAttacker();
        Player defender = game.getTurnManager().getDefender();
        int attackerIndex = players.indexOf(attacker);
        if (attackerIndex < 0) {
            attackerIndex = 0;
        }

        for (int offset = 0; offset < players.size(); offset++) {
            Player candidate = players.get((attackerIndex + offset) % players.size());
            if (candidate != defender) {
                cardDrawOrder.add(candidate);
            }
        }
        if (defender != null && players.contains(defender)) {
            cardDrawOrder.add(defender);
        }
    }

    public List<Player> getCardDrawOrder() {
        return Collections.unmodifiableList(cardDrawOrder);
    }

    public boolean isRoundFinished() {
        return roundFinished;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    /** Compatibility overload for the earlier version of the class. */
    public void startRound(GameState state, List<Player> players) {
        if (state != GameState.PLAYING || players == null || players.size() < 2) {
            return;
        }
        if (game == null) {
            roundFinished = false;
            roundNumber++;
            cardDrawOrder.clear();
        } else {
            startRound();
        }
    }

    private void requireGame() {
        if (game == null) {
            throw new IllegalStateException("RoundManager is not attached to a Game");
        }
    }
}
