package com.durak.ui;

import com.durak.cards.Card;
import com.durak.cards.CardImageProvider;
import com.durak.cards.Rank;
import com.durak.cards.Suit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CardImageProviderTest {

    @Test
    public void testGetImageNotNull() {
        Card card = new Card(Suit.HEARTS, Rank.ACE);
        // Перевіряємо, чи не падає програма при спробі взяти картинку
        assertNotNull(CardImageProvider.getImage(card), "Картинка для Туза Черви має існувати");
    }

    @Test
    public void testGetBackImage() {
        // Перевіряємо сорочку
        assertNotNull(CardImageProvider.getBackImage(), "Сорочка карти має існувати");
    }
}