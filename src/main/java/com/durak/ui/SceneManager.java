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
        // Порожній підказкою не показуємо банер щоразу при вході в повний екран.
        mainStage.setFullScreenExitHint("");
        mainStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        // Перемикання між вікнами (Alt+Tab) може вивести JavaFX з повного екрана.
        // Якщо в налаштуваннях повний екран увімкнений — повертаємо його, коли вікно знову активне.
        mainStage.focusedProperty().addListener((obs, was, now) -> {
            if (now) {
                restoreFullscreenIfNeeded();
            }
        });
        mainStage.iconifiedProperty().addListener((obs, was, now) -> {
            if (!now) {
                restoreFullscreenIfNeeded();
            }
        });
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
     * Показує готовий корінь. Сцена створюється один раз, далі міняється лише її корінь —
     * так вікно не «збивається» і не виходить із повноекранного режиму при зміні екрана.
     * Для ігрового столу (game = true) фон малює сам стіл.
     */
    public static Scene showRoot(Parent content, String title, boolean game) {
        requireStage();
        javafx.scene.layout.StackPane shell = ThemeManager.createShell(content, game ? null : "bg_main");

        Scene scene = mainStage.getScene();
        if (scene == null) {
            scene = new Scene(shell, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.F11) {
                    toggleFullscreen();
                    event.consume();
                }
            });
            mainStage.setScene(scene);
        } else {
            scene.setOnKeyPressed(null);   // обробники попереднього екрана більше не потрібні
            scene.setRoot(shell);
        }
        ThemeManager.apply(scene);

        mainStage.setTitle(title);
        if (!mainStage.isShowing()) {
            mainStage.show();
        }
        return scene;
    }

    public static void toggleFullscreen() {
        if (mainStage != null) {
            boolean value = !mainStage.isFullScreen();
            AppSettings.setFullscreen(value);
            mainStage.setFullScreen(value);
        }
    }

    private static void restoreFullscreenIfNeeded() {
        if (mainStage != null && AppSettings.isFullscreen()
                && !mainStage.isFullScreen() && !mainStage.isIconified()) {
            Platform.runLater(() -> {
                if (AppSettings.isFullscreen() && !mainStage.isFullScreen() && !mainStage.isIconified()) {
                    mainStage.setFullScreen(true);
                }
            });
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
