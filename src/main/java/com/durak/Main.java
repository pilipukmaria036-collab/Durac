package com.durak;

import com.durak.audio.SoundManager;
import com.durak.settings.AppSettings;
import com.durak.ui.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        SceneManager.setStage(primaryStage);
        SceneManager.switchScene("/fxml/MainMenu.fxml", "Durak: Ultimate Card Game");
        primaryStage.setFullScreen(AppSettings.isFullscreen());
        SoundManager.startMusic();
    }

    @Override
    public void stop() {
        SoundManager.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
