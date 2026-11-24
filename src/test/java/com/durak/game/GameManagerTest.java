package com.durak.game;

import com.durak.cards.Card;
import com.durak.cards.Rank;
import com.durak.cards.Suit;
import com.durak.players.Player;
import com.durak.players.PlayerType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameManagerTest {

    private GameManager gm;
    private Player a;
    private Player b;
    private Suit trump;
    private Suit s1;
    private Suit s2;

    /** Нова гра двох гравців; руки далі підміняються під конкретний сценарій. */
    private void newGame(int players) {
        gm = new GameManager(12345L);
        List<Player> list = new ArrayList<>();
        for (int i = 0; i < players; i++) {
            list.add(new Player("P" + i, PlayerType.LOCAL));
        }
        gm.startGame(list);
        trump = gm.getCurrentTrump();
        List<Suit> plain = new ArrayList<>();
        for (Suit s : Suit.values()) {
            if (s != trump) plain.add(s);
        }
        s1 = plain.get(0);
        s2 = plain.get(1);
        a = gm.getAttacker();
        b = gm.getDefender();
    }

    private static void rig(Player p, Card... cards) {
        for (Card c : p.getHand().getCards()) {
            p.getHand().removeCard(c);
        }
        for (Card c : cards) {
            p.getHand().addCard(c);
        }
    }

    private static Card c(Suit s, Rank r) {
        return new Card(s, r);
    }

    @Test
    void dealGivesSixCardsToEveryoneAndDeckKeepsTheRest() {
        for (int n = 2; n <= 4; n++) {
            newGame(n);
            for (Player p : gm.getGame().getPlayerList()) {
                assertEquals(6, p.getHand().size());
            }
            assertEquals(36 - 6 * n, gm.getDeck().size());
            assertEquals(36, gm.countAllCards());
            assertNotNull(gm.getCurrentTrump());
            assertEquals(TurnPhase.ATTACK, gm.getPhase());
            assertSame(gm.getAttacker(), gm.getActor());
        }
    }

    @Test
    void defenderCannotAttackAndAttackerMustOwnTheCard() {
        newGame(2);
        rig(a, c(s1, Rank.SEVEN), c(s2, Rank.NINE));
        rig(b, c(s1, Rank.EIGHT));
        assertThrows(IllegalArgumentException.class, () -> gm.attack(b, c(s1, Rank.EIGHT)));
        assertThrows(IllegalArgumentException.class, () -> gm.attack(a, c(s1, Rank.KING)));
    }

    @Test
    void sameSuitMustBeHigherAndTrumpBeatsOnlyNonTrump() {
        newGame(2);
        assertTrue(gm.canBeat(c(s1, Rank.EIGHT), c(s1, Rank.SEVEN)));
        assertFalse(gm.canBeat(c(s1, Rank.SIX), c(s1, Rank.SEVEN)));
        assertFalse(gm.canBeat(c(s2, Rank.ACE), c(s1, Rank.SEVEN)));
        assertTrue(gm.canBeat(c(trump, Rank.SIX), c(s1, Rank.ACE)));
        assertFalse(gm.canBeat(c(s1, Rank.ACE), c(trump, Rank.SIX)));
        assertTrue(gm.canBeat(c(trump, Rank.SEVEN), c(trump, Rank.SIX)));
        assertFalse(gm.canBeat(c(trump, Rank.SIX), c(trump, Rank.SEVEN)));
    }

    @Test
    void throwInAllowsOnlyRanksAlreadyOnTable() {
        newGame(2);
        rig(a, c(s1, Rank.SEVEN), c(s2, Rank.SEVEN), c(s1, Rank.NINE), c(s2, Rank.KING));
        rig(b, c(s1, Rank.EIGHT), c(s1, Rank.TEN), c(s2, Rank.ACE), c(trump, Rank.SIX),
                c(trump, Rank.SEVEN), c(s2, Rank.JACK));

        gm.attack(a, c(s1, Rank.SEVEN));
        // у атакуючого є друга сімка — йому дають змогу підкинути
        assertEquals(TurnPhase.THROW_IN, gm.getPhase());
        assertSame(a, gm.getActor());
        assertEquals(List.of(c(s2, Rank.SEVEN)), gm.legalAttackCards(a));
        assertThrows(IllegalArgumentException.class, () -> gm.attack(a, c(s1, Rank.NINE)));

        gm.attack(a, c(s2, Rank.SEVEN));
        assertEquals(2, gm.getTablePairs().size());

        // більше підкидати нічого → хід захисника
        assertEquals(TurnPhase.DEFEND, gm.getPhase());
        assertSame(b, gm.getActor());
    }

    @Test
    void attackerWithNothingToThrowPassesTurnToDefenderAutomatically() {
        newGame(2);
        rig(a, c(s1, Rank.SEVEN), c(s2, Rank.KING));
        rig(b, c(s1, Rank.EIGHT), c(s1, Rank.TEN), c(s2, Rank.ACE), c(trump, Rank.SIX),
                c(trump, Rank.SEVEN), c(s2, Rank.JACK));
        gm.attack(a, c(s1, Rank.SEVEN));
        assertEquals(TurnPhase.DEFEND, gm.getPhase());
        assertSame(b, gm.getActor());
    }

    @Test
    void defendingEverythingEndsRoundAndDefenderBecomesAttacker() {
        newGame(2);
        rig(a, c(s1, Rank.SEVEN), c(s2, Rank.KING));
        rig(b, c(s1, Rank.EIGHT), c(s1, Rank.TEN), c(s2, Rank.ACE), c(trump, Rank.SIX),
                c(trump, Rank.SEVEN), c(s2, Rank.JACK));

        gm.attack(a, c(s1, Rank.SEVEN));
        gm.defend(b, c(s1, Rank.SEVEN), c(s1, Rank.EIGHT));

        // У атакуючого немає ні сімок, ні вісімок — раунд завершується автоматично.
        assertEquals(1, gm.getDiscardPile().size() / 2);
        assertTrue(gm.getTablePairs().isEmpty());
        assertSame(b, gm.getAttacker());
        assertSame(a, gm.getDefender());
        assertEquals(TurnPhase.ATTACK, gm.getPhase());
        assertEquals(6, a.getHand().size());   // добір до шести
        assertEquals(6, b.getHand().size());
    }

    @Test
    void wrongDefenseIsRejected() {
        newGame(2);
        rig(a, c(s1, Rank.NINE), c(s2, Rank.KING));
        rig(b, c(s1, Rank.EIGHT), c(s2, Rank.ACE), c(trump, Rank.SIX));
        gm.attack(a, c(s1, Rank.NINE));
        assertThrows(IllegalArgumentException.class,
                () -> gm.defend(b, c(s1, Rank.NINE), c(s1, Rank.EIGHT)));
        assertThrows(IllegalArgumentException.class,
                () -> gm.defend(b, c(s1, Rank.NINE), c(s2, Rank.ACE)));
        gm.defend(b, c(s1, Rank.NINE), c(trump, Rank.SIX));
        assertTrue(gm.getTablePairs().isEmpty() || gm.getTablePairs().get(0).isBeaten());
    }

    @Test
    void takingLetsAttackerThrowInThenDefenderTakesEverything() {
        newGame(2);
        rig(a, c(s1, Rank.SEVEN), c(s2, Rank.SEVEN), c(s1, Rank.KING), c(s2, Rank.KING));
        rig(b, c(s2, Rank.SIX), c(s1, Rank.SIX), c(s2, Rank.EIGHT), c(s1, Rank.EIGHT),
                c(s2, Rank.NINE), c(s1, Rank.NINE));

        gm.attack(a, c(s1, Rank.SEVEN));
        gm.pass(a);                                   // «готово» — хід захисника
        assertTrue(gm.canTake(b));
        gm.take(b);                                   // захисник береться
        assertTrue(gm.isDefenderTaking());
        assertSame(a, gm.getActor());                 // ще можна підкинути
        assertEquals(List.of(c(s2, Rank.SEVEN)), gm.legalAttackCards(a));
        gm.attack(a, c(s2, Rank.SEVEN));
        // більше нічого підкидати → захисник забирає 2 карти
        assertTrue(gm.getTablePairs().isEmpty());
        assertTrue(b.getHand().contains(c(s1, Rank.SEVEN)));
        assertTrue(b.getHand().contains(c(s2, Rank.SEVEN)));
        // Після «беру» атакує той, хто сидить після захисника, тобто знову a (2 гравці).
        assertSame(a, gm.getAttacker());
        assertSame(b, gm.getDefender());
    }

    @Test
    void numberOfAttackCardsIsLimitedByDefenderHandSize() {
        newGame(2);
        Suit s3 = null;
        for (Suit s : Suit.values()) {
            if (s != trump && s != s1 && s != s2) s3 = s;
        }
        // У захисника лише 2 карти → на столі може лежати щонайбільше 2 атакуючі карти.
        rig(a, c(s1, Rank.SEVEN), c(s2, Rank.SEVEN), c(s3, Rank.SEVEN), c(trump, Rank.SEVEN), c(s1, Rank.KING));
        rig(b, c(s1, Rank.EIGHT), c(s2, Rank.EIGHT));

        gm.attack(a, c(s1, Rank.SEVEN));
        assertEquals(TurnPhase.THROW_IN, gm.getPhase());
        gm.attack(a, c(s2, Rank.SEVEN));

        assertEquals(2, gm.getPairLimit());
        // ліміт вичерпано, хоча ще є сімки — підкидати більше не можна
        assertEquals(TurnPhase.DEFEND, gm.getPhase());
        assertTrue(gm.legalAttackCards(a).isEmpty());
    }

    @Test
    void deckAndTableCardsAreConservedOverRandomPlay() {
        newGame(3);
        assertEquals(36, gm.countAllCards());
        Player first = gm.getActor();
        Card lead = first.getHand().getCard(0);
        gm.attack(first, lead);
        assertEquals(36, gm.countAllCards());
    }
}
