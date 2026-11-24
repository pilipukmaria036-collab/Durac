
package com.durak.players;

import com.durak.cards.Card;
import com.durak.cards.Rank;
import com.durak.cards.Suit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    // Перевірка створення гравця
    @Test
    void constructorShouldSetNameAndType() {
        Player player = new Player("Вікторія", PlayerType.LOCAL);

        assertEquals("Вікторія", player.getName());
        assertEquals(PlayerType.LOCAL, player.getType());
    }

    // Перевірка створення порожньої руки
    @Test
    void newPlayerShouldHaveEmptyHand() {
        Player player = new Player("Вікторія", PlayerType.LOCAL);

        assertTrue(player.getHand().isEmpty());
        assertFalse(player.hasCards());
        assertEquals(0, player.getCardCount());
    }

    // Перевірка наявності карт
    @Test
    void hasCardsShouldReturnTrueWhenHandIsNotEmpty() {
        Player player = new Player("Вікторія", PlayerType.LOCAL);

        player.getHand().addCard(
                new Card(Suit.HEARTS, Rank.SIX)
        );

        assertTrue(player.hasCards());
    }

    // Перевірка кількості карт
    @Test
    void getCardCountShouldReturnCorrectNumber() {
        Player player = new Player("Вікторія", PlayerType.LOCAL);

        player.getHand().addCard(
                new Card(Suit.HEARTS, Rank.SIX)
        );
        player.getHand().addCard(
                new Card(Suit.SPADES, Rank.SEVEN)
        );

        assertEquals(2, player.getCardCount());
    }

    // Перевірка створення бота
    @Test
    void botPlayerShouldHaveBotType() {
        BotPlayer bot = new BotPlayer("Bot");

        assertEquals("Bot", bot.getName());
        assertEquals(PlayerType.BOT, bot.getType());
    }
}
