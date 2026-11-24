package com.durak.cards;

import com.durak.settings.AppSettings;
import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Завантажує та кешує зображення карт і сорочок. */
public class CardImageProvider {

    private static final Map<String, Image> CACHE = new HashMap<>();

    private static final String DEFAULT_FOLDER = "/images/cards/";

    /**
     * Зображення лицьової сторони карти з обраної в налаштуваннях колоди: hearts_ace.png тощо.
     * Якщо в обраній колоді такого файлу немає — береться карта зі стандартної колоди.
     */
    public static Image getImage(Card card) {
        if (card == null) return null;

        String fileName = card.getSuit().name().toLowerCase() + "_" + card.getRank().name().toLowerCase() + ".png";
        Image image = load(AppSettings.getDeckFolder() + fileName);
        return image != null ? image : load(DEFAULT_FOLDER + fileName);
    }

    /** Назви колод, для яких у resources справді є картинки (перевіряється наявність hearts_ace.png). */
    public static List<String> getAvailableDecks() {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, String> deck : AppSettings.DECKS.entrySet()) {
            if (CardImageProvider.class.getResource(deck.getValue() + "hearts_ace.png") != null) {
                result.add(deck.getKey());
            }
        }
        if (result.isEmpty()) {
            result.add("Піксельна");
        }
        return result;
    }

    /** Сорочка карти за вибором у налаштуваннях (із запасним варіантом cards/back.png). */
    public static Image getBackImage() {
        Image image = load("/images/card-backs/back_" + AppSettings.getCardBackFileId() + ".png");
        return image != null ? image : load("/images/cards/back.png");
    }

    /** Чи варто малювати без згладжування (піксель-арт). Для великих картинок — згладжування ввімкнене. */
    public static boolean isPixelArt(Image image) {
        return image != null && image.getWidth() <= 200;
    }

    private static Image load(String path) {
        if (CACHE.containsKey(path)) {
            return CACHE.get(path);
        }
        Image image = null;
        try (InputStream stream = CardImageProvider.class.getResourceAsStream(path)) {
            if (stream == null) {
                System.out.println("Увага: не знайдено картинку " + path);
            } else {
                image = new Image(stream);
            }
        } catch (Exception e) {
            System.out.println("Помилка завантаження картинки: " + path);
        }
        CACHE.put(path, image);
        return image;
    }
}
