package com.durak.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

/** Табличка гравця: аватар, ім'я, кількість карт і роль у раунді. */
final class PlayerPlate extends HBox {

    static final double WIDTH = 196;
    static final double HEIGHT = 66;

    private final Label infoLabel = new Label();
    private final Label roleLabel = new Label();
    private final StackPane avatarHolder = new StackPane();

    PlayerPlate(String name, int avatar, String subtitle) {
        getStyleClass().add("plate");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(10);
        setPrefSize(WIDTH, HEIGHT);
        setMinSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);
        setMouseTransparent(true);

        ImageView face = new ImageView();
        Image image = AvatarRegistry.getImage(avatar, 48);
        if (image != null) {
            face.setImage(image);
        }
        face.setFitWidth(48);
        face.setFitHeight(48);
        face.setPreserveRatio(true);
        face.setSmooth(true);
        face.setClip(new Circle(24, 24, 24));

        avatarHolder.getChildren().add(face);
        avatarHolder.getStyleClass().add("plate-avatar");
        avatarHolder.setPrefSize(52, 52);
        avatarHolder.setMinSize(52, 52);
        avatarHolder.setMaxSize(52, 52);

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("plate-name");
        nameLabel.setMaxWidth(WIDTH - 82);

        infoLabel.getStyleClass().add("plate-info");
        roleLabel.getStyleClass().add("role-chip");
        roleLabel.setVisible(false);
        roleLabel.setManaged(false);

        VBox texts = new VBox(1, nameLabel, infoLabel, roleLabel);
        texts.setAlignment(Pos.CENTER_LEFT);
        if (subtitle != null && !subtitle.isEmpty()) {
            infoLabel.setUserData(subtitle);
        }
        getChildren().addAll(avatarHolder, texts);
    }

    /** role: порожньо — без плашки; active — підсвічування активного гравця. */
    void update(int cards, String role, String roleStyle, boolean active) {
        Object subtitle = infoLabel.getUserData();
        String base = plural(cards);
        infoLabel.setText(subtitle == null ? base : base + " · " + subtitle);

        boolean hasRole = role != null && !role.isEmpty();
        roleLabel.setText(role == null ? "" : role);
        roleLabel.setVisible(hasRole);
        roleLabel.setManaged(hasRole);
        roleLabel.getStyleClass().removeAll("role-attack", "role-defend", "role-take", "role-throw");
        if (hasRole && roleStyle != null) {
            roleLabel.getStyleClass().add(roleStyle);
        }

        getStyleClass().remove("plate-active");
        if (active) {
            getStyleClass().add("plate-active");
        }
    }

    static String plural(int n) {
        int mod100 = n % 100;
        int mod10 = n % 10;
        if (mod100 >= 11 && mod100 <= 14) return n + " карт";
        if (mod10 == 1) return n + " карта";
        if (mod10 >= 2 && mod10 <= 4) return n + " карти";
        return n + " карт";
    }
}
