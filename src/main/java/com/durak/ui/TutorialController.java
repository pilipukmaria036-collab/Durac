
package com.durak.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;

public class TutorialController {

    @FXML private Label stepCounter;
    @FXML private Label lessonTitle;
    @FXML private Label lessonDescription;
    @FXML private Label exampleTitle;
    @FXML private Label exampleDescription;
    @FXML private Label tipLabel;

    @FXML private HBox exampleCards;

    @FXML private ProgressBar progressBar;

    @FXML private Button backButton;
    @FXML private Button nextButton;

    private int currentStep = 0;

    private final Lesson[] lessons = {

            new Lesson(
                    "Мета гри",
                    "Підкидний дурень — карткова гра, у якій потрібно позбутися всіх своїх карт. Гравці атакують один одного, а захисник намагається відбити всі карти.",
                    "Хто перемагає?",
                    new String[]{"♠", "♥", "♦", "♣"},
                    "Перемагає гравець, який позбувся всіх карт. Останній гравець із картами, коли колода вже закінчилася, програє.",
                    "Твоя головна мета — першим залишитися без карт."
            ),

            new Lesson(
                    "Карти та їхня сила",
                    "У грі використовуються чотири масті: піки, черви, бубни та трефи. Карти мають різну силу: від шістки до туза.",
                    "Порядок карт",
                    new String[]{"6", "7", "8", "9", "10", "J", "Q", "K", "A"},
                    "Сила карт зростає зліва направо: 6, 7, 8, 9, 10, валет, дама, король, туз.",
                    "Туз — найстарша карта, шістка — наймолодша."
            ),

            new Lesson(
                    "Козир",
                    "Козирна масть сильніша за будь-яку іншу масть. Козирем можна побити некозирну карту незалежно від її значення.",
                    "Приклад козиря",
                    new String[]{"9♥", "6♠"},
                    "Якщо піки — козирна масть, шістка пік побиває дев'ятку червів.",
                    "Старший козир побиває молодший козир."
            ),

            new Lesson(
                    "Початок гри",
                    "Кожен гравець отримує по шість карт. Наступна карта відкривається та визначає козирну масть. Решта карт утворюють колоду.",
                    "Роздача",
                    new String[]{"6♠", "8♥", "Q♦", "K♣"},
                    "Гравці отримують карти, а відкрита карта під колодою показує козирну масть.",
                    "Зазвичай першим атакує гравець із наймолодшим козирем."
            ),

            new Lesson(
                    "Атака",
                    "Атакувальний гравець викладає карту на стіл. Захисник повинен побити її картою тієї самої масті, але вищого значення, або будь-яким козирем, якщо атакувальна карта не козирна.",
                    "Приклад атаки",
                    new String[]{"8♥", "Q♥"},
                    "Вісімку червів можна побити дамою червів. Також її можна побити будь-яким козирем.",
                    "Не можна захищатися молодшою картою тієї самої масті."
            ),

            new Lesson(
                    "Підкидання та захист",
                    "Після першої атаки інші гравці можуть підкидати карти того самого значення, яке вже є на столі. Захисник повинен побити кожну атакувальну карту.",
                    "Приклад підкидання",
                    new String[]{"8♥", "8♣", "Q♥", "K♣"},
                    "На столі є вісімка. Можна підкинути іншу вісімку. Захисник має побити обидві карти. Кількість атакувальних карт не може перевищувати кількість карт у захисника на початку захисту.",
                    "Якщо не можеш побити всі карти, доведеться забрати їх."
            ),

            new Lesson(
                    "Завершення раунду та перемога",
                    "Якщо захисник побив усі карти й атакувальники більше не підкидають, карти відправляються у відбій. Якщо захисник не може або не хоче захищатися, він забирає всі карти зі столу.",
                    "Хто забирає карти?",
                    new String[]{"6", "7", "8", "9"},
                    "Після завершення раунду гравці добирають карти з колоди до шести. Спочатку атакувальник, потім інші гравці за чергою, а захисник — останнім.",
                    "Коли колода закінчилася, намагайся позбутися всіх карт. Останній гравець із картами програє."
            ),

            new Lesson(
                    "Керування в цій грі",
                    "Просто клацайте по картах у своїй руці. Карти, якими зараз можна ходити, підсвічуються золотом, решта темніють (підказки можна вимкнути в налаштуваннях).",
                    "Кнопки дій",
                    new String[]{"БЕРУ", "БИТО", "ГОТОВО", "ДОСИТЬ"},
                    "«Беру» — забрати карти зі столу. «Бито» — усе побито, нових карт немає. «Готово» — більше не підкидаю, нехай захисник відбивається. «Досить» — захисник уже бере, підкидати більше не треба.",
                    "Якщо треба побити кілька карт, спершу оберіть свою карту, а потім натисніть на ту, яку б'єте. Esc — пауза, F11 — повний екран."
            )
    };

    @FXML
    public void initialize() {
        showLesson();
    }

    private void showLesson() {

        Lesson lesson = lessons[currentStep];

        stepCounter.setText(
                "Урок " + (currentStep + 1) + " із " + lessons.length
        );

        progressBar.setProgress(
                (double) (currentStep + 1) / lessons.length
        );

        lessonTitle.setText(lesson.title());
        lessonDescription.setText(lesson.description());

        exampleTitle.setText(lesson.exampleTitle());
        exampleDescription.setText(lesson.exampleDescription());

        tipLabel.setText("ПОРАДА: " + lesson.tip());

        exampleCards.getChildren().clear();

        for (String card : lesson.cards()) {
            Label cardLabel = new Label(card);

            cardLabel.getStyleClass().add("tutorial-card");

            if (card.contains("♥") || card.contains("♦")) {
                cardLabel.setStyle("-fx-text-fill: #c83232;");
            }

            exampleCards.getChildren().add(cardLabel);
        }

        backButton.setDisable(currentStep == 0);

        if (currentStep == lessons.length - 1) {
            nextButton.setText("ЗАВЕРШИТИ");
        } else {
            nextButton.setText("ДАЛІ");
        }
    }

    @FXML
    private void onNext() {

        if (currentStep < lessons.length - 1) {
            currentStep++;
            showLesson();
        } else {
            onMenu();
        }
    }

    @FXML
    private void onPrevious() {

        if (currentStep > 0) {
            currentStep--;
            showLesson();
        }
    }

    @FXML
    private void onMenu() {
        SceneManager.switchScene(
                "/fxml/MainMenu.fxml",
                "Durak: Ultimate Card Game"
        );
    }

    private record Lesson(
            String title,
            String description,
            String exampleTitle,
            String[] cards,
            String exampleDescription,
            String tip
    ) {}
}
