package com.durak.game;

import com.durak.cards.AttackPair;
import com.durak.cards.Card;
import com.durak.cards.Deck;
import com.durak.cards.Suit;
import com.durak.mechanics.GameRules;
import com.durak.players.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Рушій гри «Підкидний дурень».
 *
 * <p>Хід раунду:
 * <ol>
 *   <li>{@link TurnPhase#ATTACK} — атакуючий кладе першу карту.</li>
 *   <li>{@link TurnPhase#THROW_IN} — атакуючі (по черзі) можуть підкидати карти рангів, що вже є на столі,
 *       або сказати «бито/досить» ({@link #pass}). Гравці без придатних карт пропускаються автоматично.</li>
 *   <li>{@link TurnPhase#DEFEND} — захисник б'є карти у довільному порядку або береться ({@link #take}).</li>
 *   <li>Коли все побито — знову підкидання; коли ніхто не підкидає — відбій.
 *       Якщо захисник береться — атакуючі можуть ще підкинути, потім він забирає всі карти.</li>
 * </ol>
 * Усі видимі зміни фіксуються у списку подій ({@link #drainEvents()}) для анімацій.
 */
public class GameManager {

    public static final int HAND_SIZE = 6;
    public static final int MAX_PAIRS = 6;

    private final GameRules gameRules = new GameRules();
    private final Random random;

    private Game game;
    private Deck deck;
    private Suit trumpSuit;

    private TurnPhase phase = TurnPhase.ATTACK;
    private Player actor;
    private boolean defenderTaking;
    private int pairLimit;

    private final Set<Player> passed = new HashSet<>();
    private final List<Card> discardPile = new ArrayList<>();
    private final Map<Player, List<Card>> knownCards = new HashMap<>();
    private final List<Player> finishOrder = new ArrayList<>();
    private final List<GameEvent> events = new ArrayList<>();

    public GameManager() {
        this(new Random());
    }

    public GameManager(long seed) {
        this(new Random(seed));
    }

    public GameManager(Random random) {
        this.random = Objects.requireNonNull(random);
    }

    // ------------------------------------------------------------------
    // Створення партії
    // ------------------------------------------------------------------

    public void createNewParty(GameMode gameMode) {
        game = new Game();
        game.setGameMode(Objects.requireNonNull(gameMode, "gameMode cannot be null"));
        game.setGameState(GameState.WAITING_FOR_PLAYERS);
        deck = null;
        trumpSuit = null;
        phase = TurnPhase.ATTACK;
        actor = null;
        defenderTaking = false;
        pairLimit = 0;
        passed.clear();
        discardPile.clear();
        knownCards.clear();
        finishOrder.clear();
        events.clear();
    }

    public void startGame(List<Player> players) {
        startGame(players, GameMode.LOCAL_MULTIPLAYER);
    }

    /** Готує та починає нову партію: колода, роздача, перший атакуючий. */
    public void startGame(List<Player> players, GameMode mode) {
        if (players == null || players.size() < 2 || players.size() > 4) {
            throw new IllegalArgumentException("У грі має бути від 2 до 4 гравців");
        }
        validatePlayerObjects(players);
        for (Player player : players) {
            if (!player.getHand().isEmpty()) {
                throw new IllegalArgumentException("Перед новою грою руки гравців мають бути порожні");
            }
        }

        createNewParty(mode);
        game.setGameState(GameState.PREPARING);
        game.setPlayerList(players);
        for (Player player : players) {
            knownCards.put(player, new ArrayList<>());
        }

        deckInitialization();
        dealInitialCards();
        chooseFirstAttacker();

        game.setGameState(GameState.PLAYING);
        game.getRoundManager().startRound();
        beginRound();
    }

    /** Створює й перемішує колоду з 36 карт; козир — нижня карта. */
    public void deckInitialization() {
        deck = new Deck();
        if (deck.size() != 36) {
            throw new IllegalStateException("Колода має містити рівно 36 карт");
        }
        deck.shuffle(random);
        trumpSuit = deck.getTrumpSuit();
        if (trumpSuit == null) {
            throw new IllegalStateException("Не вдалося визначити козир");
        }
    }

    /** Роздає по шість карт, по одній по колу (так гарніше анімується). */
    private void dealInitialCards() {
        List<Player> players = game.getPlayerList();
        for (int i = 0; i < HAND_SIZE; i++) {
            for (Player player : players) {
                Card card = deck.drawCard();
                if (card == null) {
                    throw new IllegalStateException("Колода закінчилась під час роздачі");
                }
                player.getHand().addCard(card);
                events.add(GameEvent.deal(player, card));
            }
        }
        sortAllHands();
    }

    /** Першим ходить власник наймолодшого козиря; якщо козирів ні в кого немає — випадковий гравець. */
    private void chooseFirstAttacker() {
        List<Player> players = game.getPlayerList();
        Player first = null;
        int lowest = Integer.MAX_VALUE;
        for (Player player : players) {
            for (Card card : player.getHand().getCards()) {
                if (card.isTrump(trumpSuit) && card.getRank().getValue() < lowest) {
                    lowest = card.getRank().getValue();
                    first = player;
                }
            }
        }
        if (first == null) {
            first = players.get(random.nextInt(players.size()));
        }
        game.getTurnManager().initialize(players, first);
    }

    private void beginRound() {
        Player defender = game.getTurnManager().getDefender();
        pairLimit = Math.min(MAX_PAIRS, defender.getHand().size());
        phase = TurnPhase.ATTACK;
        defenderTaking = false;
        passed.clear();
        actor = game.getTurnManager().getAttacker();
        game.getTurnManager().setCurrentPlayer(actor, game.getPlayerList().indexOf(actor));
    }

    // ------------------------------------------------------------------
    // Дії гравців
    // ------------------------------------------------------------------

    /** Перша карта атаки або підкидання. */
    public void attack(Player player, Card card) {
        requirePlaying();
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(card, "card cannot be null");

        if (phase == TurnPhase.DEFEND) {
            throw new IllegalStateException("Зараз хід захисника");
        }
        if (player != actor) {
            throw new IllegalArgumentException("Зараз не ваша черга");
        }
        if (!player.getHand().contains(card)) {
            throw new IllegalArgumentException("Цієї карти немає в руці гравця");
        }
        if (phase == TurnPhase.THROW_IN && !canThrowCard(card)) {
            throw new IllegalArgumentException("Підкидати можна лише карти рангу, що вже лежить на столі");
        }
        if (phase == TurnPhase.ATTACK) {
            // Ліміт карт на столі = кількість карт у захисника на початку атаки (але не більше шести).
            pairLimit = Math.min(MAX_PAIRS, game.getTurnManager().getDefender().getHand().size());
        }

        player.getHand().removeCard(card);
        forgetKnown(player, card);
        game.addAttackPair(new AttackPair(card));
        events.add(GameEvent.attack(player, card));

        passed.clear();
        phase = TurnPhase.THROW_IN;
        pickThrower(player);
    }

    /** Побити карту attackCard картою defenseCard. */
    public void defend(Player player, Card attackCard, Card defenseCard) {
        requirePlaying();
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(attackCard, "attackCard cannot be null");
        Objects.requireNonNull(defenseCard, "defenseCard cannot be null");

        if (phase != TurnPhase.DEFEND) {
            throw new IllegalStateException("Зараз не фаза захисту");
        }
        if (player != game.getTurnManager().getDefender()) {
            throw new IllegalArgumentException("Захищатися може лише захисник");
        }
        if (!player.getHand().contains(defenseCard)) {
            throw new IllegalArgumentException("Цієї карти немає в руці захисника");
        }
        AttackPair pair = findAttackPair(attackCard);
        if (pair == null) {
            throw new IllegalArgumentException("Такої атакуючої карти немає на столі");
        }
        if (pair.isBeaten()) {
            throw new IllegalStateException("Ця карта вже побита");
        }
        if (!gameRules.canBeat(defenseCard, attackCard, trumpSuit)) {
            throw new IllegalArgumentException("Цією картою не можна побити обрану карту");
        }

        player.getHand().removeCard(defenseCard);
        forgetKnown(player, defenseCard);
        pair.setDefenseCard(defenseCard);
        events.add(GameEvent.defend(player, attackCard, defenseCard));

        if (allBeaten()) {
            phase = TurnPhase.THROW_IN;
            passed.clear();
            pickThrower(game.getTurnManager().getAttacker());
        }
    }

    /** Захисник вирішив узяти карти: атакуючі ще можуть підкинути. */
    public void take(Player player) {
        requirePlaying();
        Objects.requireNonNull(player, "player cannot be null");
        if (phase != TurnPhase.DEFEND) {
            throw new IllegalStateException("Взяти карти можна лише під час захисту");
        }
        if (player != game.getTurnManager().getDefender()) {
            throw new IllegalArgumentException("Карти бере лише захисник");
        }
        defenderTaking = true;
        events.add(GameEvent.takeDeclared(player));
        phase = TurnPhase.THROW_IN;
        passed.clear();
        pickThrower(game.getTurnManager().getAttacker());
    }

    /** «Бито» (усе побито) / «Готово» (віддати хід захисникові) / «Досить» (захисник бере). */
    public void pass(Player player) {
        requirePlaying();
        Objects.requireNonNull(player, "player cannot be null");
        if (phase != TurnPhase.THROW_IN || player != actor) {
            throw new IllegalStateException("Зараз ви не можете завершити підкидання");
        }
        passed.add(player);
        events.add(GameEvent.pass(player));
        pickThrower(null);
    }

    // ------------------------------------------------------------------
    // Керування чергою підкидання
    // ------------------------------------------------------------------

    /** Вибирає наступного, хто може підкинути; якщо таких немає — підкидання завершується. */
    private void pickThrower(Player preferred) {
        Player defender = game.getTurnManager().getDefender();
        List<Player> order = new ArrayList<>();
        if (preferred != null && preferred != defender) {
            order.add(preferred);
        }
        for (Player p : throwerOrder()) {
            if (!order.contains(p)) {
                order.add(p);
            }
        }
        for (Player p : order) {
            if (!passed.contains(p) && hasLegalThrow(p)) {
                actor = p;
                phase = TurnPhase.THROW_IN;
                game.getTurnManager().setCurrentPlayer(p, game.getPlayerList().indexOf(p));
                return;
            }
        }
        finishThrowing();
    }

    private void finishThrowing() {
        if (defenderTaking) {
            resolveTake();
        } else if (hasUnbeaten()) {
            phase = TurnPhase.DEFEND;
            actor = game.getTurnManager().getDefender();
            game.getTurnManager().setCurrentPlayer(actor, game.getPlayerList().indexOf(actor));
        } else {
            resolveDiscard();
        }
    }

    /** Атакуючий першим, далі решта (крім захисника) за годинниковою стрілкою. */
    private List<Player> throwerOrder() {
        List<Player> players = game.getPlayerList();
        Player attacker = game.getTurnManager().getAttacker();
        Player defender = game.getTurnManager().getDefender();
        List<Player> order = new ArrayList<>();
        int start = players.indexOf(attacker);
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get((start + i) % players.size());
            if (p != defender && p.hasCards()) {
                order.add(p);
            }
        }
        return order;
    }

    // ------------------------------------------------------------------
    // Завершення раунду
    // ------------------------------------------------------------------

    private void resolveTake() {
        Player defender = game.getTurnManager().getDefender();
        List<Card> taken = new ArrayList<>();
        for (AttackPair pair : game.getAttackPairs()) {
            taken.add(pair.getAttackCard());
            if (pair.getDefenseCard() != null) {
                taken.add(pair.getDefenseCard());
            }
        }
        for (Card card : taken) {
            defender.getHand().addCard(card);
        }
        knownCards.get(defender).addAll(taken);
        events.add(GameEvent.take(defender, taken));
        game.clearAttackPairs();
        completeRound(false);
    }

    private void resolveDiscard() {
        List<Card> discarded = new ArrayList<>();
        for (AttackPair pair : game.getAttackPairs()) {
            discarded.add(pair.getAttackCard());
            if (pair.getDefenseCard() != null) {
                discarded.add(pair.getDefenseCard());
            }
        }
        discardPile.addAll(discarded);
        events.add(GameEvent.discard(discarded));
        game.clearAttackPairs();
        completeRound(true);
    }

    private void completeRound(boolean defenderBeatEverything) {
        Player previousDefender = game.getTurnManager().getDefender();
        List<Player> players = game.getPlayerList();

        game.getRoundManager().endRound();
        drawCardsInRoundOrder();
        sortAllHands();
        updateFinishOrder();
        checkWinner();
        if (game.getGameState() == GameState.GAME_FINISHED) {
            events.add(GameEvent.gameOver());
            actor = null;
            return;
        }

        // Успішний захист — захисник атакує першим, інакше хід переходить далі за ним.
        Player nextAttacker = firstWithCards(previousDefender, defenderBeatEverything);
        Player nextDefender = nextAttacker == null ? null : firstWithCards(nextAttacker, false);
        if (nextAttacker == null || nextDefender == null || nextAttacker == nextDefender) {
            checkWinner();
            events.add(GameEvent.gameOver());
            actor = null;
            return;
        }

        game.getTurnManager().setRoles(nextAttacker, nextDefender, players);
        game.getRoundManager().nextRound();
        beginRound();
    }

    /** Добір до шести карт: атакуючий, інші атакуючі, захисник — останнім. */
    private void drawCardsInRoundOrder() {
        for (Player player : game.getRoundManager().getCardDrawOrder()) {
            while (player.getHand().size() < HAND_SIZE && !deck.isEmpty()) {
                Card card = deck.drawCard();
                if (card == null) {
                    break;
                }
                player.getHand().addCard(card);
                events.add(GameEvent.draw(player, card));
            }
        }
    }

    private void updateFinishOrder() {
        if (!deck.isEmpty()) {
            return;
        }
        for (Player player : game.getPlayerList()) {
            if (!player.hasCards() && !finishOrder.contains(player)) {
                finishOrder.add(player);
            }
        }
    }

    /** Завершує гру, якщо колода порожня й карти лишилися щонайбільше в одного гравця. */
    public void checkWinner() {
        if (game == null || deck == null || !deck.isEmpty()) {
            return;
        }
        if (game.getGameState() == GameState.GAME_FINISHED) {
            return;
        }
        List<Player> players = game.getPlayerList();
        List<Player> withCards = new ArrayList<>();
        for (Player player : players) {
            if (player.hasCards()) {
                withCards.add(player);
            }
        }
        if (withCards.size() <= 1) {
            Player last = withCards.isEmpty() ? null : withCards.get(0);
            GameResult result = new GameResult();
            result.complete(players, last);
            game.setGameResult(result);
            game.setGameState(GameState.GAME_FINISHED);
        }
    }

    private Player firstWithCards(Player from, boolean inclusive) {
        List<Player> players = game.getPlayerList();
        int index = players.indexOf(from);
        for (int step = inclusive ? 0 : 1; step <= players.size(); step++) {
            Player candidate = players.get((index + step) % players.size());
            if (candidate.hasCards()) {
                return candidate;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Запити про стан (для UI та ШІ)
    // ------------------------------------------------------------------

    /** Чи можна цю карту зараз підкинути на стіл (ранг уже є на столі, ліміт не вичерпано). */
    public boolean canThrowCard(Card card) {
        List<AttackPair> pairs = game.getAttackPairs();
        if (pairs.isEmpty() || pairs.size() >= pairLimit) {
            return false;
        }
        for (AttackPair pair : pairs) {
            if (pair.getAttackCard().getRank() == card.getRank()) {
                return true;
            }
            if (pair.getDefenseCard() != null && pair.getDefenseCard().getRank() == card.getRank()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasLegalThrow(Player player) {
        for (Card card : player.getHand().getCards()) {
            if (canThrowCard(card)) {
                return true;
            }
        }
        return false;
    }

    /** Карти, якими player може зараз атакувати/підкидати. */
    public List<Card> legalAttackCards(Player player) {
        if (game == null || game.getGameState() != GameState.PLAYING || player != actor) {
            return List.of();
        }
        if (phase == TurnPhase.ATTACK) {
            return player.getHand().getCards();
        }
        if (phase == TurnPhase.THROW_IN) {
            List<Card> result = new ArrayList<>();
            for (Card card : player.getHand().getCards()) {
                if (canThrowCard(card)) {
                    result.add(card);
                }
            }
            return result;
        }
        return List.of();
    }

    /** Карти з руки захисника, якими можна побити attackCard. */
    public List<Card> legalDefenseCards(Card attackCard) {
        if (game == null || phase != TurnPhase.DEFEND) {
            return List.of();
        }
        Player defender = game.getTurnManager().getDefender();
        List<Card> result = new ArrayList<>();
        for (Card card : defender.getHand().getCards()) {
            if (gameRules.canBeat(card, attackCard, trumpSuit)) {
                result.add(card);
            }
        }
        return result;
    }

    public boolean canBeat(Card defenseCard, Card attackCard) {
        return gameRules.canBeat(defenseCard, attackCard, trumpSuit);
    }

    public boolean canPass(Player player) {
        return game != null && game.getGameState() == GameState.PLAYING
                && phase == TurnPhase.THROW_IN && player == actor;
    }

    public boolean canTake(Player player) {
        return game != null && game.getGameState() == GameState.PLAYING
                && phase == TurnPhase.DEFEND && player == game.getTurnManager().getDefender();
    }

    public List<Card> getUnbeatenCards() {
        List<Card> result = new ArrayList<>();
        if (game != null) {
            for (AttackPair pair : game.getAttackPairs()) {
                if (!pair.isBeaten()) {
                    result.add(pair.getAttackCard());
                }
            }
        }
        return result;
    }

    private boolean hasUnbeaten() {
        for (AttackPair pair : game.getAttackPairs()) {
            if (!pair.isBeaten()) {
                return true;
            }
        }
        return false;
    }

    private boolean allBeaten() {
        if (game.getAttackPairs().isEmpty()) {
            return false;
        }
        return !hasUnbeaten();
    }

    private AttackPair findAttackPair(Card attackCard) {
        for (AttackPair pair : game.getAttackPairs()) {
            if (pair.getAttackCard().equals(attackCard)) {
                return pair;
            }
        }
        return null;
    }

    private void forgetKnown(Player player, Card card) {
        List<Card> known = knownCards.get(player);
        if (known != null) {
            known.remove(card);
        }
    }

    private void sortAllHands() {
        for (Player player : game.getPlayerList()) {
            player.getHand().sortCards(trumpSuit);
        }
    }

    /** Карти, які всі бачили в руці гравця (він забирав їх зі столу й ще не зіграв). */
    public List<Card> getKnownCards(Player player) {
        List<Card> known = knownCards.get(player);
        return known == null ? List.of() : Collections.unmodifiableList(known);
    }

    public List<Card> getDiscardPile() {
        return Collections.unmodifiableList(discardPile);
    }

    public List<Player> getFinishOrder() {
        return Collections.unmodifiableList(finishOrder);
    }

    /** Повертає накопичені події та очищає чергу. */
    public List<GameEvent> drainEvents() {
        List<GameEvent> copy = new ArrayList<>(events);
        events.clear();
        return copy;
    }

    public TurnPhase getPhase() {
        return phase;
    }

    /** Гравець, від якого рушій зараз чекає дію (null, якщо гру завершено). */
    public Player getActor() {
        return game != null && game.getGameState() == GameState.PLAYING ? actor : null;
    }

    public Player getAttacker() {
        return game == null ? null : game.getTurnManager().getAttacker();
    }

    public Player getDefender() {
        return game == null ? null : game.getTurnManager().getDefender();
    }

    public boolean isDefenderTaking() {
        return defenderTaking;
    }

    public int getPairLimit() {
        return pairLimit;
    }

    public Suit getCurrentTrump() {
        return trumpSuit;
    }

    public Card getTrumpCard() {
        return deck == null ? null : deck.getTrumpCard();
    }

    public GameState getGameState() {
        return game == null ? GameState.WAITING_FOR_PLAYERS : game.getGameState();
    }

    public boolean isGameOver() {
        return getGameState() == GameState.GAME_FINISHED;
    }

    public GameResult getResult() {
        return game == null ? null : game.getGameResult();
    }

    public Game getGame() {
        return game;
    }

    public Deck getDeck() {
        return deck;
    }

    public List<AttackPair> getTablePairs() {
        return game == null ? List.of() : game.getAttackPairs();
    }

    /** Усі 36 карт на місці: руки + колода + стіл + відбій (для тестів). */
    public int countAllCards() {
        int total = deck == null ? 0 : deck.size();
        total += discardPile.size();
        if (game != null) {
            for (Player p : game.getPlayerList()) {
                total += p.getHand().size();
            }
            for (AttackPair pair : game.getAttackPairs()) {
                total += pair.isBeaten() ? 2 : 1;
            }
        }
        return total;
    }

    // ------------------------------------------------------------------
    // Перевірки
    // ------------------------------------------------------------------

    private void requirePlaying() {
        if (game == null || game.getGameState() != GameState.PLAYING) {
            throw new IllegalStateException("Гра зараз не в стані PLAYING");
        }
        if (deck == null || trumpSuit == null) {
            throw new IllegalStateException("Колода та козир не ініціалізовані");
        }
    }

    private void validatePlayerObjects(List<Player> players) {
        for (Player player : players) {
            if (player == null) {
                throw new IllegalArgumentException("Список гравців не може містити null");
            }
        }
        for (int i = 0; i < players.size(); i++) {
            for (int j = i + 1; j < players.size(); j++) {
                if (players.get(i) == players.get(j)) {
                    throw new IllegalArgumentException("Один і той самий гравець доданий двічі");
                }
            }
        }
    }
}
