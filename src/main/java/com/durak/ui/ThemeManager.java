package com.durak.ui;

import com.durak.settings.AppSettings;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Теми оформлення: CSS-клас теми на корені сцени + тематичний фон.
 * Теми: Смарагдова, Темна, Синя, Бордова (перефарбовується сукно, золото залишається).
 */
public final class ThemeManager {

    private static final String CSS_PATH = "/css/style.css";
    private static final String[] ALL_CLASSES = {"theme-emerald", "theme-dark", "theme-blue", "theme-burgundy"};
    private static final Map<String, Image> IMAGE_CACHE = new HashMap<>();

    private ThemeManager() {
    }

    public static String getThemeName() {
        return AppSettings.getTheme();
    }

    public static void setThemeName(String name) {
        AppSettings.setTheme(name);
    }

    private static String themeClass() {
        return switch (AppSettings.getTheme()) {
            case "Темна" -> "theme-dark";
            case "Синя" -> "theme-blue";
            case "Бордова" -> "theme-burgundy";
            default -> "theme-emerald";
        };
    }

    /** Суфікс файлів фону: "" для смарагдової, інакше _dark/_blue/_burgundy. */
    private static String backgroundSuffix() {
        return switch (AppSettings.getTheme()) {
            case "Темна" -> "_dark";
            case "Синя" -> "_blue";
            case "Бордова" -> "_burgundy";
            default -> "";
        };
    }

    /** Застосовує стилі й клас теми до сцени (і оновлює фон, якщо він є). */
    public static void apply(Scene scene) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }
        var css = ThemeManager.class.getResource(CSS_PATH);
        if (css != null && !scene.getStylesheets().contains(css.toExternalForm())) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        Parent root = scene.getRoot();
        root.getStyleClass().removeAll(ALL_CLASSES);
        root.getStyleClass().add(themeClass());

        Object kind = root.getProperties().get("bgKind");
        Object view = root.getProperties().get("bgView");
        if (kind instanceof String k && view instanceof ImageView iv) {
            iv.setImage(backgroundImage(k));
        }
    }

    /** Фон за назвою ("bg_main" / "bg_table") відповідно до поточної теми. */
    public static Image backgroundImage(String baseName) {
        String suffix = backgroundSuffix();
        Image image = suffix.isEmpty() ? null : loadImage("/images/" + baseName + suffix + ".jpg");
        return image != null ? image : loadImage("/images/" + baseName + ".png");
    }

    /**
     * Обгортка для екрана: шар фону (заповнює вікно з обрізанням по краях) + сам екран.
     * Якщо bgBase == null — фону немає (екран малює його сам, як ігровий стіл).
     */
    public static StackPane createShell(Parent content, String bgBase) {
        StackPane shell = new StackPane();
        shell.getStyleClass().add("shell");
        if (bgBase != null) {
            ImageView bg = createCoverView(shell);
            shell.getChildren().add(bg);
            shell.getProperties().put("bgKind", bgBase);
            shell.getProperties().put("bgView", bg);
        }
        shell.getChildren().add(content);
        return shell;
    }

    private static ImageView createCoverView(StackPane shell) {
        ImageView iv = new ImageView();
        iv.setManaged(false);
        iv.setMouseTransparent(true);
        iv.setPreserveRatio(false);
        iv.setSmooth(true);

        Runnable fit = () -> {
            Image img = iv.getImage();
            double w = shell.getWidth();
            double h = shell.getHeight();
            if (img == null || w <= 0 || h <= 0) {
                return;
            }
            double iw = img.getWidth();
            double ih = img.getHeight();
            double scale = Math.max(w / iw, h / ih);
            double vw = w / scale;
            double vh = h / scale;
            iv.setViewport(new Rectangle2D((iw - vw) / 2, (ih - vh) / 2, vw, vh));
            iv.setFitWidth(w);
            iv.setFitHeight(h);
        };
        shell.widthProperty().addListener((o, a, b) -> fit.run());
        shell.heightProperty().addListener((o, a, b) -> fit.run());
        iv.imageProperty().addListener((o, a, b) -> fit.run());
        Platform.runLater(fit);
        return iv;
    }

    private static Image loadImage(String path) {
        if (IMAGE_CACHE.containsKey(path)) {
            return IMAGE_CACHE.get(path);
        }
        Image image = null;
        try (InputStream in = ThemeManager.class.getResourceAsStream(path)) {
            if (in != null) {
                image = new Image(in);
            }
        } catch (Exception ignored) {
            // повернемо null — буде запасний варіант
        }
        IMAGE_CACHE.put(path, image);
        return image;
    }
}
