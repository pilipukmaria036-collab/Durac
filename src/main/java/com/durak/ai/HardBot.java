package com.durak.ai;

import com.durak.cards.Card;
import com.durak.cards.Suit;
import com.durak.game.GameManager;
import com.durak.game.TurnPhase;
import com.durak.mechanics.GameRules;
import com.durak.players.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Складний бот:
 * <ul>
 *   <li>береже козирі, поки колода не закінчилась, і скидає найслабші некозирні карти (особливо пари);</li>
 *   <li>пам'ятає карти, які суперник забрав зі столу, і не атакує тим, що той точно відіб'є;</li>
 *   <li>захищається найдешевшими картами, заздалегідь перевіряє, чи зможе побити все, і береться,
 *       якщо відбій коштував би надто дорого (кілька козирів / старший козир на дрібницю);</li>
 *   <li>підкидає, коли це безпечно (дрібні некозирні карти) та доточує, коли захисник уже береться.</li>
 * </ul>
 */
public class HardBot implements BotStrategy {

    private final GameRules rules = new GameRules();

    @Override
    public BotMove decide(GameManager gm, Player bot) {
        if (gm.canTake(bot)) {
            return defend(gm, bot);
        }
        if (gm.getPhase() == TurnPhase.ATTACK) {
            return lead(gm, bot);
        }
        return throwIn(gm, bot);
    }

    // ------------------------------------------------------------------ атака

    private BotMove lead(GameManager gm, Player bot) {
        Suit trump = gm.getCurrentTrump();
        List<Card> hand = bot.getHand().getCards();
        Player defender = gm.getDefender();
        List<Card> known = gm.getKnownCards(defender);
        boolean endgame = gm.getDeck().isEmpty();

        Card best = null;
        double bestScore = Double.MAX_VALUE;
        for (Card card : hand) {
            double score = card.getRank().getValue();
            if (card.isTrump(trump)) {
                score += endgame ? 8 : 20;
            }
            // Пари та трійки зручно скидати разом.
            long sameRank = hand.stream().filter(c -> c.getRank() == card.getRank()).count();
            if (!card.isTrump(trump) && sameRank > 1) {
                score -= 2.5 * (sameRank - 1);
            }
            // Те, що суперник точно відіб'є вже відомою картою, — слабкий вибір.
            if (!known.isEmpty()) {
                boolean beatable = known.stream().anyMatch(k -> rules.canBeat(k, card, trump));
                score += beatable ? 3 : -3;
            }
            if (score < bestScore) {
                bestScore = score;
                best = card;
            }
        }
        return BotMove.attack(best);
    }

    // ------------------------------------------------------------------ підкидання

    private BotMove throwIn(GameManager gm, Player bot) {
        List<Card> legal = new ArrayList<>(gm.legalAttackCards(bot));
        if (legal.isEmpty()) {
            return BotMove.pass();
        }
        Suit trump = gm.getCurrentTrump();
        boolean endgame = gm.getDeck().isEmpty();
        boolean taking = gm.isDefenderTaking();

        legal.sort(Comparator.comparingInt(c -> rules.strength(c, trump)));

        for (Card card : legal) {
            boolean isTrump = card.isTrump(trump);
            if (taking) {
                // Захисник усе одно бере: додаємо все зайве, козирі лише в кінцівці.
                if (!isTrump || endgame) {
                    return BotMove.attack(card);
                }
            } else {
                // Захисник відбивається: підкидаємо тільки дрібне, щоб не віддавати сильні карти.
                boolean cheap = !isTrump && card.getRank().getValue() <= 10;
                if ((cheap && bot.getHand().size() > 2) || (endgame && !isTrump)) {
                    return BotMove.attack(card);
                }
            }
        }
        return BotMove.pass();
    }

    // ------------------------------------------------------------------ захист

    private BotMove defend(GameManager gm, Player bot) {
        Suit trump = gm.getCurrentTrump();
        List<Card> unbeaten = new ArrayList<>(gm.getUnbeatenCards());
        unbeaten.sort(Comparator.comparingInt((Card c) -> rules.strength(c, trump)).reversed());

        Set<Card> used = new HashSet<>();
        List<Card[]> plan = new ArrayList<>(); // {карта захисту, ціль}
        int trumpsSpent = 0;
        int highestTrumpSpent = 0;

        for (Card target : unbeaten) {
            Card cheapest = null;
            for (Card card : bot.getHand().getCards()) {
                if (used.contains(card) || !rules.canBeat(card, target, trump)) {
                    continue;
                }
                if (cheapest == null
                        || rules.strength(card, trump) < rules.strength(cheapest, trump)) {
                    cheapest = card;
                }
            }
            if (cheapest == null) {
                return BotMove.take(); // всі карти побити не вдасться — не марнуємо їх
            }
            used.add(cheapest);
            plan.add(new Card[]{cheapest, target});
            if (cheapest.isTrump(trump) && !target.isTrump(trump)) {
                trumpsSpent++;
                highestTrumpSpent = Math.max(highestTrumpSpent, cheapest.getRank().getValue());
            }
        }

        boolean deckHasCards = !gm.getDeck().isEmpty();
        if (deckHasCards && wouldBeTooExpensive(gm, trumpsSpent, highestTrumpSpent, unbeaten, trump)) {
            return BotMove.take();
        }

        // Спершу б'ємо найдешевшою відповіддю з плану.
        Card[] step = plan.get(0);
        for (Card[] candidate : plan) {
            if (rules.strength(candidate[0], trump) < rules.strength(step[0], trump)) {
                step = candidate;
            }
        }
        return BotMove.defend(step[0], step[1]);
    }

    private boolean wouldBeTooExpensive(GameManager gm, int trumpsSpent, int highestTrumpSpent,
                                        List<Card> unbeaten, Suit trump) {
        if (trumpsSpent == 0) {
            return false;
        }
        int attackValue = 0;
        for (Card c : unbeaten) {
            attackValue = Math.max(attackValue, c.getRank().getValue());
        }
        boolean lowAttack = attackValue <= 10 && unbeaten.stream().noneMatch(c -> c.isTrump(trump));
        if (trumpsSpent >= 2 && lowAttack) {
            return true;
        }
        // Старший козир (король/туз) на дрібну карту поки в колоді ще багато карт — задорого.
        return highestTrumpSpent >= 13 && lowAttack && gm.getDeck().size() > 4;
    }
}
