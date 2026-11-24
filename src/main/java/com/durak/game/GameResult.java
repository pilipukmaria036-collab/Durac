package com.durak.game;

import com.durak.players.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Підсумок партії. Останній гравець із картами — «дурень». */
public class GameResult {
    private boolean finished;
    private Player lastPlayerWithCards;
    private final List<Player> winners = new ArrayList<>();
    private final Map<Player, Integer> remainingCards = new LinkedHashMap<>();

    public void complete(List<Player> players, Player lastPlayerWithCards) {
        if (finished) {
            return;
        }
        this.lastPlayerWithCards = lastPlayerWithCards;
        winners.clear();
        remainingCards.clear();

        for (Player player : players) {
            remainingCards.put(player, player.getHand().size());
            if (player != lastPlayerWithCards) {
                winners.add(player);
            }
        }
        finished = true;
    }

    public boolean isFinished() {
        return finished;
    }

    /** Нічия: усі одночасно лишилися без карт. */
    public boolean isDraw() {
        return finished && lastPlayerWithCards == null;
    }

    /** Єдиний переможець або null, якщо переможців кілька / нічия. */
    public Player getWinner() {
        return winners.size() == 1 ? winners.get(0) : null;
    }

    public List<Player> getWinners() {
        return Collections.unmodifiableList(winners);
    }

    /** «Дурень» — гравець, що залишився з картами. */
    public Player getLoser() {
        return lastPlayerWithCards;
    }

    public Player getLastPlayerWithCards() {
        return lastPlayerWithCards;
    }

    public Map<Player, Integer> getRemainingCards() {
        return Collections.unmodifiableMap(remainingCards);
    }
}
