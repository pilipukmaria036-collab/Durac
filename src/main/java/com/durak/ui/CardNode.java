package com.durak.ui;

import com.durak.cards.Card;
import com.durak.cards.CardImageProvider;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Візуальна карта на столі. Зовнішній вузол лежить на місці (позиція/поворот/масштаб),
 * а внутрішній «корпус» підстрибує при наведенні — тому курсор не «тремтить» на краю карти.
 */
final class CardNode extends StackPane {

    static final double W = 96;
    static final double H = 128;

    enum Mode { NORMAL, PLAYABLE, DIM, SELECTED, TARGET }

    private static final DropShadow SHADOW =
            new DropShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0.55), 10, 0.1, 0, 4);
    private static final ColorAdjust DIM = new ColorAdjust(0, -0.25, -0.42, 0);

    private final StackPane body = new StackPane();
    private final ImageView view = new ImageView();

    private Card card;          // null — карта прихована (суперник)
    private boolean faceUp;
    private Mode mode = Mode.NORMAL;
    private boolean hoverable;
    private boolean hovered;
    private boolean flying;
    private Timeline moveAnim;
    private Timeline liftAnim;

    CardNode(Card card, boolean faceUp) {
        this.card = card;
        this.faceUp = faceUp && card != null;

        setPrefSize(W, H);
        setMinSize(W, H);
        setMaxSize(W, H);

        view.setFitWidth(W);
        view.setFitHeight(H);
        view.setPreserveRatio(false);
        view.setMouseTransparent(true);

        body.setPrefSize(W, H);
        body.setMinSize(W, H);
        body.setMaxSize(W, H);
        body.getChildren().add(view);
        getChildren().add(body);

        refreshImage();
        applyMode();

        setOnMouseEntered(e -> {
            hovered = true;
            updateLift();
        });
        setOnMouseExited(e -> {
            hovered = false;
            updateLift();
        });
    }

    // ------------------------------------------------------------------ дані

    Card getCard() { return card; }

    void setCard(Card card) {
        this.card = card;
        refreshImage();
    }

    boolean isFaceUp() { return faceUp; }

    boolean isFlying() { return flying; }

    void setHoverable(boolean value) {
        hoverable = value;
        updateLift();
    }

    private void refreshImage() {
        Image image = (faceUp && card != null) ? CardImageProvider.getImage(card) : CardImageProvider.getBackImage();
        view.setImage(image);
        view.setSmooth(!CardImageProvider.isPixelArt(image));
    }

    // ------------------------------------------------------------------ вигляд

    void setMode(Mode newMode) {
        if (mode != newMode) {
            mode = newMode;
            applyMode();
            updateLift();
        }
    }

    private void applyMode() {
        view.setEffect(mode == Mode.DIM ? DIM : null);
        switch (mode) {
            case PLAYABLE -> body.setEffect(glow(Color.web("#f4d36e"), 0.5));
            case SELECTED -> body.setEffect(glow(Color.web("#ffe98a"), 0.85));
            case TARGET -> body.setEffect(glow(Color.web("#7dffa4"), 0.85));
            default -> body.setEffect(SHADOW);
        }
    }

    private static DropShadow glow(Color color, double spread) {
        return new DropShadow(BlurType.GAUSSIAN, color, 22, spread, 0, 0);
    }

    private void updateLift() {
        double target = 0;
        if (mode == Mode.SELECTED) {
            target = -34;
        } else if (hoverable && hovered) {
            target = -22;
        }
        if (liftAnim != null) {
            liftAnim.stop();
        }
        liftAnim = new Timeline(new KeyFrame(Duration.millis(110),
                new KeyValue(body.translateYProperty(), target, Interpolator.EASE_OUT)));
        liftAnim.play();
    }

    /** Коротке «струшування» при спробі нелегального ходу. */
    void shake() {
        Timeline t = new Timeline(
                new KeyFrame(Duration.millis(0), new KeyValue(body.translateXProperty(), 0)),
                new KeyFrame(Duration.millis(50), new KeyValue(body.translateXProperty(), -7)),
                new KeyFrame(Duration.millis(110), new KeyValue(body.translateXProperty(), 7)),
                new KeyFrame(Duration.millis(170), new KeyValue(body.translateXProperty(), -4)),
                new KeyFrame(Duration.millis(220), new KeyValue(body.translateXProperty(), 0)));
        t.play();
    }

    // ------------------------------------------------------------------ рух

    /** Переміщує центр карти в (cx, cy) з поворотом і масштабом. ms <= 0 — миттєво. */
    Timeline moveTo(double cx, double cy, double rotation, double scale, double ms) {
        double lx = cx - W / 2;
        double ly = cy - H / 2;
        // найкоротший шлях повороту
        double diff = ((rotation - getRotate() + 540) % 360 + 360) % 360 - 180;
        double targetRotation = getRotate() + diff;

        if (moveAnim != null) {
            moveAnim.stop();
        }
        if (ms <= 0) {
            setLayoutX(lx);
            setLayoutY(ly);
            setRotate(rotation);
            setScaleX(scale);
            setScaleY(scale);
            flying = false;
            moveAnim = null;
            return null;
        }
        flying = true;
        moveAnim = new Timeline(new KeyFrame(Duration.millis(ms),
                new KeyValue(layoutXProperty(), lx, Interpolator.EASE_BOTH),
                new KeyValue(layoutYProperty(), ly, Interpolator.EASE_BOTH),
                new KeyValue(rotateProperty(), targetRotation, Interpolator.EASE_BOTH),
                new KeyValue(scaleXProperty(), scale, Interpolator.EASE_BOTH),
                new KeyValue(scaleYProperty(), scale, Interpolator.EASE_BOTH)));
        moveAnim.setOnFinished(e -> flying = false);
        moveAnim.play();
        return moveAnim;
    }

    // ------------------------------------------------------------------ перевертання

    /** Перевертає карту. Для відкриття потрібно, щоб card було відоме. */
    void flipTo(boolean up, double ms) {
        flip(up, ms, false);
    }

    /** Перевертає сорочкою догори й забуває, що це за карта (карта пішла в приховану руку/відбій). */
    void flipDownAndForget(double ms) {
        flip(false, ms, true);
    }

    private void flip(boolean up, double ms, boolean forget) {
        if (up && card == null) {
            return;
        }
        if (up == faceUp) {
            if (forget && !up) {
                card = null;
            }
            return;
        }
        if (ms <= 0) {
            faceUp = up;
            if (forget) {
                card = null;
            }
            refreshImage();
            return;
        }
        Timeline close = new Timeline(new KeyFrame(Duration.millis(ms / 2),
                new KeyValue(view.scaleXProperty(), 0, Interpolator.EASE_IN)));
        close.setOnFinished(e -> {
            faceUp = up;
            if (forget) {
                card = null;
            }
            refreshImage();
            new Timeline(new KeyFrame(Duration.millis(ms / 2),
                    new KeyValue(view.scaleXProperty(), 1, Interpolator.EASE_OUT))).play();
        });
        close.play();
    }
}
