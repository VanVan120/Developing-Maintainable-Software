package com.comp2042;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * Cooperative GUI controller that extends the single-player GUI to render two active pieces
 * and route per-player inputs to a CoopGameController.
 */
public class CoopGuiController extends GuiController {

    private CoopGameController coop;
    // configurable keys for left/right players (initialized from preferences)
    private javafx.scene.input.KeyCode leftMoveLeftKey = javafx.scene.input.KeyCode.A;
    private javafx.scene.input.KeyCode leftMoveRightKey = javafx.scene.input.KeyCode.D;
    private javafx.scene.input.KeyCode leftRotateKey = javafx.scene.input.KeyCode.W;
    private javafx.scene.input.KeyCode leftDownKey = javafx.scene.input.KeyCode.S;
    private javafx.scene.input.KeyCode leftHardKey = javafx.scene.input.KeyCode.SHIFT;
    private javafx.scene.input.KeyCode leftSwapKey = javafx.scene.input.KeyCode.Q;

    private javafx.scene.input.KeyCode rightMoveLeftKey = javafx.scene.input.KeyCode.LEFT;
    private javafx.scene.input.KeyCode rightMoveRightKey = javafx.scene.input.KeyCode.RIGHT;
    private javafx.scene.input.KeyCode rightRotateKey = javafx.scene.input.KeyCode.UP;
    private javafx.scene.input.KeyCode rightDownKey = javafx.scene.input.KeyCode.DOWN;
    private javafx.scene.input.KeyCode rightHardKey = javafx.scene.input.KeyCode.SPACE;
    private javafx.scene.input.KeyCode rightSwapKey = javafx.scene.input.KeyCode.C;
    // timeline used specifically for cooperative tick; keep separate from GuiController.timeLine
    private Timeline coopTimeline = null;
    // stored scene filters so they can be removed when the view is torn down
    private javafx.event.EventHandler<KeyEvent> coopPressFilter = null;
    private javafx.event.EventHandler<KeyEvent> coopReleaseFilter = null;
    // the Scene these filters were attached to (so they can be removed even if the node is detached)
    private Scene coopScene = null;
    // cooperative background music player (loops). Managed by this controller.
    private javafx.scene.media.MediaPlayer coopMusicPlayer = null;

    @Override
    protected boolean shouldStartSingleplayerMusic() {
        // Coop mode manages its own music (CorporateBattle) and therefore
        // suppresses the base controller's auto-start of the singleplayer track.
        return false;
    }

    // second-player visuals
    private Pane secondBrickPanel = new Pane();
    private Pane secondGhostPanel = new Pane();
    private Rectangle[][] rectangles2;
    private Rectangle[][] ghostRectangles2;
    // cached right view (not strictly required)

    // left preview container placed programmatically
    private javafx.scene.layout.VBox leftNextBox = null;

    private Text scoreText = null;
    // local paused flag kept in sync with GuiController.pause notifications so we can pause our coopTimeline
    private volatile boolean coopPaused = false;

    public CoopGuiController() {
        super();
    }

