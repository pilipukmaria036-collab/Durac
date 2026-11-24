package com.durak.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.InputStream;

public class MainMenuController {

    @FXML private ImageView logoView;

    @FXML
    private void initialize() {
        // Логотип 1536x1024 завантажуємо одразу зменшеним.
        try (InputStream in = getClass().getResourceAsStream("/images/logo.png")) {
            if (in != null) {
                logoView.setImage(new Image(in, 620, 0, true, true));
            }
        } catch (Exception ignored) {
            // без логотипа меню працює
        }
    }

    @FXML
    public void onBotGameClick() {
        SceneManager.switchScene("/fxml/BotSetup.fxml", "Гра проти комп'ютера");
    }

    @FXML
    public void onLocalGameClick() {
        SceneManager.switchScene("/fxml/LocalSetup.fxml", "Локальна гра");
    }

    @FXML
    public void onTutorialClick() {
        SceneManager.switchScene("/fxml/Tutorial.fxml", "Навчання");
    }

    @FXML
    public void onSettingsClick() {
        SceneManager.switchScene("/fxml/Settings.fxml", "Налаштування");
    }

    @FXML
    public void onExitClick() {
        Platform.exit();
    }
}
