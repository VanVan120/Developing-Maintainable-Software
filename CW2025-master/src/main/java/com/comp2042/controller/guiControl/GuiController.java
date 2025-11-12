package com.comp2042.controller.guiControl;

import com.comp2042.audio.soundManager.SoundManager;
import com.comp2042.controller.controls.ControlsController;
import com.comp2042.controller.cooperateBattle.coopGUI.CoopGuiController;
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
import com.comp2042.utils.MatrixOperations;

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
import javafx.scene.effect.Reflection;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.util.Duration;
import javafx.animation.ScaleTransition;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.net.URL;
import javafx.scene.input.MouseEvent;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
 

public class GuiController implements Initializable {

    private static final int BRICK_SIZE = 24;
    private static final int DROP_INTERVAL_MS = 1000;
    private int dropIntervalMs = DROP_INTERVAL_MS;
    private static final double SOFT_DROP_RATE = 4.0;
    private static final double NORMAL_RATE = 1.0;
    private static final double SCORE_BOX_OFFSET_X = 250.0;           
    private static final double SCORE_BOX_OFFSET_FROM_BOTTOM = 120.0; 

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
    private long startTimeMs = 0;
    private long pausedElapsedMs = 0;
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
    private boolean suppressMultiplayerPauseNotify = false;
    private String multiplayerPlayerId = null;
    private java.util.function.Consumer<GuiController> multiplayerRequestControlsHandler = null;
    private int highScore = 0;
    private static final String HIGHSCORE_FILE = System.getProperty("user.home") + File.separator + ".tetris_highscore";
    private IntegerProperty currentScoreProperty = null;
    private int prevHighBeforeGame = 0;
    private final BooleanProperty isPause = new SimpleBooleanProperty();
    private final BooleanProperty isGameOver = new SimpleBooleanProperty();
    private final BooleanProperty countdownFinished = new SimpleBooleanProperty(false);
    private final BooleanProperty countdownStarted = new SimpleBooleanProperty(false);
    private SoundManager soundManager = null;
    private BoardView boardView = null;
    protected BoardView getBoardView() { return boardView; }
    protected SoundManager getSoundManager() { return soundManager; }
    private javafx.event.EventHandler<KeyEvent> globalPressHandler = null;
    private javafx.event.EventHandler<KeyEvent> globalReleaseHandler = null;
    private javafx.event.EventHandler<KeyEvent> escHandler = null;
    private Scene attachedScene = null;
    private javafx.animation.Animation gameOverPulse = null;
    private KeyCode ctrlMoveLeft = null;
    private KeyCode ctrlMoveRight = null;
    private KeyCode ctrlRotate = null;
    private KeyCode ctrlSoftDrop = null;
    private KeyCode ctrlHardDrop = null;
    private KeyCode ctrlSwap = null;
    private final javafx.beans.property.DoubleProperty timeBoxOffsetX = new javafx.beans.property.SimpleDoubleProperty(-100.0);
    private final javafx.beans.property.DoubleProperty timeBoxOffsetY = new javafx.beans.property.SimpleDoubleProperty(12.0);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // keep initialize skinny: delegate responsibilities to helpers
        loadFontAndFocus();
        initSoundManager();

        // Attach generic sound handlers to commonly-interacted controls (pause button etc.)
        if (pauseBtn != null) attachButtonSoundHandlers(pauseBtn);

        setupMusicAndGameListeners();
        setupSceneKeyHandlers();

        gameOverPanel.setVisible(false);

