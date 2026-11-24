package com.durak.players;

import com.durak.ai.BotBrain;
import com.durak.ai.BotStrategy;
import com.durak.ai.Difficulty;

/** Комп'ютерний гравець. Рішення приймає стратегія відповідного рівня складності. */
public class BotPlayer extends Player {

    private final Difficulty difficulty;
    private final BotStrategy strategy;

    public BotPlayer(String name) {
        this(name, Difficulty.EASY);
    }

    public BotPlayer(String name, Difficulty difficulty) {
        super(name, PlayerType.BOT);
        this.difficulty = difficulty == null ? Difficulty.EASY : difficulty;
        this.strategy = BotBrain.forDifficulty(this.difficulty);
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public BotStrategy getStrategy() {
        return strategy;
    }
}
