package com.comp2042.controller.guiControl;

import com.comp2042.audio.soundManager.SoundManager;
import com.comp2042.controller.controls.ControlsController;
import com.comp2042.controller.gameOver.GameOverPanel;
import com.comp2042.input.EventSource;
import com.comp2042.input.EventType;
import com.comp2042.input.InputEventListener;
import com.comp2042.input.MoveEvent;
import com.comp2042.model.ClearRow;
import com.comp2042.model.DownData;
import com.comp2042.model.ViewData;
import com.comp2042.view.BoardView;
import com.comp2042.view.NotificationPanel;
import com.comp2042.view.ParticleHelper;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
 
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;
   
import javafx.scene.layout.StackPane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;



import java.net.URL;
import javafx.scene.input.MouseEvent;
import java.util.ResourceBundle;
 
 
/**
 * Primary JavaFX controller for the single-player / local-multiplayer game view.
 *
 * <p>This class coordinates the UI components for the game board, preview pane,
 * score/time display, overlays (pause, controls, game-over) and delegates
 * rendering and input handling to a set of small helper classes. It deliberately
 * keeps only minimal game logic and instead calls into the application's
 * event listener / game engine via {@link #setEventListener}.</p>
 *
 * <p>Most heavy-lifting UI helpers were extracted to package-private helper
 * classes in the same package (e.g. {@code GuiRenderingHelpers},
 * {@code GuiParticleHelpers}, {@code GuiInputHandler}) to keep this controller
 * focused on wiring and lifecycle management.</p>
 */
public class GuiController implements Initializable {

    private static final int BRICK_SIZE = 24;
    private static final int DROP_INTERVAL_MS = 1000;
    private int dropIntervalMs = DROP_INTERVAL_MS;

    @FXML protected GridPane gamePanel;
    @FXML protected BorderPane gameBoard;
    @FXML protected Pane brickPanel;
    @FXML protected Pane ghostPanel;
    @FXML protected Canvas bgCanvas;
    @FXML protected Group groupNotification;
    @FXML protected GameOverPanel gameOverPanel;
    @FXML protected Text scoreValue;
    @FXML protected Text highScoreValue;
    @FXML protected VBox scoreBox;
    @FXML protected javafx.scene.control.Button pauseBtn;
    @FXML protected VBox nextBox;
    @FXML protected VBox nextContent; 
    @FXML protected Rectangle gameBoardFrame;
    @FXML protected Rectangle nextBoxFrame;
    @FXML protected Pane particlePane;
    @FXML protected VBox timeBox;
    @FXML protected Text timeValue;
    @FXML protected VBox levelBox;
    @FXML protected Text levelValue;

    protected java.util.List<com.comp2042.logic.Brick> upcomingCache = null;
    protected Rectangle[][] displayMatrix;
    protected InputEventListener eventListener;
    protected Rectangle[][] rectangles;
    protected Rectangle[][] ghostRectangles;
    protected int[][] currentBoardMatrix;
    protected ViewData currentViewData;
    protected Timeline timeLine;
    protected Timeline clockTimeline;
    protected GuiClockManager clockManager;
    protected GuiHighScoreManager highScoreManager;
    protected double cellW = BRICK_SIZE;
    protected double cellH = BRICK_SIZE;
    protected double baseOffsetX = 0;
    protected double baseOffsetY = 0;
    private boolean isMultiplayer = false;
    private boolean lastWasHardDrop = false;
    private boolean hardDropAllowed = true;
    private Runnable multiplayerRestartHandler = null;
    private Runnable multiplayerExitToMenuHandler = null;
    private java.util.function.Consumer<Boolean> multiplayerPauseHandler = null;
    private String multiplayerPlayerId = null;
    private java.util.function.Consumer<GuiController> multiplayerRequestControlsHandler = null;
    private boolean suppressMultiplayerPauseNotify = false;
    private IntegerProperty currentScoreProperty = null;
    private int prevHighBeforeGame = 0;
    final BooleanProperty isPause = new SimpleBooleanProperty();
    final BooleanProperty isGameOver = new SimpleBooleanProperty();
    final BooleanProperty countdownFinished = new SimpleBooleanProperty(false);
    final BooleanProperty countdownStarted = new SimpleBooleanProperty(false);
    protected SoundManager soundManager = null;
    private BoardView boardView = null;
    protected BoardView getBoardView() { return boardView; }
    protected SoundManager getSoundManager() { return soundManager; }
    protected Scene attachedScene = null;
    private javafx.animation.Animation gameOverPulse = null;
    KeyCode ctrlMoveLeft = null;
    KeyCode ctrlMoveRight = null;
    KeyCode ctrlRotate = null;
    KeyCode ctrlSoftDrop = null;
    KeyCode ctrlHardDrop = null;
    KeyCode ctrlSwap = null;
    private final javafx.beans.property.DoubleProperty timeBoxOffsetX = new javafx.beans.property.SimpleDoubleProperty(-100.0);
    private final javafx.beans.property.DoubleProperty timeBoxOffsetY = new javafx.beans.property.SimpleDoubleProperty(12.0);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // create helpers
        this.inputHandler = new GuiInputHandler(this);
        this.clockManager = new GuiClockManager(this);
        this.highScoreManager = new GuiHighScoreManager(this);

