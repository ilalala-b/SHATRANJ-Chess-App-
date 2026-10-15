package org.example;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class ChessController {

    public enum Mode { VS_BOT, VS_PLAYER, BEGINNER }
    public enum BoardTheme { NEUTRAL, BROWN, GREEN }

    private static class NotationEntry {
        int moveNumber;
        boolean isWhite;
        String text;
        NotationEntry(int n, boolean w, String t) {
            moveNumber = n; isWhite = w; text = t;
        }
    }

    private static class SavedGame {
        Board board;
        List<Board.MoveInfo> history;
        List<NotationEntry> notation;
        int whiteTime;
        int blackTime;
        int startingTime;
        Mode mode;
        Piece.Color humanColor;
        boolean boardFlipped;
        boolean saved;
    }

    @FXML
    private StackPane rootStack;

    @FXML
    private GridPane boardGrid;

    @FXML
    private Label statusLabel;

    @FXML
    private Label blackClockLabel;

    @FXML
    private Label whiteClockLabel;

    @FXML
    private Button undoButton;

    @FXML
    private VBox movesList;

    @FXML
    private Button hamburgerButton;

    @FXML
    private Button playAsWhiteButton;

    @FXML
    private Button playAsBlackButton;

    private Board board = new Board();
    private final Engine engine = new Engine();
    private final StackPane[][] cells = new StackPane[8][8];
    private final Text[][] pieceTexts = new Text[8][8];
    private final Rectangle[][] highlights = new Rectangle[8][8];
    private final Circle[][] dots = new Circle[8][8];

    private int selectedRow = -1;
    private int selectedCol = -1;
    private List<Move> legalMoves;
    private boolean botThinking = false;
    private boolean gameOver = false;
    private boolean paused = false;

    private Piece.Color humanColor = Piece.Color.WHITE;
    private boolean boardFlipped = false;

    private Mode mode = Mode.VS_BOT;
    private BoardTheme theme = BoardTheme.NEUTRAL;

    private Timeline clock;
    private boolean clockRunning = false;
    private int whiteTime = 180;
    private int blackTime = 180;
    private int startingTime = 180;

    private StackPane drawerOverlay;
    private boolean showingStartScreen = true;

    private final List<Board.MoveInfo> history = new ArrayList<>();
    private final List<NotationEntry> historyNotation = new ArrayList<>();

    private final SavedGame savedGame = new SavedGame();

    private boolean modeExpanded = false;
    private boolean gameExpanded = false;
    private boolean themeExpanded = false;
    private boolean timeExpanded = false;

    private String lightColor() {
        switch (theme) {
            case BROWN: return "#F0D9B5";
            case GREEN: return "#EEEED2";
            default:    return "#EBECF0";
        }
    }

    private String darkColor() {
        switch (theme) {
            case BROWN: return "#B58863";
            case GREEN: return "#769656";
            default:    return "#A8ACB5";
        }
    }

    private static final String SELECTED_COLOR = "#F7D774";
    private static final String PIECE_FONT = "Segoe UI Symbol";

    private static final String SIDE_BTN_BASE = "-fx-background-color: #3a3a3a;" +
            "-fx-text-fill: #ffffff;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 12;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;";

    private static final String SIDE_BTN_ACTIVE = "-fx-background-color: #81b64c;" +
            "-fx-text-fill: #ffffff;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 12;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;";

    private static final String DANGER_BTN = "-fx-background-color: #c05242;" +
            "-fx-text-fill: #ffffff;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 12;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;";

    private static final String WARN_BTN = "-fx-background-color: #d17c4a;" +
            "-fx-text-fill: #ffffff;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 12;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;";

    private static final String TIME_BTN_BASE = "-fx-background-color: #3a3a3a;" +
            "-fx-text-fill: #ffffff;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 8 0 8 0;" +
            "-fx-background-radius: 6;" +
            "-fx-cursor: hand;";

    private static final String TIME_BTN_ACTIVE = "-fx-background-color: #81b64c;" +
            "-fx-text-fill: #ffffff;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 8 0 8 0;" +
            "-fx-background-radius: 6;" +
            "-fx-cursor: hand;";

    @FXML
    public void initialize() {
        buildBoardGrid();
        legalMoves = MoveGenerator.generateLegalMoves(board);
        redrawBoard();
        startClock();

        undoButton.setOnAction(e -> undoLastTwoMoves());
        hamburgerButton.setOnAction(e -> openDrawer());
        playAsWhiteButton.setOnAction(e -> startNewGame(Piece.Color.WHITE));
        playAsBlackButton.setOnAction(e -> startNewGame(Piece.Color.BLACK));

        styleHamburgerButton(hamburgerButton);
        styleSideButton(playAsWhiteButton);
        styleSideButton(playAsBlackButton);

        updateStatus();
        updateClocks();
        updateUndoButton();
        updateMovesList();
        updateSideButtons();

        showStartScreen();
    }

    private void showStartScreen() {
        if (drawerOverlay != null) return;

        showingStartScreen = true;
        paused = true;

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: #1e1e1e;");
        overlay.setPickOnBounds(true);

        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40, 60, 40, 60));
        card.setMaxWidth(420);

        Label title = new Label("CHESS");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 52));
        title.setStyle("-fx-text-fill: #f5c542;");

        Label sub = new Label("A class project by you");
        sub.setStyle("-fx-text-fill: #9a9a9a; -fx-font-size: 13px;");

        Button startBtn = new Button("Start Game");
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setPrefHeight(50);
        String primaryBase = "-fx-background-color: #81b64c;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 14 30 14 30;" +
                "-fx-background-radius: 10;" +
                "-fx-cursor: hand;";
        String primaryHover = "-fx-background-color: #95c959;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 14 30 14 30;" +
                "-fx-background-radius: 10;" +
                "-fx-cursor: hand;";
        startBtn.setStyle(primaryBase);
        startBtn.setOnMouseEntered(e -> startBtn.setStyle(primaryHover));
        startBtn.setOnMouseExited(e -> startBtn.setStyle(primaryBase));
        startBtn.setOnAction(e -> {
            rootStack.getChildren().remove(overlay);
            showingStartScreen = false;
            paused = false;
            startNewGame(humanColor);
        });

        Button continueBtn = new Button("Continue");
        continueBtn.setMaxWidth(Double.MAX_VALUE);
        continueBtn.setPrefHeight(50);
        String secondaryBase = "-fx-background-color: #3a3a3a;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 14 30 14 30;" +
                "-fx-background-radius: 10;" +
                "-fx-cursor: hand;";
        String secondaryHover = "-fx-background-color: #4a4a4a;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 14 30 14 30;" +
                "-fx-background-radius: 10;" +
                "-fx-cursor: hand;";
        String secondaryDisabled = "-fx-background-color: #2a2a2a;" +
                "-fx-text-fill: #6a6a6a;" +
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 14 30 14 30;" +
                "-fx-background-radius: 10;";
        continueBtn.setStyle(savedGame.saved ? secondaryBase : secondaryDisabled);
        continueBtn.setDisable(!savedGame.saved);
        continueBtn.setOnMouseEntered(e -> {
            if (savedGame.saved) continueBtn.setStyle(secondaryHover);
        });
        continueBtn.setOnMouseExited(e -> {
            if (savedGame.saved) continueBtn.setStyle(secondaryBase);
        });
        continueBtn.setOnAction(e -> {
            if (!savedGame.saved) return;
            rootStack.getChildren().remove(overlay);
            showingStartScreen = false;
            paused = false;
            loadSavedGame();
        });

        Button quitBtn = new Button("Quit");
        quitBtn.setMaxWidth(Double.MAX_VALUE);
        quitBtn.setPrefHeight(50);
        String dangerBase = "-fx-background-color: #c05242;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 14 30 14 30;" +
                "-fx-background-radius: 10;" +
                "-fx-cursor: hand;";
        String dangerHover = "-fx-background-color: #d16252;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 14 30 14 30;" +
                "-fx-background-radius: 10;" +
                "-fx-cursor: hand;";
        quitBtn.setStyle(dangerBase);
        quitBtn.setOnMouseEntered(e -> quitBtn.setStyle(dangerHover));
        quitBtn.setOnMouseExited(e -> quitBtn.setStyle(dangerBase));
        quitBtn.setOnAction(e -> Platform.exit());

        card.getChildren().addAll(title, sub, startBtn, continueBtn, quitBtn);

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web("#000000", 0.7));
        shadow.setRadius(30);
        shadow.setOffsetY(8);
        card.setEffect(shadow);

        StackPane.setAlignment(card, Pos.CENTER);
        overlay.getChildren().add(card);
        rootStack.getChildren().add(overlay);
    }

    private void saveCurrentGame() {
        savedGame.board = copyBoard(board);
        savedGame.history = new ArrayList<>(history);
        savedGame.notation = new ArrayList<>(historyNotation);
        savedGame.whiteTime = whiteTime;
        savedGame.blackTime = blackTime;
        savedGame.startingTime = startingTime;
        savedGame.mode = mode;
        savedGame.humanColor = humanColor;
        savedGame.boardFlipped = boardFlipped;
        savedGame.saved = true;
    }

    private void clearSavedGame() {
        savedGame.saved = false;
        savedGame.board = null;
        savedGame.history = null;
        savedGame.notation = null;
    }

    private Board copyBoard(Board source) {
        Board copy = new Board();
        copy.loadFen(boardToFen(source));
        return copy;
    }

    private String boardToFen(Board b) {
        StringBuilder fen = new StringBuilder();
        for (int r = 0; r < 8; r++) {
            int empty = 0;
            for (int c = 0; c < 8; c++) {
                Piece p = b.getPiece(r, c);
                if (p == null) {
                    empty++;
                } else {
                    if (empty > 0) {
                        fen.append(empty);
                        empty = 0;
                    }
                    char ch;
                    switch (p.getType()) {
                        case PAWN:   ch = 'p'; break;
                        case KNIGHT: ch = 'n'; break;
                        case BISHOP: ch = 'b'; break;
                        case ROOK:   ch = 'r'; break;
                        case QUEEN:  ch = 'q'; break;
                        case KING:   ch = 'k'; break;
                        default:     ch = '?'; break;
                    }
                    if (p.getColor() == Piece.Color.WHITE) ch = Character.toUpperCase(ch);
                    fen.append(ch);
                }
            }
            if (empty > 0) fen.append(empty);
            if (r < 7) fen.append('/');
        }
        fen.append(' ');
        fen.append(b.getSideToMove() == Piece.Color.WHITE ? 'w' : 'b');
        fen.append(" - - 0 1");
        return fen.toString();
    }

    private void loadSavedGame() {
        board = copyBoard(savedGame.board);
        history.clear();
        if (savedGame.history != null) history.addAll(savedGame.history);
        historyNotation.clear();
        if (savedGame.notation != null) historyNotation.addAll(savedGame.notation);
        whiteTime = savedGame.whiteTime;
        blackTime = savedGame.blackTime;
        startingTime = savedGame.startingTime;
        mode = savedGame.mode;
        humanColor = savedGame.humanColor;
        boardFlipped = savedGame.boardFlipped;
        clockRunning = false;
        botThinking = false;
        gameOver = false;
        paused = false;

        redrawBoard();
        updateStatus();
        updateClocks();
        updateUndoButton();
        updateMovesList();
        updateSideButtons();
    }

    private void updateSideButtons() {
        if (playAsWhiteButton == null || playAsBlackButton == null) return;
        boolean pvp = (mode == Mode.VS_PLAYER);
        playAsWhiteButton.setDisable(pvp);
        playAsBlackButton.setDisable(pvp);
        String style = pvp
                ? "-fx-background-color: #2a2a2a;" +
                "-fx-text-fill: #6a6a6a;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10;" +
                "-fx-background-radius: 8;"
                : "-fx-background-color: #3a3a3a;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;";
        playAsWhiteButton.setStyle(style);
        playAsBlackButton.setStyle(style);
    }

    private void updateUndoButton() {
        if (undoButton == null) return;
        boolean enabled = (mode == Mode.BEGINNER && !botThinking && !gameOver && !paused && history.size() >= 2);
        undoButton.setDisable(!enabled);
        if (enabled) {
            undoButton.setStyle(WARN_BTN);
        } else {
            undoButton.setStyle("-fx-background-color: #3a3a3a;" +
                    "-fx-text-fill: #6a6a6a;" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 12;" +
                    "-fx-background-radius: 10;");
        }
    }

    private void updateMovesList() {
        if (movesList == null) return;
        movesList.getChildren().clear();
        for (int i = 0; i < historyNotation.size(); i += 2) {
            NotationEntry whiteMove = historyNotation.get(i);
            String line = whiteMove.moveNumber + ". " + whiteMove.text;
            if (i + 1 < historyNotation.size()) {
                line += "   " + historyNotation.get(i + 1).text;
            }
            Label l = new Label(line);
            l.setStyle("-fx-text-fill: #e0e0e0;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-family: 'Consolas', monospace;");
            movesList.getChildren().add(l);
        }
    }

    private String moveToNotation(Move move, Piece movingPiece, Piece capturedPiece) {
        StringBuilder sb = new StringBuilder();

        if (move.isCastle()) {
            sb.append(move.getFlag() == Move.Flag.CASTLE_KINGSIDE ? "O-O" : "O-O-O");
        } else {
            char file = (char)('a' + move.getToCol());
            char rank = (char)('1' + (7 - move.getToRow()));
            String dest = "" + file + rank;

            switch (movingPiece.getType()) {
                case KING:   sb.append("K"); break;
                case QUEEN:  sb.append("Q"); break;
                case ROOK:   sb.append("R"); break;
                case BISHOP: sb.append("B"); break;
                case KNIGHT: sb.append("N"); break;
                case PAWN:   break;
            }

            if (movingPiece.getType() == Piece.Type.PAWN && capturedPiece != null) {
                char fromFile = (char)('a' + move.getFromCol());
                sb.append(fromFile);
            }

            if (capturedPiece != null) sb.append("x");

            sb.append(dest);

            if (move.isPromotion()) {
                switch (move.getFlag()) {
                    case PROMOTION_QUEEN:  sb.append("=Q"); break;
                    case PROMOTION_ROOK:   sb.append("=R"); break;
                    case PROMOTION_BISHOP: sb.append("=B"); break;
                    case PROMOTION_KNIGHT: sb.append("=N"); break;
                    default: break;
                }
            }
        }
        return sb.toString();
    }

    private void openDrawer() {
        if (drawerOverlay != null) return;
        if (showingStartScreen) return;

        drawerOverlay = new StackPane();
        drawerOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");
        drawerOverlay.setOnMouseClicked(e -> {
            if (e.getTarget() == drawerOverlay) {
                closeDrawer();
            }
        });

        VBox drawer = new VBox(10);
        drawer.setAlignment(Pos.TOP_LEFT);
        drawer.setPadding(new Insets(24));
        drawer.setPrefWidth(320);
        drawer.setMaxWidth(320);
        drawer.setMaxHeight(Double.MAX_VALUE);
        drawer.setStyle("-fx-background-color: #262421;");

        StackPane.setAlignment(drawer, Pos.CENTER_LEFT);

        VBox modeSection = collapsibleSection("GAME MODE", modeExpanded, () -> modeExpanded = !modeExpanded);
        if (modeExpanded) {
            Button botBtn = drawerButton("Player vs Bot", mode == Mode.VS_BOT);
            botBtn.setOnAction(e -> { mode = Mode.VS_BOT; closeDrawer(); resetGame(); });
            Button pvpBtn = drawerButton("Player vs Player", mode == Mode.VS_PLAYER);
            pvpBtn.setOnAction(e -> { mode = Mode.VS_PLAYER; closeDrawer(); resetGame(); });
            Button beginnerBtn = drawerButton("Beginner", mode == Mode.BEGINNER);
            beginnerBtn.setOnAction(e -> { mode = Mode.BEGINNER; closeDrawer(); resetGame(); });
            modeSection.getChildren().addAll(botBtn, pvpBtn, beginnerBtn);
        }

        VBox gameSection = collapsibleSection("GAME", gameExpanded, () -> gameExpanded = !gameExpanded);
        if (gameExpanded) {
            Button newGameBtn = drawerButton("New Game", false);
            newGameBtn.setOnAction(e -> { closeDrawer(); resetGame(); });
            Button pauseBtn = drawerButton(paused ? "Resume" : "Pause", false);
            pauseBtn.setOnAction(e -> { paused = !paused; closeDrawer(); updateStatus(); updateUndoButton(); });
            Button quitBtn = new Button("Quit to Menu");
            quitBtn.setMaxWidth(Double.MAX_VALUE);
            quitBtn.setStyle(DANGER_BTN);
            quitBtn.setOnAction(e -> {
                closeDrawer();
                saveCurrentGame();
                paused = true;
                showStartScreen();
            });
            gameSection.getChildren().addAll(newGameBtn, pauseBtn, quitBtn);
        }

        VBox timeSection = collapsibleSection("TIME CONTROL", timeExpanded, () -> timeExpanded = !timeExpanded);
        if (timeExpanded) {
            HBox row1 = new HBox(6);
            Button bulletBtn = timeButton("Bullet", 60);
            Button blitzBtn = timeButton("Blitz", 180);
            HBox.setHgrow(bulletBtn, Priority.ALWAYS);
            HBox.setHgrow(blitzBtn, Priority.ALWAYS);
            row1.getChildren().addAll(bulletBtn, blitzBtn);

            HBox row2 = new HBox(6);
            Button rapidBtn = timeButton("Rapid", 600);
            Button classicBtn = timeButton("Classic", 1800);
            HBox.setHgrow(rapidBtn, Priority.ALWAYS);
            HBox.setHgrow(classicBtn, Priority.ALWAYS);
            row2.getChildren().addAll(rapidBtn, classicBtn);

            timeSection.getChildren().addAll(row1, row2);
        }

        VBox themeSection = collapsibleSection("THEME", themeExpanded, () -> themeExpanded = !themeExpanded);
        if (themeExpanded) {
            HBox themeRow = new HBox(6);
            Button neutralBtn = themeButton("Neutral", BoardTheme.NEUTRAL);
            Button brownBtn = themeButton("Brown", BoardTheme.BROWN);
            Button greenBtn = themeButton("Green", BoardTheme.GREEN);
            HBox.setHgrow(neutralBtn, Priority.ALWAYS);
            HBox.setHgrow(brownBtn, Priority.ALWAYS);
            HBox.setHgrow(greenBtn, Priority.ALWAYS);
            themeRow.getChildren().addAll(neutralBtn, brownBtn, greenBtn);
            themeSection.getChildren().add(themeRow);
        }

        Button closeBtn = new Button("Close");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setStyle(SIDE_BTN_BASE);
        closeBtn.setOnAction(e -> closeDrawer());

        drawer.getChildren().addAll(modeSection, gameSection, timeSection, themeSection, closeBtn);

        drawerOverlay.getChildren().add(drawer);
        rootStack.getChildren().add(drawerOverlay);

        drawerOverlay.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(200), drawerOverlay);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private VBox collapsibleSection(String title, boolean expanded, Runnable onToggle) {
        VBox section = new VBox(6);
        Button header = new Button((expanded ? "▼  " : "▶  ") + title);
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #1e1c1a;" +
                "-fx-text-fill: #c8c6c3;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 12 10 12;" +
                "-fx-background-radius: 6;" +
                "-fx-cursor: hand;");
        header.setOnAction(e -> {
            onToggle.run();
            closeDrawer();
            openDrawer();
        });
        section.getChildren().add(header);
        return section;
    }

    private Button drawerButton(String text, boolean active) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle(active ? SIDE_BTN_ACTIVE : SIDE_BTN_BASE);
        return b;
    }

    private Button themeButton(String text, BoardTheme t) {
        Button b = new Button(text);
        boolean active = (theme == t);
        b.setStyle(active ? TIME_BTN_ACTIVE : TIME_BTN_BASE);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> { theme = t; closeDrawer(); redrawBoard(); });
        return b;
    }

    private Button timeButton(String text, int seconds) {
        Button b = new Button(text);
        boolean active = (startingTime == seconds);
        b.setStyle(active ? TIME_BTN_ACTIVE : TIME_BTN_BASE);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> {
            startingTime = seconds;
            closeDrawer();
            resetGame();
        });
        return b;
    }

    private void closeDrawer() {
        if (drawerOverlay == null) return;
        rootStack.getChildren().remove(drawerOverlay);
        drawerOverlay = null;
    }

    private void undoLastTwoMoves() {
        if (mode != Mode.BEGINNER) return;
        if (botThinking) return;
        if (gameOver) return;
        if (paused) return;
        if (history.size() < 2) return;

        int count = 0;
        while (count < 2 && !history.isEmpty()) {
            Board.MoveInfo info = history.remove(history.size() - 1);
            board.unmakeMove(info);
            if (!historyNotation.isEmpty()) {
                historyNotation.remove(historyNotation.size() - 1);
            }
            count++;
        }

        selectedRow = -1;
        selectedCol = -1;
        redrawBoard();
        updateStatus();
        updateClocks();
        updateUndoButton();
        updateMovesList();
    }

    private void startNewGame(Piece.Color human) {
        humanColor = human;

        if (mode == Mode.VS_PLAYER) {
            boardFlipped = false;
        } else {
            boardFlipped = (human == Piece.Color.BLACK);
        }

        board = new Board();
        history.clear();
        historyNotation.clear();
        selectedRow = -1;
        selectedCol = -1;
        botThinking = false;
        gameOver = false;
        paused = false;
        clockRunning = false;
        whiteTime = startingTime;
        blackTime = startingTime;

        redrawBoard();
        updateStatus();
        updateClocks();
        updateUndoButton();
        updateMovesList();
        updateSideButtons();

        if (mode != Mode.VS_PLAYER && board.getSideToMove() != humanColor) {
            startBotTurn();
        }
    }

    private void startClock() {
        clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickClock()));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    private void tickClock() {
        if (gameOver) return;
        if (paused) return;
        if (showingStartScreen) return;
        if (!clockRunning) return;

        if (board.getSideToMove() == Piece.Color.WHITE) {
            whiteTime--;
            if (whiteTime <= 0) {
                whiteTime = 0;
                updateClocks();
                endOnTime(Piece.Color.BLACK);
                return;
            }
        } else {
            blackTime--;
            if (blackTime <= 0) {
                blackTime = 0;
                updateClocks();
                endOnTime(Piece.Color.WHITE);
                return;
            }
        }
        updateClocks();
    }

    private void endOnTime(Piece.Color winner) {
        gameOver = true;
        botThinking = false;
        clockRunning = false;
        clearSavedGame();
        String winnerName = (winner == Piece.Color.WHITE) ? "White" : "Black";
        statusLabel.setText("Time out! " + winnerName + " wins.");
        showGameOverCard("TIME OUT", winnerName + " wins on time!");
    }

    private void updateClocks() {
        if (whiteClockLabel != null) {
            whiteClockLabel.setText(formatTime(whiteTime));
            whiteClockLabel.setStyle("-fx-text-fill: "
                    + (board.getSideToMove() == Piece.Color.WHITE && !gameOver ? "#81b64c" : "#ffffff")
                    + "; -fx-font-size: 34px; -fx-font-weight: bold;");
        }
        if (blackClockLabel != null) {
            blackClockLabel.setText(formatTime(blackTime));
            blackClockLabel.setStyle("-fx-text-fill: "
                    + (board.getSideToMove() == Piece.Color.BLACK && !gameOver ? "#81b64c" : "#ffffff")
                    + "; -fx-font-size: 34px; -fx-font-weight: bold;");
        }
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    private void buildBoardGrid() {
        double cellSize = 80.0;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                StackPane cell = new StackPane();

                Rectangle highlight = new Rectangle(cellSize, cellSize);
                highlight.setFill(Color.web((r + c) % 2 == 0 ? lightColor() : darkColor()));
                highlights[r][c] = highlight;

                Circle dot = new Circle(8);
                dot.setFill(Color.web("#3a3a3a", 0.55));
                dot.setVisible(false);
                dot.setMouseTransparent(true);
                dots[r][c] = dot;

                Text pieceText = new Text("");
                pieceText.setFont(Font.font(PIECE_FONT, 56));
                pieceTexts[r][c] = pieceText;

                cell.getChildren().addAll(highlight, dot, pieceText);

                final int row = r;
                final int col = c;
                cell.setOnMouseClicked(event -> onCellClicked(row, col));

                cells[r][c] = cell;
                boardGrid.add(cell, c, r);
            }
        }
    }

    private int displayRow(int boardRow) {
        return boardFlipped ? 7 - boardRow : boardRow;
    }

    private int displayCol(int boardCol) {
        return boardFlipped ? 7 - boardCol : boardCol;
    }

    private void onCellClicked(int displayRow, int displayCol) {
        if (botThinking || gameOver || paused) return;
        if (showingStartScreen) return;

        if (mode != Mode.VS_PLAYER) {
            if (board.getSideToMove() != humanColor) return;
        }

        int row = boardFlipped ? 7 - displayRow : displayRow;
        int col = boardFlipped ? 7 - displayCol : displayCol;

        Piece clicked = board.getPiece(row, col);
        Piece.Color turn = board.getSideToMove();

        if (selectedRow == -1) {
            if (clicked != null && clicked.getColor() == turn) {
                selectedRow = row;
                selectedCol = col;
                redrawBoard();
            }
            return;
        }

        Move chosen = findMove(selectedRow, selectedCol, row, col);
        if (chosen != null) {
            applyMoveAndRecord(chosen, turn);
            selectedRow = -1;
            selectedCol = -1;
            clockRunning = true;
            redrawBoard();
            updateStatus();
            updateClocks();
            updateUndoButton();
            updateMovesList();

            if (isGameOver()) {
                annotateLastMoveWithCheckOrMate();
                updateMovesList();
                return;
            }

            if (mode != Mode.VS_PLAYER) {
                startBotTurn();
            }
        } else {
            if (clicked != null && clicked.getColor() == turn) {
                selectedRow = row;
                selectedCol = col;
            } else {
                selectedRow = -1;
                selectedCol = -1;
            }
            redrawBoard();
        }
    }

    private void applyMoveAndRecord(Move move, Piece.Color mover) {
        Piece movingPiece = board.getPiece(move.getFromRow(), move.getFromCol());
        Piece capturedPiece = board.getPiece(move.getToRow(), move.getToCol());
        if (move.getFlag() == Move.Flag.EN_PASSANT) {
            capturedPiece = board.getPiece(move.getFromRow(), move.getToCol());
        }

        String notation = moveToNotation(move, movingPiece, capturedPiece);

        Board.MoveInfo info = board.makeMove(move);
        history.add(info);

        int moveNum = (history.size() + 1) / 2;
        boolean isWhite = (mover == Piece.Color.WHITE);
        historyNotation.add(new NotationEntry(moveNum, isWhite, notation));
    }

    private void annotateLastMoveWithCheckOrMate() {
        if (historyNotation.isEmpty()) return;
        NotationEntry last = historyNotation.get(historyNotation.size() - 1);
        if (board.isInCheck(board.getSideToMove())) {
            boolean mate = MoveGenerator.generateLegalMoves(board).isEmpty();
            last.text = last.text + (mate ? "#" : "+");
        }
    }

    private Move findMove(int fromRow, int fromCol, int toRow, int toCol) {
        List<Move> current = MoveGenerator.generateLegalMoves(board);
        Move fallback = null;
        for (Move m : current) {
            if (m.getFromRow() == fromRow && m.getFromCol() == fromCol
                    && m.getToRow() == toRow && m.getToCol() == toCol) {
                if (m.getFlag() == Move.Flag.PROMOTION_QUEEN) return m;
                if (fallback == null) fallback = m;
            }
        }
        return fallback;
    }

    private void startBotTurn() {
        botThinking = true;
        updateStatus();
        updateUndoButton();

        Task<Move> botTask = new Task<>() {
            @Override
            protected Move call() throws Exception {
                Thread.sleep(2200);
                return engine.findBestMove(board, 3);
            }
        };

        botTask.setOnSucceeded(event -> {
            Move botMove = botTask.getValue();
            if (botMove != null) {
                Piece.Color mover = board.getSideToMove();
                applyMoveAndRecord(botMove, mover);
                clockRunning = true;
            }
            botThinking = false;
            redrawBoard();
            updateStatus();
            updateClocks();
            updateUndoButton();

            if (!gameOver) {
                boolean over = isGameOver();
                if (over) annotateLastMoveWithCheckOrMate();
            }
            updateMovesList();
        });

        botTask.setOnFailed(event -> {
            botThinking = false;
            redrawBoard();
            updateStatus();
            updateUndoButton();
        });

        Thread thread = new Thread(botTask);
        thread.setDaemon(true);
        thread.start();
    }

    private boolean isGameOver() {
        List<Move> moves = MoveGenerator.generateLegalMoves(board);
        if (moves.isEmpty()) {
            gameOver = true;
            botThinking = false;
            clockRunning = false;
            updateClocks();
            updateUndoButton();
            clearSavedGame();

            if (board.isInCheck(board.getSideToMove())) {
                Piece.Color winner = (board.getSideToMove() == Piece.Color.WHITE)
                        ? Piece.Color.BLACK : Piece.Color.WHITE;
                String winnerName = (winner == Piece.Color.WHITE) ? "White" : "Black";
                statusLabel.setText("Checkmate! " + winnerName + " wins.");
                showGameOverCard("CHECKMATE", winnerName + " wins!");
            } else {
                statusLabel.setText("Stalemate — draw.");
                showGameOverCard("STALEMATE", "It's a draw.");
            }
            return true;
        }
        return false;
    }

    private void showGameOverCard(String title, String message) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.65);");
        overlay.setPickOnBounds(true);

        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(32, 48, 32, 48));
        card.setPrefSize(360, 340);
        card.setMinSize(360, 340);
        card.setMaxSize(360, 340);
        card.setStyle(
                "-fx-background-color: #312e2b;" +
                        "-fx-background-radius: 14;"
        );

        DropShadow cardShadow = new DropShadow();
        cardShadow.setColor(Color.web("#000000", 0.7));
        cardShadow.setRadius(30);
        cardShadow.setOffsetY(8);
        card.setEffect(cardShadow);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        titleLabel.setStyle("-fx-text-fill: #f5c542;");

        Label messageLabel = new Label(message);
        messageLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 18));
        messageLabel.setStyle("-fx-text-fill: #e8e8e8;");

        Label timeLabel = new Label("Time used — White: " + formatTime(startingTime - whiteTime)
                + "   Black: " + formatTime(startingTime - blackTime));
        timeLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        timeLabel.setStyle("-fx-text-fill: #9a9a9a;");

        Button newGameBtn = new Button("New Game");
        newGameBtn.setMaxWidth(Double.MAX_VALUE);
        stylePrimaryButton(newGameBtn);
        newGameBtn.setOnAction(e -> {
            rootStack.getChildren().remove(overlay);
            resetGame();
        });

        Button viewBoardBtn = new Button("View Board");
        viewBoardBtn.setMaxWidth(Double.MAX_VALUE);
        styleSecondaryButton(viewBoardBtn);
        viewBoardBtn.setOnAction(e -> rootStack.getChildren().remove(overlay));

        Button quitBtn = new Button("Quit to Menu");
        quitBtn.setMaxWidth(Double.MAX_VALUE);
        styleDangerButton(quitBtn);
        quitBtn.setOnAction(e -> {
            rootStack.getChildren().remove(overlay);
            paused = true;
            showStartScreen();
        });

        card.getChildren().addAll(titleLabel, messageLabel, timeLabel, newGameBtn, viewBoardBtn, quitBtn);

        StackPane.setAlignment(card, Pos.CENTER);
        overlay.getChildren().add(card);
        overlay.setOpacity(0);

        rootStack.getChildren().add(overlay);

        FadeTransition fade = new FadeTransition(Duration.millis(250), overlay);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void stylePrimaryButton(Button button) {
        String base = "-fx-background-color: #81b64c;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 12 20 12 20;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;";
        String hover = "-fx-background-color: #95c959;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 12 20 12 20;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;";
        button.setStyle(base);
        button.setOnMouseEntered(e -> button.setStyle(hover));
        button.setOnMouseExited(e -> button.setStyle(base));
    }

    private void styleSecondaryButton(Button button) {
        String base = "-fx-background-color: #3a3a3a;" +
                "-fx-text-fill: #e0e0e0;" +
                "-fx-font-size: 15px;" +
                "-fx-padding: 12 20 12 20;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;";
        String hover = "-fx-background-color: #4a4a4a;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-size: 15px;" +
                "-fx-padding: 12 20 12 20;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;";
        button.setStyle(base);
        button.setOnMouseEntered(e -> button.setStyle(hover));
        button.setOnMouseExited(e -> button.setStyle(base));
    }

    private void styleDangerButton(Button button) {
        String base = "-fx-background-color: #c05242;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 12 20 12 20;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;";
        String hover = "-fx-background-color: #d16252;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 12 20 12 20;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;";
        button.setStyle(base);
        button.setOnMouseEntered(e -> button.setStyle(hover));
        button.setOnMouseExited(e -> button.setStyle(base));
    }

    private void styleSideButton(Button button) {
        String base = "-fx-background-color: #3a3a3a;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;";
        String hover = "-fx-background-color: #4a4a4a;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;";
        button.setStyle(base);
        button.setOnMouseEntered(e -> button.setStyle(hover));
        button.setOnMouseExited(e -> button.setStyle(base));
    }

    private void styleHamburgerButton(Button button) {
        String base = "-fx-background-color: #262421;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 6 14 6 14;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;";
        String hover = "-fx-background-color: #3a3a3a;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 6 14 6 14;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;";
        button.setStyle(base);
        button.setOnMouseEntered(e -> button.setStyle(hover));
        button.setOnMouseExited(e -> button.setStyle(base));
    }

    private void resetGame() {
        startNewGame(humanColor);
    }

    private void redrawBoard() {
        legalMoves = MoveGenerator.generateLegalMoves(board);

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int dr = displayRow(r);
                int dc = displayCol(c);

                Color bg = Color.web(((r + c) % 2 == 0) ? lightColor() : darkColor());

                if (r == selectedRow && c == selectedCol) {
                    bg = Color.web(SELECTED_COLOR);
                }

                highlights[dr][dc].setFill(bg);

                boolean isLegalDest = (selectedRow != -1) && isLegalDestination(r, c);
                Piece target = board.getPiece(r, c);

                Circle dot = dots[dr][dc];
                if (isLegalDest && target == null) {
                    dot.setRadius(8);
                    dot.setFill(Color.web("#3a3a3a", 0.55));
                    dot.setStroke(Color.TRANSPARENT);
                    dot.setVisible(true);
                } else if (isLegalDest && target != null) {
                    dot.setRadius(30);
                    dot.setFill(Color.TRANSPARENT);
                    dot.setStroke(Color.web("#3a3a3a", 0.6));
                    dot.setStrokeWidth(4);
                    dot.setVisible(true);
                } else {
                    dot.setVisible(false);
                }

                Piece piece = board.getPiece(r, c);
                pieceTexts[dr][dc].setText(piece == null ? "" : pieceSymbol(piece));
            }
        }
    }

    private boolean isLegalDestination(int row, int col) {
        for (Move m : legalMoves) {
            if (m.getFromRow() == selectedRow && m.getFromCol() == selectedCol
                    && m.getToRow() == row && m.getToCol() == col) {
                return true;
            }
        }
        return false;
    }

    private String pieceSymbol(Piece p) {
        boolean white = p.getColor() == Piece.Color.WHITE;
        switch (p.getType()) {
            case KING:   return white ? "\u2654" : "\u265A";
            case QUEEN:  return white ? "\u2655" : "\u265B";
            case ROOK:   return white ? "\u2656" : "\u265C";
            case BISHOP: return white ? "\u2657" : "\u265D";
            case KNIGHT: return white ? "\u2658" : "\u265E";
            case PAWN:   return white ? "\u2659" : "\u265F";
            default:     return "";
        }
    }

    private void updateStatus() {
        if (gameOver) return;
        if (paused) {
            statusLabel.setText("Paused");
            return;
        }
        if (botThinking) {
            statusLabel.setText("Bot is thinking...");
        } else {
            Piece.Color turn = board.getSideToMove();
            String check = board.isInCheck(turn) ? " (CHECK!)" : "";
            statusLabel.setText((turn == Piece.Color.WHITE ? "White" : "Black") + " to move" + check);
        }
    }
}