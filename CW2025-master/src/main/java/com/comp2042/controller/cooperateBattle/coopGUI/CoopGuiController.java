package com.comp2042.controller.cooperateBattle.coopGUI;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.comp2042.controller.cooperateBattle.coopController.CoopGameController;
import com.comp2042.controller.cooperateBattle.coopController.CoopTickResult;
import com.comp2042.controller.guiControl.GuiController;
import com.comp2042.model.ViewData;
import com.comp2042.model.DownData;
import com.comp2042.view.NotificationPanel;
import com.comp2042.input.InputEventListener;
import com.comp2042.input.MoveEvent;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.Scene;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class CoopGuiController extends GuiController {

    private CoopGameController coop;
    private static final Logger LOGGER = Logger.getLogger(CoopGuiController.class.getName());
    private CoopKeyBindings keyBindings = new CoopKeyBindings();
    private Timeline coopTimeline = null;
    private CoopInputHandler coopInputHandler = null;
    private Scene coopScene = null;
    private CoopMusicManager coopMusicManager = new CoopMusicManager();

    @Override
    protected boolean shouldStartSingleplayerMusic() {
        return false;
    }

    private Pane secondBrickPanel = new Pane();
    private Pane secondGhostPanel = new Pane();
    private CoopSecondPlayerView secondPlayerView = null;
    private javafx.scene.layout.VBox leftNextBox = null;
    private Text scoreText = null;
    private volatile boolean coopPaused = false;
    private CoopPreviewManager previewManager = null;

    public CoopGuiController() {
        super();
    }
    
    javafx.scene.input.KeyCode getLeftMoveLeftKey() { return keyBindings.getLeftMoveLeft(); }
    javafx.scene.input.KeyCode getLeftMoveRightKey() { return keyBindings.getLeftMoveRight(); }
    javafx.scene.input.KeyCode getLeftRotateKey() { return keyBindings.getLeftRotate(); }
    javafx.scene.input.KeyCode getLeftDownKey() { return keyBindings.getLeftDown(); }
    javafx.scene.input.KeyCode getLeftHardKey() { return keyBindings.getLeftHard(); }
    javafx.scene.input.KeyCode getLeftSwapKey() { return keyBindings.getLeftSwap(); }

    public void setLeftKeys(javafx.scene.input.KeyCode left, javafx.scene.input.KeyCode right, javafx.scene.input.KeyCode rotate, javafx.scene.input.KeyCode down, javafx.scene.input.KeyCode hard, javafx.scene.input.KeyCode swap) {
        keyBindings.setLeftKeys(left, right, rotate, down, hard, swap);
    }

    javafx.scene.input.KeyCode getRightMoveLeftKey() { return keyBindings.getRightMoveLeft(); }
    javafx.scene.input.KeyCode getRightMoveRightKey() { return keyBindings.getRightMoveRight(); }
    javafx.scene.input.KeyCode getRightRotateKey() { return keyBindings.getRightRotate(); }
    javafx.scene.input.KeyCode getRightDownKey() { return keyBindings.getRightDown(); }
    javafx.scene.input.KeyCode getRightHardKey() { return keyBindings.getRightHard(); }
    javafx.scene.input.KeyCode getRightSwapKey() { return keyBindings.getRightSwap(); }

    public void setRightKeys(javafx.scene.input.KeyCode left, javafx.scene.input.KeyCode right, javafx.scene.input.KeyCode rotate, javafx.scene.input.KeyCode down, javafx.scene.input.KeyCode hard, javafx.scene.input.KeyCode swap) {
        keyBindings.setRightKeys(left, right, rotate, down, hard, swap);
    }

    private void safeRun(Runnable r, Level level, String msg) {
        if (r == null) return;
        try { r.run(); } catch (Exception e) { LOGGER.log(level != null ? level : Level.FINER, msg, e); }
    }

    public void initCoop(CoopGameController coopController) {
        // orchestrate coop initialization by delegating to focused helper methods
        this.coop = coopController;

        loadPreferences();

        ViewData leftView = coop.getViewDataLeft();
        ViewData rightView = coop.getViewDataRight();
        if (leftView == null) leftView = new ViewData(new int[][]{{0}}, 0, 2, new int[][]{{0}});
        if (rightView == null) rightView = new ViewData(new int[][]{{0}}, 0, 2, new int[][]{{0}});

        initGameView(coop.getBoardMatrix(), leftView);
        attachSecondPanels();
        buildSecondPlayerView(rightView);
        setupLeftNextBox();
        setupScoreBox();
        refreshPreviews();
        setupTimeline();
        setupEventListener();
        attachSceneAndHandlers();
        setupCountdownAndHandlers();
        bindTimeBoxLayout();
    }

    private void loadPreferences() {
        java.util.prefs.Preferences prefs = null;
        try { prefs = java.util.prefs.Preferences.userNodeForPackage(com.comp2042.controller.mainMenu.MainMenuController.class); } catch (Exception e) { LOGGER.log(Level.FINER, "Failed to obtain preferences node", e); }
        if (prefs != null) {
            keyBindings.loadFromPreferences(prefs);
        }
    }

    private void attachSecondPanels() {
        safeRun(() -> {
            if (brickPanel != null && brickPanel.getParent() instanceof Pane) {
                Pane parent = (Pane) brickPanel.getParent();
                secondBrickPanel.setPickOnBounds(false);
                int insertIndex = Math.max(0, parent.getChildren().indexOf(brickPanel)) + 1;
                parent.getChildren().add(insertIndex, secondBrickPanel);
            } else if (brickPanel != null) {
                secondBrickPanel.setPickOnBounds(false);
                brickPanel.getChildren().add(secondBrickPanel);
            }

            if (ghostPanel != null && ghostPanel.getParent() instanceof Pane) {
                Pane ghostParent = (Pane) ghostPanel.getParent();
                secondGhostPanel.setPickOnBounds(false);
                int idx = Math.max(0, ghostParent.getChildren().indexOf(ghostPanel)) + 1;
                ghostParent.getChildren().add(idx, secondGhostPanel);
            } else if (ghostPanel != null) {
                secondGhostPanel.setPickOnBounds(false);
                ghostPanel.getChildren().add(secondGhostPanel);
            }
        }, Level.FINER, "attach second panels");
    }

    private void buildSecondPlayerView(ViewData rightView) {
        secondPlayerView = new CoopSecondPlayerView(secondBrickPanel, secondGhostPanel);
        secondPlayerView.build(rightView, cellW, cellH, gamePanel != null ? gamePanel.getHgap() : 1, gamePanel != null ? gamePanel.getVgap() : 1);
    }

    private void setupLeftNextBox() {
        safeRun(() -> {
            if (nextBox != null && gameBoard != null && gameBoard.getScene() != null) {
                leftNextBox = new javafx.scene.layout.VBox(8);
                leftNextBox.setAlignment(javafx.geometry.Pos.TOP_CENTER);
                leftNextBox.getStyleClass().add("gameBoard");
                Text t = new Text("Next:"); t.getStyleClass().add("nextBrickLabel");
                leftNextBox.getChildren().add(t);
                safeRun(() -> {
                    Pane root = (Pane) gameBoard.getScene().getRoot();
                    root.getChildren().add(leftNextBox);
                    leftNextBox.layoutXProperty().bind(gameBoard.layoutXProperty().subtract(160));
                    leftNextBox.layoutYProperty().bind(gameBoard.layoutYProperty().add(8));
                    // initialize preview manager (may accept null nextContent)
                    previewManager = new CoopPreviewManager(this, coop, leftNextBox, nextContent);
                        // normalize the right-hand nextBox to match leftNextBox appearance and layout
                        safeRun(() -> {
                            if (nextBox != null) {
                                try {
                                    nextBox.setAlignment(javafx.geometry.Pos.TOP_CENTER);
                                    nextBox.setSpacing(8);
                                    // ensure style class includes 'gameBoard' so visual framing matches
                                    if (!nextBox.getStyleClass().contains("gameBoard")) nextBox.getStyleClass().add("gameBoard");
                                    // ensure there is a top label "Next:" with the correct style
                                    boolean hasLabel = false;
                                    for (javafx.scene.Node n : nextBox.getChildren()) {
                                        if (n instanceof Text) {
                                            Text lbl = (Text) n;
                                            lbl.getStyleClass().add("nextBrickLabel");
                                            lbl.setText("Next:");
                                            hasLabel = true; break;
                                        }
                                    }
                                    if (!hasLabel) {
                                        Text lbl2 = new Text("Next:"); lbl2.getStyleClass().add("nextBrickLabel");
                                        nextBox.getChildren().add(0, lbl2);
                                    }
                                    // ensure nextContent exists and is a VBox aligned like leftNextBox's preview container
                                    if (nextContent == null) {
                                        javafx.scene.layout.VBox nc = new javafx.scene.layout.VBox(8);
                                        nc.setAlignment(javafx.geometry.Pos.TOP_CENTER);
                                        // remove any non-text children except we place this content at index 1
                                        nextBox.getChildren().removeIf(n -> !(n instanceof Text));
                                        nextBox.getChildren().add(nc);
                                    } else {
                                        nextContent.setSpacing(8);
                                        nextContent.setAlignment(javafx.geometry.Pos.TOP_CENTER);
                                    }
                                } catch (Exception ignored) {}
                            }
                        }, Level.FINER, "normalize right nextBox");
                }, Level.FINER, "attach leftNextBox to scene root");
            }
        }, Level.FINER, "setup leftNextBox");
    }

    private void setupScoreBox() {
        safeRun(() -> {
            if (scoreBox != null) {
                scoreText = new Text("Score: 0");
                scoreText.getStyleClass().add("scoreClass");
                scoreBox.getChildren().clear();
                if (highScoreValue != null) scoreBox.getChildren().add(highScoreValue);
                scoreBox.getChildren().add(scoreText);

                // Try the preferred binding approach; fall back to a safe binding on failure
                safeRun(() -> bindScore(coop.getTotalScoreProperty()), Level.FINER, "bindScore failed, falling back");
                safeRun(() -> scoreText.textProperty().bind(
                    javafx.beans.binding.Bindings.createStringBinding(
                        () -> "Score: " + coop.getTotalScoreProperty().get(),
                        coop.getTotalScoreProperty()
                    )
                ), Level.FINER, "scoreText binding failed");

                if (highScoreValue != null) {
                    safeRun(() -> {
                        javafx.beans.binding.IntegerBinding bindHigh = new javafx.beans.binding.IntegerBinding() {
                            { bind(coop.getTotalHighScoreProperty()); }
                            @Override protected int computeValue() { return coop.getTotalHighScoreProperty().get(); }
                        };
                        highScoreValue.textProperty().bind(new javafx.beans.binding.StringBinding() {
                            { bind(bindHigh); }
                            @Override protected String computeValue() { return "Highest: " + bindHigh.get(); }
                        });
                    }, Level.FINER, "highScore binding failed");
                }
            }
        }, Level.FINER, "setup score box");
    }

    private void setupTimeline() {
        try { if (timeLine != null) { timeLine.stop(); timeLine = null; } } catch (Exception ignored) {}
        double intervalMs = 1000.0;
        try { if (timeLine != null && !timeLine.getKeyFrames().isEmpty()) intervalMs = timeLine.getKeyFrames().get(0).getTime().toMillis(); } catch (Exception ignored) {}
        final double usedInterval = intervalMs;
        coopTimeline = new Timeline(new KeyFrame(Duration.millis(usedInterval), ae -> {
            ae.consume();
            ViewData beforeLeft = coop.getViewDataLeft();
            ViewData beforeRight = coop.getViewDataRight();
            CoopTickResult result = coop.tick();
            try { refreshGameBackground(coop.getBoardMatrix()); } catch (Exception ignored) {}
            try { refreshCurrentView(coop.getViewDataLeft()); } catch (Exception ignored) {}
            try { if (secondPlayerView != null) secondPlayerView.refresh(coop.getViewDataRight(), coop.getBoardMatrix(), getBoardView(), currentViewData, cellW, cellH, baseOffsetX, baseOffsetY); } catch (Exception ignored) {}
            if (result != null && result.isMerged()) {
                try {
                    if (result.getLeftData() != null) {
                        DownData ld = result.getLeftData();
                        if (ld.getClearRow() != null) {
                            NotificationPanel notificationPanel = new NotificationPanel("+" + ld.getClearRow().getScoreBonus());
                            groupNotification.getChildren().add(notificationPanel);
                            notificationPanel.showScore(groupNotification.getChildren());
                        }
                        try { playLockEffect(beforeLeft, ld.getViewData(), false); } catch (Exception ignored) {}
                        try { spawnExplosion(ld.getClearRow(), ld.getViewData()); } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
                try {
                    if (result.getRightData() != null) {
                        DownData rd = result.getRightData();
                        if (rd.getClearRow() != null) {
                            NotificationPanel notificationPanel = new NotificationPanel("+" + rd.getClearRow().getScoreBonus());
                            groupNotification.getChildren().add(notificationPanel);
                            notificationPanel.showScore(groupNotification.getChildren());
                        }
                        try { playLockEffect(beforeRight, rd.getViewData(), false); } catch (Exception ignored) {}
                        try { spawnExplosion(rd.getClearRow(), rd.getViewData()); } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
                refreshPreviews();
            }
        }));
        coopTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void setupEventListener() {
        try {
            setEventListener(new InputEventListener() {
                @Override
                public DownData onDownEvent(MoveEvent event) { return null; }

                @Override
                public ViewData onLeftEvent(MoveEvent event) { return coop.getViewDataLeft(); }

                @Override
                public ViewData onRightEvent(MoveEvent event) { return coop.getViewDataRight(); }

                @Override
                public ViewData onRotateEvent(MoveEvent event) { return coop.getViewDataLeft(); }

                @Override
                public void createNewGame() {
                    try {
                        coop.createNewGame();
                    } catch (Exception ignored) {}
                    javafx.application.Platform.runLater(() -> {
                        try {
                            try { if (coopTimeline != null) coopTimeline.stop(); } catch (Exception ignored) {}
                            try { coopMusicManager.stopAndDispose(getSoundManager()); } catch (Exception ignored) {}
                            try { refreshGameBackground(coop.getBoardMatrix()); } catch (Exception ignored) {}
                            try { if (brickPanel != null) brickPanel.setVisible(false); } catch (Exception ignored) {}
                            try { if (ghostPanel != null) ghostPanel.setVisible(false); } catch (Exception ignored) {}
                            try { if (secondBrickPanel != null) secondBrickPanel.setVisible(false); } catch (Exception ignored) {}
                            try { if (secondGhostPanel != null) secondGhostPanel.setVisible(false); } catch (Exception ignored) {}
                            try { refreshPreviews(); } catch (Exception ignored) {}
                            try { startCountdown(3); } catch (Exception ignored) {}
                        } catch (Exception ignored) {}
                    });
                }
            });
        } catch (Exception ignored) {}
    }

    private void attachSceneAndHandlers() {
        javafx.application.Platform.runLater(() -> {
            if (gamePanel.getScene() != null) {
                Scene s = gamePanel.getScene();
                // ensure coop stylesheet is present
                try { CoopStyleManager.ensureStylesheet(s); } catch (Exception ignored) {}
                coopScene = s;
                // attach centralized input handler
                coopInputHandler = new CoopInputHandler(this);
                coopInputHandler.attachToScene(s);
            }
            try { refreshGameBackground(coop.getBoardMatrix()); } catch (Exception ignored) {}
            try { refreshCurrentView(coop.getViewDataLeft()); } catch (Exception ignored) {}
            try { if (secondPlayerView != null) secondPlayerView.refresh(coop.getViewDataRight(), coop.getBoardMatrix(), getBoardView(), currentViewData, cellW, cellH, baseOffsetX, baseOffsetY); } catch (Exception ignored) {}
            try { refreshPreviews(); } catch (Exception ignored) {}
        });
    }

    private void setupCountdownAndHandlers() {
        javafx.application.Platform.runLater(() -> {
            try {
                countdownFinishedProperty().addListener(new javafx.beans.value.ChangeListener<Boolean>() {
                    @Override
                    public void changed(javafx.beans.value.ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                        try {
                            if (Boolean.TRUE.equals(newValue)) {
                                refreshGameBackground(coop.getBoardMatrix());
                                refreshCurrentView(coop.getViewDataLeft());
                                if (secondPlayerView != null) secondPlayerView.refresh(coop.getViewDataRight(), coop.getBoardMatrix(), getBoardView(), currentViewData, cellW, cellH, baseOffsetX, baseOffsetY);
                                try { secondBrickPanel.setVisible(true); secondGhostPanel.setVisible(true); } catch (Exception ignored) {}
                                try { if (coopTimeline != null) coopTimeline.play(); } catch (Exception ignored) {}
                                try { coopMusicManager.playOrCreate(getSoundManager()); } catch (Exception ignored) {}
                            }
                        } catch (Exception ignored) {}
                    }
                });
            } catch (Exception ignored) {}
            try { secondBrickPanel.setVisible(false); secondGhostPanel.setVisible(false); } catch (Exception ignored) {}
            startCountdown(3);

            try {
                setMultiplayerPauseHandler(paused -> {
                    try {
                        coopPaused = Boolean.TRUE.equals(paused);
                        if (coopTimeline != null) {
                            if (coopPaused) coopTimeline.pause(); else coopTimeline.play();
                        }
                    } catch (Exception ignored) {}
                });
                try { setMultiplayerRequestControlsHandler(this::showMultiplayerControlsOverlay); } catch (Exception ignored) {}
                try { setMultiplayerMode(false); } catch (Exception ignored) {}
            } catch (Exception ignored) {}

            try {
                if (coop != null) {
                    coop.gameOverProperty().addListener((obs, oldV, newV) -> {
                        try {
                            if (Boolean.TRUE.equals(newV)) {
                                javafx.application.Platform.runLater(() -> {
                                    try {
                                        gameOver();
                                    } catch (Exception ignored) {}
                                });
                            }
                        } catch (Exception ignored) {}
                    });
                }
            } catch (Exception ignored) {}

            try { if (levelBox != null) levelBox.setVisible(false); } catch (Exception ignored) {}
        });
    }

    private void bindTimeBoxLayout() {
        javafx.application.Platform.runLater(() -> {
            try {
                if (timeBox == null || gameBoard == null) return;
                // remove any previous bindings so we can replace them
                try { timeBox.layoutXProperty().unbind(); } catch (Exception ignored) {}
                try { timeBox.layoutYProperty().unbind(); } catch (Exception ignored) {}

                // Place the time box at the bottom-right of the gameBoard (outside the board)
                final double marginX = 16.0;
                final double marginY = 12.0;

                timeBox.layoutXProperty().bind(
                    javafx.beans.binding.Bindings.createDoubleBinding(
                        () -> {
                            double x = gameBoard.getLayoutX() + gameBoard.getWidth() + marginX;
                            try {
                                Scene s = gameBoard.getScene();
                                if (s != null) {
                                    double sceneW = s.getWidth();
                                    if (x + timeBox.getWidth() > sceneW - marginX) {
                                        // clamp inside the board right edge to avoid overflow
                                        x = gameBoard.getLayoutX() + gameBoard.getWidth() - timeBox.getWidth() - marginX;
                                    }
                                }
                            } catch (Exception ignored) {}
                            return x;
                        },
                        gameBoard.layoutXProperty(), gameBoard.widthProperty(), timeBox.widthProperty()
                    )
                );

                timeBox.layoutYProperty().bind(
                    javafx.beans.binding.Bindings.createDoubleBinding(
                        () -> gameBoard.getLayoutY() + gameBoard.getHeight() - timeBox.getHeight() - marginY,
                        gameBoard.layoutYProperty(), gameBoard.heightProperty(), timeBox.heightProperty()
                    )
                );
            } catch (Exception ignored) {}
        });
    }

    private void refreshPreviews() {
        if (previewManager != null) {
            safeRun(() -> previewManager.refreshPreviews(), Level.FINER, "refresh previews via manager");
            return;
        }
        // fallback: preserve previous behaviour if preview manager not initialized
        safeRun(() -> {
            if (coop == null) return;
            if (leftNextBox != null) {
                leftNextBox.getChildren().removeIf(n -> !(n instanceof Text));
                java.util.List<com.comp2042.logic.Brick> leftUp = coop.getUpcomingLeft(3);
                javafx.scene.layout.VBox built = buildNextPreview(leftUp);
                if (built != null) leftNextBox.getChildren().addAll(built.getChildren());
            }
        }, Level.FINER, "refresh left previews");

        safeRun(() -> {
            if (nextContent != null) {
                nextContent.getChildren().clear();
                java.util.List<com.comp2042.logic.Brick> rightUp = coop.getUpcomingRight(3);
                javafx.scene.layout.VBox built2 = buildNextPreview(rightUp);
                if (built2 != null) nextContent.getChildren().addAll(built2.getChildren());
            }
        }, Level.FINER, "refresh right previews");
    }

    void onKeyPressed(KeyEvent e) {
        KeyCode code = e.getCode();
        boolean consumed = false;
        try {
            if (isInputBlocked()) { e.consume(); return; }
            consumed = handleKeyCode(code);
        } catch (Exception ignored) {}

        if (consumed) {
            try { refreshGameBackground(coop.getBoardMatrix()); } catch (Exception ignored) {}
            try { refreshCurrentView(coop.getViewDataLeft()); } catch (Exception ignored) {}
            try { if (secondPlayerView != null) secondPlayerView.refresh(coop.getViewDataRight(), coop.getBoardMatrix(), getBoardView(), currentViewData, cellW, cellH, baseOffsetX, baseOffsetY); } catch (Exception ignored) {}
            try { refreshPreviews(); } catch (Exception ignored) {}
            e.consume();
        }
    }

    private boolean isInputBlocked() {
        try {
            return Boolean.FALSE.equals(countdownFinishedProperty().getValue())
                || Boolean.TRUE.equals(isGameOverProperty().getValue())
                || coopPaused;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean handleKeyCode(KeyCode code) {
        try {
            // Left player controls
            if (code == keyBindings.getLeftMoveLeft()) return coop.moveLeftPlayerLeft();
            if (code == keyBindings.getLeftMoveRight()) return coop.moveLeftPlayerRight();
            if (code == keyBindings.getLeftRotate()) return coop.rotateLeftPlayer();
            if (code == keyBindings.getLeftDown()) { handleLeftDown(); return true; }
            if (code == keyBindings.getLeftHard()) { handleLeftHard(); return true; }
            if (code == keyBindings.getLeftSwap()) return coop.swapLeft();

            // Right player controls
            if (code == keyBindings.getRightMoveLeft()) return coop.moveRightPlayerLeft();
            if (code == keyBindings.getRightMoveRight()) return coop.moveRightPlayerRight();
            if (code == keyBindings.getRightRotate()) return coop.rotateRightPlayer();
            if (code == keyBindings.getRightDown()) { handleRightDown(); return true; }
            if (code == keyBindings.getRightHard()) { handleRightHard(); return true; }
            if (code == keyBindings.getRightSwap()) return coop.swapRight();
        } catch (Exception ignored) {}
        return false;
    }

    private void handleLeftDown() {
        try {
            DownData dd = coop.onLeftDown();
            if (dd != null && dd.getClearRow() != null && dd.getClearRow().getLinesRemoved() > 0) {
                NotificationPanel notificationPanel = new NotificationPanel("+" + dd.getClearRow().getScoreBonus());
                groupNotification.getChildren().add(notificationPanel);
                notificationPanel.showScore(groupNotification.getChildren());
                try { spawnExplosion(dd.getClearRow(), dd.getViewData()); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private void handleRightDown() {
        try {
            DownData dd = coop.onRightDown();
            if (dd != null && dd.getClearRow() != null && dd.getClearRow().getLinesRemoved() > 0) {
                NotificationPanel notificationPanel = new NotificationPanel("+" + dd.getClearRow().getScoreBonus());
                groupNotification.getChildren().add(notificationPanel);
                notificationPanel.showScore(groupNotification.getChildren());
                try { spawnExplosion(dd.getClearRow(), dd.getViewData()); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private void handleLeftHard() {
        try {
            ViewData startView = coop.getViewDataLeft();
            int safety = 0;
            DownData last = null;
            while (safety++ < 200) {
                last = coop.onLeftDown();
                if (last != null && last.getClearRow() != null) break;
            }
            if (last != null && last.getClearRow() != null) {
                try { try { playHardDropSound(); } catch (Exception ignored) {} playLockEffect(startView, last.getViewData(), true); } catch (Exception ignored) {}
                if (last.getClearRow().getLinesRemoved() > 0) try { spawnExplosion(last.getClearRow(), last.getViewData()); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private void handleRightHard() {
        try {
            ViewData startView = coop.getViewDataRight();
            int safety = 0;
            DownData last = null;
            while (safety++ < 200) { last = coop.onRightDown(); if (last != null && last.getClearRow() != null) break; }
            if (last != null && last.getClearRow() != null) {
                try { try { playHardDropSound(); } catch (Exception ignored) {} playLockEffect(startView, last.getViewData(), true); } catch (Exception ignored) {}
                if (last.getClearRow().getLinesRemoved() > 0) try { spawnExplosion(last.getClearRow(), last.getViewData()); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    void onKeyReleased(KeyEvent e) {
    }

    private void showMultiplayerControlsOverlay(GuiController requester) {
        try {
            Scene scene = gameBoard.getScene();
            if (scene == null) return;

            CoopControlsOverlay.KeySet leftCurrent = new CoopControlsOverlay.KeySet(keyBindings.getLeftMoveLeft(), keyBindings.getLeftMoveRight(), keyBindings.getLeftRotate(), keyBindings.getLeftDown(), keyBindings.getLeftHard(), keyBindings.getLeftSwap());
            CoopControlsOverlay.KeySet rightCurrent = new CoopControlsOverlay.KeySet(keyBindings.getRightMoveLeft(), keyBindings.getRightMoveRight(), keyBindings.getRightRotate(), keyBindings.getRightDown(), keyBindings.getRightHard(), keyBindings.getRightSwap());
            CoopControlsOverlay.KeySet leftDefaults = new CoopControlsOverlay.KeySet(CoopKeyBindings.DEFAULT_LEFT_LEFT, CoopKeyBindings.DEFAULT_LEFT_RIGHT, CoopKeyBindings.DEFAULT_LEFT_ROTATE, CoopKeyBindings.DEFAULT_LEFT_DOWN, CoopKeyBindings.DEFAULT_LEFT_HARD, CoopKeyBindings.DEFAULT_LEFT_SWAP);
            CoopControlsOverlay.KeySet rightDefaults = new CoopControlsOverlay.KeySet(CoopKeyBindings.DEFAULT_RIGHT_LEFT, CoopKeyBindings.DEFAULT_RIGHT_RIGHT, CoopKeyBindings.DEFAULT_RIGHT_ROTATE, CoopKeyBindings.DEFAULT_RIGHT_DOWN, CoopKeyBindings.DEFAULT_RIGHT_HARD, CoopKeyBindings.DEFAULT_RIGHT_SWAP);

            CoopControlsOverlay.show(scene, leftCurrent, leftDefaults, rightCurrent, rightDefaults, (lks, rks) -> {
                try {
                    keyBindings.setLeftKeys(lks.left, lks.right, lks.rotate, lks.down, lks.hard, lks.swap);
                    keyBindings.setRightKeys(rks.left, rks.right, rks.rotate, rks.down, rks.hard, rks.swap);
                    java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(com.comp2042.controller.mainMenu.MainMenuController.class);
                    keyBindings.saveToPreferences(prefs);
                } catch (Exception ignored) {}
            }, null, null);
        } catch (Exception ignored) {}
    }

    @Override
    public void gameOver() {
        try { if (coopTimeline != null) coopTimeline.stop(); } catch (Exception ignored) {}
        try { coopMusicManager.stopAndDispose(getSoundManager()); } catch (Exception ignored) {}
        try { super.gameOver(); } catch (Exception ignored) {}
        try {
            javafx.application.Platform.runLater(() -> {
                try {
                    if (coop == null) return;
                    int coopHigh = coop.getTotalHighScoreProperty().get();
                    javafx.scene.Scene s = null;
                    try { s = gameBoard.getScene(); } catch (Exception ignored) {}
                    if (s == null) return;
                    javafx.scene.Parent root = s.getRoot();
                    java.util.function.Consumer<javafx.scene.Node> walker = new java.util.function.Consumer<javafx.scene.Node>() {
                        @Override public void accept(javafx.scene.Node n) {
                            try {
                                if (n instanceof javafx.scene.text.Text) {
                                    javafx.scene.text.Text t = (javafx.scene.text.Text) n;
                                    String txt = t.getText();
                                    if (txt != null && txt.toLowerCase().contains("previous")) {
                                        t.setText("Previous Best: " + coopHigh);
                                    }
                                }
                                if (n instanceof javafx.scene.Parent) {
                                    for (javafx.scene.Node child : ((javafx.scene.Parent) n).getChildrenUnmodifiable()) {
                                        accept(child);
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    };
                    walker.accept(root);
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    @Override
    protected void onSceneDetach() {
    LOGGER.fine("[CoopGuiController] onSceneDetach invoked for scene=" + (coopScene != null ? coopScene.hashCode() : "null"));
        try {
            if (coopScene != null) {
                try { if (coopInputHandler != null) coopInputHandler.detach(); } catch (Exception ignored) {}
                LOGGER.fine("[CoopGuiController] removed coop filters from scene");
                coopScene = null;
            }
        } catch (Exception ignored) {}
    try { if (coopTimeline != null) { coopTimeline.stop(); coopTimeline = null; LOGGER.fine("[CoopGuiController] stopped coopTimeline"); } } catch (Exception ignored) {}
                try { coopMusicManager.stopAndDispose(getSoundManager()); LOGGER.fine("[CoopGuiController] disposed coopMusicPlayer"); } catch (Exception ignored) {}
    }
}