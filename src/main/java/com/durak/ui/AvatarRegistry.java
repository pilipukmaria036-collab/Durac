package com.durak.ui;

import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Аватарки гравців: хто яку обрав + кеш зменшених зображень. */
public final class AvatarRegistry {

    public static final int AVATAR_COUNT = 4;

    private static final Map<String, Integer> BY_PLAYER = new HashMap<>();
    private static final Map<String, Image> CACHE = new HashMap<>();

    private AvatarRegistry() {
    }

    public static void register(String playerName, int avatar) {
        BY_PLAYER.put(playerName, normalize(avatar));
    }

    public static int getAvatar(String playerName) {
        return BY_PLAYER.getOrDefault(playerName, 1);
    }

    public static String getPath(int avatar) {
        return "/images/avatars/avatar" + normalize(avatar) + ".png";
    }

    /** Квадратна аватарка потрібного розміру (завантажується одразу зменшеною — оригінали 1254x1254). */
    public static Image getImage(int avatar, double size) {
        int a = normalize(avatar);
        String key = a + "@" + (int) size;
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }
        Image image = null;
        try (InputStream in = AvatarRegistry.class.getResourceAsStream(getPath(a))) {
            if (in != null) {
                image = new Image(in, size * 2, size * 2, true, true);
            }
        } catch (Exception ignored) {
            // без аватарки гра працює
        }
        CACHE.put(key, image);
        return image;
    }

    private static int normalize(int avatar) {
        return Math.max(1, Math.min(AVATAR_COUNT, avatar));
    }
}
