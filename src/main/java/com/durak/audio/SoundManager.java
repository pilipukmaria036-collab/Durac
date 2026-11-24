package com.durak.audio;

import com.durak.settings.AppSettings;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.EnumMap;
import java.util.Map;

/**
 * Відтворення звуків і фонової музики з урахуванням налаштувань.
 *
 * <p>Фонова музика: {@code /sounds/music/background.mp3} (або .wav).
 * Ефекти: див. {@link Sound}. Усі відсутні файли тихо пропускаються.
 */
public final class SoundManager {

    private static final String[] EXTENSIONS = {".wav", ".mp3"};
    private static final String MUSIC_BASE = "/sounds/music/background";

    private static final Map<Sound, AudioClip> CLIPS = new EnumMap<>(Sound.class);
    private static final Map<Sound, Boolean> MISSING = new EnumMap<>(Sound.class);

    private static MediaPlayer musicPlayer;
    private static boolean musicMissing;

    private SoundManager() {
    }

    // ------------------------------------------------------------------ ефекти

    public static void play(Sound sound) {
        if (sound == null || !AppSettings.isSoundsEnabled()) {
            return;
        }
        AudioClip clip = loadClip(sound);
        if (clip != null) {
            try {
                clip.play(AppSettings.getSoundsVolume());
            } catch (RuntimeException ignored) {
                // звук не повинен ламати гру
            }
        }
    }

    private static AudioClip loadClip(Sound sound) {
        if (CLIPS.containsKey(sound)) {
            return CLIPS.get(sound);
        }
        if (MISSING.containsKey(sound)) {
            return null;
        }
        URL url = findResource("/sounds/music/" + sound.getFileName());
        if (url == null) {
            MISSING.put(sound, Boolean.TRUE);
            return null;
        }
        try {
            AudioClip clip = new AudioClip(url.toExternalForm());
            CLIPS.put(sound, clip);
            return clip;
        } catch (RuntimeException e) {
            MISSING.put(sound, Boolean.TRUE);
            return null;
        }
    }

    // ------------------------------------------------------------------ музика

    /** Запускає (або відновлює) фонову музику, якщо вона ввімкнена в налаштуваннях. */
    public static void startMusic() {
        if (!AppSettings.isMusicEnabled()) {
            stopMusic();
            return;
        }
        if (musicPlayer == null && !musicMissing) {
            URL url = findResource(MUSIC_BASE);
            if (url == null) {
                musicMissing = true;
                return;
            }
            try {
                musicPlayer = new MediaPlayer(new Media(url.toExternalForm()));
                musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            } catch (RuntimeException e) {
                musicMissing = true;
                musicPlayer = null;
                return;
            }
        }
        if (musicPlayer != null) {
            musicPlayer.setVolume(AppSettings.getMusicVolume());
            musicPlayer.play();
        }
    }

    public static void stopMusic() {
        if (musicPlayer != null) {
            musicPlayer.pause();
        }
    }

    /** Застосовує зміни гучності/вимикачів після зміни налаштувань. */
    public static void refreshSettings() {
        if (AppSettings.isMusicEnabled()) {
            startMusic();
            if (musicPlayer != null) {
                musicPlayer.setVolume(AppSettings.getMusicVolume());
            }
        } else {
            stopMusic();
        }
    }

    public static void shutdown() {
        if (musicPlayer != null) {
            musicPlayer.stop();
            musicPlayer.dispose();
            musicPlayer = null;
        }
    }

    private static URL findResource(String basePath) {
        for (String extension : EXTENSIONS) {
            URL url = SoundManager.class.getResource(basePath + extension);
            if (url != null) {
                return url;
            }
        }
        return null;
    }
}