        setupReflection();
        setupLayoutBindings();
    }

    // Helper: load font and prepare focus
    private void loadFontAndFocus() {
        try {
            Font.loadFont(getClass().getClassLoader().getResource("digital.ttf").toExternalForm(), 38);
        } catch (Exception ignored) {}
        try { if (gamePanel != null) { gamePanel.setFocusTraversable(true); gamePanel.requestFocus(); } } catch (Exception ignored) {}
    }

    // Helper: initialize SoundManager
    private void initSoundManager() {
        try {
            soundManager = new SoundManager(getClass());
            soundManager.init();
        } catch (Exception ex) {
            System.err.println("[GuiController] sound manager init failed: " + ex.getMessage());
        }
    }

    // Helper: wire up music/game listeners
    private void setupMusicAndGameListeners() {
        // Start singleplayer background music once the countdown finishes (gameplay begins)
        countdownFinished.addListener((obs, oldV, newV) -> {
            java.util.Objects.requireNonNull(obs);
            java.util.Objects.requireNonNull(oldV);
            if (Boolean.TRUE.equals(newV)) {
                if (!shouldStartSingleplayerMusic()) return;
                if (soundManager != null) soundManager.startSingleplayerMusic();
            }
        });

        // Stop singleplayer music when game is over
        isGameOver.addListener((obs, oldV, newV) -> {
            // reference unused params to satisfy static analyzers
            java.util.Objects.requireNonNull(obs);
            java.util.Objects.requireNonNull(oldV);
            if (Boolean.TRUE.equals(newV)) {
                // stop background looping music and play the game over tune once
                stopSingleplayerMusic();
                playGameOverMusic();
            }
        });
    }

    // Helper: create and attach global key handlers
    private void setupSceneKeyHandlers() {
        javafx.application.Platform.runLater(() -> {
            globalPressHandler = new javafx.event.EventHandler<KeyEvent>() {
                @Override public void handle(KeyEvent keyEvent) { processKeyPressed(keyEvent); }
            };
            globalReleaseHandler = new javafx.event.EventHandler<KeyEvent>() {
                @Override public void handle(KeyEvent keyEvent) { processKeyReleased(keyEvent); }
            };
            escHandler = new javafx.event.EventHandler<KeyEvent>() {
                @Override public void handle(KeyEvent e) {
                    if (e.getCode() == KeyCode.ESCAPE) {
                        togglePauseOverlay();
                        e.consume();
                    }
                }
            };

            if (gamePanel != null && gamePanel.getScene() != null) {
                Scene s = gamePanel.getScene();
                attachedScene = s;
                s.addEventHandler(KeyEvent.KEY_PRESSED, globalPressHandler);
                s.addEventHandler(KeyEvent.KEY_RELEASED, globalReleaseHandler);
                s.addEventHandler(KeyEvent.KEY_PRESSED, escHandler);
            } else if (gamePanel != null) {
                gamePanel.sceneProperty().addListener(new javafx.beans.value.ChangeListener<>() {
                    @Override
                    public void changed(javafx.beans.value.ObservableValue<? extends javafx.scene.Scene> observable, javafx.scene.Scene oldScene, javafx.scene.Scene newScene) {
                        if (oldScene != null) {
                            oldScene.removeEventHandler(KeyEvent.KEY_PRESSED, globalPressHandler);
                            oldScene.removeEventHandler(KeyEvent.KEY_RELEASED, globalReleaseHandler);
                            oldScene.removeEventHandler(KeyEvent.KEY_PRESSED, escHandler);

                            // clear attachedScene if it referenced the oldScene
                            if (attachedScene == oldScene) attachedScene = null;
                        }
                        if (newScene != null) {
                            newScene.addEventHandler(KeyEvent.KEY_PRESSED, globalPressHandler);
                            newScene.addEventHandler(KeyEvent.KEY_RELEASED, globalReleaseHandler);
                            newScene.addEventHandler(KeyEvent.KEY_PRESSED, escHandler);
                            attachedScene = newScene;
                        }
                    }
                });
            }
        });
    }

    // Helper: small visual reflection setup
    private void setupReflection() {
        try {
            final Reflection reflection = new Reflection();
            reflection.setFraction(0.8);
            reflection.setTopOpacity(0.9);
            reflection.setTopOffset(-12);
        } catch (Exception ignored) {}
    }

    // Helper: all layout-related bindings executed on the FX thread
    private void setupLayoutBindings() {
        // Center the gameBoard within the root Pane when the scene is ready
        javafx.application.Platform.runLater(() -> {
                bindGameBoardCenter();
                bindScoreBox();
                bindGameBoardFrame();
                bindTimeBox();
                styleScoreValue();
                bindGroupNotification();
                bindNextBox();
                bindLevelBox();
        });
    }

    // Helpers used inside Platform.runLater in setupLayoutBindings()
    private void bindGameBoardCenter() {
        if (gameBoard == null) return;
        if (gameBoard.getParent() instanceof javafx.scene.layout.Region) {
            javafx.scene.layout.Region parent = (javafx.scene.layout.Region) gameBoard.getParent();
            gameBoard.layoutXProperty().bind(parent.widthProperty().subtract(gameBoard.widthProperty()).divide(2));
            gameBoard.layoutYProperty().bind(parent.heightProperty().subtract(gameBoard.heightProperty()).divide(2));
        } else if (gameBoard.getScene() != null) {
            // fallback to centering within the Scene
            gameBoard.layoutXProperty().bind(gameBoard.getScene().widthProperty().subtract(gameBoard.widthProperty()).divide(2));
            gameBoard.layoutYProperty().bind(gameBoard.getScene().heightProperty().subtract(gameBoard.heightProperty()).divide(2));
        }
    }

    private void bindScoreBox() {
        if (scoreBox != null && gameBoard != null) {
            scoreBox.layoutXProperty().bind(gameBoard.layoutXProperty().subtract(SCORE_BOX_OFFSET_X));
            scoreBox.layoutYProperty().bind(gameBoard.layoutYProperty().add(gameBoard.heightProperty().subtract(SCORE_BOX_OFFSET_FROM_BOTTOM)));
        }
    }

    private void bindGameBoardFrame() {
        try {
            if (gameBoardFrame != null && gameBoard != null) {
                gameBoardFrame.widthProperty().bind(gameBoard.widthProperty());
                gameBoardFrame.heightProperty().bind(gameBoard.heightProperty());
                gameBoardFrame.layoutXProperty().bind(gameBoard.layoutXProperty());
                gameBoardFrame.layoutYProperty().bind(gameBoard.layoutYProperty());
                gameBoardFrame.setArcWidth(24);
                gameBoardFrame.setArcHeight(24);
                gameBoardFrame.setStrokeWidth(8);
                gameBoardFrame.setStroke(javafx.scene.paint.Color.web("#2A5058"));
                gameBoardFrame.setFill(javafx.scene.paint.Color.web("#111"));
            }
        } catch (Exception ignored) {}
    }

    private void bindTimeBox() {
        if (timeBox != null && gameBoard != null) {
            timeBox.layoutXProperty().bind(
                javafx.beans.binding.Bindings.createDoubleBinding(
                    () -> gameBoard.getLayoutX() + timeBoxOffsetX.get() - timeBox.getWidth(),
                    gameBoard.layoutXProperty(), gameBoard.widthProperty(), timeBox.widthProperty(), timeBoxOffsetX
                )
            );
            timeBox.layoutYProperty().bind(
                javafx.beans.binding.Bindings.createDoubleBinding(
                    () -> gameBoard.getLayoutY() + timeBoxOffsetY.get(),
                    gameBoard.layoutYProperty(), gameBoard.heightProperty(), timeBox.heightProperty(), timeBoxOffsetY
                )
            );
        }
    }

    private void styleScoreValue() {
        if (scoreValue != null) {
            scoreValue.getStyleClass().remove("scoreClass");
            scoreValue.getStyleClass().add("highScoreClass");
        }
    }

    private void bindGroupNotification() {
        if (groupNotification != null && groupNotification.getParent() != null && gameBoard != null && gameBoard.getParent() != null) {
            groupNotification.layoutXProperty().bind(
                javafx.beans.binding.Bindings.createDoubleBinding(
                    () -> gameBoard.getLayoutX() + gameBoard.getWidth() / 2.0 - groupNotification.getLayoutBounds().getWidth() / 2.0,
                    gameBoard.layoutXProperty(), gameBoard.widthProperty(), groupNotification.layoutBoundsProperty()
                )
            );

            groupNotification.layoutYProperty().bind(
                javafx.beans.binding.Bindings.createDoubleBinding(
                    () -> gameBoard.getLayoutY() + gameBoard.getHeight() / 2.0 - groupNotification.getLayoutBounds().getHeight() / 2.0,
                    gameBoard.layoutYProperty(), gameBoard.heightProperty(), groupNotification.layoutBoundsProperty()
                )
            );
        }
    }

    private void bindNextBox() {
        if (nextBox == null || gameBoard == null) return;
        final double outsideGap = 70.0;
        nextBox.layoutXProperty().bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () -> gameBoard.getLayoutX() + gameBoard.getWidth() + outsideGap,
                gameBoard.layoutXProperty(), gameBoard.widthProperty()
            )
        );

        nextBox.layoutYProperty().bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () -> gameBoard.getLayoutY() + 8.0,
                gameBoard.layoutYProperty(), gameBoard.heightProperty()
            )
        );

        try {
            if (nextBoxFrame != null && nextBox != null) {
                nextBoxFrame.widthProperty().bind(nextBox.widthProperty());
                nextBoxFrame.heightProperty().bind(nextBox.heightProperty());
                nextBoxFrame.layoutXProperty().bind(nextBox.layoutXProperty());
                nextBoxFrame.layoutYProperty().bind(nextBox.layoutYProperty());
                nextBoxFrame.setArcWidth(24);
                nextBoxFrame.setArcHeight(24);
                nextBoxFrame.setStrokeWidth(8);
                nextBoxFrame.setStroke(javafx.scene.paint.Color.web("#2A5058"));
                nextBoxFrame.setFill(javafx.scene.paint.Color.web("#111"));
            }
        } catch (Exception ignored) {}
    }

    private void bindLevelBox() {
        if (levelBox == null || gameBoard == null) return;
        final double outsideGap = 70.0;
        levelBox.layoutXProperty().bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () -> gameBoard.getLayoutX() + gameBoard.getWidth() + outsideGap,
                gameBoard.layoutXProperty(), gameBoard.widthProperty()
            )
        );
        levelBox.layoutYProperty().bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () -> gameBoard.getLayoutY() + gameBoard.getHeight() - levelBox.getHeight() - 12.0,
                gameBoard.layoutYProperty(), gameBoard.heightProperty(), levelBox.heightProperty()
            )
        );
    }

    // Toggle the pause overlay: show overlay if paused, otherwise resume
    private StackPane pauseOverlay = null;
    private boolean isPauseOverlayVisible = false;

    private void togglePauseOverlay() {
        if (isGameOver.getValue() == Boolean.TRUE) return;

        if (!isPauseOverlayVisible) {
            javafx.application.Platform.runLater(() -> {
                try {
                    Scene scene = gameBoard.getScene();
                    if (scene == null) return;
                    pauseOverlay = buildPauseOverlay(scene);
                    addPauseOverlayToRoot(scene, pauseOverlay);
                    pauseTimelinesAndNotify();
                } catch (Exception ignored) {}
            });
        } else {
            hidePauseOverlay();
        }
    }

    // Build the pause overlay (dark background + dialog) and wire resume/settings handlers
    private StackPane buildPauseOverlay(Scene scene) {
        StackPane overlay = new StackPane();
        overlay.setId("GLOBAL_PAUSE_OVERLAY");
        overlay.setPickOnBounds(true);

        Rectangle dark = new Rectangle();
        dark.widthProperty().bind(scene.widthProperty());
        dark.heightProperty().bind(scene.heightProperty());
        dark.setFill(Color.rgb(0,0,0,0.55));

        VBox dialog = new VBox(14);
        dialog.setAlignment(Pos.CENTER);
        dialog.setStyle("-fx-background-color: rgba(30,30,30,0.85); -fx-padding: 18px; -fx-background-radius: 8px;");

        Label title = new Label("Paused");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 36px; -fx-font-weight: bold;");

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER);
        Button resume = new Button("Resume");
        Button settings = new Button("Settings");
        resume.getStyleClass().add("menu-button");
        settings.getStyleClass().add("menu-button");
        attachButtonSoundHandlers(resume);
        attachButtonSoundHandlers(settings);

        resume.setOnAction(ev -> { ev.consume(); hidePauseOverlay(); });

        settings.setOnAction(ev -> { ev.consume(); showControlsOverlay(); });

        buttons.getChildren().addAll(resume, settings);
        dialog.getChildren().addAll(title, buttons);

        overlay.getChildren().addAll(dark, dialog);
        return overlay;
    }

    // Show the controls overlay (opened from the pause menu settings button)
    private void showControlsOverlay() {
        try {
            // Load controller and pane
            javafx.scene.layout.StackPane[] paneOut = new javafx.scene.layout.StackPane[1];
            ControlsController cc = loadControlsController("controls.fxml", paneOut);
            if (cc == null || paneOut[0] == null) { hidePauseOverlay(); return; }

            configureControlsController(cc);

            StackPane controlsOverlay = buildControlsOverlayUI(paneOut[0], cc);

            // add the overlay to the scene root, hiding any existing pause overlays
            Scene sceneLocal = gameBoard.getScene();
            if (sceneLocal != null && sceneLocal.getRoot() instanceof javafx.scene.layout.Pane) {
                javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) sceneLocal.getRoot();
                java.util.List<javafx.scene.Node> hidden = new java.util.ArrayList<>();
                for (javafx.scene.Node n : root.getChildren()) {
                    if (n != null && "GLOBAL_PAUSE_OVERLAY".equals(n.getId())) {
                        n.setVisible(false);
                        hidden.add(n);
                    }
                }
                controlsOverlay.getProperties().put("hiddenPauseNodes", hidden);
                root.getChildren().add(controlsOverlay);
            }

        } catch (Exception ex) {
            try { hidePauseOverlay(); } catch (Exception ignored) {}
        }
    }

    // Load controls.fxml and return its controller; pane is returned via outPane[0]
    private ControlsController loadControlsController(String resource, javafx.scene.layout.StackPane[] outPane) throws java.io.IOException {
        if (multiplayerRequestControlsHandler != null) {
            try { multiplayerRequestControlsHandler.accept(this); } catch (Exception ex) { System.err.println("[GuiController] Exception in multiplayerRequestControlsHandler: " + ex); }
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
    private void configureControlsController(ControlsController cc) {
        KeyCode left = ctrlMoveLeft != null ? ctrlMoveLeft : KeyCode.A;
        KeyCode right = ctrlMoveRight != null ? ctrlMoveRight : KeyCode.D;
        KeyCode rotate = ctrlRotate != null ? ctrlRotate : KeyCode.W;
        KeyCode down = ctrlSoftDrop != null ? ctrlSoftDrop : KeyCode.S;
        KeyCode hard = ctrlHardDrop != null ? ctrlHardDrop : KeyCode.SHIFT;
        KeyCode sw = ctrlSwap != null ? ctrlSwap : KeyCode.C;
        cc.init(left, right, rotate, down, hard, sw);
        try {
            if (isMultiplayer && multiplayerPlayerId != null) {
                if ("left".equalsIgnoreCase(multiplayerPlayerId)) {
                    cc.setDefaultKeys(KeyCode.A, KeyCode.D, KeyCode.W, KeyCode.S, KeyCode.SHIFT, KeyCode.Q);
                    cc.setHeaderText("Left Player Controls");
                } else if ("right".equalsIgnoreCase(multiplayerPlayerId)) {
                    cc.setDefaultKeys(KeyCode.NUMPAD4, KeyCode.NUMPAD6, KeyCode.NUMPAD8, KeyCode.NUMPAD5, KeyCode.SPACE, KeyCode.C);
                    cc.setHeaderText("Right Player Controls");
                } else {
                    cc.setDefaultKeys(KeyCode.A, KeyCode.D, KeyCode.W, KeyCode.S, KeyCode.SHIFT, KeyCode.C);
                    cc.setHeaderText("In-Game Controls");
                }
            } else {
                cc.setDefaultKeys(KeyCode.A, KeyCode.D, KeyCode.W, KeyCode.S, KeyCode.SHIFT, KeyCode.C);
                cc.setHeaderText("In-Game Controls");
            }
        } catch (Exception ignored) {}
        cc.hideActionButtons();
        cc.setHeaderText("In-Game Controls");
    }

    // Build the controls overlay UI and wire cancel/save handlers (saves prefs and restores hidden nodes)
    private StackPane buildControlsOverlayUI(javafx.scene.layout.StackPane pane, ControlsController cc) {
        StackPane controlsOverlay = new StackPane();
        controlsOverlay.setStyle("-fx-padding:0;");
        Rectangle dark2 = new Rectangle();
        Scene sceneLocal = gameBoard.getScene();
        if (sceneLocal != null) {
            dark2.widthProperty().bind(sceneLocal.widthProperty());
            dark2.heightProperty().bind(sceneLocal.heightProperty());
        }
        dark2.setFill(Color.rgb(8,8,10,0.82));

        BorderPane container = new BorderPane();
        container.setMaxWidth(Double.MAX_VALUE);
        container.setMaxHeight(Double.MAX_VALUE);
        container.setStyle("-fx-padding:18;");
        javafx.scene.text.Text header = new javafx.scene.text.Text("Controls");
        header.setStyle("-fx-font-size:34px; -fx-fill: #9fb0ff; -fx-font-weight:700;");
        javafx.scene.layout.HBox actionBox = new javafx.scene.layout.HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        javafx.scene.control.Button btnCancel2 = new javafx.scene.control.Button("Cancel");
        javafx.scene.control.Button btnSave2 = new javafx.scene.control.Button("Save");
        btnCancel2.getStyleClass().add("menu-button"); btnSave2.getStyleClass().add("menu-button");
        attachButtonSoundHandlers(btnCancel2);
        attachButtonSoundHandlers(btnSave2);
        actionBox.getChildren().addAll(btnCancel2, btnSave2);
        BorderPane topBar = new BorderPane();
        topBar.setLeft(header);
        topBar.setRight(actionBox);
        topBar.setStyle("-fx-padding:8 18 18 18;");
        container.setTop(topBar);
        javafx.scene.layout.VBox center = new javafx.scene.layout.VBox(18);
        center.setStyle("-fx-padding:12; -fx-background-color: transparent;");
        center.getChildren().add(pane);
        container.setCenter(center);

        // Cancel handler: remove overlay and restore previously-hidden pause nodes
        btnCancel2.setOnAction(ev2 -> {
            ev2.consume();
            if (controlsOverlay.getParent() instanceof javafx.scene.layout.Pane) {
                javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) controlsOverlay.getParent();
                root.getChildren().remove(controlsOverlay);
            }
            Object o = controlsOverlay.getProperties().get("hiddenPauseNodes");
            if (o instanceof java.util.List<?>) {
                for (Object n : (java.util.List<?>) o) {
                    if (n instanceof javafx.scene.Node) ((javafx.scene.Node) n).setVisible(true);
                }
            }
            isPauseOverlayVisible = true; isPause.setValue(Boolean.TRUE);
        });

        // Save handler: update keys, persist preferences, remove overlay and restore hidden nodes
        btnSave2.setOnAction(ev2 -> {
            ev2.consume();
            try {
                ctrlMoveLeft = cc.getLeft();
                ctrlMoveRight = cc.getRight();
                ctrlRotate = cc.getRotate();
                ctrlSoftDrop = cc.getDown();
                ctrlHardDrop = cc.getHard();
                ctrlSwap = cc.getSwitch();
            } catch (Exception ignored) {}
            try {
                Preferences prefs = Preferences.userNodeForPackage(com.comp2042.controller.MainMenuController.class);
                if (isMultiplayer && multiplayerPlayerId != null) {
                    if ("left".equalsIgnoreCase(multiplayerPlayerId)) {
                        prefs.put("mpLeft_left", ctrlMoveLeft != null ? ctrlMoveLeft.name() : "");
                        prefs.put("mpLeft_right", ctrlMoveRight != null ? ctrlMoveRight.name() : "");
                        prefs.put("mpLeft_rotate", ctrlRotate != null ? ctrlRotate.name() : "");
                        prefs.put("mpLeft_down", ctrlSoftDrop != null ? ctrlSoftDrop.name() : "");
                        prefs.put("mpLeft_hard", ctrlHardDrop != null ? ctrlHardDrop.name() : "");
                        prefs.put("mpLeft_switch", ctrlSwap != null ? ctrlSwap.name() : "");
                    } else if ("right".equalsIgnoreCase(multiplayerPlayerId)) {
                        prefs.put("mpRight_left", ctrlMoveLeft != null ? ctrlMoveLeft.name() : "");
                        prefs.put("mpRight_right", ctrlMoveRight != null ? ctrlMoveRight.name() : "");
                        prefs.put("mpRight_rotate", ctrlRotate != null ? ctrlRotate.name() : "");
                        prefs.put("mpRight_down", ctrlSoftDrop != null ? ctrlSoftDrop.name() : "");
                        prefs.put("mpRight_hard", ctrlHardDrop != null ? ctrlHardDrop.name() : "");
                        prefs.put("mpRight_switch", ctrlSwap != null ? ctrlSwap.name() : "");
                    } else {
                        prefs.put("spLeft", ctrlMoveLeft != null ? ctrlMoveLeft.name() : "");
                        prefs.put("spRight", ctrlMoveRight != null ? ctrlMoveRight.name() : "");
                        prefs.put("spRotate", ctrlRotate != null ? ctrlRotate.name() : "");
                        prefs.put("spDown", ctrlSoftDrop != null ? ctrlSoftDrop.name() : "");
                        prefs.put("spHard", ctrlHardDrop != null ? ctrlHardDrop.name() : "");
                        prefs.put("spSwitch", ctrlSwap != null ? ctrlSwap.name() : "");
                    }
                } else {
                    prefs.put("spLeft", ctrlMoveLeft != null ? ctrlMoveLeft.name() : "");
                    prefs.put("spRight", ctrlMoveRight != null ? ctrlMoveRight.name() : "");
                    prefs.put("spRotate", ctrlRotate != null ? ctrlRotate.name() : "");
                    prefs.put("spDown", ctrlSoftDrop != null ? ctrlSoftDrop.name() : "");
                    prefs.put("spHard", ctrlHardDrop != null ? ctrlHardDrop.name() : "");
                    prefs.put("spSwitch", ctrlSwap != null ? ctrlSwap.name() : "");
                }
            } catch (Exception ignored) {}
            if (controlsOverlay.getParent() instanceof javafx.scene.layout.Pane) {
                javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) controlsOverlay.getParent();
                root.getChildren().remove(controlsOverlay);
            }
            Object o2 = controlsOverlay.getProperties().get("hiddenPauseNodes");
            if (o2 instanceof java.util.List<?>) {
                for (Object n : (java.util.List<?>) o2) {
                    if (n instanceof javafx.scene.Node) ((javafx.scene.Node) n).setVisible(true);
                }
            }
            isPauseOverlayVisible = true; isPause.setValue(Boolean.TRUE);
        });

        controlsOverlay.getChildren().addAll(dark2, container);
        return controlsOverlay;
    }

    // Add the pause overlay to the scene root (or groupNotification fallback) after removing any previous ones
    private void addPauseOverlayToRoot(Scene scene, StackPane pauseOverlay) {
        if (scene.getRoot() instanceof javafx.scene.layout.Pane) {
            javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) scene.getRoot();
            try {
                java.util.List<javafx.scene.Node> toRemove = new java.util.ArrayList<>();
                for (javafx.scene.Node n : root.getChildren()) {
                    if (n != null && "GLOBAL_PAUSE_OVERLAY".equals(n.getId())) toRemove.add(n);
                }
                root.getChildren().removeAll(toRemove);
            } catch (Exception ignored) {}
            root.getChildren().add(pauseOverlay);
        } else if (groupNotification != null) {
            try {
                java.util.List<javafx.scene.Node> toRemove = new java.util.ArrayList<>();
                for (javafx.scene.Node n : groupNotification.getChildren()) {
                    if (n != null && "GLOBAL_PAUSE_OVERLAY".equals(n.getId())) toRemove.add(n);
                }
                groupNotification.getChildren().removeAll(toRemove);
            } catch (Exception ignored) {}
            groupNotification.getChildren().add(pauseOverlay);
        }
    }

    // Pause timelines, update state and notify multiplayer handler
    private void pauseTimelinesAndNotify() {
        if (timeLine != null) timeLine.pause();
        if (clockTimeline != null && clockTimeline.getStatus() == Timeline.Status.RUNNING) {
            pausedElapsedMs = System.currentTimeMillis() - startTimeMs;
            clockTimeline.pause();
        }
        isPause.setValue(Boolean.TRUE);
        isPauseOverlayVisible = true;
        if (!suppressMultiplayerPauseNotify && multiplayerPauseHandler != null) {
            try { multiplayerPauseHandler.accept(Boolean.TRUE); } catch (Exception ex) { System.err.println("[GuiController] multiplayerPauseHandler threw: " + ex); }
        }
    }

    private void hidePauseOverlay() {
        javafx.application.Platform.runLater(() -> {
            try {
                // remove any existing global pause overlays from the scene root or groupNotification
                Scene scene = gameBoard.getScene();
                if (scene != null && scene.getRoot() instanceof javafx.scene.layout.Pane) {
                    javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) scene.getRoot();
                    java.util.List<javafx.scene.Node> toRemove = new java.util.ArrayList<>();
                    for (javafx.scene.Node n : root.getChildren()) {
                        if (n != null && "GLOBAL_PAUSE_OVERLAY".equals(n.getId())) toRemove.add(n);
                    }
                    root.getChildren().removeAll(toRemove);
                }
                if (groupNotification != null) {
                    java.util.List<javafx.scene.Node> toRemove2 = new java.util.ArrayList<>();
                    for (javafx.scene.Node n : groupNotification.getChildren()) {
                        if (n != null && "GLOBAL_PAUSE_OVERLAY".equals(n.getId())) toRemove2.add(n);
                    }
                    groupNotification.getChildren().removeAll(toRemove2);
                }
                pauseOverlay = null;
                // resume timeline and input
                if (timeLine != null) timeLine.play();
                isPause.setValue(Boolean.FALSE);
                isPauseOverlayVisible = false;
                // notify multiplayer coordinator (if any) that this player resumed
                if (!suppressMultiplayerPauseNotify && multiplayerPauseHandler != null) {
                    try { multiplayerPauseHandler.accept(Boolean.FALSE); } catch (Exception ex) { System.err.println("[GuiController] multiplayerPauseHandler threw: " + ex); }
                }
                startClock();
                gamePanel.requestFocus();
            } catch (Exception ignored) {}
        });
    }

    private void detachSceneKeyHandlers() {
        try {
            javafx.application.Platform.runLater(() -> {
                
                try {
                    if (gamePanel != null && gamePanel.getScene() != null) {
                        javafx.scene.Scene s = gamePanel.getScene();
                        
                        if (globalPressHandler != null) s.removeEventHandler(KeyEvent.KEY_PRESSED, globalPressHandler);
                        if (globalReleaseHandler != null) s.removeEventHandler(KeyEvent.KEY_RELEASED, globalReleaseHandler);
                        if (escHandler != null) s.removeEventHandler(KeyEvent.KEY_PRESSED, escHandler);
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

    private void processKeyPressed(KeyEvent keyEvent) {
        try { if (this instanceof CoopGuiController) return; } catch (Exception ignored) {}

        if (isPause.getValue() == Boolean.FALSE && isGameOver.getValue() == Boolean.FALSE) {
            KeyCode code = keyEvent.getCode();
            boolean handled = false;

            if ((ctrlMoveLeft != null && code == ctrlMoveLeft) || (ctrlMoveLeft == null && (code == KeyCode.LEFT || code == KeyCode.A))) {
                refreshBrick(eventListener.onLeftEvent(new MoveEvent(EventType.LEFT, EventSource.USER)));
                handled = true;
            }
            else if ((ctrlMoveRight != null && code == ctrlMoveRight) || (ctrlMoveRight == null && (code == KeyCode.RIGHT || code == KeyCode.D))) {
                refreshBrick(eventListener.onRightEvent(new MoveEvent(EventType.RIGHT, EventSource.USER)));
                handled = true;
            }
            else if ((ctrlRotate != null && code == ctrlRotate) || (ctrlRotate == null && (code == KeyCode.UP || code == KeyCode.W))) {
                refreshBrick(eventListener.onRotateEvent(new MoveEvent(EventType.ROTATE, EventSource.USER)));
                handled = true;
            }
            else if ((ctrlSoftDrop != null && code == ctrlSoftDrop) || (ctrlSoftDrop == null && (code == KeyCode.DOWN || code == KeyCode.S))) {
                if (timeLine != null) timeLine.setRate(SOFT_DROP_RATE);
                moveDown(new MoveEvent(EventType.DOWN, EventSource.USER));
                handled = true;
            }
            else if ((ctrlHardDrop != null && code == ctrlHardDrop) || (ctrlHardDrop == null && (code == KeyCode.SPACE || code == KeyCode.SHIFT))) {
                if (hardDropAllowed) {
                    lastWasHardDrop = true;
                    hardDrop();
                    handled = true;
                } else {
                }
            }
            if (!handled && ctrlSwap != null && code == ctrlSwap) {
                try { if (eventListener != null) eventListener.onSwapEvent(); } catch (Exception ignored) {}
                handled = true;
            }
            if (handled) keyEvent.consume();
        }
        if (keyEvent.getCode() == KeyCode.N) {
            newGame(null);
        }
    }

    private void hardDrop() {
        if (isPause.getValue() == Boolean.FALSE && isGameOver.getValue() == Boolean.FALSE && eventListener != null) {
            // temporarily speed up timeline to normal while performing hard drop
            if (timeLine != null) timeLine.pause();
            // capture starting view for the visual effect
            ViewData startViewForEffect = this.currentViewData;
            int safety = 0;
            while (safety++ < 1000) { // safety limit
                DownData d = eventListener.onDownEvent(new MoveEvent(EventType.DOWN, EventSource.USER));
                if (d == null) break;
                ViewData v = d.getViewData();
                // apply any clear row notifications
                if (d.getClearRow() != null && d.getClearRow().getLinesRemoved() > 0) {
                    NotificationPanel notificationPanel = new NotificationPanel("+" + d.getClearRow().getScoreBonus());
                    groupNotification.getChildren().add(notificationPanel);
                    notificationPanel.showScore(groupNotification.getChildren());
                }
                refreshBrick(v);
                    // if clearRow is non-null it means the piece could not move and was merged -> landing occurred
                    if (d.getClearRow() != null) {
                            // play an intense hard-drop/lock visual effect only when user explicitly hard-dropped
                            try {
                                if (lastWasHardDrop) {
                                    // play hard-drop sound and intense lock visual
                                    try { playHardDropSound(); } catch (Exception ignored) {}
                                    playLockEffect(startViewForEffect, d.getViewData(), true);
                                }
                            } catch (Exception ignored) {}
                        // reset the flag after use
                        lastWasHardDrop = false;
                        if (d.getClearRow().getLinesRemoved() > 0) {
                            try { spawnExplosion(d.getClearRow(), d.getViewData()); } catch (Exception ignored) {}
                        }
                        break;
                    }
            }
            if (timeLine != null) timeLine.play();
        }
    }

    private void processKeyReleased(KeyEvent keyEvent) {
        // CoopGuiController manages its own key filters; avoid handling releases here.
        try { if (this instanceof CoopGuiController) return; } catch (Exception ignored) {}

        KeyCode code = keyEvent.getCode();
        boolean hasCustom = (ctrlSoftDrop != null);
        if (hasCustom) {
            if (code == ctrlSoftDrop) {
                if (timeLine != null) timeLine.setRate(NORMAL_RATE);
                keyEvent.consume();
            }
        } else {
            if (code == KeyCode.DOWN || code == KeyCode.S) {
                if (timeLine != null) timeLine.setRate(NORMAL_RATE);
                keyEvent.consume();
            }
        }
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
        // initial ghost render - defer to run after measurement so baseOffset/cell size are initialized
        javafx.application.Platform.runLater(() -> updateGhost(brick, boardMatrix));
    }

    public void setMultiplayerExitToMenuHandler(Runnable handler) {
        this.multiplayerExitToMenuHandler = handler;
    }

    public void startCountdown(int seconds) {
        // delegate to CountdownUI using a small context object
        try { prevHighBeforeGame = highScore; } catch (Exception ignored) {}
        GUICountdownContext ctx = new GUICountdownContext();
        ctx.gameBoard = this.gameBoard;
        ctx.brickPanel = this.brickPanel;
        ctx.ghostPanel = this.ghostPanel;
        ctx.groupNotification = this.groupNotification;
        ctx.timeLine = this.timeLine;
        ctx.resetClock = this::resetClock;
        ctx.startClock = this::startClock;
        ctx.isPause = this.isPause;
        ctx.countdownFinished = this.countdownFinished;
        ctx.countdownStarted = this.countdownStarted;
        ctx.currentViewData = this.currentViewData;
        ctx.currentBoardMatrix = this.currentBoardMatrix;
        ctx.gamePanelNode = this.gamePanel;
        ctx.playCountdownMusic = this::playCountdownMusic;
        ctx.stopCountdownMusic = this::stopCountdownMusic;
        // wire panel/refresh callbacks so the countdown can hide visuals and restore them after finishing
        ctx.refreshAndSnap = this::refreshAndSnapBrickAsync;
        ctx.hidePanels = this::hideBrickAndGhostPanelsAsync;
        ctx.showPanels = () -> javafx.application.Platform.runLater(() -> {
            try { if (brickPanel != null) brickPanel.setVisible(true); } catch (Exception ignored) {}
            try { if (ghostPanel != null) ghostPanel.setVisible(true); } catch (Exception ignored) {}
        });
        ctx.refreshVisible = () -> {
            try { if (currentViewData != null) doRefreshBrick(currentViewData); } catch (Exception ignored) {}
        };

        Timeline cd = GUICountdown.startCountdown(seconds, ctx);
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
        if (boardView != null) {
            boardView.updateGhost(brick, boardMatrix);
            return;
        }
        if (brick == null || boardMatrix == null) return;
        int startX = brick.getxPosition();
        int startY = brick.getyPosition();
        int[][] shape = brick.getBrickData();
        int landingY = startY;
        int effectiveBrickHeight = shape.length;
        for (int i = shape.length - 1; i >= 0; i--) {
            boolean rowHas = false;
            for (int j = 0; j < shape[i].length; j++) {
                if (shape[i][j] != 0) { rowHas = true; break; }
            }
            if (rowHas) { effectiveBrickHeight = i + 1; break; }
        }
        // The maximum y we can try is board rows - effective brick height (so brick bottom doesn't go past board)
        int maxY = boardMatrix.length - effectiveBrickHeight;
        for (int y = startY; y <= maxY; y++) {
            boolean conflict = MatrixOperations.intersectForGhost(boardMatrix, shape, startX, y);
            if (conflict) {
                // conflict detected while computing ghost landing position
                landingY = y - 1;
                break;
            }
            // if no conflict all the way to maxY, land at maxY
            if (y == maxY) landingY = y;
        }

        javafx.geometry.Point2D pt = boardToPixel(startX, landingY - 2);
            // convert board-local point into ghostPanel parent's local coords so ghost aligns with brickPanel
            try {
                javafx.geometry.Point2D scenePt = (gamePanel != null && gamePanel.getParent() != null) ? gamePanel.localToScene(pt) : pt;
                if (ghostPanel != null && ghostPanel.getParent() != null) {
                    javafx.geometry.Point2D parentLocal = ghostPanel.getParent().sceneToLocal(scenePt);
                    ghostPanel.setTranslateX(Math.round(parentLocal.getX()));
                    ghostPanel.setTranslateY(Math.round(parentLocal.getY()));
                } else {
                    ghostPanel.setTranslateX(Math.round(pt.getX()));
                    ghostPanel.setTranslateY(Math.round(pt.getY()));
                }
            } catch (Exception ex) {
                ghostPanel.setTranslateX(Math.round(pt.getX()));
                ghostPanel.setTranslateY(Math.round(pt.getY()));
            }
    
        for (int i = 0; i < shape.length; i++) {
            for (int j = 0; j < shape[i].length; j++) {
                Rectangle r = ghostRectangles[i][j];
                if (r == null) continue;
                if (shape[i][j] == 0) {
                    r.setVisible(false);
                    continue;
                }
                int boardY = landingY + i; // absolute board row
                int boardX = startX + j;
                boolean visible = true;
                if (boardX < 0 || boardX >= boardMatrix[0].length) visible = false;
                if (boardY < 2) visible = false;
                if (boardY >= 0 && boardY < boardMatrix.length && boardMatrix[boardY][boardX] != 0) visible = false;
                r.setVisible(visible);
            }
        }
    }

    public void showNextBricks(java.util.List<com.comp2042.logic.Brick> upcoming) {
    if (nextContent == null) return;
    nextContent.getChildren().clear();
        if (upcoming == null) return;
        upcomingCache = new java.util.ArrayList<>(upcoming);
        javafx.scene.layout.VBox built = buildNextPreview(upcoming);
        if (built != null) {
            nextContent.getChildren().addAll(built.getChildren());
        }
    }

    public javafx.scene.layout.VBox buildNextPreview(java.util.List<com.comp2042.logic.Brick> upcoming) {
        javafx.scene.layout.VBox container = new javafx.scene.layout.VBox(8);
        container.setAlignment(Pos.TOP_CENTER);
        if (upcoming == null || upcoming.isEmpty()) return container;

        double pW = Math.max(4.0, cellW);
        double pH = Math.max(4.0, cellH);

        int count = Math.min(upcoming.size(), 3);
        for (int i = 0; i < count; i++) {
            com.comp2042.logic.Brick b = upcoming.get(i);
            int[][] shape = b.getShapeMatrix().get(0); // default orientation for preview
            int rows = shape.length;
            int cols = shape[0].length;
            javafx.scene.layout.StackPane slot = new javafx.scene.layout.StackPane();
            slot.setPrefWidth(cols * pW + 8.0);
            slot.setPrefHeight(rows * pH + 8.0);
            slot.setStyle("-fx-background-color: transparent;");

            int minR = Integer.MAX_VALUE, minC = Integer.MAX_VALUE, maxR = Integer.MIN_VALUE, maxC = Integer.MIN_VALUE;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (shape[r][c] != 0) {
                        if (r < minR) minR = r;
                        if (c < minC) minC = c;
                        if (r > maxR) maxR = r;
                        if (c > maxC) maxC = c;
                    }
                }
            }
            if (minR == Integer.MAX_VALUE) {
                minR = 0; minC = 0; maxR = rows - 1; maxC = cols - 1;
            }
            int visibleCols = maxC - minC + 1;
            int visibleRows = maxR - minR + 1;

            Pane inner = new Pane();
            inner.setPrefWidth(visibleCols * pW);
            inner.setPrefHeight(visibleRows * pH);
            inner.setMinWidth(visibleCols * pW);
            inner.setMinHeight(visibleRows * pH);
            inner.setMaxWidth(visibleCols * pW);
            inner.setMaxHeight(visibleRows * pH);

            for (int r = minR; r <= maxR; r++) {
                for (int c = minC; c <= maxC; c++) {
                    int val = shape[r][c];
                    if (val == 0) continue;
                    Rectangle rect = new Rectangle(pW, pH);
                    rect.setFill(BoardView.mapCodeToPaint(val));
                    rect.setLayoutX((c - minC) * pW);
                    rect.setLayoutY((r - minR) * pH);
                    rect.setArcHeight(6);
                    rect.setArcWidth(6);
                    inner.getChildren().add(rect);
                }
            }
            slot.getChildren().add(inner);
            container.getChildren().add(slot);
        }
        return container;
    }

    // color mapping delegated to BoardView.mapCodeToPaint(int)

    private void refreshBrick(ViewData brick) {
        this.currentViewData = brick;
        if (isPause.getValue() == Boolean.FALSE) {
            if (boardView != null) boardView.refreshBrick(brick); else doRefreshBrick(brick);
        }
    }

    private void doRefreshBrick(ViewData brick) {
        if (boardView != null) { boardView.refreshBrick(brick); return; }
        if (brick == null) return;
        int offsetX = brick.getxPosition();
        int offsetY = brick.getyPosition() - 2;

        javafx.geometry.Point2D pt = boardToPixel(offsetX, offsetY);
        // convert board-local point into brickPanel parent's local coords (keep fallback behavior)
        try {
            javafx.geometry.Point2D scenePt = (gamePanel != null && gamePanel.getParent() != null) ? gamePanel.localToScene(pt) : pt;
            if (brickPanel != null && brickPanel.getParent() != null) {
                javafx.geometry.Point2D parentLocal = brickPanel.getParent().sceneToLocal(scenePt);
                brickPanel.setTranslateX(Math.round(parentLocal.getX()));
                brickPanel.setTranslateY(Math.round(parentLocal.getY()));
            } else {
                brickPanel.setTranslateX(Math.round(pt.getX()));
                brickPanel.setTranslateY(Math.round(pt.getY()));
            }
        } catch (Exception ex) {
            brickPanel.setTranslateX(Math.round(pt.getX()));
            brickPanel.setTranslateY(Math.round(pt.getY()));
        }

        int[][] data = brick.getBrickData();
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                Rectangle r = rectangles[i][j];
                int val = data[i][j];
                setRectangleData(val, r);
                r.setVisible(val != 0);
                r.setLayoutX(Math.round(j * cellW));
                r.setLayoutY(Math.round(i * cellH));
            }
        }

        updateGhost(brick, currentBoardMatrix);
    }

    private javafx.geometry.Point2D boardToPixel(int boardX, int boardY) {
        if (boardView != null) return boardView.boardToPixel(boardX, boardY);
        double x = baseOffsetX + (boardX * cellW);
        double y = baseOffsetY + (boardY * cellH);
        return new javafx.geometry.Point2D(x, y);
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
        rectangle.setFill(BoardView.mapCodeToPaint(color));
        rectangle.setArcHeight(9);
        rectangle.setArcWidth(9);
    }

    private void attachButtonSoundHandlers(javafx.scene.control.Button btn) {
        if (btn == null) return;
        btn.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> { if (soundManager != null) { try { soundManager.playHoverSound(); } catch (Exception ignored) {} } });
        // mouse press for immediate click feedback
        btn.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> { if (soundManager != null) { try { soundManager.playClickSound(); } catch (Exception ignored) {} } });
        // also listen for action events (keyboard activation)
        btn.addEventHandler(javafx.event.ActionEvent.ACTION, e -> { if (soundManager != null) { try { soundManager.playClickSound(); } catch (Exception ignored) {} } });
    }

    protected void playHardDropSound() {
        try { if (soundManager != null) soundManager.playHardDropSound(); } catch (Exception ignored) {}
    }

    private void playGameOverMusic() {
        if (isMultiplayer) return;
        try {
            if (soundManager != null) soundManager.playGameOverMusic();
        } catch (Exception ex) {
            System.err.println("[GuiController] Exception while playing GameOver music: " + ex);
            ex.printStackTrace();
        }
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


    private void moveDown(MoveEvent event) {
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

    // Spawn explosions for cleared rows. If ClearRow contains explicit cleared row indices,
    protected void spawnExplosion(ClearRow clearRow, ViewData v) {
        if (particlePane == null) return;
        try {
            if (clearRow != null && clearRow.getLinesRemoved() > 0) {
                int[] rows = clearRow.getClearedRows();
                if (rows != null && rows.length > 0) {
                    // visual flash per cleared row and a board shake
                    BoardView bv = getBoardView();
                    for (int r : rows) {
                        if (bv != null) {
                            // get the scene point of the left-most cell of this row and convert into particlePane local space
                            javafx.geometry.Point2D sceneLeft = bv.boardCellScenePoint(0, r);
                            javafx.geometry.Point2D leftLocal = (particlePane != null && sceneLeft != null) ? particlePane.sceneToLocal(sceneLeft) : new javafx.geometry.Point2D(0,0);
                            double width = bv.getCellWidth() * bv.getColumns();
                            flashRowAt(Math.round(leftLocal.getX()), Math.round(leftLocal.getY()), width, bv.getCellHeight());
                        } else {
                            double flashY = Math.round(baseOffsetY + (r - 2) * cellH);
                            // convert flash position from board (parent) coords into particlePane local coords
                            javafx.geometry.Point2D flashLocal = boardCoordsToParticleLocal(Math.round(baseOffsetX), flashY);
                            flashRowAt(Math.round(flashLocal.getX()), Math.round(flashLocal.getY()), cellW * displayMatrix[0].length, cellH);
                        }
                    }
                    // shake the board once when rows are removed
                    shakeBoard();
                    // spawn per-cell falling square particles for each cleared cell
                    try { spawnRowClearParticles(clearRow); } catch (Exception ignored) {}
                    // spawn a burst for each cleared row using its center in particlePane local coords
                    for (int r : rows) {
                        if (bv != null) {
                            int midCol = Math.max(0, bv.getColumns() / 2);
                            javafx.geometry.Point2D sceneCenter = bv.boardCellScenePoint(midCol, r);
                            javafx.geometry.Point2D centerLocal = (particlePane != null && sceneCenter != null) ? particlePane.sceneToLocal(sceneCenter) : new javafx.geometry.Point2D(0,0);
                            spawnParticlesAt(centerLocal.getX(), centerLocal.getY(), v != null ? v.getBrickData() : null);
                        } else {
                            double centerY = Math.round(baseOffsetY + (r - 2 + 0.5) * cellH); // center of the row in visible coords
                            double centerX = Math.round(baseOffsetX + (displayMatrix[0].length * 0.5) * cellW);
                            javafx.geometry.Point2D centerLocal = boardCoordsToParticleLocal(centerX, centerY);
                            spawnParticlesAt(centerLocal.getX(), centerLocal.getY(), v != null ? v.getBrickData() : null);
                        }
                    }
                    return;
                }
            }
        } catch (Exception ignored) {}
        // fallback: spawn at brick landing position
        spawnExplosion(v);
    }

    // Convert coordinates expressed in the board's parent coordinate space into particlePane local coords
    private javafx.geometry.Point2D boardCoordsToParticleLocal(double x, double y) {
        javafx.geometry.Point2D parentPt = new javafx.geometry.Point2D(x, y);
        javafx.geometry.Point2D scenePt = (brickPanel != null && brickPanel.getParent() != null)
                ? brickPanel.getParent().localToScene(parentPt)
                : parentPt;
        return (particlePane != null) ? particlePane.sceneToLocal(scenePt) : scenePt;
    }

    // Show a brief flash rectangle using coordinates already in particlePane local space
    protected void flashRowAt(double leftXLocal, double topYLocal, double width, double height) {
        if (particlePane == null) return;
        try {
            ParticleHelper.flashRowAt(particlePane, leftXLocal, topYLocal, width, height);
        } catch (Exception ignored) {}
    }

    // Spawn small square particles for each brick in the cleared rows that then fall down and fade out.
    protected void spawnRowClearParticles(ClearRow clearRow) {
        try {
            ParticleHelper.spawnRowClearParticles(particlePane, clearRow, displayMatrix, getBoardView(), baseOffsetX, baseOffsetY, cellW, cellH, (gameBoard != null) ? gameBoard.getScene() : null);
        } catch (Exception ignored) {}
    }

    // Show a brief flash rectangle at vertical position y (top of the row) with given width/height
    protected void flashRow(double topY, double width, double height) {
        if (particlePane == null) return;
        try {
            ParticleHelper.flashRow(particlePane, baseOffsetX, topY, width, height);
        } catch (Exception ignored) {}
    }

    // Briefly shake the gameBoard to emphasize row clear.
    protected void shakeBoard() {
        if (gameBoard == null) return;
        try {
            final int magnitude = 8; // px
            final int shakes = 6;
            Timeline t = new Timeline();
            for (int i = 0; i < shakes; i++) {
                int dir = (i % 2 == 0) ? 1 : -1;
                KeyFrame kf = new KeyFrame(Duration.millis( (i * 30) ),
                        new javafx.event.EventHandler<javafx.event.ActionEvent>() {
                            @Override
                            public void handle(javafx.event.ActionEvent event) {
                                gameBoard.setTranslateX(dir * magnitude);
                            }
                        }
                );
                t.getKeyFrames().add(kf);
            }
            // final frame: reset to 0
            t.getKeyFrames().add(new KeyFrame(Duration.millis(shakes * 30), new javafx.event.EventHandler<javafx.event.ActionEvent>() {
                @Override
                public void handle(javafx.event.ActionEvent event) {
                    gameBoard.setTranslateX(0);
                }
            }));
            t.play();
        } catch (Exception ignored) {}
    }

    // compatibility fallback: spawn at the piece's landing position (old behaviour)
    protected void spawnExplosion(ViewData v) {
        if (v == null || particlePane == null) return;
        int brickX = v.getxPosition();
        int brickY = v.getyPosition() - 2; // visible offset
        double centerX = Math.round(baseOffsetX + brickX * cellW + (cellW * 2) );
        double centerY = Math.round(baseOffsetY + brickY * cellH + (cellH * 2) );
        spawnParticlesAt(centerX, centerY, v.getBrickData());
    }

    protected void spawnParticlesAt(double centerX, double centerY, int[][] brickShape) {
        try {
            ParticleHelper.spawnParticlesAt(particlePane, centerX, centerY, brickShape);
        } catch (Exception ignored) {}
    }

    public void setEventListener(InputEventListener eventListener) {
        this.eventListener = eventListener;
    }

    public void setMultiplayerMode(boolean multiplayer) {
        this.isMultiplayer = multiplayer;
        // If switching into multiplayer, ensure any singleplayer music is stopped to avoid overlap
        if (multiplayer) {
            try { stopSingleplayerMusic(); } catch (Exception ignored) {}
        }
    }

    protected boolean shouldStartSingleplayerMusic() {
        return !isMultiplayer;
    }

    public void setHardDropEnabled(boolean enabled) {
        this.hardDropAllowed = enabled;
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

    public KeyCode getCtrlMoveLeft() { return this.ctrlMoveLeft; }
    public KeyCode getCtrlMoveRight() { return this.ctrlMoveRight; }
    public KeyCode getCtrlRotate() { return this.ctrlRotate; }
    public KeyCode getCtrlSoftDrop() { return this.ctrlSoftDrop; }
    public KeyCode getCtrlHardDrop() { return this.ctrlHardDrop; }
    public KeyCode getCtrlSwap() { return this.ctrlSwap; }

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

        loadHighScore();
        if (highScoreValue != null) {
            highScoreValue.setText("Highest: " + highScore);
        }
         scoreValue.textProperty().bind(Bindings.createStringBinding(
                 () -> "Current: " + integerProperty.get(),
                 integerProperty
         ));
 
            integerProperty.addListener((obs, oldV, newV) -> {
                java.util.Objects.requireNonNull(obs);
                java.util.Objects.requireNonNull(oldV);
             int current = newV.intValue();
             if (current > highScore) {
                highScore = current;
                 saveHighScore();
                 if (highScoreValue != null) {
                     highScoreValue.setText("Highest: " + highScore);
                     try {
                         ScaleTransition st = new ScaleTransition(Duration.millis(200), highScoreValue);
                         st.setFromX(1.0);
                         st.setFromY(1.0);
                         st.setToX(1.25);
                         st.setToY(1.25);
                         st.setCycleCount(2);
                         st.setAutoReverse(true);
                         st.play();
                     } catch (Exception ignored) {}
                 }
             }
         });
    }

    private void loadHighScore() {
        Path p = Paths.get(HIGHSCORE_FILE);
        if (Files.exists(p)) {
            try {
                String s = Files.readString(p, StandardCharsets.UTF_8).trim();
                highScore = Integer.parseInt(s);
            } catch (Exception ignored) {
                highScore = 0;
            }
        } else {
            highScore = 0;
        }
    }

    private void saveHighScore(){
        Path p = Paths.get(HIGHSCORE_FILE);
        try {
            Files.writeString(p, Integer.toString(highScore), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }

    public void gameOver() {
        // Prevent duplicate game-over handling if already in game-over state
        try { if (Boolean.TRUE.equals(isGameOver.getValue())) return; } catch (Exception ignored) {}

        try { if (timeLine != null) timeLine.stop(); } catch (Exception ignored) {}

        try { if (gameOverPanel != null) gameOverPanel.setVisible(false); } catch (Exception ignored) {}
        isGameOver.setValue(Boolean.TRUE);
        stopClock();
        if (isMultiplayer) return;

        javafx.application.Platform.runLater(() -> {
            try {
                if (gameBoard == null || gameBoard.getScene() == null) return;
                javafx.scene.Scene scene = gameBoard.getScene();

                StackPane overlay = buildGameOverOverlay(scene);

                if (scene.getRoot() instanceof javafx.scene.layout.Pane) {
                    javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) scene.getRoot();
                    root.getChildren().add(overlay);
                } else if (groupNotification != null) {
                    groupNotification.getChildren().add(overlay);
                }

                // subtle entrance animation for dialog node (stored in overlay properties)
                Object dlg = overlay.getProperties().get("dialogNode");
                if (dlg instanceof javafx.scene.Node) {
                    javafx.scene.Node dialog = (javafx.scene.Node) dlg;
                    javafx.animation.FadeTransition f = new javafx.animation.FadeTransition(Duration.millis(420), dialog);
                    dialog.setOpacity(0.0);
                    f.setFromValue(0.0);
                    f.setToValue(1.0);
                    f.play();
                }
            } catch (Exception ignored) {}
        });
    }

    /**
     * Build the game-over overlay (dialog + dark background) and wire button handlers.
     * The returned overlay has the dialog node stored in overlay.getProperties().get("dialogNode").
     */
    private StackPane buildGameOverOverlay(Scene scene) {
        // Main overlay container
        StackPane overlay = new StackPane();
        try { overlay.setPickOnBounds(true); } catch (Exception ignored) {}
        try { overlay.setStyle("-fx-background-color: transparent;"); } catch (Exception ignored) {}

        // dark background
        Rectangle dark = new Rectangle();
        try { dark.widthProperty().bind(scene.widthProperty()); dark.heightProperty().bind(scene.heightProperty()); } catch (Exception ignored) {}
        try { dark.setFill(Color.rgb(0,0,0,0.95)); } catch (Exception ignored) {}

        // dialog and title
        VBox dialog = new VBox(14);
        try { dialog.setAlignment(Pos.CENTER); dialog.setMouseTransparent(false); dialog.setStyle("-fx-background-color: rgba(0,0,0,1.0); -fx-padding: 18px; -fx-background-radius: 8px;"); } catch (Exception ignored) {}

        Text title = createGameOverTitle();
        startGameOverPulse(title);

        // compute score/time values (kept inline for clarity)
        String scoreStr = "";
        int currentScore = -1;
        try {
            if (currentScoreProperty != null) currentScore = currentScoreProperty.get();
            if (currentScore < 0 && scoreValue != null) {
                String s = scoreValue.getText();
                if (s != null) {
                    int idx = s.lastIndexOf(':');
                    if (idx >= 0 && idx + 1 < s.length()) {
                        String num = s.substring(idx + 1).trim();
                        try { currentScore = Integer.parseInt(num); } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
        if (currentScore >= 0) scoreStr = Integer.toString(currentScore);
        String timePlayed = (timeValue != null) ? timeValue.getText() : "00:00";

        VBox subtitleBox = buildGameOverSubtitleBox(scoreStr, currentScore, timePlayed);

        HBox buttons = buildGameOverButtons(overlay, scene, title);

        buttons.getChildren().addAll(); // ensure non-null
        dialog.getChildren().addAll(title, subtitleBox, buttons);
        dialog.setTranslateY(0);

        overlay.setOnMouseClicked(event -> event.consume());
        overlay.getChildren().addAll(dark, dialog);

        // store dialog node for later animations
        try { overlay.getProperties().put("dialogNode", dialog); } catch (Exception ignored) {}

        return overlay;
    }

    // Create and style the title text for game-over dialog
    private Text createGameOverTitle() {
        Text title = new Text("Match Over");
        try { title.setStyle("-fx-font-weight: 700;"); } catch (Exception ignored) {}
        try { title.setFill(javafx.scene.paint.LinearGradient.valueOf("from 0% 0% to 100% 0% , #ffd166 0%, #ff7b7b 100%")); } catch (Exception ignored) {}
        try { title.setOpacity(1.0); } catch (Exception ignored) {}
        try { title.setFont(Font.font(72)); } catch (Exception ignored) {}
        return title;
    }

    // Build the glowing/animating pulse used by the game-over title
    private void startGameOverPulse(Text title) {
        try {
            DropShadow glow = new DropShadow();
            glow.setColor(javafx.scene.paint.Color.web("#ffd166"));
            glow.setRadius(10);
            glow.setSpread(0.45);
            title.setEffect(glow);

            javafx.animation.ScaleTransition scalePulse = new javafx.animation.ScaleTransition(Duration.millis(900), title);
            scalePulse.setFromX(1.0); scalePulse.setFromY(1.0);
            scalePulse.setToX(1.05); scalePulse.setToY(1.05);
            scalePulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
            scalePulse.setAutoReverse(true);

            javafx.animation.Timeline glowTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.ZERO, new javafx.animation.KeyValue(glow.radiusProperty(), 10)),
                new javafx.animation.KeyFrame(Duration.millis(900), new javafx.animation.KeyValue(glow.radiusProperty(), 36))
            );
            glowTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
            glowTimeline.setAutoReverse(true);

            javafx.animation.Timeline colorPulse = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.ZERO, ae -> { ae.consume(); title.setFill(javafx.scene.paint.Color.WHITE); }),
                new javafx.animation.KeyFrame(Duration.millis(420), ae -> { ae.consume(); title.setFill(javafx.scene.paint.Color.web("#ffd166")); }),
                new javafx.animation.KeyFrame(Duration.millis(900), ae -> { ae.consume(); title.setFill(javafx.scene.paint.Color.WHITE); })
            );
            colorPulse.setCycleCount(javafx.animation.Animation.INDEFINITE);

            javafx.animation.ParallelTransition combined = new javafx.animation.ParallelTransition(scalePulse, glowTimeline, colorPulse);
            combined.setCycleCount(javafx.animation.Animation.INDEFINITE);
            gameOverPulse = combined;
            combined.play();
        } catch (Exception ignored) {}
    }

    // Build subtitle (score/time/compare) box
    private VBox buildGameOverSubtitleBox(String scoreStr, int currentScore, String timePlayed) {
        Text scoreText = new Text("Score: " + (scoreStr.isEmpty() ? (scoreValue != null ? scoreValue.getText() : "0") : scoreStr));
        try { scoreText.setStyle("-fx-font-size: 28px; -fx-fill: white; -fx-opacity: 0.95;"); } catch (Exception ignored) {}
        Text timeText = new Text("Time: " + timePlayed);
        try { timeText.setStyle("-fx-font-size: 22px; -fx-fill: #dddddd; -fx-opacity: 0.95;"); } catch (Exception ignored) {}

        Text compareText = new Text();
        try {
            if (prevHighBeforeGame <= 0) {
                compareText.setText("No previous record");
                compareText.setStyle("-fx-font-size: 18px; -fx-fill: #cccccc;");
            } else if (currentScore >= 0 && currentScore > prevHighBeforeGame) {
                compareText.setText("New High Score! Previous: " + prevHighBeforeGame);
                compareText.setStyle("-fx-font-size: 20px; -fx-fill: #ffd166; -fx-font-weight: bold;");
            } else {
                compareText.setText("Previous Best: " + prevHighBeforeGame);
                compareText.setStyle("-fx-font-size: 18px; -fx-fill: #cccccc;");
            }
        } catch (Exception ignored) { compareText.setText(""); }

        VBox subtitleBox = new VBox(6);
        try { subtitleBox.setAlignment(Pos.CENTER); subtitleBox.getChildren().addAll(scoreText, timeText, compareText); } catch (Exception ignored) {}
        return subtitleBox;
    }

    // Build restart/menu buttons and wire handlers (needs overlay and scene context)
    private HBox buildGameOverButtons(StackPane overlay, Scene scene, Text title) {
        HBox buttons = new HBox(12);
        try { buttons.setAlignment(Pos.CENTER); } catch (Exception ignored) {}

        Button btnRestart = new Button("Restart");
        Button btnMenu = new Button("Main Menu");
        try { btnRestart.getStyleClass().add("menu-button"); btnMenu.getStyleClass().add("menu-button"); } catch (Exception ignored) {}
        try { attachButtonSoundHandlers(btnRestart); } catch (Exception ignored) {}
        try { attachButtonSoundHandlers(btnMenu); } catch (Exception ignored) {}

        // Restart handler
        btnRestart.setOnAction(ev -> {
            ev.consume();
            try {
                try { stopGameOverMusic(); } catch (Exception ignored) {}
                try { stopCountdownMusic(); } catch (Exception ignored) {}
                try { if (gameOverPulse != null) { gameOverPulse.stop(); gameOverPulse = null; } } catch (Exception ignored) {}
                if (overlay.getParent() instanceof javafx.scene.layout.Pane) {
                    ((javafx.scene.layout.Pane) overlay.getParent()).getChildren().remove(overlay);
                } else if (groupNotification != null) {
                    groupNotification.getChildren().remove(overlay);
                }
            } catch (Exception ignored) {}

            if (isMultiplayer && multiplayerRestartHandler != null) {
                try { multiplayerRestartHandler.run(); } catch (Exception ignored) {}
                return;
            }

            try { if (eventListener != null) eventListener.createNewGame(); } catch (Exception ignored) {}
            try {
                if (timeLine != null) timeLine.stop();
                if (gameOverPanel != null) gameOverPanel.setVisible(false);
                resetClock(); stopClock();
                isPause.setValue(Boolean.TRUE);
                isGameOver.setValue(Boolean.FALSE);
                startCountdown(3);
            } catch (Exception ignored) {}
        });

        // Menu handler
        btnMenu.setOnAction(ev -> {
            ev.consume();
            try {
                try { stopGameOverMusic(); } catch (Exception ignored) {}
                try { stopCountdownMusic(); } catch (Exception ignored) {}
                try { if (gameOverPulse != null) { gameOverPulse.stop(); gameOverPulse = null; } } catch (Exception ignored) {}
                if (isMultiplayer && multiplayerExitToMenuHandler != null) {
                    try { detachSceneKeyHandlers(); } catch (Exception ignored) {}
                    try { multiplayerExitToMenuHandler.run(); } catch (Exception ignored) {}
                    return;
                }
                try { detachSceneKeyHandlers(); } catch (Exception ignored) {}
                try { stopSingleplayerMusic(); } catch (Exception ignored) {}
                URL loc = getClass().getClassLoader().getResource("mainMenu.fxml");
                if (loc == null) return;
                FXMLLoader loader = new FXMLLoader(loc);
                Parent menuRoot = loader.load();
                javafx.stage.Stage stage = (javafx.stage.Stage) scene.getWindow();
                if (stage.getScene() != null) {
                    stage.getScene().setRoot(menuRoot);
                    try {
                        String css = getClass().getClassLoader().getResource("css/menu.css").toExternalForm();
                        if (!stage.getScene().getStylesheets().contains(css)) stage.getScene().getStylesheets().add(css);
                    } catch (Exception ignored) {}
                } else {
                    Scene s2 = new Scene(menuRoot, Math.max(420, stage.getWidth()), Math.max(700, stage.getHeight()));
                    try {
                        String css = getClass().getClassLoader().getResource("css/menu.css").toExternalForm();
                        s2.getStylesheets().add(css);
                    } catch (Exception ignored) {}
                    stage.setScene(s2);
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        buttons.getChildren().addAll(btnRestart, btnMenu);
        return buttons;
    }

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
        resetClock();
        startClock();
    }

    private void startClock() {
        startTimeMs = System.currentTimeMillis() - pausedElapsedMs;
        if (clockTimeline != null) clockTimeline.stop();
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                updateClock();
            }
        }));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    private void stopClock() {
        if (clockTimeline != null) clockTimeline.stop();
    }

    private void resetClock() {
        // reset both the start timestamp and any paused-accumulated time
        pausedElapsedMs = 0;
        startTimeMs = System.currentTimeMillis();
        if (timeValue != null) timeValue.setText("00:00");
    }

    private void updateClock() {
        if (startTimeMs == 0) return;
        long elapsed = System.currentTimeMillis() - startTimeMs;
        long seconds = elapsed / 1000;
        long mins = seconds / 60;
        long secs = seconds % 60;
        if (timeValue != null) {
            timeValue.setText(String.format("%02d:%02d", mins, secs));
        }
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
