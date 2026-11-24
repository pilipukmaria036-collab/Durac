package com.durak.ui;

import com.durak.settings.AppSettings;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public final class SceneManager {

    private static Stage mainStage;

    private static final double DEFAULT_WIDTH = 1280;
    private static final double DEFAULT_HEIGHT = 720;

    private SceneManager() {
    }

    public static void setStage(Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("Stage не може бути null.");
        }
        mainStage = stage;
        mainStage.setMinWidth(960);
        mainStage.setMinHeight(560);
        mainStage.setFullScreenExitHint("F11 — вийти з повноекранного режиму");
        mainStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
    }

    public static Stage getStage() {
        return mainStage;
    }

    /** Завантажує FXML-екран на тлі головного фону й показує його. */
    public static void switchScene(String fxmlPath, String title) {
        requireStage();
        FXMLLoader loader = getLoader(fxmlPath);
        try {
            Parent root = loader.load();
            showRoot(root, title, false);
        } catch (IOException e) {
            throw new RuntimeException("Помилка завантаження екрана: " + fxmlPath, e);
        }
    }

    /**
     * Показує готовий корінь як нову сцену. Для ігрового столу (game = true) фон малює сам стіл.
     * Розмір вікна зберігається між екранами.
     */
    public static Scene showRoot(Parent content, String title, boolean game) {
        requireStage();
        double w = DEFAULT_WIDTH;
        double h = DEFAULT_HEIGHT;
        if (mainStage.getScene() != null) {
            w = mainStage.getScene().getWidth();
            h = mainStage.getScene().getHeight();
        }
        Scene scene = new Scene(ThemeManager.createShell(content, game ? null : "bg_main"), w, h);
        ThemeManager.apply(scene);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F11) {
                toggleFullscreen();
                event.consume();
            }
        });

        mainStage.setTitle(title);
        mainStage.setScene(scene);
        mainStage.show();
        return scene;
    }

    public static void toggleFullscreen() {
        if (mainStage != null) {
            boolean value = !mainStage.isFullScreen();
            mainStage.setFullScreen(value);
            AppSettings.setFullscreen(value);
        }
    }

    public static FXMLLoader getLoader(String fxmlPath) {
        if (fxmlPath == null || fxmlPath.isBlank()) {
            throw new IllegalArgumentException("Шлях до FXML-файлу не може бути порожнім.");
        }
        URL resource = SceneManager.class.getResource(fxmlPath);
        if (resource == null) {
            throw new IllegalStateException("Не знайдено FXML-файл: " + fxmlPath);
        }
        return new FXMLLoader(resource);
    }

    private static void requireStage() {
        if (mainStage == null) {
            throw new IllegalStateException("Головне вікно (Stage) ще не встановлено.");
        }
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("Перемикання сцен має виконуватися в JavaFX Application Thread.");
        }
    }
}
