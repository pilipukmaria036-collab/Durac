package com.durak.audio;

/**
 * Звукові ефекти гри. Файли кладіть у {@code src/main/resources/sounds/}:
 * шукається {@code <файл>.wav}, потім {@code <файл>.mp3}.
 * Якщо файлу немає — звук просто не відтворюється (помилок не буде).
 */
public enum Sound {
    /** Клік по кнопці меню. */
    CLICK("click"),
    /** Карта летить з колоди до гравця (роздача / добір). */
    CARD_DEAL("card_deal"),
    /** Карта лягає на стіл (атака, підкидання, захист). */
    CARD_PLACE("card_place"),
    /** Карта перевертається (відкривається в руці/на столі). */
    CARD_FLIP("card_flip"),
    /** Гравець забирає карти зі столу. */
    CARD_TAKE("card_take"),
    /** Карти йдуть у відбій. */
    CARD_DISCARD("card_discard"),
    /** Перетасовка колоди на початку гри. */
    SHUFFLE("shuffle"),
    /** Починається твій хід. */
    TURN("turn"),
    /** Спроба нелегального ходу. */
    ERROR("error"),
    /** Перемога. */
    WIN("win"),
    /** Поразка («дурень»). */
    LOSE("lose"),
    /** Нічия. */
    DRAW("draw");

    private final String fileName;

    Sound(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
