package com.durak.ai;

import com.durak.cards.Card;
import com.durak.game.GameManager;
import com.durak.game.TurnPhase;
import com.durak.players.BotPlayer;
import com.durak.players.Player;

import java.util.List;

/** Фабрика стратегій і безпечне виконання ходів бота. */
public final class BotBrain {

    private BotBrain() {
    }

    public static BotStrategy forDifficulty(Difficulty difficulty) {
        return difficulty == Difficulty.HARD ? new HardBot() : new EasyBot();
    }

    /** Приймає рішення за стратегією бота; у разі помилки повертає найбезпечніший легальний хід. */
    public static BotMove decide(GameManager gm, Player bot) {
        try {
            BotStrategy strategy = bot instanceof BotPlayer bp ? bp.getStrategy() : new EasyBot();
            BotMove move = strategy.decide(gm, bot);
            if (move != null && isLegal(gm, bot, move)) {
                return move;
            }
        } catch (RuntimeException ignored) {
            // падаємо назад на безпечний хід
        }
        return fallback(gm, bot);
    }

    /** Виконує хід на рушії. */
    public static void apply(GameManager gm, Player bot, BotMove move) {
        switch (move.getType()) {
            case ATTACK -> gm.attack(bot, move.getCard());
            case DEFEND -> gm.defend(bot, move.getTarget(), move.getCard());
            case TAKE -> gm.take(bot);
            case PASS -> gm.pass(bot);
        }
    }

    public static boolean isLegal(GameManager gm, Player bot, BotMove move) {
        switch (move.getType()) {
            case ATTACK:
                return gm.legalAttackCards(bot).contains(move.getCard());
            case DEFEND:
                return gm.canTake(bot)
                        && gm.getUnbeatenCards().contains(move.getTarget())
                        && gm.legalDefenseCards(move.getTarget()).contains(move.getCard());
            case TAKE:
                return gm.canTake(bot);
            case PASS:
                return gm.canPass(bot) && gm.getPhase() == TurnPhase.THROW_IN;
            default:
                return false;
        }
    }

    private static BotMove fallback(GameManager gm, Player bot) {
        if (gm.canTake(bot)) {
            return BotMove.take();
        }
        if (gm.getPhase() == TurnPhase.ATTACK) {
            List<Card> cards = gm.legalAttackCards(bot);
            if (!cards.isEmpty()) {
                return BotMove.attack(cards.get(0));
            }
        }
        return BotMove.pass();
    }
}
