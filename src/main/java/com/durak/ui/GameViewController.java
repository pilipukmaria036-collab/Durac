package com.durak.ui;

import com.durak.ai.BotBrain;
import com.durak.ai.BotMove;
import com.durak.audio.Sound;
import com.durak.audio.SoundManager;
import com.durak.cards.AttackPair;
import com.durak.cards.Card;
import com.durak.cards.Suit;
import com.durak.game.GameEvent;
import com.durak.game.GameManager;
import com.durak.game.GameMode;
import com.durak.game.GameResult;
import com.durak.game.TurnPhase;
import com.durak.players.Player;
import com.durak.settings.AppSettings;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Ігровий стіл. Рушій ({@link GameManager}) працює синхронно й видає події;
 * тут вони по черзі відтворюються з анімацією (роздача, добір, атака, захист, «беру», відбій).
 *
 * <p>Уся сцена малюється у фіксованих координатах 1280×720 і масштабується під розмір вікна.
 */
public class GameViewController {

    // ------------------------------------------------------------------ геометрія
    private static final double SW = 1280;
    private static final double SH = 720;

    private static final Point2D DECK = new Point2D(200, 352);
    private static final Point2D DISCARD = new Point2D(1086, 352);
    private static final Point2D TABLE_CENTER = new Point2D(640, 356);
    private static final double PAIR_SPACING = 172;
    private static final double OPPONENT_SCALE = 0.72;

    private enum Seat { BOTTOM, LEFT, TOP, RIGHT }

    private static final class PairView {
        CardNode attack;
        CardNode defense;
    }

    private interface Step {
        /** Виконує крок і повертає, скільки мс чекати до наступного. */
        double run();
    }

    // ------------------------------------------------------------------ FXML та шари
    @FXML private StackPane root;

    private Pane stage;
    private Pane cardLayer;
    private Pane hudLayer;
    private Pane bubbleLayer;

    private Label bannerLabel;
    private Label deckCountLabel;
    private Label discardLabel;
    private Label deckMarker;
    private Label trumpSymbol;
    private VBox trumpBadge;
    private Button actionButton;
    private Button pauseButton;

    // ------------------------------------------------------------------ візуальний стан
    private final Map<Player, PlayerPlate> plates = new HashMap<>();
    private final Map<Player, Seat> seats = new HashMap<>();
    private final Map<Player, List<CardNode>> hands = new HashMap<>();
    private final List<PairView> table = new ArrayList<>();
    private final List<CardNode> deckLayers = new ArrayList<>();
    private final List<CardNode> discardLayers = new ArrayList<>();
    private final List<CardNode> transit = new ArrayList<>();
    private CardNode trumpNode;
    private int deckVisualCount;
    private int discardVisualCount;
    private boolean visualTaking;

    private CardNode selectedHandNode;
    private PairView selectedTarget;

    // ------------------------------------------------------------------ логіка
    private GameLauncher.Config config;
    private GameManager gm;
    private List<Player> players;
    private Player viewer;
    private boolean hotSeat;

    private final ArrayDeque<Step> queue = new ArrayDeque<>();
    private boolean pumping;
    private boolean disposed;
    private boolean paused;
    private boolean botWaiting;
    private Player lastTurnActor;

    private Node currentOverlay;
    private Node pauseOverlay;
    private boolean overlayOpen;

    private final Random random = new Random();

    // =====================================================================
    // Ініціалізація
    // =====================================================================

    @FXML
    private void initialize() {
        root.getStyleClass().add("game-root");

        stage = new Pane();
        stage.setPrefSize(SW, SH);
        stage.setMinSize(SW, SH);
        stage.setMaxSize(SW, SH);

        ImageView bg = new ImageView();
        Image bgImage = ThemeManager.backgroundImage("bg_table");
        if (bgImage != null) {
            bg.setImage(bgImage);
        }
        bg.setFitWidth(SW);
        bg.setFitHeight(SH);
        bg.setPreserveRatio(false);
        bg.setSmooth(true);
        bg.setMouseTransparent(true);

        cardLayer = transparentLayer();
        hudLayer = transparentLayer();
        bubbleLayer = transparentLayer();
        bubbleLayer.setMouseTransparent(true);

        stage.getChildren().addAll(bg, cardLayer, hudLayer, bubbleLayer);
        root.getChildren().add(stage);

        buildStaticHud();

        root.widthProperty().addListener((o, a, b) -> updateScale());
        root.heightProperty().addListener((o, a, b) -> updateScale());
    }

    private Pane transparentLayer() {
        Pane pane = new Pane();
        pane.setPickOnBounds(false);
        pane.setPrefSize(SW, SH);
        pane.setMinSize(SW, SH);
        pane.setMaxSize(SW, SH);
        return pane;
    }

    private void updateScale() {
        double s = Math.min(root.getWidth() / SW, root.getHeight() / SH);
        if (s > 0) {
            stage.setScaleX(s);
            stage.setScaleY(s);
        }
    }

