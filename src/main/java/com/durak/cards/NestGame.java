package com.durak.cards;

import com.durak.cards.Card;
import com.durak.cards.Deck;
import com.durak.players.Hand;

public class NestGame {
    public static void main(String[] args) {
        System.out.println("--- Початок тестування колоди ---");

        // 1. Створюємо колоду
        Deck deck = new Deck();
        System.out.println("Створено колоду. Карт у ній: " + deck.size()); // Має бути 36

        // 2. Перемішуємо
        deck.shuffle();
        System.out.println("Колоду перемішано.");

        // 3. Створюємо руку гравця (ніби це рука Учасника 4)
        Hand playerHand = new Hand();

        // 4. Роздаємо 6 карт
        for (int i = 0; i < 6; i++) {
            Card drawnCard = deck.drawCard();
            if (drawnCard != null) {
                playerHand.addCard(drawnCard);
            }
        }

        // 5. Перевіряємо результати
        System.out.println("\n--- Результати роздачі ---");
        System.out.println("Карт у руці гравця: " + playerHand.size()); // Має бути 6
        System.out.println("Карт залишилось у колоді: " + deck.size()); // Має бути 30

        System.out.println("\nКарти в руці гравця:");
        // Проходимось циклом по всіх картах у руці і виводимо їх
        for (Card card : playerHand.getCards()) {
            System.out.println("- " + card.toString());
        }

        // 6. Тестуємо видалення карти (ніби гравець зіграв карту)
        System.out.println("\n--- Тест: гравець зіграв першу карту ---");
        if (!playerHand.isEmpty()) {
            Card firstCard = playerHand.getCards().get(0); // Беремо першу карту
            playerHand.removeCard(firstCard);              // Видаляємо її
            System.out.println("Видалено карту: " + firstCard);
            System.out.println("Карт у руці після ходу: " + playerHand.size()); // Має стати 5
        }

        System.out.println("\n--- Тестування завершено успішно! ---");
    }
}