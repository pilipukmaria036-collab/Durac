package com.durak.game;

import com.durak.cards.Card;
import com.durak.players.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public enum GameState {
    WAITING_FOR_PLAYERS, // очікування гравців
    PREPARING, // підготовка
    PLAYING, // гра
    ROUND_FINISHED, // раунд завершено
    GAME_FINISHED; // гру завершено
}
