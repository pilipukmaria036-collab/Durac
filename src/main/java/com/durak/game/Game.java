package com.durak.game;

import com.durak.cards.AttackPair;
import com.durak.players.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Represents one Durak game and owns its shared state. */
public class Game {
    private GameMode gameMode;
    private GameState gameState = GameState.WAITING_FOR_PLAYERS;
    private RoundManager roundManager;
    private TurnManager turnManager;
    private GameResult gameResult;
    private final List<Player> players = new ArrayList<>();

    /** Cards currently on the table, stored as attack/defense pairs. */
    private final List<AttackPair> attackPairs = new ArrayList<>();

    public Game() {
        this.turnManager = new TurnManager();
        this.roundManager = new RoundManager(this);
    }

    public List<Player> getPlayerList() {
        return Collections.unmodifiableList(players);
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public GameState getGameState() {
        return gameState;
    }

    public GameResult getGameResult() {
        return gameResult;
    }

    public RoundManager getRoundManager() {
        return roundManager;
    }

    public TurnManager getTurnManager() {
        return turnManager;
    }

    /** Returns a read-only view of the current attack/defense pairs on the table. */
    public List<AttackPair> getAttackPairs() {
        return Collections.unmodifiableList(attackPairs);
    }

    public void addAttackPair(AttackPair attackPair) {
        attackPairs.add(Objects.requireNonNull(attackPair, "attackPair cannot be null"));
    }

    public void clearAttackPairs() {
        attackPairs.clear();
    }

    public void setGameResult(GameResult gameResult) {
        this.gameResult = gameResult;
    }

    public void setRoundManager(RoundManager roundManager) {
        this.roundManager = Objects.requireNonNull(roundManager, "roundManager cannot be null");
        this.roundManager.setGame(this);
    }

    public void setTurnManager(TurnManager turnManager) {
        this.turnManager = Objects.requireNonNull(turnManager, "turnManager cannot be null");
    }

    public void setGameState(GameState gameState) {
        this.gameState = Objects.requireNonNull(gameState, "gameState cannot be null");
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = Objects.requireNonNull(gameMode, "gameMode cannot be null");
    }

    /** Replaces the player list; calling this twice will not duplicate players. */
    public void setPlayerList(List<Player> players) {
        Objects.requireNonNull(players, "players cannot be null");
        List<Player> copy = new ArrayList<>(players);
        this.players.clear();
        this.players.addAll(copy);
    }
}