    private void buildStaticHud() {
        bannerLabel = centered("banner", 640, 170, 620, 38);

        deckCountLabel = centered("deck-count", DECK.getX(), DECK.getY() - 92, 120, 28);
        discardLabel = centered("deck-count", DISCARD.getX(), DISCARD.getY() - 92, 120, 28);

        deckMarker = centered("deck-marker", DECK.getX(), DECK.getY(), 96, 96);
        deckMarker.setVisible(false);

        Label trumpTitle = new Label("КОЗИР");
        trumpTitle.getStyleClass().add("trump-title");
        trumpSymbol = new Label("");
        trumpSymbol.getStyleClass().add("trump-symbol");
        trumpBadge = new VBox(0, trumpTitle, trumpSymbol);
        trumpBadge.setAlignment(Pos.CENTER);
        trumpBadge.getStyleClass().add("trump-badge");
        trumpBadge.setPrefSize(92, 76);
        trumpBadge.setLayoutX(DECK.getX() - 46);
        trumpBadge.setLayoutY(DECK.getY() + 84);
        trumpBadge.setMouseTransparent(true);

        actionButton = new Button();
        actionButton.getStyleClass().addAll("btn-gold", "action-button");
        actionButton.setPrefSize(206, 56);
        actionButton.setLayoutX(1140 - 103);
        actionButton.setLayoutY(606 - 28);
        actionButton.setOnAction(e -> onActionButton());
        actionButton.setVisible(false);

        pauseButton = new Button("☰");
        pauseButton.getStyleClass().addAll("btn-ghost", "pause-button");
        pauseButton.setPrefSize(46, 46);
        pauseButton.setLayoutX(1204 - 23);
        pauseButton.setLayoutY(122 - 23);
        pauseButton.setOnAction(e -> togglePause());

        hudLayer.getChildren().addAll(bannerLabel, deckCountLabel, discardLabel, deckMarker,
                trumpBadge, actionButton, pauseButton);
    }

    private Label centered(String styleClass, double cx, double cy, double w, double h) {
        Label label = new Label();
        label.getStyleClass().add(styleClass);
        label.setAlignment(Pos.CENTER);
        label.setPrefSize(w, h);
        label.setMinSize(w, h);
        label.setMaxSize(w, h);
        label.setLayoutX(cx - w / 2);
        label.setLayoutY(cy - h / 2);
        label.setMouseTransparent(true);
        return label;
    }

