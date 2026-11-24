package com.durak.ai;

import com.durak.cards.Card;
import com.durak.game.GameManager;
import com.durak.game.TurnPhase;
import com.durak.mechanics.GameRules;
import com.durak.players.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Легкий бот: ходить майже навмання, не рахує карти й часто розкидається козирями.
 * Має трохи «людських» помилок: інколи береться без потреби, інколи підкидає зайве.
 */
public class EasyBot implements BotStrategy {

    private final Random random = new Random();
    private final GameRules rules = new GameRules();

    @Override
    public BotMove decide(GameManager gm, Player bot) {
        if (gm.canTake(bot)) {
            return defend(gm);
        }
        if (gm.getPhase() == TurnPhase.ATTACK) {
            return lead(gm, bot);
        }
        return throwIn(gm, bot);
    }

    private BotMove lead(GameManager gm, Player bot) {
        List<Card> cards = new ArrayList<>(bot.getHand().getCards());
        List<Card> plain = nonTrump(cards, gm);
        List<Card> pool = (!plain.isEmpty() && random.nextInt(100) < 75) ? plain : cards;
        return BotMove.attack(pool.get(random.nextInt(pool.size())));
    }

    private BotMove throwIn(GameManager gm, Player bot) {
        List<Card> legal = gm.legalAttackCards(bot);
        if (legal.isEmpty()) {
            return BotMove.pass();
        }
        int chance = gm.isDefenderTaking() ? 65 : 30;
        if (random.nextInt(100) >= chance) {
            return BotMove.pass();
        }
        List<Card> plain = nonTrump(legal, gm);
        List<Card> pool = plain.isEmpty() ? legal : plain;
        if (plain.isEmpty() && random.nextInt(100) < 70) {
            return BotMove.pass(); // козирі без потреби майже не підкидає
        }
        return BotMove.attack(pool.get(random.nextInt(pool.size())));
    }

    private BotMove defend(GameManager gm) {
        // Іноді береться, навіть коли міг би відбитися.
        if (random.nextInt(100) < 8) {
            return BotMove.take();
        }
        List<Card> targets = new ArrayList<>(gm.getUnbeatenCards());
        Collections.shuffle(targets, random);
        for (Card target : targets) {
            List<Card> options = new ArrayList<>(gm.legalDefenseCards(target));
            if (options.isEmpty()) {
                continue;
            }
            Card choice;
            if (random.nextBoolean()) {
                choice = cheapest(options, gm);
            } else {
                choice = options.get(random.nextInt(options.size()));
            }
            return BotMove.defend(choice, target);
        }
        return BotMove.take();
    }

    private Card cheapest(List<Card> cards, GameManager gm) {
        Card best = cards.get(0);
        for (Card card : cards) {
            if (rules.strength(card, gm.getCurrentTrump()) < rules.strength(best, gm.getCurrentTrump())) {
                best = card;
            }
        }
        return best;
    }

    private List<Card> nonTrump(List<Card> cards, GameManager gm) {
        List<Card> result = new ArrayList<>();
        for (Card card : cards) {
            if (!card.isTrump(gm.getCurrentTrump())) {
                result.add(card);
            }
        }
        return result;
    }
}
