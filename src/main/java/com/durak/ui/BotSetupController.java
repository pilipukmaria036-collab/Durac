package com.durak.ui;

import com.durak.ai.Difficulty;
import com.durak.game.GameMode;
import com.durak.settings.AppSettings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;

/** Налаштування партії проти комп'ютера: ім'я, аватар, кількість суперників, складність. */
public class BotSetupController {

    private static final String[] BOT_NAMES = {"Бот Олесь", "Бот Марта", "Бот Данило"};

    @FXML private TextField nameField;
    @FXML private HBox avatarChoices;
    @FXML private HBox botCountBox;
    @FXML private HBox difficultyBox;
    @FXML private Label difficultyHint;
    @FXML private Label errorLabel;

    private final ToggleGroup countGroup = new ToggleGroup();
    private final ToggleGroup difficultyGroup = new ToggleGroup();
    private int selectedAvatar;

    @FXML
    private void initialize() {
        nameField.setText(AppSettings.getLastPlayerName());
        selectedAvatar = AppSettings.getLastAvatar();

        buildAvatarChoices();

        for (int i = 1; i <= 3; i++) {
            ToggleButton button = segment(i + (i == 1 ? " суперник" : " суперники"), i);
            button.setToggleGroup(countGroup);
            botCountBox.getChildren().add(button);
            if (i == AppSettings.getLastBotCount()) {
                button.setSelected(true);
            }
        }

        String last = AppSettings.getLastDifficulty();
        for (Difficulty difficulty : Difficulty.values()) {
            ToggleButton button = segment(difficulty.getTitle(), difficulty);
            button.setToggleGroup(difficultyGroup);
            difficultyBox.getChildren().add(button);
            if (difficulty.getTitle().equals(last)) {
                button.setSelected(true);
            }
        }
        keepOneSelected(countGroup);
        keepOneSelected(difficultyGroup);
        if (countGroup.getSelectedToggle() == null) {
            countGroup.selectToggle(countGroup.getToggles().get(0));
        }
        if (difficultyGroup.getSelectedToggle() == null) {
            difficultyGroup.selectToggle(difficultyGroup.getToggles().get(0));
        }
        difficultyGroup.selectedToggleProperty().addListener((obs, old, now) -> updateHint());
        updateHint();
    }

    private ToggleButton segment(String text, Object userData) {
        ToggleButton button = new ToggleButton(text);
        button.getStyleClass().add("segment");
        button.setUserData(userData);
        button.setPrefHeight(40);
        HBox.setHgrow(button, javafx.scene.layout.Priority.ALWAYS);
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    /** Не дає зняти вибір повторним натисканням на вже обрану кнопку. */
    private void keepOneSelected(ToggleGroup group) {
        group.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null && old != null) {
                group.selectToggle(old);
            }
        });
    }

    private void buildAvatarChoices() {
        avatarChoices.getChildren().clear();
        for (int i = 1; i <= AvatarRegistry.AVATAR_COUNT; i++) {
            final int avatar = i;
            ImageView view = new ImageView(AvatarRegistry.getImage(avatar, 39));
            view.setFitWidth(39);
            view.setFitHeight(39);
            view.setPreserveRatio(true);
            view.setSmooth(true);
            view.setClip(new Circle(19.5, 19.5, 19.5));

            StackPane tile = new StackPane(view);
            tile.setPrefSize(43, 43);
            tile.setMinSize(43, 43);
            tile.setMaxSize(43, 43);
            tile.getStyleClass().add("avatar-option");
            tile.setOnMouseClicked(e -> {
                selectedAvatar = avatar;
                updateAvatarSelection();
            });
            avatarChoices.getChildren().add(tile);
        }
        updateAvatarSelection();
    }

    private void updateAvatarSelection() {
        for (int i = 0; i < avatarChoices.getChildren().size(); i++) {
            var tile = avatarChoices.getChildren().get(i);
            tile.getStyleClass().remove("avatar-selected");
            if (i + 1 == selectedAvatar) {
                tile.getStyleClass().add("avatar-selected");
            }
        }
    }

    private void updateHint() {
        Toggle t = difficultyGroup.getSelectedToggle();
        if (t != null && t.getUserData() == Difficulty.HARD) {
            difficultyHint.setText("Рахує карти, береже козирі й вдало підкидає. Піддається тільки уважним гравцям.");
        } else {
            difficultyHint.setText("Ходить майже навмання, розкидається козирями й часто помиляється. Добре для початку.");
        }
    }

    @FXML
    private void onStart() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            errorLabel.setText("Введіть своє ім'я.");
            return;
        }
        int bots = (Integer) countGroup.getSelectedToggle().getUserData();
        Difficulty difficulty = (Difficulty) difficultyGroup.getSelectedToggle().getUserData();

        AppSettings.setLastPlayerName(name);
        AppSettings.setLastAvatar(selectedAvatar);
        AppSettings.setLastBotCount(bots);
        AppSettings.setLastDifficulty(difficulty.getTitle());

        List<GameLauncher.PlayerSpec> specs = new ArrayList<>();
        specs.add(new GameLauncher.PlayerSpec(name, false, difficulty, selectedAvatar));
        int avatar = selectedAvatar;
        for (int i = 0; i < bots; i++) {
            avatar = avatar % AvatarRegistry.AVATAR_COUNT + 1;
            String botName = BOT_NAMES[i];
            if (botName.equalsIgnoreCase(name)) {
                botName = botName + " II";
            }
            specs.add(new GameLauncher.PlayerSpec(botName, true, difficulty, avatar));
        }
        GameLauncher.start(new GameLauncher.Config(GameMode.VS_BOT, specs));
    }

    @FXML
    private void onBack() {
        SceneManager.switchScene("/fxml/MainMenu.fxml", "Durak Ultimate — Головне меню");
    }
}