        // delegate full initialization to GuiInitialize
        GuiInitialize.initialize(this, location, resources);
    }

    /**
     * Initialize the controller and helper classes. This method is called by
     * the JavaFX runtime during FXML loading and should not be invoked directly
     * by consumers.
     */

    // Toggle the pause overlay: show overlay if paused, otherwise resume
    private StackPane pauseOverlay = null;
    private boolean isPauseOverlayVisible = false;

    // helper collaborators extracted to smaller classes
    private GuiInputHandler inputHandler;

    void togglePauseOverlay() {
        if (isGameOver.getValue() == Boolean.TRUE) return;
        if (!isPauseOverlayVisible) {
            GuiOverlays.showPauseOverlay(this);
        } else {
            hidePauseOverlay();
        }
    }    

    // Show the controls overlay (opened from the pause menu settings button)
    void showControlsOverlay() {
        GuiOverlays.showControlsOverlay(this);
    }

    // Load controls.fxml and return its controller; pane is returned via outPane[0]
    ControlsController loadControlsController(String resource, javafx.scene.layout.StackPane[] outPane) throws java.io.IOException {
        if (multiplayerRequestControlsHandler != null) {
            try { multiplayerRequestControlsHandler.accept(this); } catch (Exception ex) { System.err.println("[GuiController] Exception in multiplayerRequestControlsHandler: " + ex); }
            try {
                if (outPane != null && outPane.length > 0) outPane[0] = new javafx.scene.layout.StackPane();
            } catch (Exception ignored) {}
            return null;
        }
        URL loc = getClass().getClassLoader().getResource(resource);
        if (loc == null) return null;
        FXMLLoader fx = new FXMLLoader(loc);
        javafx.scene.layout.StackPane pane = fx.load();
        outPane[0] = pane;
        return fx.getController();
    }

    // Configure initial key mappings on the ControlsController
    void configureControlsController(ControlsController cc) {
        KeyCode left = ctrlMoveLeft != null ? ctrlMoveLeft : KeyCode.LEFT;
        KeyCode right = ctrlMoveRight != null ? ctrlMoveRight : KeyCode.RIGHT;
        KeyCode rotate = ctrlRotate != null ? ctrlRotate : KeyCode.UP;
        KeyCode down = ctrlSoftDrop != null ? ctrlSoftDrop : KeyCode.DOWN;
        KeyCode hard = ctrlHardDrop != null ? ctrlHardDrop : KeyCode.SPACE;
        KeyCode sw = ctrlSwap != null ? ctrlSwap : KeyCode.C;
        cc.init(left, right, rotate, down, hard, sw);
        try {
            if (isMultiplayer && multiplayerPlayerId != null) {
                if ("left".equalsIgnoreCase(multiplayerPlayerId)) {
                    cc.setDefaultKeys(KeyCode.A, KeyCode.D, KeyCode.W, KeyCode.S, KeyCode.SHIFT, KeyCode.Q);
                    cc.setHeaderText("Left Player Controls");
                } else if ("right".equalsIgnoreCase(multiplayerPlayerId)) {
                    cc.setDefaultKeys(KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, KeyCode.DOWN, KeyCode.SPACE, KeyCode.C);
                    cc.setHeaderText("Right Player Controls");
                } else {
                    cc.setDefaultKeys(KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, KeyCode.DOWN, KeyCode.SPACE, KeyCode.C);
                    cc.setHeaderText("In-Game Controls");
                }
            } else {
                cc.setDefaultKeys(KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, KeyCode.DOWN, KeyCode.SPACE, KeyCode.C);
                cc.setHeaderText("In-Game Controls");
            }
        } catch (Exception ignored) {}
        cc.hideActionButtons();
        cc.setHeaderText("In-Game Controls");
    }

    // pause timelines and record pausedElapsedMs
    void pauseTimelinesInternal() {
        if (timeLine != null) timeLine.pause();
        try { if (clockManager != null) clockManager.pauseAndRecord(); } catch (Exception ignored) {}
    }

    // package helper: notify multiplayer pause/resume while respecting suppression flag
    void notifyMultiplayerPause(boolean paused) {
        if (!suppressMultiplayerPauseNotify && multiplayerPauseHandler != null) {
            try { multiplayerPauseHandler.accept(Boolean.valueOf(paused)); } catch (Exception ex) { System.err.println("[GuiController] multiplayerPauseHandler threw: " + ex); }
        }
    }

    void hidePauseOverlay() {
        javafx.application.Platform.runLater(() -> {
            try {
                GuiOverlays.hidePauseOverlay(this, pauseOverlay);
            } catch (Exception ignored) {}
        });
    }

    // Clear the stored overlay reference (called by helpers)
    void clearPauseOverlay() { this.pauseOverlay = null; }

    // Resume timelines and input state after pause overlay has been removed.
    void resumeFromPauseOverlay() {
        try {
            if (timeLine != null) timeLine.play();
            try { this.isPause.setValue(Boolean.FALSE); } catch (Exception ignored) {}
            this.isPauseOverlayVisible = false;
            if (!suppressMultiplayerPauseNotify && multiplayerPauseHandler != null) {
                try { multiplayerPauseHandler.accept(Boolean.FALSE); } catch (Exception ex) { System.err.println("[GuiController] multiplayerPauseHandler threw: " + ex); }
            }
            try { if (clockManager != null) clockManager.startClock(); } catch (Exception ignored) {}
            try { if (gamePanel != null) gamePanel.requestFocus(); } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    // allow overlay builders to restore the pause overlay visibility/state after closing nested dialogs
    void setPauseOverlayVisible(boolean visible) {
        this.isPauseOverlayVisible = visible;
        try { this.isPause.setValue(Boolean.valueOf(visible)); } catch (Exception ignored) {}
    }

    private void detachSceneKeyHandlers() {
        try {
            javafx.application.Platform.runLater(() -> {
                
                try {
                    if (gamePanel != null && gamePanel.getScene() != null) {
                        javafx.scene.Scene s = gamePanel.getScene();
                        try { if (inputHandler != null) inputHandler.detachFromScene(s); } catch (Exception ignored) {}
                    }
                    // give subclasses a chance to remove their own filters
                    try { onSceneDetach(); } catch (Exception ex) { System.err.println("[GuiController] onSceneDetach threw: " + ex); }
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    protected void onSceneDetach() {
    }

    public void cleanup() {
        try {
            
            // stop timelines
            try { if (timeLine != null) timeLine.stop(); } catch (Exception ignored) {}
            try { if (clockTimeline != null) clockTimeline.stop(); } catch (Exception ignored) {}
            // stop music (background + any one-shot game-over tune) and any countdown audio
            try { stopSingleplayerMusic(); } catch (Exception ignored) {}
            try { stopGameOverMusic(); } catch (Exception ignored) {}
            try { stopCountdownMusic(); } catch (Exception ignored) {}
            // detach handlers and allow subclasses to remove filters
            try { detachSceneKeyHandlers(); } catch (Exception ignored) {}
            
        } catch (Exception ignored) {}
    }

    public void initGameView(int[][] boardMatrix, ViewData brick) {
        if (boardView == null) boardView = new BoardView(gamePanel, brickPanel, ghostPanel, bgCanvas);
        boardView.initGameView(boardMatrix, brick);

        timeLine = new Timeline(new KeyFrame(
            Duration.millis(dropIntervalMs),
            ae -> {
                ae.consume();
                moveDown(new MoveEvent(EventType.DOWN, EventSource.THREAD));
            }
        ));
        timeLine.setCycleCount(Timeline.INDEFINITE);
        javafx.application.Platform.runLater(() -> updateGhost(brick, boardMatrix));
    }

    public void setMultiplayerExitToMenuHandler(Runnable handler) {
        this.multiplayerExitToMenuHandler = handler;
    }

    public void startCountdown(int seconds) {
        Timeline cd = GuiCountdownController.startCountdown(this, seconds);
        if (cd != null) cd.playFromStart();
    }

    // Immediately hide brick/ghost panels on the FX thread
    private void hideBrickAndGhostPanelsAsync() {
        try {
            javafx.application.Platform.runLater(() -> {
                try { if (brickPanel != null) brickPanel.setVisible(false); } catch (Exception ignored) {}
                try { if (ghostPanel != null) ghostPanel.setVisible(false); } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    // Refresh visuals and snap brick to ghost position so underlying visuals align with overlay
    private void refreshAndSnapBrickAsync() {
        try {
            if (this.currentViewData != null && this.currentBoardMatrix != null) {
                javafx.application.Platform.runLater(() -> {
                    try {
                        doRefreshBrick(currentViewData);
                        updateGhost(currentViewData, currentBoardMatrix);
                        try { if (brickPanel != null && ghostPanel != null) {
                            brickPanel.setTranslateX(ghostPanel.getTranslateX());
                            brickPanel.setTranslateY(ghostPanel.getTranslateY());
                        } } catch (Exception ignored) {}
                        try { if (brickPanel != null) brickPanel.setVisible(false); } catch (Exception ignored) {}
                        try { if (ghostPanel != null) ghostPanel.setVisible(false); } catch (Exception ignored) {}
                    } catch (Exception ignored) {}
                });
            }
        } catch (Exception ignored) {}
    }

    public BooleanProperty countdownFinishedProperty() {
        return countdownFinished;
    }

    public BooleanProperty countdownStartedProperty() {
        return countdownStarted;
    }

    public BooleanProperty isGameOverProperty() {
        return isGameOver;
    }

    public void setDropIntervalMs(int ms) {
        if (ms <= 0) return;
        this.dropIntervalMs = ms;
        try {
            boolean running = false;
            if (timeLine != null) {
                running = timeLine.getStatus() == Timeline.Status.RUNNING;
                timeLine.stop();
            }
            timeLine = new Timeline(new KeyFrame(
                    Duration.millis(dropIntervalMs),
                    ae -> {
                        ae.consume();
                        moveDown(new MoveEvent(EventType.DOWN, EventSource.THREAD));
                    }
            ));
            timeLine.setCycleCount(Timeline.INDEFINITE);
            if (running) timeLine.play();
        } catch (Exception ignored) {}
    }

    private void updateGhost(ViewData brick, int[][] boardMatrix) {
        GuiRenderingHelpers.updateGhost(this, brick, boardMatrix);
    }

    public void showNextBricks(java.util.List<com.comp2042.logic.Brick> upcoming) {
    if (nextContent == null) return;
    nextContent.getChildren().clear();
        if (upcoming == null) return;
        upcomingCache = new java.util.ArrayList<>(upcoming);
        javafx.scene.layout.VBox built = GuiViewHelpers.buildNextPreview(this, upcoming);
        if (built != null) {
            nextContent.getChildren().addAll(built.getChildren());
        }
    }
    public javafx.scene.layout.VBox buildNextPreview(java.util.List<com.comp2042.logic.Brick> upcoming) {
        return GuiViewHelpers.buildNextPreview(this, upcoming);
    }

    void refreshBrick(ViewData brick) {
        this.currentViewData = brick;
        if (isPause.getValue() == Boolean.FALSE) {
            if (boardView != null) boardView.refreshBrick(brick); else doRefreshBrick(brick);
        }
    }

    void doRefreshBrick(ViewData brick) {
        GuiRenderingHelpers.doRefreshBrick(this, brick);
    }

    
    public void refreshGameBackground(int[][] board) {
        if (boardView != null) { boardView.refreshGameBackground(board); return; }
        this.currentBoardMatrix = board;
        for (int i = 2; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                setRectangleData(board[i][j], displayMatrix[i][j]);
            }
        }
    }

    private void setRectangleData(int color, Rectangle rectangle) {
        GuiRenderingHelpers.setRectangleData(this, color, rectangle);
    }

    void attachButtonSoundHandlers(javafx.scene.control.Button btn) {
        if (btn == null) return;
        btn.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> { if (soundManager != null) { try { soundManager.playHoverSound(); } catch (Exception ignored) {} } });
        btn.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> { if (soundManager != null) { try { soundManager.playClickSound(); } catch (Exception ignored) {} } });
        btn.addEventHandler(javafx.event.ActionEvent.ACTION, e -> { if (soundManager != null) { try { soundManager.playClickSound(); } catch (Exception ignored) {} } });
    }

    protected void playHardDropSound() {
        try { if (soundManager != null) soundManager.playHardDropSound(); } catch (Exception ignored) {}
    }

    private void stopGameOverMusic() {
        if (soundManager == null) return;
        try { soundManager.stopGameOverMusic(); } catch (Exception ignored) {}
    }

    private void playCountdownMusic() {
        if (isMultiplayer) return;
        try {
            if (soundManager != null) soundManager.playCountdownMusic();
        } catch (Exception ex) {
            System.err.println("[GuiController] Exception while playing Countdown music: " + ex);
            ex.printStackTrace();
        }
    }

    private void stopCountdownMusic() {
        if (soundManager == null) return;
        try { soundManager.stopCountdownMusic(); } catch (Exception ignored) {}
    }


    void moveDown(MoveEvent event) {
        try {
            if (Boolean.TRUE.equals(isGameOver.getValue())) {
                try { if (timeLine != null) timeLine.stop(); } catch (Exception ignored) {}
                return;
            }
        } catch (Exception ignored) {}

        if (isPause.getValue() == Boolean.FALSE) {
            ViewData startViewForEffect = this.currentViewData;
            DownData downData = eventListener.onDownEvent(event);
            if (downData.getClearRow() != null && downData.getClearRow().getLinesRemoved() > 0) {
                NotificationPanel notificationPanel = new NotificationPanel("+" + downData.getClearRow().getScoreBonus());
                groupNotification.getChildren().add(notificationPanel);
                notificationPanel.showScore(groupNotification.getChildren());
            }
            refreshBrick(downData.getViewData());
                if (downData.getClearRow() != null) {
                    try {
                        if (lastWasHardDrop) {
                            try { playHardDropSound(); } catch (Exception ignored) {}
                            playLockEffect(startViewForEffect, downData.getViewData(), false);
                        }
                    } catch (Exception ignored) {}
                    // reset after using
                    lastWasHardDrop = false;
                    if (downData.getClearRow().getLinesRemoved() > 0) {
                        try { spawnExplosion(downData.getClearRow(), downData.getViewData()); } catch (Exception ignored) {}
                    }
                }
        }
        gamePanel.requestFocus();
    }

    /**
     *
     * @param start  the ViewData before the drop (may be null)
     * @param end    the ViewData after the drop/lock (may be null)
     * @param intense when true use shorter/faster animation and higher opacity (for hard drops)
     */
    protected void playLockEffect(ViewData start, ViewData end, boolean intense) {
        try {
            ParticleHelper.playLockEffect(particlePane, start, end, intense, brickPanel, getBoardView(), cellW, cellH);
        } catch (Exception ignored) {}
    }

    protected void spawnExplosion(ClearRow clearRow, ViewData v) {
        GuiParticleHelpers.spawnExplosion(this, clearRow, v);
    }

    protected void flashRowAt(double leftXLocal, double topYLocal, double width, double height) {
        GuiParticleHelpers.flashRowAt(this, leftXLocal, topYLocal, width, height);
    }

    protected void spawnRowClearParticles(ClearRow clearRow) {
        GuiParticleHelpers.spawnRowClearParticles(this, clearRow);
    }

    protected void flashRow(double topY, double width, double height) {
        GuiParticleHelpers.flashRow(this, topY, width, height);
    }

    public void shakeBoard() {
        GuiParticleHelpers.shakeBoard(this);
    }

    public void setMultiplayerRestartHandler(Runnable handler) {
        this.multiplayerRestartHandler = handler;
        if (handler != null) this.isMultiplayer = true;
    }

    public void setMultiplayerPauseHandler(java.util.function.Consumer<Boolean> handler) {
        this.multiplayerPauseHandler = handler;
        if (handler != null) this.isMultiplayer = true;
    }

    public void setMultiplayerPlayerId(String id) {
        this.multiplayerPlayerId = id;
        if (id != null) this.isMultiplayer = true;
    }

    public void setMultiplayerRequestControlsHandler(java.util.function.Consumer<GuiController> handler) {
        this.multiplayerRequestControlsHandler = handler;
        if (handler != null) this.isMultiplayer = true;
    }

    public void setEventListener(InputEventListener eventListener) {
        this.eventListener = eventListener;
    }

    public void setMultiplayerMode(boolean multiplayer) {
        this.isMultiplayer = multiplayer;
        if (multiplayer) {
            try { stopSingleplayerMusic(); } catch (Exception ignored) {}
        }
    }

    public void setHardDropEnabled(boolean enabled) {
        this.hardDropAllowed = enabled;
    }

    protected boolean shouldStartSingleplayerMusic() { return !isMultiplayer; }

    boolean isMultiplayerMode() { return this.isMultiplayer; }
    String getMultiplayerPlayerId() { return this.multiplayerPlayerId; }

    void setLastWasHardDrop(boolean v) { this.lastWasHardDrop = v; }
    boolean isLastWasHardDrop() { return this.lastWasHardDrop; }

    public KeyCode getCtrlMoveLeft() { return this.ctrlMoveLeft; }
    public KeyCode getCtrlMoveRight() { return this.ctrlMoveRight; }
    public KeyCode getCtrlRotate() { return this.ctrlRotate; }
    public KeyCode getCtrlSoftDrop() { return this.ctrlSoftDrop; }
    public KeyCode getCtrlHardDrop() { return this.ctrlHardDrop; }
    public KeyCode getCtrlSwap() { return this.ctrlSwap; }
    javafx.beans.property.DoubleProperty timeBoxOffsetXProperty() { return this.timeBoxOffsetX; }
    javafx.beans.property.DoubleProperty timeBoxOffsetYProperty() { return this.timeBoxOffsetY; }

    public boolean isHardDropAllowed() { return this.hardDropAllowed; }

    public void applyExternalPause(boolean paused) {
        if (paused == isPauseOverlayVisible) return;
        try {
            suppressMultiplayerPauseNotify = true;
            togglePauseOverlay();
        } catch (Exception ignored) {
        } finally {
            suppressMultiplayerPauseNotify = false;
        }
    }

    public void refreshCurrentView(ViewData v) {
        try { refreshBrick(v); } catch (Exception ignored) {}
    }

    public void setControlKeys(KeyCode moveLeft, KeyCode moveRight, KeyCode rotate, KeyCode softDrop, KeyCode hardDrop) {
        this.ctrlMoveLeft = moveLeft;
        this.ctrlMoveRight = moveRight;
        this.ctrlRotate = rotate;
        this.ctrlSoftDrop = softDrop;
        this.ctrlHardDrop = hardDrop;
    }

    public void setSwapKey(KeyCode swapKey) {
        this.ctrlSwap = swapKey;
    }

    public void bindScore(IntegerProperty integerProperty) {
        this.currentScoreProperty = integerProperty;
        try { if (highScoreManager != null) highScoreManager.loadHighScore(); } catch (Exception ignored) {}
        scoreValue.textProperty().bind(Bindings.createStringBinding(
                () -> "Current: " + integerProperty.get(),
                integerProperty
        ));

        integerProperty.addListener((obs, oldV, newV) -> {
            java.util.Objects.requireNonNull(obs);
            java.util.Objects.requireNonNull(oldV);
            int current = newV.intValue();
            try { if (highScoreManager != null) highScoreManager.onNewScore(current); } catch (Exception ignored) {}
        });
    }

    public void gameOver() {
        try { if (Boolean.TRUE.equals(isGameOver.getValue())) return; } catch (Exception ignored) {}
        try { if (timeLine != null) timeLine.stop(); } catch (Exception ignored) {}
        try { if (gameOverPanel != null) gameOverPanel.setVisible(false); } catch (Exception ignored) {}
        isGameOver.setValue(Boolean.TRUE);
        try { if (clockManager != null) clockManager.stopClock(); } catch (Exception ignored) {}
        if (isMultiplayer) return;
        try {
            GuiGameOverUI.showGameOver(this);
        } catch (Exception ignored) {}
    }

    void performRestartFromGameOver(StackPane overlay) {
        GuiGameOverController.performRestartFromGameOver(this, overlay);
    }

    void performExitToMenuFromGameOver(StackPane overlay, Scene scene) {
        GuiGameOverController.performExitToMenuFromGameOver(this, overlay, scene);
    }

    int getPrevHighBeforeGame() { return prevHighBeforeGame; }
    void setPrevHighBeforeGame(int v) { this.prevHighBeforeGame = v; }
    void playCountdownMusicInternal() { playCountdownMusic(); }
    void stopCountdownMusicInternal() { stopCountdownMusic(); }
    void refreshAndSnapBrickAsyncInternal() { refreshAndSnapBrickAsync(); }
    void hideBrickAndGhostPanelsAsyncInternal() { hideBrickAndGhostPanelsAsync(); }
    void stopGameOverMusicInternal() { stopGameOverMusic(); }
    void stopSingleplayerMusicInternal() { stopSingleplayerMusic(); }
    void stopAndClearGameOverPulseInternal() { try { if (gameOverPulse != null) { gameOverPulse.stop(); gameOverPulse = null; } } catch (Exception ignored) {} }
    void detachSceneKeyHandlersInternal() { detachSceneKeyHandlers(); }

    /**
     * Public convenience: stop any overlay-related audio/pulse used by this GUI.
     * Intended for external coordinators (e.g. multiplayer controllers) to
     * request immediate audio cleanup without exposing internal implementation.
     */
    public void stopOverlayAudio() {
        try { stopGameOverMusic(); } catch (Exception ignored) {}
        try { stopCountdownMusic(); } catch (Exception ignored) {}
        try { if (gameOverPulse != null) { gameOverPulse.stop(); gameOverPulse = null; } } catch (Exception ignored) {}
    }

    boolean isMultiplayerEnabled() { return this.isMultiplayer; }
    void runMultiplayerRestartHandler() { try { if (this.multiplayerRestartHandler != null) this.multiplayerRestartHandler.run(); } catch (Exception ignored) {} }
    void runMultiplayerExitToMenuHandler() { try { if (this.multiplayerExitToMenuHandler != null) this.multiplayerExitToMenuHandler.run(); } catch (Exception ignored) {} }

    void setupInputHandlers() { try { if (this.inputHandler != null) this.inputHandler.setupSceneKeyHandlers(); } catch (Exception ignored) {} }

    public int getCurrentScore() {
        try {
            if (this.currentScoreProperty != null) return this.currentScoreProperty.get();
            if (this.scoreValue != null) {
                String s = this.scoreValue.getText();
                if (s != null) {
                    int idx = s.lastIndexOf(':');
                    if (idx >= 0 && idx + 1 < s.length()) {
                        String num = s.substring(idx + 1).trim();
                        try { return Integer.parseInt(num); } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
        return -1;
    }

    void setGameOverPulse(javafx.animation.Animation a) { this.gameOverPulse = a; }

    public void newGame(ActionEvent actionEvent) {
        try { stopGameOverMusic(); } catch (Exception ignored) {}
        try { stopCountdownMusic(); } catch (Exception ignored) {}
        timeLine.stop();
        gameOverPanel.setVisible(false);
        eventListener.createNewGame();
        gamePanel.requestFocus();
        timeLine.play();
        isPause.setValue(Boolean.FALSE);
        isGameOver.setValue(Boolean.FALSE);
        try { if (clockManager != null) clockManager.resetClock(); } catch (Exception ignored) {}
        try { if (clockManager != null) clockManager.startClock(); } catch (Exception ignored) {}
    }
    
    public void hideScoreAndTimeUI() {
        try {
            javafx.application.Platform.runLater(() -> {
                try {
                    if (scoreBox != null) { scoreBox.setVisible(false); scoreBox.setManaged(false); }
                } catch (Exception ignored) {}
                try {
                    if (timeBox != null) { timeBox.setVisible(false); timeBox.setManaged(false); }
                } catch (Exception ignored) {}
                try {
                    if (scoreValue != null) {
                        try { scoreValue.textProperty().unbind(); } catch (Exception ignored) {}
                    }
                    if (highScoreValue != null) {
                        try { highScoreValue.textProperty().unbind(); } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    public void pauseGame(ActionEvent actionEvent) {
        gamePanel.requestFocus();
    }

    public void setLevelText(String text) {
        if (levelValue != null) {
            javafx.application.Platform.runLater(() -> levelValue.setText(text));
        }
    }

    private void stopSingleplayerMusic() {
        try { if (soundManager != null) soundManager.stopSingleplayerMusic(); } catch (Exception ignored) {}
    }
}
