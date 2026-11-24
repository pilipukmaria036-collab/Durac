package com.durak.ui;

import com.durak.audio.Sound;
import com.durak.audio.SoundManager;
import com.durak.cards.Card;
import com.durak.cards.CardImageProvider;
import com.durak.cards.Rank;
import com.durak.cards.Suit;
import com.durak.settings.AppSettings;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;

/** Усі зміни застосовуються й зберігаються одразу — окремої кнопки «Зберегти» не потрібно. */
public class SettingsController {

    @FXML private ComboBox<String> themeBox;
    @FXML private ComboBox<String> deckBox;
    @FXML private ImageView deckPreview;
    @FXML private ComboBox<String> cardBackBox;
    @FXML private ImageView cardBackPreview;
    @FXML private CheckBox fullscreenCheck;

    @FXML private ComboBox<String> speedBox;
    @FXML private CheckBox hintsCheck;

    @FXML private CheckBox musicCheck;
    @FXML private Slider musicSlider;
    @FXML private CheckBox soundsCheck;
    @FXML private Slider soundsSlider;

    @FXML private Label statusLabel;

    private boolean loading;

    @FXML
    private void initialize() {
        loading = true;

        themeBox.getItems().setAll(AppSettings.THEMES);
        themeBox.setValue(AppSettings.getTheme());

        deckBox.getItems().setAll(CardImageProvider.getAvailableDecks());
        String deck = AppSettings.getDeck();
        deckBox.setValue(deckBox.getItems().contains(deck) ? deck : deckBox.getItems().get(0));
        updateDeckPreview();

        cardBackBox.getItems().setAll(AppSettings.CARD_BACKS);
        cardBackBox.setValue(AppSettings.getCardBack());
        cardBackPreview.setSmooth(false);
        updateCardBackPreview();

        fullscreenCheck.setSelected(AppSettings.isFullscreen());

        speedBox.getItems().setAll(AppSettings.SPEEDS);
        speedBox.setValue(AppSettings.getSpeed());
        hintsCheck.setSelected(AppSettings.isHintsEnabled());

        musicCheck.setSelected(AppSettings.isMusicEnabled());
        musicSlider.setValue(AppSettings.getMusicVolume() * 100);
        soundsCheck.setSelected(AppSettings.isSoundsEnabled());
        soundsSlider.setValue(AppSettings.getSoundsVolume() * 100);
        updateSliderState();

        loading = false;

        themeBox.setOnAction(e -> {
            if (loading || themeBox.getValue() == null) return;
            AppSettings.setTheme(themeBox.getValue());
            ThemeManager.apply(themeBox.getScene());
            saved();
        });
        deckBox.setOnAction(e -> {
            if (loading || deckBox.getValue() == null) return;
            AppSettings.setDeck(deckBox.getValue());
            updateDeckPreview();
            saved();
        });
        cardBackBox.setOnAction(e -> {
            if (loading || cardBackBox.getValue() == null) return;
            AppSettings.setCardBack(cardBackBox.getValue());
            updateCardBackPreview();
            saved();
        });
        fullscreenCheck.setOnAction(e -> {
            if (loading) return;
            AppSettings.setFullscreen(fullscreenCheck.isSelected());
            SceneManager.getStage().setFullScreen(fullscreenCheck.isSelected());
            saved();
        });
        speedBox.setOnAction(e -> {
            if (loading || speedBox.getValue() == null) return;
            AppSettings.setSpeed(speedBox.getValue());
            saved();
        });
        hintsCheck.setOnAction(e -> {
            if (loading) return;
            AppSettings.setHintsEnabled(hintsCheck.isSelected());
            saved();
        });
        musicCheck.setOnAction(e -> {
            if (loading) return;
            AppSettings.setMusicEnabled(musicCheck.isSelected());
            updateSliderState();
            SoundManager.refreshSettings();
            saved();
        });
        musicSlider.valueProperty().addListener((obs, old, now) -> {
            if (loading) return;
            AppSettings.setMusicVolume(now.doubleValue() / 100.0);
            SoundManager.refreshSettings();
        });
        soundsCheck.setOnAction(e -> {
            if (loading) return;
            AppSettings.setSoundsEnabled(soundsCheck.isSelected());
            updateSliderState();
            saved();
        });
        soundsSlider.valueProperty().addListener((obs, old, now) -> {
            if (loading) return;
            AppSettings.setSoundsVolume(now.doubleValue() / 100.0);
        });
        // Відпустили повзунок — програємо приклад звуку
        soundsSlider.setOnMouseReleased(e -> SoundManager.play(Sound.CARD_PLACE));
    }

    private void updateSliderState() {
        musicSlider.setDisable(!musicCheck.isSelected());
        soundsSlider.setDisable(!soundsCheck.isSelected());
    }

    private void updateDeckPreview() {
        var image = CardImageProvider.getImage(new Card(Suit.SPADES, Rank.ACE));
        deckPreview.setImage(image);
        deckPreview.setSmooth(!CardImageProvider.isPixelArt(image));
    }

    private void updateCardBackPreview() {
        cardBackPreview.setImage(CardImageProvider.getBackImage());
    }

    private void saved() {
        statusLabel.setText("Збережено ✓");
    }

    @FXML
    private void onTestSound() {
        SoundManager.play(Sound.CARD_PLACE);
        SoundManager.play(Sound.CARD_FLIP);
    }

    @FXML
    private void onReset() {
        AppSettings.resetToDefaults();
        SceneManager.getStage().setFullScreen(false);
        SoundManager.refreshSettings();
        ThemeManager.apply(themeBox.getScene());
        initialize();
        statusLabel.setText("Налаштування скинуто до стандартних");
    }

    @FXML
    private void onBack() {
        SceneManager.switchScene("/fxml/MainMenu.fxml", "Durak Ultimate — Головне меню");
    }
}
