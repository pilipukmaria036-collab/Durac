package com.durak.ui;

import com.durak.ai.Difficulty;
import com.durak.game.GameMode;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Локальна гра 2–4 людей за одним пристроєм (з екраном передачі ходу). */
public class LocalSetupController {

    @FXML private ComboBox<Integer> countBox;
    @FXML private TextField name1;
    @FXML private TextField name2;
    @FXML private TextField name3;
    @FXML private TextField name4;
    @FXML private Label errorLabel;
    @FXML private VBox playerRow1;
    @FXML private VBox playerRow2;
    @FXML private VBox playerRow3;
    @FXML private VBox playerRow4;
    @FXML private HBox avatarChoices1;
    @FXML private HBox avatarChoices2;
    @FXML private HBox avatarChoices3;
    @FXML private HBox avatarChoices4;

    private List<TextField> nameFields;
    private List<VBox> playerRows;
    private List<HBox> avatarContainers;

    private final int[] selectedAvatars = {1, 2, 3, 4};

    @FXML
    private void initialize() {
        nameFields = List.of(name1, name2, name3, name4);
        playerRows = List.of(playerRow1, playerRow2, playerRow3, playerRow4);
        avatarContainers = List.of(avatarChoices1, avatarChoices2, avatarChoices3, avatarChoices4);

        countBox.getItems().setAll(2, 3, 4);
        countBox.setValue(2);

        createAvatarChoices();
        onCountChanged();
    }

    private void createAvatarChoices() {
        for (int playerIndex = 0; playerIndex < avatarContainers.size(); playerIndex++) {
            HBox container = avatarContainers.get(playerIndex);
            container.getChildren().clear();

            for (int avatar = 1; avatar <= AvatarRegistry.AVATAR_COUNT; avatar++) {
                final int currentPlayer = playerIndex;
                final int currentAvatar = avatar;

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
                tile.setOnMouseClicked(event -> {
                    selectedAvatars[currentPlayer] = currentAvatar;
                    updateAvatarSelection(currentPlayer);
                    errorLabel.setText("");
                });
                container.getChildren().add(tile);
            }
            updateAvatarSelection(playerIndex);
        }
    }

    private void updateAvatarSelection(int playerIndex) {
        HBox container = avatarContainers.get(playerIndex);
        for (int i = 0; i < container.getChildren().size(); i++) {
            var tile = container.getChildren().get(i);
            tile.getStyleClass().remove("avatar-selected");
            if (i + 1 == selectedAvatars[playerIndex]) {
                tile.getStyleClass().add("avatar-selected");
            }
        }
    }

    @FXML
    private void onCountChanged() {
        if (countBox == null || countBox.getValue() == null) {
            return;
        }
        int count = countBox.getValue();
        for (int i = 0; i < playerRows.size(); i++) {
            boolean visible = i < count;
            playerRows.get(i).setVisible(visible);
            playerRows.get(i).setManaged(visible);
        }
        errorLabel.setText("");
    }

    @FXML
    private void onStart() {
        Integer count = countBox.getValue();
        if (count == null || count < 2 || count > 4) {
            errorLabel.setText("Оберіть кількість гравців.");
            return;
        }

        List<GameLauncher.PlayerSpec> specs = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        for (int i = 0; i < count; i++) {
            String name = nameFields.get(i).getText().trim();
            if (name.isEmpty()) {
                errorLabel.setText("Заповніть імена всіх гравців.");
                return;
            }
            if (!unique.add(name.toLowerCase(Locale.ROOT))) {
                errorLabel.setText("Імена гравців мають бути різними.");
                return;
            }
            specs.add(new GameLauncher.PlayerSpec(name, false, Difficulty.EASY, selectedAvatars[i]));
        }
        GameLauncher.start(new GameLauncher.Config(GameMode.LOCAL_MULTIPLAYER, specs));
    }

    @FXML
    private void onBack() {
        SceneManager.switchScene("/fxml/MainMenu.fxml", "Durak Ultimate — Головне меню");
    }
}
