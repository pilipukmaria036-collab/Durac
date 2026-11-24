package com.durak.ai;

import com.durak.game.GameManager;
import com.durak.players.Player;

/** Стратегія комп'ютерного гравця: за станом гри вирішує, що робити саме зараз. */
public interface BotStrategy {

    /** Викликається, коли рушій чекає дію від bot (gm.getActor() == bot). */
    BotMove decide(GameManager gm, Player bot);
}
