package com.durak.settings;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/** Усі налаштування гри. Зберігаються між запусками (java.util.prefs). */
public final class AppSettings {

    public static final List<String> THEMES = List.of("Смарагдова", "Темна", "Синя", "Бордова");
    /**
     * Колоди: назва в налаштуваннях → папка з картинками в resources.
     * Щоб додати нову колоду, покладіть її файли в окрему папку й додайте рядок у блок нижче.
     */
    public static final Map<String, String> DECKS = new LinkedHashMap<>();

    static {
        DECKS.put("Піксельна", "/images/cards/");
        DECKS.put("Класична", "/images/decks/classic/");
    }

    public static final List<String> CARD_BACKS = List.of("Синя", "Червона", "Зелена");
    public static final List<String> SPEEDS = List.of("Швидко", "Звичайно", "Повільно");

    private static final Preferences PREFS = Preferences.userNodeForPackage(AppSettings.class);

    private static final String K_THEME = "theme";
    private static final String K_MUSIC_ON = "musicEnabled";
    private static final String K_MUSIC_VOL = "musicVolume";
    private static final String K_SFX_ON = "soundsEnabled";
    private static final String K_SFX_VOL = "soundsVolume";
    private static final String K_CARD_BACK = "cardBack";
    private static final String K_DECK = "deck";
    private static final String K_SPEED = "animationSpeed";
    private static final String K_HINTS = "hints";
    private static final String K_FULLSCREEN = "fullscreen";

    private static final String K_LAST_NAME = "lastPlayerName";
    private static final String K_LAST_AVATAR = "lastAvatar";
    private static final String K_LAST_BOTS = "lastBotCount";
    private static final String K_LAST_DIFFICULTY = "lastDifficulty";

    private AppSettings() {
    }

    // ---- вигляд
    public static String getTheme() {
        String value = PREFS.get(K_THEME, THEMES.get(0));
        return THEMES.contains(value) ? value : THEMES.get(0);
    }

    public static void setTheme(String theme) {
        if (THEMES.contains(theme)) {
            PREFS.put(K_THEME, theme);
        }
    }

    public static String getDeck() {
        String value = PREFS.get(K_DECK, "Піксельна");
        return DECKS.containsKey(value) ? value : "Піксельна";
    }

    public static void setDeck(String deck) {
        if (DECKS.containsKey(deck)) {
            PREFS.put(K_DECK, deck);
        }
    }

    /** Папка поточної колоди, напр. "/images/decks/classic/". */
    public static String getDeckFolder() {
        return DECKS.get(getDeck());
    }

    public static String getCardBack() {
        String value = PREFS.get(K_CARD_BACK, CARD_BACKS.get(0));
        return CARD_BACKS.contains(value) ? value : CARD_BACKS.get(0);
    }

    public static void setCardBack(String back) {
        if (CARD_BACKS.contains(back)) {
            PREFS.put(K_CARD_BACK, back);
        }
    }

    /** Ідентифікатор файлу сорочки: back_blue.png / back_red.png / back_green.png. */
    public static String getCardBackFileId() {
        return switch (getCardBack()) {
            case "Червона" -> "red";
            case "Зелена" -> "green";
            default -> "blue";
        };
    }

    public static boolean isFullscreen() {
        return PREFS.getBoolean(K_FULLSCREEN, false);
    }

    public static void setFullscreen(boolean value) {
        PREFS.putBoolean(K_FULLSCREEN, value);
    }

    // ---- геймплей
    public static String getSpeed() {
        String value = PREFS.get(K_SPEED, SPEEDS.get(1));
        return SPEEDS.contains(value) ? value : SPEEDS.get(1);
    }

    public static void setSpeed(String speed) {
        if (SPEEDS.contains(speed)) {
            PREFS.put(K_SPEED, speed);
        }
    }

    /** Множник тривалості анімацій і пауз бота (менше = швидше). */
    public static double getTimeFactor() {
        return switch (getSpeed()) {
            case "Швидко" -> 0.55;
            case "Повільно" -> 1.6;
            default -> 1.0;
        };
    }

    public static boolean isHintsEnabled() {
        return PREFS.getBoolean(K_HINTS, true);
    }

    public static void setHintsEnabled(boolean value) {
        PREFS.putBoolean(K_HINTS, value);
    }

    // ---- звук
    public static boolean isMusicEnabled() {
        return PREFS.getBoolean(K_MUSIC_ON, true);
    }

    public static void setMusicEnabled(boolean value) {
        PREFS.putBoolean(K_MUSIC_ON, value);
    }

    public static double getMusicVolume() {
        return clamp(PREFS.getDouble(K_MUSIC_VOL, 0.45));
    }

    public static void setMusicVolume(double value) {
        PREFS.putDouble(K_MUSIC_VOL, clamp(value));
    }

    public static boolean isSoundsEnabled() {
        return PREFS.getBoolean(K_SFX_ON, true);
    }

    public static void setSoundsEnabled(boolean value) {
        PREFS.putBoolean(K_SFX_ON, value);
    }

    public static double getSoundsVolume() {
        return clamp(PREFS.getDouble(K_SFX_VOL, 0.8));
    }

    public static void setSoundsVolume(double value) {
        PREFS.putDouble(K_SFX_VOL, clamp(value));
    }

    // ---- запам'ятовування останнього вибору в налаштуванні гри проти комп'ютера
    public static String getLastPlayerName() {
        return PREFS.get(K_LAST_NAME, "");
    }

    public static void setLastPlayerName(String name) {
        PREFS.put(K_LAST_NAME, name == null ? "" : name);
    }

    public static int getLastAvatar() {
        return Math.max(1, Math.min(4, PREFS.getInt(K_LAST_AVATAR, 1)));
    }

    public static void setLastAvatar(int avatar) {
        PREFS.putInt(K_LAST_AVATAR, avatar);
    }

    public static int getLastBotCount() {
        return Math.max(1, Math.min(3, PREFS.getInt(K_LAST_BOTS, 1)));
    }

    public static void setLastBotCount(int count) {
        PREFS.putInt(K_LAST_BOTS, count);
    }

    public static String getLastDifficulty() {
        return PREFS.get(K_LAST_DIFFICULTY, "Легкий");
    }

    public static void setLastDifficulty(String title) {
        PREFS.put(K_LAST_DIFFICULTY, title);
    }

    public static void resetToDefaults() {
        for (String key : new String[]{K_THEME, K_MUSIC_ON, K_MUSIC_VOL, K_SFX_ON, K_SFX_VOL,
                K_CARD_BACK, K_DECK, K_SPEED, K_HINTS, K_FULLSCREEN}) {
            PREFS.remove(key);
        }
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