    /**
     * Initialize cooperative view and wire to the provided controller.
     */
    public void initCoop(CoopGameController coopController) {
        this.coop = coopController;
        // Load persisted multiplayer key overrides if present so controls overlay edits apply immediately
        try {
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(com.comp2042.MainMenuController.class);
            try { String s = prefs.get("mpLeft_left", ""); if (!s.isEmpty()) leftMoveLeftKey = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
            try { String s = prefs.get("mpLeft_right", ""); if (!s.isEmpty()) leftMoveRightKey = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
            try { String s = prefs.get("mpLeft_rotate", ""); if (!s.isEmpty()) leftRotateKey = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
            try { String s = prefs.get("mpLeft_down", ""); if (!s.isEmpty()) leftDownKey = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
            try { String s = prefs.get("mpLeft_hard", ""); if (!s.isEmpty()) leftHardKey = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
            try { String s = prefs.get("mpLeft_switch", ""); if (!s.isEmpty()) leftSwapKey = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}

            try { String s = prefs.get("mpRight_left", ""); if (!s.isEmpty()) rightMoveLeftKey = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
            try { String s = prefs.get("mpRight_right", ""); if (!s.isEmpty()) rightMoveRightKey = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
            try { String s = prefs.get("mpRight_rotate", ""); if (!s.isEmpty()) rightRotateKey = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
            try { String s = prefs.get("mpRight_down", ""); if (!s.isEmpty()) rightDownKey = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
            try { String s = prefs.get("mpRight_hard", ""); if (!s.isEmpty()) rightHardKey = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
            try { String s = prefs.get("mpRight_switch", ""); if (!s.isEmpty()) rightSwapKey = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
        } catch (Exception ignored) {}
        // initialize using left player's view as primary so base measurements are established
        ViewData leftView = coop.getViewDataLeft();
        ViewData rightView = coop.getViewDataRight();
        initGameView(coop.getBoardMatrix(), leftView);

        // create second brick and ghost panels and add to the scene graph.
        // Instead of nesting them inside the primary panels we add them as siblings to
        // the primary panels' parent so both players share the same coordinate space.
        try {
            if (brickPanel != null && brickPanel.getParent() instanceof Pane) {
                Pane parent = (Pane) brickPanel.getParent();
                secondBrickPanel.setPickOnBounds(false);
                // ensure it's above the background but below overlays by adding after brickPanel
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
        } catch (Exception ignored) {}

        // build the second brick rectangles based on right view data
        buildSecondBrickVisuals(rightView);

        // add left preview to the root pane programmatically (to the left of board)
        try {
            if (nextBox != null && gameBoard != null && gameBoard.getScene() != null) {
                leftNextBox = new javafx.scene.layout.VBox(8);
                leftNextBox.setAlignment(javafx.geometry.Pos.TOP_CENTER);
                leftNextBox.getStyleClass().add("gameBoard");
                Text t = new Text("Next:"); t.getStyleClass().add("nextBrickLabel");
                leftNextBox.getChildren().add(t);
                // place leftNextBox to the left of the game board
                try {
                    Pane root = (Pane) gameBoard.getScene().getRoot();
                    root.getChildren().add(leftNextBox);
                    leftNextBox.layoutXProperty().bind(gameBoard.layoutXProperty().subtract(160));
                    leftNextBox.layoutYProperty().bind(gameBoard.layoutYProperty().add(8));
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        // create per-player score displays
        try {
            if (scoreBox != null) {
                scoreText = new Text("Score: 0");
                scoreText.getStyleClass().add("scoreClass");
                scoreBox.getChildren().clear();
                if (highScoreValue != null) scoreBox.getChildren().add(highScoreValue);
                scoreBox.getChildren().add(scoreText);
                // bind to shared cooperative total score
                try {
                    bindScore(coop.getTotalScoreProperty());
                } catch (Exception ignored) {
                    scoreText.textProperty().bind(new javafx.beans.binding.StringBinding() {
                        { bind(coop.getTotalScoreProperty()); }
                        @Override protected String computeValue() { return "Score: " + coop.getTotalScoreProperty().get(); }
                    });
                }
                // Ensure the visible scoreText we added to the scoreBox stays in sync with the coop score
                try {
                    scoreText.textProperty().bind(
                        javafx.beans.binding.Bindings.createStringBinding(
                            () -> "Score: " + coop.getTotalScoreProperty().get(),
                            coop.getTotalScoreProperty()
                        )
                    );
                } catch (Exception ignored) {}
                // override the default high-score display to show cooperative-mode high score
                try {
                    if (highScoreValue != null) {
                        javafx.beans.binding.IntegerBinding bindHigh = new javafx.beans.binding.IntegerBinding() {
                            { bind(coop.getTotalHighScoreProperty()); }
                            @Override protected int computeValue() { return coop.getTotalHighScoreProperty().get(); }
                        };
                        highScoreValue.textProperty().bind(new javafx.beans.binding.StringBinding() {
                            { bind(bindHigh); }
                            @Override protected String computeValue() { return "Highest: " + bindHigh.get(); }
                        });
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        // show both upcoming previews
        refreshPreviews();

    // stop the base timeline and create our own timeline to advance both pieces
        try { if (timeLine != null) { timeLine.stop(); timeLine = null; } } catch (Exception ignored) {}
        double intervalMs = 1000.0;
        try { if (timeLine != null && !timeLine.getKeyFrames().isEmpty()) intervalMs = timeLine.getKeyFrames().get(0).getTime().toMillis(); } catch (Exception ignored) {}
        final double usedInterval = intervalMs;
        // create a dedicated coop timeline (don't assign to GuiController.timeLine so startCountdown
        // won't accidentally control it). We'll start coopTimeline explicitly after countdown.
        coopTimeline = new Timeline(new KeyFrame(Duration.millis(usedInterval), ae -> {
            ae.consume();
            // capture views before tick so we can animate locks when pieces land
            ViewData beforeLeft = coop.getViewDataLeft();
            ViewData beforeRight = coop.getViewDataRight();
            CoopTickResult result = coop.tick();
            // refresh board and both bricks
            try { refreshGameBackground(coop.getBoardMatrix()); } catch (Exception ignored) {}
            try { refreshCurrentView(coop.getViewDataLeft()); } catch (Exception ignored) {}
            try { refreshSecondView(coop.getViewDataRight()); } catch (Exception ignored) {}
            if (result != null && result.isMerged()) {
                // show score notification and explosion/lock effects per-player if they landed
                try {
                    if (result.getLeftData() != null) {
                        DownData ld = result.getLeftData();
                        if (ld.getClearRow() != null) {
                            NotificationPanel notificationPanel = new NotificationPanel("+" + ld.getClearRow().getScoreBonus());
                            groupNotification.getChildren().add(notificationPanel);
                            notificationPanel.showScore(groupNotification.getChildren());
                        }
                        // play lock effect from beforeLeft -> landed view
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

        // Install an InputEventListener so the base GuiController's Restart button
        // (which calls eventListener.createNewGame()) will correctly reset the coop model
        // and refresh the UI.
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
                    // Reset model and refresh UI on FX thread
                    try {
                        coop.createNewGame();
                    } catch (Exception ignored) {}
                    javafx.application.Platform.runLater(() -> {
                        try {
                            // stop any running coop timeline
                            try { if (coopTimeline != null) coopTimeline.stop(); } catch (Exception ignored) {}
                            // stop and dispose coop music so it restarts cleanly on the next countdown
                            try {
                                if (coopMusicPlayer != null) {
                                    try { coopMusicPlayer.stop(); } catch (Exception ignored) {}
                                    try { coopMusicPlayer.dispose(); } catch (Exception ignored) {}
                                    coopMusicPlayer = null;
                                }
                            } catch (Exception ignored) {}
                            // refresh visuals
                            try { refreshGameBackground(coop.getBoardMatrix()); } catch (Exception ignored) {}
                            // hide both active-piece panels during countdown so pieces don't appear before Start
                            try { if (brickPanel != null) brickPanel.setVisible(false); } catch (Exception ignored) {}
                            try { if (ghostPanel != null) ghostPanel.setVisible(false); } catch (Exception ignored) {}
                            try { if (secondBrickPanel != null) secondBrickPanel.setVisible(false); } catch (Exception ignored) {}
                            try { if (secondGhostPanel != null) secondGhostPanel.setVisible(false); } catch (Exception ignored) {}
                            // refresh previews only (keep next boxes up-to-date)
                            try { refreshPreviews(); } catch (Exception ignored) {}
                            // restart countdown and timeline (countdownFinished listener will re-show panels)
                            try { startCountdown(3); } catch (Exception ignored) {}
                        } catch (Exception ignored) {}
                    });
                }
            });
        } catch (Exception ignored) {}

        // attach key filter to route inputs to left/right players
        javafx.application.Platform.runLater(() -> {
            if (gamePanel.getScene() != null) {
                coopPressFilter = new javafx.event.EventHandler<KeyEvent>() { @Override public void handle(KeyEvent e) { onKeyPressed(e); } };
                coopReleaseFilter = new javafx.event.EventHandler<KeyEvent>() { @Override public void handle(KeyEvent e) { onKeyReleased(e); } };
                Scene s = gamePanel.getScene();
                coopScene = s;
                s.addEventFilter(KeyEvent.KEY_PRESSED, coopPressFilter);
                s.addEventFilter(KeyEvent.KEY_RELEASED, coopReleaseFilter);
            }
            // refresh initial views now that layout is available so pieces start from the top
            try { refreshGameBackground(coop.getBoardMatrix()); } catch (Exception ignored) {}
            try { refreshCurrentView(coop.getViewDataLeft()); } catch (Exception ignored) {}
            try { refreshSecondView(coop.getViewDataRight()); } catch (Exception ignored) {}
            try { refreshPreviews(); } catch (Exception ignored) {}

            // start countdown and timeline when ready
            // Ensure we refresh both player views again after the countdown finishes so
            // any temporary alignment performed during countdown is corrected.
            try {
                countdownFinishedProperty().addListener(new javafx.beans.value.ChangeListener<Boolean>() {
                    @Override
                    public void changed(javafx.beans.value.ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                        try {
                            if (Boolean.TRUE.equals(newValue)) {
                                // re-run refreshes to ensure second player's local translations
                                // are computed against the final measured brickPanel translation
                                refreshGameBackground(coop.getBoardMatrix());
                                refreshCurrentView(coop.getViewDataLeft());
                                refreshSecondView(coop.getViewDataRight());
                                // show second-player visuals now that countdown finished
                                try { secondBrickPanel.setVisible(true); secondGhostPanel.setVisible(true); } catch (Exception ignored) {}
                                // start the coop timeline now that countdown is complete
                                try { if (coopTimeline != null) coopTimeline.play(); } catch (Exception ignored) {}

                                // Start cooperative background music (looping). Managed here to ensure
                                // the correct CorporateBattle.wav is used instead of singleplayer music.
                                try {
                                    if (coopMusicPlayer == null) {
                                        java.net.URL mus = getClass().getClassLoader().getResource("sounds/CorporateBattle.wav");
                                        if (mus == null) mus = getClass().getResource("/sounds/CorporateBattle.wav");
                                        if (mus != null) {
                                            try {
                                                javafx.scene.media.Media mm = new javafx.scene.media.Media(mus.toExternalForm());
                                                coopMusicPlayer = new javafx.scene.media.MediaPlayer(mm);
                                                coopMusicPlayer.setCycleCount(javafx.scene.media.MediaPlayer.INDEFINITE);
                                                coopMusicPlayer.setAutoPlay(true);
                                                coopMusicPlayer.setVolume(0.6);
                                                coopMusicPlayer.setOnError(() -> System.err.println("[CoopGuiController] CorporateBattle music error: " + coopMusicPlayer.getError()));
                                                System.out.println("[CoopGuiController] CorporateBattle.wav loaded and playing: " + mus);
                                            } catch (Exception ex) {
                                                System.err.println("[CoopGuiController] Failed to initialize CorporateBattle music: " + ex);
                                            }
                                        } else {
                                            System.out.println("[CoopGuiController] CorporateBattle.wav not found in resources (expected sounds/CorporateBattle.wav)");
                                        }
                                    } else {
                                        try { coopMusicPlayer.play(); } catch (Exception ignored) {}
                                    }
                                } catch (Exception ignored) {}
                            }
                        } catch (Exception ignored) {}
                    }
                });
            } catch (Exception ignored) {}
            // hide second-player visuals while countdown runs (GuiController hides the primary panels)
            try { secondBrickPanel.setVisible(false); secondGhostPanel.setVisible(false); } catch (Exception ignored) {}
            startCountdown(3);

            // Register a pause handler so when the base GuiController toggles pause it will notify us
            // and we can pause/resume the dedicated coop timeline accordingly.
            try {
                setMultiplayerPauseHandler(paused -> {
                    try {
                        coopPaused = Boolean.TRUE.equals(paused);
                        if (coopTimeline != null) {
                            if (coopPaused) coopTimeline.pause(); else coopTimeline.play();
                        }
                    } catch (Exception ignored) {}
                });
                // Also register a multiplayer controls handler so pause->Settings can show a two-pane overlay
                try { setMultiplayerRequestControlsHandler(this::showMultiplayerControlsOverlay); } catch (Exception ignored) {}
                // We registered a local pause/request handler which would normally mark this GUI as "multiplayer".
                // For cooperative single-window mode we still want the standard game-over overlay to appear,
                // so ensure multiplayer mode flag remains false while preserving the registered handlers.
                try { setMultiplayerMode(false); } catch (Exception ignored) {}
            } catch (Exception ignored) {}

            // Listen for game-over from the coop controller so we can show the standard game-over UI
            try {
                if (coop != null) {
                    coop.gameOverProperty().addListener((obs, oldV, newV) -> {
                        try {
                            if (Boolean.TRUE.equals(newV)) {
                                // ensure UI changes happen on FX thread
                                javafx.application.Platform.runLater(() -> {
                                    try {
                                        // mark GUI as game over and display overlay
                                        gameOver();
                                    } catch (Exception ignored) {}
                                });
                            }
                        } catch (Exception ignored) {}
                    });
                }
            } catch (Exception ignored) {}

            // hide the level display for cooperative mode (we don't show per-mode level text here)
            try { if (levelBox != null) levelBox.setVisible(false); } catch (Exception ignored) {}

            // move the timeBox to the centre-top of the game board for coop mode
            try {
                if (timeBox != null && gameBoard != null) {
                    // bind X to center of gameBoard minus half width of timeBox
                    timeBox.layoutXProperty().bind(
                        javafx.beans.binding.Bindings.createDoubleBinding(
                            () -> gameBoard.getLayoutX() + gameBoard.getWidth() / 2.0 - timeBox.getWidth() / 2.0,
                            gameBoard.layoutXProperty(), gameBoard.widthProperty(), timeBox.widthProperty()
                        )
                    );
                    // bind Y to slightly above the gameBoard
                    timeBox.layoutYProperty().bind(
                        javafx.beans.binding.Bindings.createDoubleBinding(
                            () -> gameBoard.getLayoutY() - timeBox.getHeight() - 8.0,
                            gameBoard.layoutYProperty(), gameBoard.heightProperty(), timeBox.heightProperty()
                        )
                    );
                }
            } catch (Exception ignored) {}
        });
    }

    private void refreshPreviews() {
        try { if (leftNextBox != null) {
            leftNextBox.getChildren().removeIf(n -> !(n instanceof Text));
            java.util.List<com.comp2042.logic.bricks.Brick> leftUp = coop.getUpcomingLeft(3);
            javafx.scene.layout.VBox built = buildNextPreview(leftUp);
            if (built != null) leftNextBox.getChildren().addAll(built.getChildren());
        } } catch (Exception ignored) {}
        try { if (nextContent != null) {
            nextContent.getChildren().clear();
            java.util.List<com.comp2042.logic.bricks.Brick> rightUp = coop.getUpcomingRight(3);
            javafx.scene.layout.VBox built2 = buildNextPreview(rightUp);
            if (built2 != null) nextContent.getChildren().addAll(built2.getChildren());
        } } catch (Exception ignored) {}
    }

    private void buildSecondBrickVisuals(ViewData rightView) {
        if (rightView == null) return;
        int[][] shape = rightView.getBrickData();
        int rows = shape.length;
        int cols = shape[0].length;
        rectangles2 = new Rectangle[rows][cols];
        ghostRectangles2 = new Rectangle[rows][cols];
    double initialCellW = cellW + (gamePanel != null ? gamePanel.getHgap() : 1);
    double initialCellH = cellH + (gamePanel != null ? gamePanel.getVgap() : 1);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Rectangle r = new Rectangle(Math.max(4.0, initialCellW - 2), Math.max(4.0, initialCellH - 2));
                r.setFill(mapFillColor(shape[i][j]));
                r.setLayoutX(j * initialCellW);
                r.setLayoutY(i * initialCellH);
                rectangles2[i][j] = r;
                secondBrickPanel.getChildren().add(r);

                Rectangle g = new Rectangle(Math.max(4.0, initialCellW - 2), Math.max(4.0, initialCellH - 2));
                g.setFill(Color.rgb(200,200,200,0.25));
                g.setVisible(false);
                g.setLayoutX(j * initialCellW);
                g.setLayoutY(i * initialCellH);
                ghostRectangles2[i][j] = g;
                secondGhostPanel.getChildren().add(g);
            }
        }
    }

    private void refreshSecondView(ViewData v) {
    if (v == null || rectangles2 == null) return;
    int offsetX = v.getxPosition();
    int offsetY = v.getyPosition() - 2;
    javafx.geometry.Point2D pt = boardToPixelLocal(offsetX, offsetY);
    double tx = Math.round(pt.getX() + blockNudgeX);
    double ty = Math.round(pt.getY() + blockNudgeY);
    // secondBrickPanel is added as a sibling to brickPanel's parent, so the
    // boardToPixelLocal coordinates are already in the parent's local space.
    // We can directly apply them to the second panel.
    try {
        secondBrickPanel.setTranslateX(tx);
        secondBrickPanel.setTranslateY(ty);
    } catch (Exception ignored) {
        // fallback
        secondBrickPanel.setTranslateX(tx);
        secondBrickPanel.setTranslateY(ty);
    }
        int[][] data = v.getBrickData();
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                Rectangle r = rectangles2[i][j];
                int val = data[i][j];
                r.setFill(mapFillColor(val));
                r.setVisible(val != 0);
                r.setLayoutX(Math.round(j * cellW));
                r.setLayoutY(Math.round(i * cellH));
            }
        }
        // update ghost2
        int[][] shape = v.getBrickData();
        // compute landing for right piece using existing helper but temporarily merging left piece into board
        // reuse updateGhost logic by swapping panels; simplified: compute landing manually
        // We'll compute landingY similar to updateGhost
    int startX = v.getxPosition();
    int startY = v.getyPosition();
        int landingY = startY;
        int effectiveBrickHeight = shape.length;
        for (int i = shape.length - 1; i >= 0; i--) {
            boolean rowHas = false;
            for (int j = 0; j < shape[i].length; j++) if (shape[i][j] != 0) { rowHas = true; break; }
            if (rowHas) { effectiveBrickHeight = i + 1; break; }
        }
        int maxY = coop.getBoardMatrix().length - effectiveBrickHeight;
        for (int y = startY; y <= maxY; y++) {
            int[][] tmp = MatrixOperations.copy(coop.getBoardMatrix());
            try { tmp = MatrixOperations.merge(tmp, currentViewData != null ? currentViewData.getBrickData() : new int[0][0], currentViewData != null ? currentViewData.getxPosition() : 0, currentViewData != null ? currentViewData.getyPosition() : 0); } catch (Exception ignored) {}
            boolean conflict = MatrixOperations.intersectForGhost(tmp, shape, startX, y);
            if (conflict) { landingY = y - 1; break; }
            if (y == maxY) landingY = y;
        }
    javafx.geometry.Point2D gpt = boardToPixelLocal(startX, landingY - 2);
        try {
            secondGhostPanel.setTranslateX(Math.round(gpt.getX()));
            secondGhostPanel.setTranslateY(Math.round(gpt.getY()));
        } catch (Exception ignored) {
            secondGhostPanel.setTranslateX(Math.round(gpt.getX()));
            secondGhostPanel.setTranslateY(Math.round(gpt.getY()));
        }
        for (int i = 0; i < shape.length; i++) for (int j = 0; j < shape[i].length; j++) {
            Rectangle r = ghostRectangles2[i][j];
            if (r == null) continue;
            if (shape[i][j] == 0) { r.setVisible(false); continue; }
            int boardY = landingY + i;
            int boardX = startX + j;
            boolean visible = true;
            if (boardX < 0 || boardX >= coop.getBoardMatrix()[0].length) visible = false;
            if (boardY < 2) visible = false;
            if (boardY >= 0 && boardY < coop.getBoardMatrix().length && coop.getBoardMatrix()[boardY][boardX] != 0) visible = false;
            r.setVisible(visible);
        }
    }

    private void onKeyPressed(KeyEvent e) {
        KeyCode code = e.getCode();
        boolean consumed = false;
        try {
            // Prevent control inputs until the countdown has finished or if the game is paused/game-over
            try {
                if (Boolean.FALSE.equals(countdownFinishedProperty().getValue()) || Boolean.TRUE.equals(isGameOverProperty().getValue()) || coopPaused) {
                    e.consume();
                    return;
                }
            } catch (Exception ignored) {}
            // Left player: configurable keys
            if (code == leftMoveLeftKey) { consumed = coop.moveLeftPlayerLeft(); }
            else if (code == leftMoveRightKey) { consumed = coop.moveLeftPlayerRight(); }
            else if (code == leftRotateKey) { consumed = coop.rotateLeftPlayer(); }
            else if (code == leftDownKey) {
                try {
                    DownData dd = coop.onLeftDown();
                    if (dd != null && dd.getClearRow() != null && dd.getClearRow().getLinesRemoved() > 0) {
                        NotificationPanel notificationPanel = new NotificationPanel("+" + dd.getClearRow().getScoreBonus());
                        groupNotification.getChildren().add(notificationPanel);
                        notificationPanel.showScore(groupNotification.getChildren());
                        try { spawnExplosion(dd.getClearRow(), dd.getViewData()); } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
                consumed = true;
            }
            else if (code == leftHardKey) { // hard drop
                try {
                    ViewData startView = coop.getViewDataLeft();
                    int safety = 0;
                    DownData last = null;
                    while (safety++ < 200) {
                        last = coop.onLeftDown();
                        if (last != null && last.getClearRow() != null) break;
                    }
                    if (last != null && last.getClearRow() != null) {
                        try { 
                            try { playHardDropSound(); } catch (Exception ignored) {}
                            playLockEffect(startView, last.getViewData(), true); 
                        } catch (Exception ignored) {}
                        if (last.getClearRow().getLinesRemoved() > 0) try { spawnExplosion(last.getClearRow(), last.getViewData()); } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
                consumed = true;
            } else if (code == leftSwapKey) { consumed = coop.swapLeft(); }

            // Right player: configurable keys
            else if (code == rightMoveLeftKey) { consumed = coop.moveRightPlayerLeft(); }
            else if (code == rightMoveRightKey) { consumed = coop.moveRightPlayerRight(); }
            else if (code == rightRotateKey) { consumed = coop.rotateRightPlayer(); }
            else if (code == rightDownKey) {
                try {
                    DownData dd = coop.onRightDown();
                    if (dd != null && dd.getClearRow() != null && dd.getClearRow().getLinesRemoved() > 0) {
                        NotificationPanel notificationPanel = new NotificationPanel("+" + dd.getClearRow().getScoreBonus());
                        groupNotification.getChildren().add(notificationPanel);
                        notificationPanel.showScore(groupNotification.getChildren());
                        try { spawnExplosion(dd.getClearRow(), dd.getViewData()); } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
                consumed = true;
            }
            else if (code == rightHardKey) {
                try {
                    ViewData startView = coop.getViewDataRight();
                    int safety = 0;
                    DownData last = null;
                    while (safety++ < 200) { last = coop.onRightDown(); if (last != null && last.getClearRow() != null) break; }
                    if (last != null && last.getClearRow() != null) {
                        try {
                            try { playHardDropSound(); } catch (Exception ignored) {}
                            playLockEffect(startView, last.getViewData(), true);
                        } catch (Exception ignored) {}
                        if (last.getClearRow().getLinesRemoved() > 0) try { spawnExplosion(last.getClearRow(), last.getViewData()); } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
                consumed = true; }
            else if (code == rightSwapKey) { consumed = coop.swapRight(); }
        } catch (Exception ignored) {}

        if (consumed) {
            try { refreshGameBackground(coop.getBoardMatrix()); } catch (Exception ignored) {}
            try { refreshCurrentView(coop.getViewDataLeft()); } catch (Exception ignored) {}
            try { refreshSecondView(coop.getViewDataRight()); } catch (Exception ignored) {}
            try { refreshPreviews(); } catch (Exception ignored) {}
            e.consume();
        }
    }

    private void onKeyReleased(KeyEvent e) {
        // no-op for now; leave for future soft-drop handling
    }

    /**
     * Show a combined controls overlay allowing both players to edit their keybindings in coop mode.
     * The requesting GuiController is provided so we can keep pause state consistent.
     */
    private void showMultiplayerControlsOverlay(GuiController requester) {
        javafx.application.Platform.runLater(() -> {
            try {
                Scene scene = gameBoard.getScene();
                if (scene == null) return;

                StackPane overlay = new StackPane();
                overlay.setPickOnBounds(true);
                Rectangle dark = new Rectangle();
                dark.widthProperty().bind(scene.widthProperty());
                dark.heightProperty().bind(scene.heightProperty());
                dark.setFill(javafx.scene.paint.Color.rgb(8,8,10,0.82));

                javafx.scene.layout.BorderPane container = new javafx.scene.layout.BorderPane();
                container.setStyle("-fx-padding:18;");

                javafx.scene.text.Text header = new javafx.scene.text.Text("Controls");
                header.setStyle("-fx-font-size:34px; -fx-fill: #9fb0ff; -fx-font-weight:700;");
                javafx.scene.layout.HBox actionBox = new javafx.scene.layout.HBox(10);
                actionBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                javafx.scene.control.Button btnResetTop = new javafx.scene.control.Button("Reset");
                javafx.scene.control.Button btnCancel = new javafx.scene.control.Button("Cancel");
                javafx.scene.control.Button btnSave = new javafx.scene.control.Button("Save");
                btnResetTop.getStyleClass().add("menu-button"); btnCancel.getStyleClass().add("menu-button"); btnSave.getStyleClass().add("menu-button");
                actionBox.getChildren().addAll(btnResetTop, btnCancel, btnSave);
                javafx.scene.layout.BorderPane topBar = new javafx.scene.layout.BorderPane();
                topBar.setLeft(header);
                topBar.setRight(actionBox);
                topBar.setStyle("-fx-padding:8 18 18 18;");
                container.setTop(topBar);

                javafx.scene.layout.HBox center = new javafx.scene.layout.HBox(120);
                center.setStyle("-fx-padding:12; -fx-background-color: transparent;");
                center.setAlignment(javafx.geometry.Pos.CENTER);

                FXMLLoader leftFx = new FXMLLoader(getClass().getClassLoader().getResource("controls.fxml"));
                javafx.scene.layout.StackPane leftPane = leftFx.load();
                ControlsController leftCC = leftFx.getController();

                FXMLLoader rightFx = new FXMLLoader(getClass().getClassLoader().getResource("controls.fxml"));
                javafx.scene.layout.StackPane rightPane = rightFx.load();
                ControlsController rightCC = rightFx.getController();

                // initialize with current keys (use coop-configurable keys)
                try { leftCC.init(leftMoveLeftKey, leftMoveRightKey, leftRotateKey, leftDownKey, leftHardKey, leftSwapKey); } catch (Exception ignored) {}
                try { rightCC.init(rightMoveLeftKey, rightMoveRightKey, rightRotateKey, rightDownKey, rightHardKey, rightSwapKey); } catch (Exception ignored) {}

                // Ensure the Default column reflects persisted preferences (fall back to sensible defaults):
                // Left defaults: A, D, W, S, SHIFT, Q
                // Right defaults: NUMPAD4 (left), NUMPAD6 (right), NUMPAD8 (rotate), NUMPAD5 (down), SPACE (hard), NUMPAD7 (swap)
                try {
                    java.util.prefs.Preferences overlayPrefs = java.util.prefs.Preferences.userNodeForPackage(com.comp2042.MainMenuController.class);
                    // left defaults
                    javafx.scene.input.KeyCode defLLeft = null;
                    javafx.scene.input.KeyCode defLRight = null;
                    javafx.scene.input.KeyCode defLRotate = null;
                    javafx.scene.input.KeyCode defLDown = null;
                    javafx.scene.input.KeyCode defLHard = null;
                    javafx.scene.input.KeyCode defLSwap = null;
                    try { String s = overlayPrefs.get("mpLeft_left", ""); if (!s.isEmpty()) defLLeft = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    try { String s = overlayPrefs.get("mpLeft_right", ""); if (!s.isEmpty()) defLRight = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    try { String s = overlayPrefs.get("mpLeft_rotate", ""); if (!s.isEmpty()) defLRotate = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    try { String s = overlayPrefs.get("mpLeft_down", ""); if (!s.isEmpty()) defLDown = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    try { String s = overlayPrefs.get("mpLeft_hard", ""); if (!s.isEmpty()) defLHard = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    try { String s = overlayPrefs.get("mpLeft_switch", ""); if (!s.isEmpty()) defLSwap = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    leftCC.setDefaultKeys(
                        defLLeft != null ? defLLeft : javafx.scene.input.KeyCode.A,
                        defLRight != null ? defLRight : javafx.scene.input.KeyCode.D,
                        defLRotate != null ? defLRotate : javafx.scene.input.KeyCode.W,
                        defLDown != null ? defLDown : javafx.scene.input.KeyCode.S,
                        defLHard != null ? defLHard : javafx.scene.input.KeyCode.SHIFT,
                        defLSwap != null ? defLSwap : javafx.scene.input.KeyCode.Q
                    );

                    // right defaults
                    javafx.scene.input.KeyCode defRLeft = null;
                    javafx.scene.input.KeyCode defRRight = null;
                    javafx.scene.input.KeyCode defRRotate = null;
                    javafx.scene.input.KeyCode defRDown = null;
                    javafx.scene.input.KeyCode defRHard = null;
                    javafx.scene.input.KeyCode defRSwap = null;
                    try { String s = overlayPrefs.get("mpRight_left", ""); if (!s.isEmpty()) defRLeft = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    try { String s = overlayPrefs.get("mpRight_right", ""); if (!s.isEmpty()) defRRight = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    try { String s = overlayPrefs.get("mpRight_rotate", ""); if (!s.isEmpty()) defRRotate = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    try { String s = overlayPrefs.get("mpRight_down", ""); if (!s.isEmpty()) defRDown = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    try { String s = overlayPrefs.get("mpRight_hard", ""); if (!s.isEmpty()) defRHard = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    try { String s = overlayPrefs.get("mpRight_switch", ""); if (!s.isEmpty()) defRSwap = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    rightCC.setDefaultKeys(
                        defRLeft != null ? defRLeft : javafx.scene.input.KeyCode.NUMPAD4,
                        defRRight != null ? defRRight : javafx.scene.input.KeyCode.NUMPAD6,
                        defRRotate != null ? defRRotate : javafx.scene.input.KeyCode.NUMPAD8,
                        defRDown != null ? defRDown : javafx.scene.input.KeyCode.NUMPAD5,
                        defRHard != null ? defRHard : javafx.scene.input.KeyCode.SPACE,
                        defRSwap != null ? defRSwap : javafx.scene.input.KeyCode.NUMPAD7
                    );
                } catch (Exception ignored) {}

                leftCC.setHeaderText("Left Player Controls");
                rightCC.setHeaderText("Right Player Controls");
                // Prevent duplicate key assignments between the two panels: consult the other pane's current keys
                try {
                    leftCC.setKeyAvailabilityChecker((code, btn) -> {
                        try {
                            java.util.Objects.requireNonNull(btn);
                            if (code == null) return true;
                            return !(code.equals(rightCC.getLeft())
                                    || code.equals(rightCC.getRight())
                                    || code.equals(rightCC.getRotate())
                                    || code.equals(rightCC.getDown())
                                    || code.equals(rightCC.getHard())
                                    || code.equals(rightCC.getSwitch()));
                        } catch (Exception ignored) {
                            return true;
                        }
                    });
                } catch (Exception ignored) {}
                try {
                    rightCC.setKeyAvailabilityChecker((code, btn) -> {
                        try {
                            java.util.Objects.requireNonNull(btn);
                            if (code == null) return true;
                            return !(code.equals(leftCC.getLeft())
                                    || code.equals(leftCC.getRight())
                                    || code.equals(leftCC.getRotate())
                                    || code.equals(leftCC.getDown())
                                    || code.equals(leftCC.getHard())
                                    || code.equals(leftCC.getSwitch()));
                        } catch (Exception ignored) {
                            return true;
                        }
                    });
                } catch (Exception ignored) {}
                try { leftCC.hideActionButtons(); } catch (Exception ignored) {}
                try { rightCC.hideActionButtons(); } catch (Exception ignored) {}

                try { leftPane.setPrefWidth(520); } catch (Exception ignored) {}
                try { rightPane.setPrefWidth(520); } catch (Exception ignored) {}
                center.getChildren().addAll(leftPane, rightPane);
                container.setCenter(center);

                overlay.getChildren().addAll(dark, container);

                if (scene.getRoot() instanceof javafx.scene.layout.Pane) {
                    javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) scene.getRoot();
                    java.util.List<javafx.scene.Node> hidden = new java.util.ArrayList<>();
                    for (javafx.scene.Node n : new java.util.ArrayList<>(root.getChildren())) {
                        if (n != null && "GLOBAL_PAUSE_OVERLAY".equals(n.getId())) {
                            n.setVisible(false);
                            hidden.add(n);
                        }
                    }
                    overlay.getProperties().put("hiddenPauseNodes", hidden);
                    root.getChildren().add(overlay);
                }

                btnResetTop.setOnAction(ev -> {
                    ev.consume();
                    try { leftCC.resetToPanelDefaults(); } catch (Exception ignored) {}
                    try { rightCC.resetToPanelDefaults(); } catch (Exception ignored) {}
                });

                btnSave.setOnAction(ev -> {
                    ev.consume();
                    try {
                        try {
                            javafx.scene.input.KeyCode lLeft = leftCC.getLeft();
                            javafx.scene.input.KeyCode lRight = leftCC.getRight();
                            javafx.scene.input.KeyCode lRotate = leftCC.getRotate();
                            javafx.scene.input.KeyCode lDown = leftCC.getDown();
                            javafx.scene.input.KeyCode lHard = leftCC.getHard();
                            javafx.scene.input.KeyCode lSwap = leftCC.getSwitch();
                            // update local fields so onKeyPressed uses them immediately
                            if (lLeft != null) leftMoveLeftKey = lLeft;
                            if (lRight != null) leftMoveRightKey = lRight;
                            if (lRotate != null) leftRotateKey = lRotate;
                            if (lDown != null) leftDownKey = lDown;
                            if (lHard != null) leftHardKey = lHard;
                            if (lSwap != null) leftSwapKey = lSwap;
                            // The coop GUI stores per-player keys in local fields (leftMoveLeftKey etc.)
                            // and routes input in onKeyPressed(), so updating those fields is sufficient
                            // to apply the new bindings immediately. No external leftGui reference exists
                            // in this controller instance, so do not attempt to call leftGui.setControlKeys().
                            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(com.comp2042.MainMenuController.class);
                            prefs.put("mpLeft_left", leftMoveLeftKey != null ? leftMoveLeftKey.name() : "");
                            prefs.put("mpLeft_right", leftMoveRightKey != null ? leftMoveRightKey.name() : "");
                            prefs.put("mpLeft_rotate", leftRotateKey != null ? leftRotateKey.name() : "");
                            prefs.put("mpLeft_down", leftDownKey != null ? leftDownKey.name() : "");
                            prefs.put("mpLeft_hard", leftHardKey != null ? leftHardKey.name() : "");
                            prefs.put("mpLeft_switch", leftSwapKey != null ? leftSwapKey.name() : "");
                        } catch (Exception ignored) {}
                        try {
                            javafx.scene.input.KeyCode rLeft = rightCC.getLeft();
                            javafx.scene.input.KeyCode rRight = rightCC.getRight();
                            javafx.scene.input.KeyCode rRotate = rightCC.getRotate();
                            javafx.scene.input.KeyCode rDown = rightCC.getDown();
                            javafx.scene.input.KeyCode rHard = rightCC.getHard();
                            javafx.scene.input.KeyCode rSwap = rightCC.getSwitch();
                            if (rLeft != null) rightMoveLeftKey = rLeft;
                            if (rRight != null) rightMoveRightKey = rRight;
                            if (rRotate != null) rightRotateKey = rRotate;
                            if (rDown != null) rightDownKey = rDown;
                            if (rHard != null) rightHardKey = rHard;
                            if (rSwap != null) rightSwapKey = rSwap;
                            // The coop GUI stores per-player keys in local fields (rightMoveLeftKey etc.)
                            // and routes input in onKeyPressed(), so updating those fields is sufficient
                            // to apply the new bindings immediately. No external rightGui reference exists
                            // in this controller instance, so do not attempt to call rightGui.setControlKeys().
                            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(com.comp2042.MainMenuController.class);
                            prefs.put("mpRight_left", rightMoveLeftKey != null ? rightMoveLeftKey.name() : "");
                            prefs.put("mpRight_right", rightMoveRightKey != null ? rightMoveRightKey.name() : "");
                            prefs.put("mpRight_rotate", rightRotateKey != null ? rightRotateKey.name() : "");
                            prefs.put("mpRight_down", rightDownKey != null ? rightDownKey.name() : "");
                            prefs.put("mpRight_hard", rightHardKey != null ? rightHardKey.name() : "");
                            prefs.put("mpRight_switch", rightSwapKey != null ? rightSwapKey.name() : "");
                        } catch (Exception ignored) {}
                    } catch (Exception ignored) {}
                    try {
                        if (overlay.getParent() instanceof javafx.scene.layout.Pane) {
                            javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) overlay.getParent();
                            root.getChildren().remove(overlay);
                        }
                        Object o = overlay.getProperties().get("hiddenPauseNodes");
                        if (o instanceof java.util.List<?>) {
                            for (Object n : (java.util.List<?>) o) {
                                if (n instanceof javafx.scene.Node) ((javafx.scene.Node) n).setVisible(true);
                            }
                        }
                    } catch (Exception ignored) {}
                });

                btnCancel.setOnAction(ev -> {
                    ev.consume();
                    try {
                        if (overlay.getParent() instanceof javafx.scene.layout.Pane) {
                            javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) overlay.getParent();
                            root.getChildren().remove(overlay);
                        }
                        Object o = overlay.getProperties().get("hiddenPauseNodes");
                        if (o instanceof java.util.List<?>) {
                            for (Object n : (java.util.List<?>) o) {
                                if (n instanceof javafx.scene.Node) ((javafx.scene.Node) n).setVisible(true);
                            }
                        }
                    } catch (Exception ignored) {}
                });

            } catch (Exception ignored) {}
        });
    }

    // Map numeric color id to Paint. Duplicate of GuiController.getFillColor since that method is private.
    private javafx.scene.paint.Paint mapFillColor(int i) {
        switch (i) {
            case 0: return Color.TRANSPARENT;
            case 1: return Color.AQUA;
            case 2: return Color.BLUEVIOLET;
            case 3: return Color.DARKGREEN;
            case 4: return Color.YELLOW;
            case 5: return Color.RED;
            case 6: return Color.BEIGE;
            case 7: return Color.BURLYWOOD;
            case 8: return Color.DARKGRAY;
            default: return Color.WHITE;
        }
    }

    // Local board->pixel conversion using protected fields from GuiController.
    private javafx.geometry.Point2D boardToPixelLocal(int boardX, int boardY) {
        double x = baseOffsetX + (boardX * cellW) + nudgeX;
        double y = baseOffsetY + (boardY * cellH) + nudgeY;
        return new javafx.geometry.Point2D(x, y);
    }

    @Override
    public void gameOver() {
        try { if (coopTimeline != null) coopTimeline.stop(); } catch (Exception ignored) {}
        // stop and dispose cooperative music when match ends
        try {
            if (coopMusicPlayer != null) {
                try { coopMusicPlayer.stop(); } catch (Exception ignored) {}
                try { coopMusicPlayer.dispose(); } catch (Exception ignored) {}
                coopMusicPlayer = null;
            }
        } catch (Exception ignored) {}
        try { super.gameOver(); } catch (Exception ignored) {}
        // The base GuiController.gameOver() builds an overlay which shows the single-player
        // "Previous Best" value. Replace that text to show the cooperative high score instead.
        try {
            javafx.application.Platform.runLater(() -> {
                try {
                    if (coop == null) return;
                    int coopHigh = coop.getTotalHighScoreProperty().get();
                    javafx.scene.Scene s = null;
                    try { s = gameBoard.getScene(); } catch (Exception ignored) {}
                    if (s == null) return;
                    javafx.scene.Parent root = s.getRoot();
                    // traverse scene graph and replace any Text nodes containing "Previous" with coop value
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
        // remove any scene filters we installed and stop coop-specific resources
        System.out.println("[CoopGuiController] onSceneDetach invoked for scene=" + (coopScene != null ? coopScene.hashCode() : "null"));
        try {
            if (coopScene != null) {
                try { if (coopPressFilter != null) coopScene.removeEventFilter(KeyEvent.KEY_PRESSED, coopPressFilter); } catch (Exception ignored) {}
                try { if (coopReleaseFilter != null) coopScene.removeEventFilter(KeyEvent.KEY_RELEASED, coopReleaseFilter); } catch (Exception ignored) {}
                System.out.println("[CoopGuiController] removed coop filters from scene");
                coopScene = null;
            }
        } catch (Exception ignored) {}
        try { if (coopTimeline != null) { coopTimeline.stop(); coopTimeline = null; System.out.println("[CoopGuiController] stopped coopTimeline"); } } catch (Exception ignored) {}
        try {
            if (coopMusicPlayer != null) {
                try { coopMusicPlayer.stop(); } catch (Exception ignored) {}
                try { coopMusicPlayer.dispose(); } catch (Exception ignored) {}
                coopMusicPlayer = null;
                System.out.println("[CoopGuiController] disposed coopMusicPlayer");
            }
        } catch (Exception ignored) {}
    }
}
