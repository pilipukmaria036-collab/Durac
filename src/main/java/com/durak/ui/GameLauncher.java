package com.durak.ui;

import com.durak.ai.Difficulty;
import com.durak.game.GameMode;
import com.durak.players.BotPlayer;
import com.durak.players.Player;
import com.durak.players.PlayerType;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.util.ArrayList;
import java.util.List;

/** Створює гравців за налаштуванням і відкриває ігровий стіл. */
public final class GameLauncher {

    /** Опис одного гравця. Для людини difficulty ігнорується. */
    public record PlayerSpec(String name, boolean bot, Difficulty difficulty, int avatar) {
    }

    /** Повне налаштування партії (використовується й для «Грати ще раз»). */
    public record Config(GameMode mode, List<PlayerSpec> players) {
    }

    private GameLauncher() {
    }

    /** Нові об'єкти гравців (руки порожні) за конфігурацією. */
    public static List<Player> createPlayers(Config config) {
        List<Player> list = new ArrayList<>();
        for (PlayerSpec spec : config.players()) {
            if (spec.bot()) {
                list.add(new BotPlayer(spec.name(), spec.difficulty()));
            } else {
                list.add(new Player(spec.name(), PlayerType.LOCAL));
            }
            AvatarRegistry.register(spec.name(), spec.avatar());
        }
        return list;
    }

    public static void start(Config config) {
        try {
            FXMLLoader loader = SceneManager.getLoader("/fxml/GameView.fxml");
            Parent root = loader.load();
            GameViewController controller = loader.getController();
            Scene scene = SceneManager.showRoot(root, "Durak Ultimate — Ігровий стіл", true);
            controller.begin(config, scene);
        } catch (Exception e) {
            throw new RuntimeException("Не вдалося відкрити ігровий стіл", e);
        }
    }
}