    /** Починає партію. Викликається після показу сцени. */
    void begin(GameLauncher.Config cfg, Scene scene) {
        this.config = cfg;
        this.hotSeat = cfg.mode() == GameMode.LOCAL_MULTIPLAYER;
        updateScale();

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                togglePause();
            }
        });

        players = GameLauncher.createPlayers(cfg);
        gm = new GameManager();
        gm.startGame(players, cfg.mode());
        List<GameEvent> initial = gm.drainEvents();

        viewer = hotSeat ? gm.getActor() : firstHuman();

        Runnable go = () -> {
            buildScene(true);
            SoundManager.play(Sound.SHUFFLE);
            playEvents(initial);
        };
        if (hotSeat) {
            showPassOverlay(viewer, go);
        } else {
            go.run();
        }
    }

    private Player firstHuman() {
        for (Player p : players) {
            if (!p.isBot()) {
                return p;
            }
        }
        return players.get(0);
    }

    // =====================================================================
    // Побудова сцени
    // =====================================================================

    /** fresh = нова партія (руки порожні, роздачу покажуть події); інакше відновлення з поточного стану. */
    private void buildScene(boolean fresh) {
        cardLayer.getChildren().clear();
        bubbleLayer.getChildren().clear();
        for (PlayerPlate plate : plates.values()) {
            hudLayer.getChildren().remove(plate);
        }
        plates.clear();
        hands.clear();
        table.clear();
        deckLayers.clear();
        discardLayers.clear();
        transit.clear();
        trumpNode = null;
        selectedHandNode = null;
        selectedTarget = null;

        assignSeats();
        createPlates();
        for (Player p : players) {
            hands.put(p, new ArrayList<>());
        }

        deckVisualCount = fresh ? 36 : gm.getDeck().size();
        discardVisualCount = fresh ? 0 : gm.getDiscardPile().size();
        visualTaking = !fresh && gm.isDefenderTaking();

        Card trumpCard = gm.getTrumpCard();
        if (deckVisualCount > 0 && trumpCard != null) {
            trumpNode = new CardNode(trumpCard, true);
            trumpNode.moveTo(DECK.getX() + 40, DECK.getY(), 90, 1.0, 0);
            cardLayer.getChildren().add(trumpNode);
        }
        updateDeckStack();
        updateDiscardStack();
        updateTrumpBadge();

        if (!fresh) {
            for (Player p : players) {
                for (Card card : p.getHand().getCards()) {
                    CardNode node = p == viewer ? new CardNode(card, true) : new CardNode(null, false);
                    cardLayer.getChildren().add(node);
                    hands.get(p).add(node);
                    if (p == viewer) {
                        bindHandClick(node);
                    }
                }
            }
            for (AttackPair ap : gm.getTablePairs()) {
                PairView pv = new PairView();
                pv.attack = new CardNode(ap.getAttackCard(), true);
                cardLayer.getChildren().add(pv.attack);
                bindTableClick(pv);
                if (ap.getDefenseCard() != null) {
                    pv.defense = new CardNode(ap.getDefenseCard(), true);
                    cardLayer.getChildren().add(pv.defense);
                }
                table.add(pv);
            }
            for (Player p : players) {
                layoutHand(p, false);
            }
            layoutTable(false);
        }
        restack();
        refreshHud();
    }

    private void assignSeats() {
        seats.clear();
        int n = players.size();
        int v = players.indexOf(viewer);
        seats.put(viewer, Seat.BOTTOM);
        Seat[] order = switch (n) {
            case 2 -> new Seat[]{Seat.TOP};
            case 3 -> new Seat[]{Seat.LEFT, Seat.RIGHT};
            default -> new Seat[]{Seat.LEFT, Seat.TOP, Seat.RIGHT};
        };
        for (int i = 1; i < n; i++) {
            seats.put(players.get((v + i) % n), order[i - 1]);
        }
    }

    private Point2D plateCenter(Seat seat) {
        return switch (seat) {
            case BOTTOM -> new Point2D(184, 650);
            case LEFT -> new Point2D(106, 205);
            case TOP -> new Point2D(300, 84);
            case RIGHT -> new Point2D(1174, 205);
        };
    }

    private void createPlates() {
        for (Player p : players) {
            String subtitle = null;
            if (p.isBot() && p instanceof com.durak.players.BotPlayer bp) {
                subtitle = bp.getDifficulty().getTitle();
            }
            PlayerPlate plate = new PlayerPlate(p.getName(), AvatarRegistry.getAvatar(p.getName()), subtitle);
            Point2D c = plateCenter(seats.get(p));
            plate.setLayoutX(c.getX() - PlayerPlate.WIDTH / 2);
            plate.setLayoutY(c.getY() - PlayerPlate.HEIGHT / 2);
            hudLayer.getChildren().add(plate);
            plates.put(p, plate);
        }
    }

    // ---- колода, відбій, козир

    private void updateDeckStack() {
        int needed = deckVisualCount <= 0 ? 0 : Math.max(1, Math.min(5, (deckVisualCount + 5) / 7));
        while (deckLayers.size() < needed) {
            int k = deckLayers.size();
            CardNode layer = new CardNode(null, false);
            layer.moveTo(DECK.getX() - k * 1.6, DECK.getY() - k * 1.6, 0, 1.0, 0);
            cardLayer.getChildren().add(layer);
            deckLayers.add(layer);
        }
        while (deckLayers.size() > needed) {
            CardNode top = deckLayers.remove(deckLayers.size() - 1);
            cardLayer.getChildren().remove(top);
        }
        deckCountLabel.setText(deckVisualCount > 0 ? "Колода: " + deckVisualCount : "");
        deckMarker.setVisible(deckVisualCount <= 0);
        restack();
    }

    private void updateDiscardStack() {
        int needed = discardVisualCount <= 0 ? 0 : Math.min(5, (discardVisualCount + 5) / 6);
        while (discardLayers.size() < needed) {
            int k = discardLayers.size();
            CardNode layer = new CardNode(null, false);
            double rot = ((k * 37) % 17) - 8;
            double dx = ((k * 13) % 7) - 3;
            double dy = ((k * 11) % 5) - 2;
            layer.moveTo(DISCARD.getX() + dx, DISCARD.getY() + dy, rot, 0.9, 0);
            cardLayer.getChildren().add(layer);
            discardLayers.add(layer);
        }
        discardLabel.setText(discardVisualCount > 0 ? "Бито: " + discardVisualCount : "");
        restack();
    }

    private void updateTrumpBadge() {
        Suit trump = gm.getCurrentTrump();
        if (trump == null) {
            return;
        }
        String symbol = suitSymbol(trump);
        boolean red = trump == Suit.HEARTS || trump == Suit.DIAMONDS;
        String color = red ? "#ff6b72" : "#f2f2f2";
        trumpSymbol.setText(symbol);
        trumpSymbol.setStyle("-fx-text-fill: " + color + ";");
        deckMarker.setText(symbol);
        deckMarker.setStyle("-fx-text-fill: " + color + ";");
    }

    private static String suitSymbol(Suit suit) {
        return switch (suit) {
            case HEARTS -> "♥";
            case DIAMONDS -> "♦";
            case CLUBS -> "♣";
            case SPADES -> "♠";
        };
    }

    // =====================================================================
    // Розкладка карт
    // =====================================================================

    private double ms(double base) {
        return base * AppSettings.getTimeFactor();
    }

    private void layoutHand(Player p, boolean animate) {
        List<CardNode> list = hands.get(p);
        Seat seat = seats.get(p);
        if (list == null || seat == null) {
            return;
        }
        if (seat == Seat.BOTTOM) {
            list.sort(Comparator
                    .comparing((CardNode n) -> n.getCard() != null && n.getCard().isTrump(gm.getCurrentTrump()))
                    .thenComparingInt(n -> n.getCard() == null ? 0 : n.getCard().getSuit().ordinal())
                    .thenComparingInt(n -> n.getCard() == null ? 0 : n.getCard().getRank().ordinal()));
        }
        int n = list.size();
        double c = (n - 1) / 2.0;
        double dur = animate ? ms(300) : 0;

        for (int i = 0; i < n; i++) {
            CardNode node = list.get(i);
            double t = (i - c) / Math.max(c, 1);
            double x;
            double y;
            double rot;
            double scale;
            switch (seat) {
                case BOTTOM -> {
                    double spacing = n <= 1 ? 0 : Math.min(74, 700.0 / (n - 1));
                    double spread = Math.min(26, 3.2 * n);
                    x = 620 + (i - c) * spacing;
                    y = 620 + t * t * 14;
                    rot = t * spread / 2;
                    scale = 1.0;
                }
                case TOP -> {
                    double spacing = n <= 1 ? 0 : Math.min(30, 330.0 / (n - 1));
                    x = 640 + (i - c) * spacing;
                    y = 80 - (1 - t * t) * 0;
                    rot = 180;
                    scale = OPPONENT_SCALE;
                }
                case LEFT -> {
                    double spacing = n <= 1 ? 0 : Math.min(26, 290.0 / (n - 1));
                    x = 70;
                    y = 392 + (i - c) * spacing;
                    rot = 90;
                    scale = OPPONENT_SCALE;
                }
                default -> {
                    double spacing = n <= 1 ? 0 : Math.min(26, 290.0 / (n - 1));
                    x = 1210;
                    y = 392 + (i - c) * spacing;
                    rot = -90;
                    scale = OPPONENT_SCALE;
                }
            }
            node.moveTo(x, y, rot, scale, dur);
            node.setHoverable(seat == Seat.BOTTOM);
        }
        restack();
    }

    private Point2D tableSlot(int index, int total) {
        if (total <= 3) {
            double x = TABLE_CENTER.getX() + (index - (total - 1) / 2.0) * PAIR_SPACING;
            return new Point2D(x, TABLE_CENTER.getY() - 6);
        }
        int row = index < 3 ? 0 : 1;
        int inRow = row == 0 ? 3 : total - 3;
        int col = row == 0 ? index : index - 3;
        double x = TABLE_CENTER.getX() + (col - (inRow - 1) / 2.0) * PAIR_SPACING;
        double y = TABLE_CENTER.getY() + (row == 0 ? -80 : 78);
        return new Point2D(x, y);
    }

    private void layoutTable(boolean animate) {
        double dur = animate ? ms(320) : 0;
        int total = table.size();
        for (int i = 0; i < total; i++) {
            PairView pv = table.get(i);
            Point2D slot = tableSlot(i, total);
            pv.attack.moveTo(slot.getX(), slot.getY(), -4, 1.0, dur);
            pv.attack.setHoverable(false);
            if (pv.defense != null) {
                pv.defense.moveTo(slot.getX() + 26, slot.getY() + 24, 7, 1.0, dur);
                pv.defense.setHoverable(false);
            }
        }
        restack();
    }

    private void restack() {
        List<CardNode> ordered = new ArrayList<>();
        if (trumpNode != null) {
            ordered.add(trumpNode);
        }
        ordered.addAll(deckLayers);
        ordered.addAll(discardLayers);
        for (Player p : players) {
            if (p != viewer) {
                ordered.addAll(hands.getOrDefault(p, List.of()));
            }
        }
        for (PairView pv : table) {
            ordered.add(pv.attack);
            if (pv.defense != null) {
                ordered.add(pv.defense);
            }
        }
        ordered.addAll(hands.getOrDefault(viewer, List.of()));

        List<CardNode> top = new ArrayList<>();
        for (CardNode n : ordered) {
            if (n.getParent() != cardLayer) {
                continue;
            }
            if (n.isFlying()) {
                top.add(n);
            } else {
                n.toFront();
            }
        }
        for (CardNode n : transit) {
            if (n.getParent() == cardLayer) {
                n.toFront();
            }
        }
        for (CardNode n : top) {
            n.toFront();
        }
    }

    // =====================================================================
    // Черга подій і їх відтворення
    // =====================================================================

    private void playEvents(List<GameEvent> events) {
        if (events.isEmpty() && !pumping) {
            onQueueEmpty();
            return;
        }
        for (GameEvent e : events) {
            queue.add(stepFor(e));
        }
        if (!pumping) {
            pump();
        }
    }

    private void pump() {
        if (disposed) {
            return;
        }
        Step step = queue.poll();
        if (step == null) {
            pumping = false;
            onQueueEmpty();
            return;
        }
        pumping = true;
        double wait = step.run() * AppSettings.getTimeFactor();
        PauseTransition pause = new PauseTransition(Duration.millis(Math.max(1, wait)));
        pause.setOnFinished(e -> pump());
        pause.play();
    }

    private Step stepFor(GameEvent e) {
        return switch (e.getType()) {
            case DEAL -> () -> doDeal(e.getPlayer(), e.getCard());
            case ATTACK -> () -> doPlay(e.getPlayer(), e.getCard(), null);
            case DEFEND -> () -> doPlay(e.getPlayer(), e.getCard(), e.getAttackCard());
            case TAKE_DECLARED -> () -> doTakeDeclared(e.getPlayer());
            case TAKE -> () -> doTake(e.getPlayer());
            case DISCARD -> this::doDiscard;
            case DRAW -> () -> doDraw(e.getPlayer(), e.getCard());
            case PASS -> () -> doPass(e.getPlayer());
            case GAME_OVER -> () -> 250;
        };
    }

    private CardNode newDeckCard(Player p, Card card) {
        CardNode node = new CardNode(p == viewer ? card : null, false);
        node.moveTo(DECK.getX(), DECK.getY(), 0, 1.0, 0);
        cardLayer.getChildren().add(node);
        if (p == viewer) {
            bindHandClick(node);
        }
        return node;
    }

    private double doDeal(Player p, Card card) {
        CardNode node = newDeckCard(p, card);
        hands.get(p).add(node);
        deckVisualCount--;
        updateDeckStack();
        SoundManager.play(Sound.CARD_DEAL);
        layoutHand(p, true);
        revealLater(node, p);
        return 110;
    }

    private double doDraw(Player p, Card card) {
        CardNode node;
        boolean lastCard = deckVisualCount == 1 && trumpNode != null;
        if (lastCard) {
            node = trumpNode;
            trumpNode = null;
            if (p == viewer) {
                bindHandClick(node);
            } else {
                node.flipDownAndForget(ms(200));
            }
        } else {
            node = newDeckCard(p, card);
        }
        hands.get(p).add(node);
        deckVisualCount--;
        updateDeckStack();
        SoundManager.play(Sound.CARD_DEAL);
        layoutHand(p, true);
        if (!lastCard) {
            revealLater(node, p);
        }
        return 230;
    }

    /** Карта, що летить до вашої руки, перевертається лицем догори після приземлення. */
    private void revealLater(CardNode node, Player p) {
        if (p == viewer) {
            schedule(ms(280), () -> node.flipTo(true, ms(170)));
        }
    }

    private CardNode takeFromHand(Player p, Card card) {
        List<CardNode> list = hands.get(p);
        CardNode found = null;
        if (p == viewer) {
            for (CardNode n : list) {
                if (card.equals(n.getCard())) {
                    found = n;
                    break;
                }
            }
        } else if (!list.isEmpty()) {
            found = list.get(random.nextInt(list.size()));
        }
        if (found != null) {
            list.remove(found);
        } else {
            found = new CardNode(card, true);
            Point2D c = plateCenter(seats.get(p));
            found.moveTo(c.getX(), c.getY(), 0, 1.0, 0);
            cardLayer.getChildren().add(found);
        }
        if (found == selectedHandNode) {
            selectedHandNode = null;
        }
        found.setMode(CardNode.Mode.NORMAL);
        found.setHoverable(false);
        return found;
    }

    /** Атака/підкидання (attackCard == null) або захист. */
    private double doPlay(Player p, Card card, Card attackCard) {
        CardNode node = takeFromHand(p, card);
        if (node.getCard() == null) {
            node.setCard(card);
        }
        node.flipTo(true, ms(180));

        if (attackCard == null) {
            PairView pv = new PairView();
            pv.attack = node;
            table.add(pv);
            bindTableClick(pv);
        } else {
            for (PairView pv : table) {
                if (attackCard.equals(pv.attack.getCard()) && pv.defense == null) {
                    pv.defense = node;
                    node.setOnMouseClicked(null);
                    break;
                }
            }
        }
        if (selectedTarget != null && attackCard != null) {
            selectedTarget = null;
        }
        layoutTable(true);
        layoutHand(p, true);
        SoundManager.play(Sound.CARD_PLACE);
        return 470;
    }

    private double doTakeDeclared(Player p) {
        visualTaking = true;
        showBubble(p, "Беру!");
        return 650;
    }

    private double doTake(Player p) {
        List<CardNode> moved = new ArrayList<>();
        for (PairView pv : table) {
            moved.add(pv.attack);
            if (pv.defense != null) {
                moved.add(pv.defense);
            }
        }
        table.clear();
        visualTaking = false;
        selectedTarget = null;

        boolean mine = p == viewer;
        List<CardNode> hand = hands.get(p);
        for (CardNode n : moved) {
            n.setOnMouseClicked(null);
            if (mine) {
                bindHandClick(n);
            } else {
                n.flipDownAndForget(ms(200));
            }
            hand.add(n);
        }
        SoundManager.play(Sound.CARD_TAKE);
        layoutHand(p, true);
        return 720;
    }

    private double doDiscard() {
        List<CardNode> moved = new ArrayList<>();
        for (PairView pv : table) {
            moved.add(pv.attack);
            if (pv.defense != null) {
                moved.add(pv.defense);
            }
        }
        table.clear();
        selectedTarget = null;

        for (CardNode n : moved) {
            n.setOnMouseClicked(null);
            n.flipDownAndForget(ms(220));
            double dx = random.nextInt(9) - 4;
            double dy = random.nextInt(9) - 4;
            n.moveTo(DISCARD.getX() + dx, DISCARD.getY() + dy, random.nextInt(21) - 10, 0.9, ms(430));
            transit.add(n);
        }
        restack();
        SoundManager.play(Sound.CARD_DISCARD);

        schedule(ms(450), () -> {
            for (CardNode n : moved) {
                cardLayer.getChildren().remove(n);
                transit.remove(n);
            }
            discardVisualCount += moved.size();
            updateDiscardStack();
        });
        return 560;
    }

    private double doPass(Player p) {
        String text;
        boolean allBeaten = !table.isEmpty() && table.stream().allMatch(pv -> pv.defense != null);
        if (visualTaking) {
            text = "Досить";
        } else if (allBeaten) {
            text = "Бито!";
        } else {
            text = "Готово";
        }
        showBubble(p, text);
        return 480;
    }

    // ---- «хмаринки» з коротким повідомленням біля таблички гравця

    private void showBubble(Player p, String text) {
        Seat seat = seats.get(p);
        if (seat == null) {
            return;
        }
        Point2D c = plateCenter(seat);
        Label bubble = new Label(text);
        bubble.getStyleClass().add("bubble");
        bubble.setPrefWidth(150);
        bubble.setAlignment(Pos.CENTER);
        bubble.setLayoutX(c.getX() - 75);
        bubble.setLayoutY(seat == Seat.BOTTOM ? c.getY() - 76 : c.getY() + 40);
        bubble.setOpacity(0);
        bubbleLayer.getChildren().add(bubble);

        Timeline t = new Timeline(
                new KeyFrame(Duration.millis(0), new KeyValue(bubble.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(ms(160)), new KeyValue(bubble.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(ms(900)), new KeyValue(bubble.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(ms(1200)), new KeyValue(bubble.opacityProperty(), 0)));
        t.setOnFinished(e -> bubbleLayer.getChildren().remove(bubble));
        t.play();
    }

    private void schedule(double ms, Runnable action) {
        PauseTransition pause = new PauseTransition(Duration.millis(Math.max(1, ms)));
        pause.setOnFinished(e -> {
            if (!disposed) {
                action.run();
            }
        });
        pause.play();
    }

    // =====================================================================
    // Після завершення анімацій: чий хід
    // =====================================================================

    private void onQueueEmpty() {
        if (disposed) {
            return;
        }
        refreshHud();
        if (gm.isGameOver()) {
            schedule(ms(500), this::showGameOver);
            return;
        }
        Player actor = gm.getActor();
        if (actor == null) {
            return;
        }
        if (actor.isBot()) {
            scheduleBot(actor);
            return;
        }
        if (hotSeat && actor != viewer) {
            showPassOverlay(actor, () -> {
                viewer = actor;
                buildScene(false);
            });
            return;
        }
        if (actor != lastTurnActor) {
            SoundManager.play(Sound.TURN);
        }
        lastTurnActor = actor;
    }

    private void scheduleBot(Player bot) {
        lastTurnActor = bot;
        BotMove move = BotBrain.decide(gm, bot);
        double delay = move.getType() == BotMove.Type.PASS ? 380 : 700 + random.nextInt(450);
        schedule(ms(delay), () -> runBotMove(bot, move));
    }

    private void runBotMove(Player bot, BotMove move) {
        if (paused) {
            botWaiting = true;
            return;
        }
        if (gm.isGameOver() || gm.getActor() != bot) {
            return;
        }
        try {
            BotBrain.apply(gm, bot, move);
        } catch (RuntimeException ex) {
            // подвійна страховка: бот ніколи не має зависати гру
            BotBrain.apply(gm, bot, BotBrain.decide(gm, bot));
        }
        playEvents(gm.drainEvents());
    }

    // =====================================================================
    // Хід людини
    // =====================================================================

    private boolean canAct() {
        return !disposed && !overlayOpen && !pumping && gm != null
                && !gm.isGameOver() && gm.getActor() == viewer;
    }

    private void bindHandClick(CardNode node) {
        node.setOnMouseClicked(e -> onHandCardClicked(node));
    }

    private void bindTableClick(PairView pv) {
        pv.attack.setOnMouseClicked(e -> onTableCardClicked(pv));
    }

    private void perform(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            flashBanner(ex.getMessage());
            SoundManager.play(Sound.ERROR);
            return;
        }
        clearModes();
        selectedHandNode = null;
        selectedTarget = null;
        playEvents(gm.drainEvents());
    }

    /** Знімає підсвітку з усіх карт (після ходу, поки йде анімація). */
    private void clearModes() {
        for (CardNode n : hands.getOrDefault(viewer, List.of())) {
            n.setMode(CardNode.Mode.NORMAL);
        }
        for (PairView pv : table) {
            pv.attack.setMode(CardNode.Mode.NORMAL);
        }
    }

    private void deny(CardNode node, String message) {
        node.shake();
        SoundManager.play(Sound.ERROR);
        flashBanner(message);
    }

    private void onHandCardClicked(CardNode node) {
        if (!canAct() || node.getCard() == null) {
            return;
        }
        Card card = node.getCard();
        TurnPhase phase = gm.getPhase();

        if (phase == TurnPhase.ATTACK || phase == TurnPhase.THROW_IN) {
            if (gm.legalAttackCards(viewer).contains(card)) {
                perform(() -> gm.attack(viewer, card));
            } else if (gm.getTablePairs().size() >= gm.getPairLimit()) {
                deny(node, "На столі вже максимум карт для цього захисника");
            } else {
                deny(node, "Підкидати можна лише карти того ж рангу, що вже лежать на столі");
            }
            return;
        }

        // фаза захисту
        List<Card> unbeaten = gm.getUnbeatenCards();
        if (selectedTarget != null) {
            Card target = selectedTarget.attack.getCard();
            if (gm.canBeat(card, target)) {
                perform(() -> gm.defend(viewer, target, card));
            } else {
                deny(node, "Цією картою не побити обрану карту");
            }
            return;
        }
        List<Card> targets = new ArrayList<>();
        for (Card t : unbeaten) {
            if (gm.canBeat(card, t)) {
                targets.add(t);
            }
        }
        if (targets.isEmpty()) {
            deny(node, "Цією картою нічого побити");
        } else if (unbeaten.size() == 1 || targets.size() == 1) {
            Card target = targets.get(0);
            perform(() -> gm.defend(viewer, target, card));
        } else {
            // кілька можливих цілей — обираємо карту, потім ціль
            if (selectedHandNode != null) {
                selectedHandNode.setMode(CardNode.Mode.NORMAL);
            }
            selectedHandNode = selectedHandNode == node ? null : node;
            refreshHud();
        }
    }

    private void onTableCardClicked(PairView pv) {
        if (!canAct() || gm.getPhase() != TurnPhase.DEFEND || pv.defense != null) {
            return;
        }
        Card target = pv.attack.getCard();
        if (selectedHandNode != null) {
            Card card = selectedHandNode.getCard();
            if (card != null && gm.canBeat(card, target)) {
                perform(() -> gm.defend(viewer, target, card));
            } else {
                deny(pv.attack, "Цією картою не побити обрану карту");
            }
            return;
        }
        selectedTarget = selectedTarget == pv ? null : pv;
        refreshHud();
    }

    private void onActionButton() {
        if (!canAct()) {
            return;
        }
        if (gm.canTake(viewer)) {
            perform(() -> gm.take(viewer));
        } else if (gm.canPass(viewer)) {
            perform(() -> gm.pass(viewer));
        }
    }

    // =====================================================================
    // HUD: таблички, напис, кнопка, підказки
    // =====================================================================

    private void refreshHud() {
        if (gm == null || gm.getGame() == null) {
            return;
        }
        Player actor = gm.getActor();
        for (Player p : players) {
            PlayerPlate plate = plates.get(p);
            if (plate == null) {
                continue;
            }
            String role = "";
            String style = null;
            if (!gm.isGameOver()) {
                if (p == gm.getAttacker()) {
                    role = "АТАКУЄ";
                    style = "role-attack";
                } else if (p == gm.getDefender()) {
                    role = gm.isDefenderTaking() ? "БЕРЕ" : "ЗАХИЩАЄТЬСЯ";
                    style = gm.isDefenderTaking() ? "role-take" : "role-defend";
                } else if (p == actor) {
                    role = "ПІДКИДАЄ";
                    style = "role-throw";
                }
            }
            plate.update(p.getHand().size(), role, style, p == actor);
        }
        updateBanner();
        updateActionButton();
        refreshHints();
    }

    private void updateBanner() {
        bannerLabel.setText(bannerText());
        bannerLabel.setVisible(!bannerLabel.getText().isEmpty());
    }

    private void flashBanner(String message) {
        bannerLabel.setText(message);
        bannerLabel.setVisible(true);
        schedule(1900, this::updateBanner);
    }

    private String bannerText() {
        if (gm.isGameOver()) {
            return "Гру завершено";
        }
        Player actor = gm.getActor();
        if (actor == null) {
            return "";
        }
        if (pumping) {
            return bannerLabel.getText();
        }
        if (actor != viewer) {
            if (actor.isBot()) {
                if (gm.getPhase() == TurnPhase.DEFEND) return actor.getName() + " захищається…";
                if (gm.getPhase() == TurnPhase.ATTACK) return actor.getName() + " атакує…";
                return actor.getName() + " підкидає…";
            }
            return actor.getName() + ", ваш хід";
        }
        String prefix = hotSeat ? actor.getName() + ": " : "";
        switch (gm.getPhase()) {
            case ATTACK:
                return prefix + "Ваш хід — оберіть карту для атаки";
            case THROW_IN:
                if (gm.isDefenderTaking()) {
                    return gm.getDefender().getName() + " бере — підкиньте ще або натисніть «Досить»";
                }
                if (!gm.getUnbeatenCards().isEmpty()) {
                    return prefix + "Додайте карти того ж рангу або натисніть «Готово»";
                }
                return prefix + "Підкиньте карту того ж рангу або натисніть «Бито»";
            default:
                if (!anyDefensePossible()) {
                    return prefix + "Нічим бити — доведеться взяти карти";
                }
                return gm.getUnbeatenCards().size() > 1
                        ? prefix + "Оберіть карту та ціль на столі — або візьміть"
                        : prefix + "Побийте карту або візьміть карти";
        }
    }

    private boolean anyDefensePossible() {
        for (Card target : gm.getUnbeatenCards()) {
            if (!gm.legalDefenseCards(target).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void updateActionButton() {
        boolean mine = !gm.isGameOver() && gm.getActor() == viewer && !pumping && !overlayOpen;
        String text = null;
        boolean danger = false;
        if (mine && gm.canTake(viewer)) {
            text = "БЕРУ";
            danger = true;
        } else if (mine && gm.canPass(viewer)) {
            if (gm.isDefenderTaking()) {
                text = "ДОСИТЬ";
            } else if (gm.getUnbeatenCards().isEmpty()) {
                text = "БИТО";
            } else {
                text = "ГОТОВО";
            }
        }
        actionButton.setVisible(text != null);
        if (text != null) {
            actionButton.setText(text);
        }
        actionButton.getStyleClass().remove("btn-danger");
        if (danger) {
            actionButton.getStyleClass().add("btn-danger");
        }
    }

    /** Підсвічує карти, якими можна ходити, і допустимі цілі на столі. */
    private void refreshHints() {
        boolean myTurn = canAct();
        boolean hints = AppSettings.isHintsEnabled();
        List<CardNode> hand = hands.getOrDefault(viewer, List.of());
        for (CardNode node : hand) {
            CardNode.Mode mode = CardNode.Mode.NORMAL;
            if (node == selectedHandNode) {
                mode = CardNode.Mode.SELECTED;
            } else if (myTurn && hints && node.getCard() != null) {
                mode = isPlayable(node.getCard()) ? CardNode.Mode.PLAYABLE : CardNode.Mode.DIM;
            }
            node.setMode(mode);
        }
        for (PairView pv : table) {
            CardNode.Mode mode = CardNode.Mode.NORMAL;
            if (myTurn && gm.getPhase() == TurnPhase.DEFEND && pv.defense == null) {
                if (pv == selectedTarget) {
                    mode = CardNode.Mode.SELECTED;
                } else if (selectedHandNode != null && selectedHandNode.getCard() != null
                        && gm.canBeat(selectedHandNode.getCard(), pv.attack.getCard())) {
                    mode = CardNode.Mode.TARGET;
                }
            }
            pv.attack.setMode(mode);
        }
    }

    private boolean isPlayable(Card card) {
        TurnPhase phase = gm.getPhase();
        if (phase == TurnPhase.DEFEND) {
            if (selectedTarget != null) {
                return gm.canBeat(card, selectedTarget.attack.getCard());
            }
            for (Card target : gm.getUnbeatenCards()) {
                if (gm.canBeat(card, target)) {
                    return true;
                }
            }
            return false;
        }
        return gm.legalAttackCards(viewer).contains(card);
    }

    // =====================================================================
    // Оверлеї: пауза, передача ходу, кінець гри
    // =====================================================================

    private void showOverlay(Node overlay) {
        closeOverlay();
        currentOverlay = overlay;
        overlayOpen = true;
        root.getChildren().add(overlay);
        refreshHud();
    }

    private void closeOverlay() {
        if (currentOverlay != null) {
            root.getChildren().remove(currentOverlay);
        }
        currentOverlay = null;
        pauseOverlay = null;
        overlayOpen = false;
    }

    private StackPane buildOverlay(String title, String titleStyle, String subtitle, Node extra,
                                   boolean opaque, Button... buttons) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().addAll("overlay-title", titleStyle);

        VBox panel = new VBox(18);
        panel.setAlignment(Pos.CENTER);
        panel.getStyleClass().addAll("panel", "overlay-panel");
        panel.setMaxWidth(520);
        panel.setMaxHeight(Region.USE_PREF_SIZE);
        panel.getChildren().add(titleLabel);

        if (subtitle != null && !subtitle.isEmpty()) {
            Label sub = new Label(subtitle);
            sub.getStyleClass().add("overlay-subtitle");
            sub.setWrapText(true);
            sub.setAlignment(Pos.CENTER);
            sub.setMaxWidth(440);
            panel.getChildren().add(sub);
        }
        if (extra != null) {
            panel.getChildren().add(extra);
        }
        VBox buttonBox = new VBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        for (Button b : buttons) {
            b.setPrefWidth(300);
            b.setPrefHeight(46);
            buttonBox.getChildren().add(b);
        }
        panel.getChildren().add(buttonBox);

        StackPane overlay = new StackPane(panel);
        overlay.getStyleClass().add(opaque ? "overlay-opaque" : "overlay-dim");
        overlay.setAlignment(Pos.CENTER);
        return overlay;
    }

    private Button gold(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("btn-gold");
        b.setOnAction(e -> action.run());
        return b;
    }

    private Button ghost(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("btn-ghost");
        b.setOnAction(e -> action.run());
        return b;
    }

    private void togglePause() {
        if (disposed || gm == null || gm.isGameOver()) {
            return;
        }
        if (pauseOverlay != null) {
            resumeFromPause();
            return;
        }
        if (overlayOpen) {
            return;
        }
        paused = true;
        StackPane overlay = buildOverlay("ПАУЗА", "title-gold", "Партія призупинена", null, false,
                gold("ПРОДОВЖИТИ", this::resumeFromPause),
                ghost("ПОЧАТИ ЗАНОВО", this::restart),
                ghost("У ГОЛОВНЕ МЕНЮ", this::leave));
        showOverlay(overlay);
        pauseOverlay = overlay;
    }

    private void resumeFromPause() {
        closeOverlay();
        paused = false;
        refreshHud();
        if (botWaiting) {
            botWaiting = false;
            if (!pumping) {
                onQueueEmpty();
            }
        }
    }

    private void showPassOverlay(Player next, Runnable after) {
        VBox hint = new VBox();
        Label warn = new Label("Інші гравці, не підглядайте!");
        warn.getStyleClass().add("overlay-hint");
        hint.getChildren().add(warn);
        hint.setAlignment(Pos.CENTER);

        StackPane overlay = buildOverlay("ПЕРЕДАЧА ХОДУ", "title-gold",
                next.getName() + ", ваш хід!", hint, true,
                gold("Я ГОТОВИЙ", () -> {
                    closeOverlay();
                    after.run();
                    refreshHud();
                    lastTurnActor = next;
                    SoundManager.play(Sound.TURN);
                }));
        showOverlay(overlay);
    }

    private void showGameOver() {
        GameResult result = gm.getResult();
        String title;
        String style;
        Sound sound;
        String subtitle;
        if (result == null || result.isDraw()) {
            title = "НІЧИЯ";
            style = "title-gold";
            sound = Sound.DRAW;
            subtitle = "Усі одночасно лишилися без карт.";
        } else if (!hotSeat) {
            boolean lost = result.getLoser() == viewer;
            title = lost ? "ВИ — ДУРЕНЬ" : "ПЕРЕМОГА!";
            style = lost ? "title-red" : "title-gold";
            sound = lost ? Sound.LOSE : Sound.WIN;
            subtitle = lost ? "Цього разу не пощастило — спробуйте ще!" : result.getLoser().getName() + " залишився дурнем.";
        } else {
            title = result.getLoser().getName() + " — ДУРЕНЬ";
            style = "title-red";
            sound = Sound.WIN;
            subtitle = "Партію завершено.";
        }
        SoundManager.play(sound);

        VBox lines = new VBox(4);
        lines.setAlignment(Pos.CENTER);
        int place = 1;
        for (Player p : gm.getFinishOrder()) {
            if (p != result.getLoser()) {
                Label l = new Label(place++ + ". " + p.getName());
                l.getStyleClass().add("result-line");
                lines.getChildren().add(l);
            }
        }
        if (result.getLoser() != null) {
            Label l = new Label(result.getLoser().getName() + " — дурень (" + PlayerPlate.plural(
                    result.getLoser().getHand().size()) + ")");
            l.getStyleClass().addAll("result-line", "result-loser");
            lines.getChildren().add(l);
        }

        StackPane overlay = buildOverlay(title, style, subtitle, lines, false,
                gold("ГРАТИ ЩЕ РАЗ", this::restart),
                ghost("У ГОЛОВНЕ МЕНЮ", this::leave));
        showOverlay(overlay);
    }

    private void restart() {
        disposed = true;
        GameLauncher.start(config);
    }

    private void leave() {
        disposed = true;
        SceneManager.switchScene("/fxml/MainMenu.fxml", "Durak Ultimate — Головне меню");
    }
}
