package com.durak.ai;

import com.durak.game.GameManager;
import com.durak.game.GameMode;
import com.durak.game.GameResult;
import com.durak.players.BotPlayer;
import com.durak.players.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BotPlayTest {

    /** Партія бот проти бота: жодного нелегального ходу, гра завершується, карти не губляться. */
    private GameResult playOut(long seed, int players) {
        GameManager gm = new GameManager(seed);
        List<Player> list = new ArrayList<>();
        for (int i = 0; i < players; i++) {
            list.add(new BotPlayer("B" + i, i % 2 == 0 ? Difficulty.EASY : Difficulty.HARD));
        }
        gm.startGame(list, GameMode.VS_BOT);
        int guard = 0;
        while (!gm.isGameOver()) {
            assertTrue(++guard < 5000, "гра зависла");
            Player actor = gm.getActor();
            assertNotNull(actor);
            BotMove move = ((BotPlayer) actor).getStrategy().decide(gm, actor);
            assertNotNull(move);
            assertTrue(BotBrain.isLegal(gm, actor, move), "нелегальний хід: " + move);
            BotBrain.apply(gm, actor, move);
            assertEquals(36, gm.countAllCards());
        }
        return gm.getResult();
    }

    @Test
    void botsFinishManyGamesForEveryPlayerCount() {
        for (int players = 2; players <= 4; players++) {
            for (int seed = 0; seed < 150; seed++) {
                GameResult result = playOut(seed, players);
                assertTrue(result.isFinished());
            }
        }
    }

    @Test
    void hardBotBeatsEasyBotMostOfTheTime() {
        int hardLost = 0;
        int easyLost = 0;
        for (int i = 0; i < 300; i++) {
            Player hard = new BotPlayer("H", Difficulty.HARD);
            Player easy = new BotPlayer("E", Difficulty.EASY);
            GameManager gm = new GameManager(5000L + i);
            gm.startGame(i % 2 == 0 ? List.of(hard, easy) : List.of(easy, hard), GameMode.VS_BOT);
            while (!gm.isGameOver()) {
                Player actor = gm.getActor();
                BotBrain.apply(gm, actor, BotBrain.decide(gm, actor));
            }
            GameResult r = gm.getResult();
            if (r.getLoser() == hard) hardLost++;
            if (r.getLoser() == easy) easyLost++;
        }
        assertTrue(easyLost > hardLost * 2, "складний бот має бути помітно сильнішим: " + easyLost + " vs " + hardLost);
    }
}
