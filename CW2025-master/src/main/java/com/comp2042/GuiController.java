package com.comp2042;

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
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Reflection;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.BlurType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.util.Duration;
import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.shape.Circle;
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

    protected java.util.List<com.comp2042.logic.bricks.Brick> upcomingCache = null;
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
        Font.loadFont(getClass().getClassLoader().getResource("digital.ttf").toExternalForm(), 38);
        gamePanel.setFocusTraversable(true);
        gamePanel.requestFocus();

        try {
            soundManager = new SoundManager(getClass());
            soundManager.init();
        } catch (Exception ex) {
            System.err.println("[GuiController] sound manager init failed: " + ex.getMessage());
        }

        // Attach generic sound handlers to commonly-interacted controls (pause button etc.)
        if (pauseBtn != null) attachButtonSoundHandlers(pauseBtn);

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

            if (gamePanel.getScene() != null) {
                Scene s = gamePanel.getScene();
                attachedScene = s;
                s.addEventHandler(KeyEvent.KEY_PRESSED, globalPressHandler);
                s.addEventHandler(KeyEvent.KEY_RELEASED, globalReleaseHandler);
                s.addEventHandler(KeyEvent.KEY_PRESSED, escHandler);
            } else {
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
        gameOverPanel.setVisible(false);

        final Reflection reflection = new Reflection();
        reflection.setFraction(0.8);
        reflection.setTopOpacity(0.9);
        reflection.setTopOffset(-12);

        // Center the gameBoard within the root Pane when the scene is ready
        javafx.application.Platform.runLater(() -> {
                if (gameBoard.getParent() instanceof javafx.scene.layout.Region) {
                    javafx.scene.layout.Region parent = (javafx.scene.layout.Region) gameBoard.getParent();
                    gameBoard.layoutXProperty().bind(parent.widthProperty().subtract(gameBoard.widthProperty()).divide(2));
                    gameBoard.layoutYProperty().bind(parent.heightProperty().subtract(gameBoard.heightProperty()).divide(2));
                } else if (gameBoard.getScene() != null) {
                    // fallback to centering within the Scene
                    gameBoard.layoutXProperty().bind(gameBoard.getScene().widthProperty().subtract(gameBoard.widthProperty()).divide(2));
                    gameBoard.layoutYProperty().bind(gameBoard.getScene().heightProperty().subtract(gameBoard.heightProperty()).divide(2));
                }
            if (scoreBox != null) {
                scoreBox.layoutXProperty().bind(gameBoard.layoutXProperty().subtract(SCORE_BOX_OFFSET_X));
                scoreBox.layoutYProperty().bind(gameBoard.layoutYProperty().add(gameBoard.heightProperty().subtract(SCORE_BOX_OFFSET_FROM_BOTTOM)));
            }

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

            if (scoreValue != null) {
                scoreValue.getStyleClass().remove("scoreClass");
                scoreValue.getStyleClass().add("highScoreClass");
            }

            if (groupNotification.getParent() != null && gameBoard.getParent() != null) {
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
            // Position the nextBox to the right outside the gameBoard frame
            if (nextBox != null && gameBoard != null) {
                // horizontal offset: small gap outside the frame
                final double outsideGap = 70.0;
                nextBox.layoutXProperty().bind(
                    javafx.beans.binding.Bindings.createDoubleBinding(
                        () -> gameBoard.getLayoutX() + gameBoard.getWidth() + outsideGap,
                        gameBoard.layoutXProperty(), gameBoard.widthProperty()
                    )
                );

                // vertically align top of nextBox slightly below the top of the gameBoard
                nextBox.layoutYProperty().bind(
                    javafx.beans.binding.Bindings.createDoubleBinding(
                        () -> gameBoard.getLayoutY() + 8.0,
                        gameBoard.layoutYProperty(), gameBoard.heightProperty()
                    )
                );
                // bind nextBoxFrame to nextBox
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
            // Position the levelBox to the right bottom near nextBox
            if (levelBox != null && gameBoard != null) {
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
        });

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
                    // create overlay root
                    pauseOverlay = new StackPane();
                    pauseOverlay.setId("GLOBAL_PAUSE_OVERLAY");
                    pauseOverlay.setPickOnBounds(true);

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
                    // attach hover/click sound handlers
                    attachButtonSoundHandlers(resume);
                    attachButtonSoundHandlers(settings);

                    resume.setOnAction(ev -> {
                        ev.consume();
                        hidePauseOverlay();
                    });

                    settings.setOnAction(ev -> {
                        ev.consume();
                        try {
                            if (multiplayerRequestControlsHandler != null) {
                                try {
                                    multiplayerRequestControlsHandler.accept(this);
                                } catch (Exception ex) {
                                    System.err.println("[GuiController] Exception in multiplayerRequestControlsHandler: " + ex);
                                }
                                return;
                            }

                            URL loc = getClass().getClassLoader().getResource("controls.fxml");
                            if (loc == null) {
                                hidePauseOverlay();
                                return;
                            }
                            FXMLLoader fx = new FXMLLoader(loc);
                            javafx.scene.layout.StackPane pane = fx.load();
                            com.comp2042.ControlsController cc = fx.getController();

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
                            StackPane overlay = new StackPane();
                            overlay.setStyle("-fx-padding:0;");
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
                            // top bar with title and action buttons
                            javafx.scene.text.Text header = new javafx.scene.text.Text("Controls");
                            header.setStyle("-fx-font-size:34px; -fx-fill: #9fb0ff; -fx-font-weight:700;");
                            javafx.scene.layout.HBox actionBox = new javafx.scene.layout.HBox(10);
                            actionBox.setAlignment(Pos.CENTER_RIGHT);
                            javafx.scene.control.Button btnCancel2 = new javafx.scene.control.Button("Cancel");
                            javafx.scene.control.Button btnSave2 = new javafx.scene.control.Button("Save");
                            btnCancel2.getStyleClass().add("menu-button"); btnSave2.getStyleClass().add("menu-button");
                            // attach sounds for these action buttons
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

                            btnCancel2.setOnAction(ev2 -> {
                                ev2.consume();
                                // remove this controls overlay and restore pause overlay visibility
                                if (overlay.getParent() instanceof javafx.scene.layout.Pane) {
                                    javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) overlay.getParent();
                                    root.getChildren().remove(overlay);
                                }
                                // restore any previously-hidden pause overlay nodes
                                Object o = overlay.getProperties().get("hiddenPauseNodes");
                                if (o instanceof java.util.List<?>) {
                                    for (Object n : (java.util.List<?>) o) {
                                        if (n instanceof javafx.scene.Node) ((javafx.scene.Node) n).setVisible(true);
                                    }
                                }
                                // ensure the pause overlay flag is correct so UI remains paused
                                isPauseOverlayVisible = true; isPause.setValue(Boolean.TRUE);
                            });

                            btnSave2.setOnAction(ev2 -> {
                                ev2.consume();
                                try {
                                    // update local control mappings so the running game uses new keys
                                    ctrlMoveLeft = cc.getLeft();
                                    ctrlMoveRight = cc.getRight();
                                    ctrlRotate = cc.getRotate();
                                    ctrlSoftDrop = cc.getDown();
                                    ctrlHardDrop = cc.getHard();
                                    ctrlSwap = cc.getSwitch();
                                } catch (Exception ignored) {}
                                // persist the updated control mappings so they survive game over and app restarts
                                try {
                                    Preferences prefs = Preferences.userNodeForPackage(com.comp2042.MainMenuController.class);
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
                                            // fallback to single-player keys if id unrecognized
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
                                // remove overlay and restore pause overlay nodes
                                if (overlay.getParent() instanceof javafx.scene.layout.Pane) {
                                    javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) overlay.getParent();
                                    root.getChildren().remove(overlay);
                                }
                                Object o2 = overlay.getProperties().get("hiddenPauseNodes");
                                if (o2 instanceof java.util.List<?>) {
                                    for (Object n : (java.util.List<?>) o2) {
                                        if (n instanceof javafx.scene.Node) ((javafx.scene.Node) n).setVisible(true);
                                    }
                                }
                                isPauseOverlayVisible = true; isPause.setValue(Boolean.TRUE);
                            });

                            overlay.getChildren().addAll(dark2, container);
                            if (sceneLocal != null && sceneLocal.getRoot() instanceof javafx.scene.layout.Pane) {
                                javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) sceneLocal.getRoot();
                                // Instead of removing the pause overlay, hide it so state remains consistent
                                java.util.List<javafx.scene.Node> hidden = new java.util.ArrayList<>();
                                for (javafx.scene.Node n : root.getChildren()) {
                                    if (n != null && "GLOBAL_PAUSE_OVERLAY".equals(n.getId())) {
                                        n.setVisible(false);
                                        hidden.add(n);
                                    }
                                }
                                // store hidden nodes so we can restore them when controls overlay closes
                                overlay.getProperties().put("hiddenPauseNodes", hidden);
                                root.getChildren().add(overlay);
                            }

                        } catch (Exception ex) {
                            // fallback: just close pause overlay
                            try { hidePauseOverlay(); } catch (Exception ignored) {}
                        }
                    });

                    buttons.getChildren().addAll(resume, settings);
                    dialog.getChildren().addAll(title, buttons);

                    pauseOverlay.getChildren().addAll(dark, dialog);

                    if (scene.getRoot() instanceof javafx.scene.layout.Pane) {
                        javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) scene.getRoot();
                        // remove any existing global pause overlays first (prevents duplicates from multiplayer)
                        try {
                            java.util.List<javafx.scene.Node> toRemove = new java.util.ArrayList<>();
                            for (javafx.scene.Node n : root.getChildren()) {
                                if (n != null && "GLOBAL_PAUSE_OVERLAY".equals(n.getId())) toRemove.add(n);
                            }
                            root.getChildren().removeAll(toRemove);
                        } catch (Exception ignored) {}
                        root.getChildren().add(pauseOverlay);
                    } else if (groupNotification != null) {
                        // fallback: remove any existing then add
                        try {
                            java.util.List<javafx.scene.Node> toRemove = new java.util.ArrayList<>();
                            for (javafx.scene.Node n : groupNotification.getChildren()) {
                                if (n != null && "GLOBAL_PAUSE_OVERLAY".equals(n.getId())) toRemove.add(n);
                            }
                            groupNotification.getChildren().removeAll(toRemove);
                        } catch (Exception ignored) {}
                        groupNotification.getChildren().add(pauseOverlay);
                    }

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
                } catch (Exception ignored) {}
            });
        } else {
            hidePauseOverlay();
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
        this.currentBoardMatrix = boardMatrix;
        displayMatrix = new Rectangle[boardMatrix.length][boardMatrix[0].length];
        for (int i = 2; i < boardMatrix.length; i++) {
            for (int j = 0; j < boardMatrix[i].length; j++) {
                Rectangle rectangle = new Rectangle(BRICK_SIZE, BRICK_SIZE);
                rectangle.setFill(Color.TRANSPARENT);
                displayMatrix[i][j] = rectangle;
                gamePanel.add(rectangle, j, i - 2);
            }
        }

        rectangles = new Rectangle[brick.getBrickData().length][brick.getBrickData()[0].length];
        // initial placement uses BRICK_SIZE; we'll compute exact cell size after layout
        double initialCellW = BRICK_SIZE + gamePanel.getHgap();
        double initialCellH = BRICK_SIZE + gamePanel.getVgap();
        for (int i = 0; i < brick.getBrickData().length; i++) {
            for (int j = 0; j < brick.getBrickData()[i].length; j++) {
                Rectangle rectangle = new Rectangle(BRICK_SIZE, BRICK_SIZE);
                rectangle.setFill(getFillColor(brick.getBrickData()[i][j]));
                rectangles[i][j] = rectangle;
                // position absolutely inside the brickPanel Pane
                rectangle.setLayoutX(j * initialCellW);
                rectangle.setLayoutY(i * initialCellH);
                brickPanel.getChildren().add(rectangle);
            }
        }
        // init ghost rectangles (same size as brick matrix)
        ghostRectangles = new Rectangle[brick.getBrickData().length][brick.getBrickData()[0].length];
        for (int i = 0; i < ghostRectangles.length; i++) {
            for (int j = 0; j < ghostRectangles[i].length; j++) {
                Rectangle r = new Rectangle(BRICK_SIZE, BRICK_SIZE);
                r.setFill(Color.rgb(200, 200, 200, 0.25)); // translucent gray
                r.setVisible(false);
                ghostRectangles[i][j] = r;
                r.setLayoutX(j * initialCellW);
                r.setLayoutY(i * initialCellH);
                ghostPanel.getChildren().add(r);
            }
        }

        javafx.application.Platform.runLater(() -> {
            try {
                Rectangle ref = null;
                for (int r = 2; r < displayMatrix.length; r++) {
                    for (int c = 0; c < displayMatrix[r].length; c++) {
                        if (displayMatrix[r][c] != null) {
                            ref = displayMatrix[r][c];
                            break;
                        }
                    }
                    if (ref != null) break;
                }
                if (ref != null) {
                    double measuredW = ref.getLayoutBounds().getWidth();
                    double measuredH = ref.getLayoutBounds().getHeight();
                    // include gaps
                    cellW = measuredW + gamePanel.getHgap();
                    cellH = measuredH + gamePanel.getVgap();
                    try {
                        javafx.geometry.Point2D scenePt = ref.localToScene(0.0, 0.0);
                        if (brickPanel != null && brickPanel.getParent() != null) {
                            javafx.geometry.Point2D localPt = brickPanel.getParent().sceneToLocal(scenePt);
                            baseOffsetX = localPt.getX();
                            baseOffsetY = localPt.getY();
                        } else {
                            baseOffsetX = ref.getBoundsInParent().getMinX();
                            baseOffsetY = ref.getBoundsInParent().getMinY();
                        }
                    } catch (Exception e) {
                        // fallback
                        baseOffsetX = ref.getBoundsInParent().getMinX();
                        baseOffsetY = ref.getBoundsInParent().getMinY();
                    }
                } else {
                    cellW = initialCellW;
                    cellH = initialCellH;
                }
            } catch (Exception ex) {
                cellW = initialCellW;
                cellH = initialCellH;
            }
            try {
                if (bgCanvas != null) {
                    double width = cellW * boardMatrix[0].length;
                    double height = cellH * (boardMatrix.length - 2); // visible rows only
                    bgCanvas.setWidth(Math.round(width));
                    bgCanvas.setHeight(Math.round(height));
                    GraphicsContext gc = bgCanvas.getGraphicsContext2D();
                    gc.clearRect(0, 0, bgCanvas.getWidth(), bgCanvas.getHeight());
                    gc.setFill(new javafx.scene.paint.LinearGradient(
                        0, 0, 0, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop[] {
                            new javafx.scene.paint.Stop(0, javafx.scene.paint.Color.rgb(28,28,30,0.30)),
                            new javafx.scene.paint.Stop(1, javafx.scene.paint.Color.rgb(12,12,14,0.48))
                        }
                    ));
                    gc.fillRect(0, 0, bgCanvas.getWidth(), bgCanvas.getHeight());

                    javafx.scene.paint.Color minorCol = javafx.scene.paint.Color.rgb(85, 90, 92, 0.60);

                    // draw vertical lines
                    for (int c = 0; c <= boardMatrix[0].length; c++) {
                        double x = Math.round(c * cellW) + 0.5; // 0.5 to draw crisp 1px lines
                        gc.setStroke(minorCol);
                        gc.setLineWidth(1.0);
                        gc.strokeLine(x, 0, x, bgCanvas.getHeight());
                    }

                    // draw horizontal lines
                    int visibleRows = boardMatrix.length - 2;
                    for (int r = 0; r <= visibleRows; r++) {
                        double y = Math.round(r * cellH) + 0.5;
                        // draw all lines with the same appearance so no standout divider exists
                        gc.setStroke(minorCol);
                        gc.setLineWidth(1.0);
                        gc.strokeLine(0, y, bgCanvas.getWidth(), y);
                    }

                    // tiny intersection dots to aid visual alignment (very subtle)
                    try {
                        gc.setFill(javafx.scene.paint.Color.rgb(200,200,200,0.04));
                        for (int r = 0; r <= visibleRows; r++) {
                            double y = Math.round(r * cellH) + 0.5;
                            for (int c = 0; c <= boardMatrix[0].length; c++) {
                                double x = Math.round(c * cellW) + 0.5;
                        // larger, more visible intersection dots
                        gc.fillOval(x - 1.5, y - 1.5, 3.0, 3.0);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
            // reposition brick and ghost rectangles to the measured grid
            for (int i = 0; i < rectangles.length; i++) {
                for (int j = 0; j < rectangles[i].length; j++) {
                    Rectangle rect = rectangles[i][j];
                    if (rect != null) {
                        // snap to whole pixels to avoid fractional-pixel rendering differences on some DPIs
                        rect.setLayoutX(Math.round(j * cellW));
                        rect.setLayoutY(Math.round(i * cellH));
                    }
                }
            }
            for (int i = 0; i < ghostRectangles.length; i++) {
                for (int j = 0; j < ghostRectangles[i].length; j++) {
                    Rectangle rect = ghostRectangles[i][j];
                    if (rect != null) {
                        // snap ghost cells as well so their internal positions line up with the background grid
                        rect.setLayoutX(Math.round(j * cellW));
                        rect.setLayoutY(Math.round(i * cellH));
                    }
                }
            }
            // position the procedural canvas so it lines up with the grid origin
            try {
                    if (bgCanvas != null) {
                    bgCanvas.setTranslateX(Math.round(baseOffsetX));
                    bgCanvas.setTranslateY(Math.round(baseOffsetY));
                }
            } catch (Exception ignored) {}
            // re-render nextBox using measured sizes so previews match the main board cells
            try {
                if (upcomingCache != null && !upcomingCache.isEmpty()) {
                    showNextBricks(upcomingCache);
                }
            } catch (Exception ignored) {}
            // ensure the falling brick is positioned correctly now that measurements are available
            try {
                if (brick != null) {
                    refreshBrick(brick);
                }
            } catch (Exception ignored) {}

            try {
                if (nextBox != null) {
                    double minW = Math.round(cellW * 4) + 24; // 4 columns + padding
                    double minH = Math.round(cellH * 3 * 1.2) + 24; // 3 previews stacked + spacing
                    nextBox.setMinWidth(minW);
                    nextBox.setMinHeight(minH);
                }
            } catch (Exception ignored) {}
        });


    timeLine = new Timeline(new KeyFrame(
        Duration.millis(dropIntervalMs),
        ae -> {
            ae.consume();
            moveDown(new MoveEvent(EventType.DOWN, EventSource.THREAD));
        }
    ));
    timeLine.setCycleCount(Timeline.INDEFINITE);
    // initial ghost render - defer to run after measurement so baseOffset/cell size are initialized
    javafx.application.Platform.runLater(() -> updateGhost(brick, currentBoardMatrix));
    }

    public void setMultiplayerExitToMenuHandler(Runnable handler) {
        this.multiplayerExitToMenuHandler = handler;
    }

    public void startCountdown(int seconds) {
        if (seconds <= 0) seconds = 3;
        try { prevHighBeforeGame = highScore; } catch (Exception ignored) {}
        isPause.setValue(Boolean.TRUE);
        countdownFinished.setValue(Boolean.FALSE);
        try { countdownStarted.setValue(Boolean.FALSE); } catch (Exception ignored) {}
        final Text countdown = new Text();
        countdown.getStyleClass().add("gameOverStyle");
        countdown.setStyle("-fx-font-size: 96px; -fx-fill: yellow; -fx-stroke: black; -fx-stroke-width:2;");

        try {
            javafx.application.Platform.runLater(() -> {
                try {
                    if (brickPanel != null) brickPanel.setVisible(false);
                    if (ghostPanel != null) ghostPanel.setVisible(false);
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}

        try {
            if (this.currentViewData != null && this.currentBoardMatrix != null) {
                // refresh positions using cached view data so the visible block matches the ghost
                javafx.application.Platform.runLater(() -> {
                    try {
                        doRefreshBrick(currentViewData);
                        updateGhost(currentViewData, currentBoardMatrix);
                        // snap visible brick to ghost position while countdown overlays UI
                        brickPanel.setTranslateX(ghostPanel.getTranslateX());
                        brickPanel.setTranslateY(ghostPanel.getTranslateY());
                        // hide both panels during countdown for a clean overlay
                        if (brickPanel != null) brickPanel.setVisible(false);
                        if (ghostPanel != null) ghostPanel.setVisible(false);
                        // debug: print both translate positions so we can compare
                        
                    } catch (Exception ignored) {}
                });
            }
        } catch (Exception ignored) {}

        // Add a full-screen transparent overlay and center the countdown in it so it's always in the middle of the window
        final javafx.scene.layout.StackPane overlay = new javafx.scene.layout.StackPane();
        overlay.setPickOnBounds(false);
        overlay.getChildren().add(countdown);
        countdown.setTranslateY(-20); // slight visual offset if needed

        javafx.application.Platform.runLater(() -> {
            try {
                if (gameBoard.getScene() != null) {
                    javafx.scene.Scene s = gameBoard.getScene();
                    // bind overlay to scene size and add to root
                    overlay.prefWidthProperty().bind(s.widthProperty());
                    overlay.prefHeightProperty().bind(s.heightProperty());
                    overlay.setMouseTransparent(true);
                    if (s.getRoot() instanceof javafx.scene.layout.Pane) {
                        ((javafx.scene.layout.Pane) s.getRoot()).getChildren().add(overlay);
                    } else {
                        // fallback: add to groupNotification
                        groupNotification.getChildren().add(countdown);
                    }
                } else {
                    groupNotification.getChildren().add(countdown);
                }
            } catch (Exception ignored) {
                groupNotification.getChildren().add(countdown);
            }
        });

        final int[] cnt = new int[]{seconds};
        final int initialCount = seconds;
        final Timeline cd = new Timeline();
        KeyFrame kf = new KeyFrame(Duration.seconds(1), new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                if (cnt[0] > 0) {
                    // Start countdown music and mark countdown started when the first number is displayed so audio aligns with visuals
                    try { if (cnt[0] == initialCount) { playCountdownMusic(); countdownStarted.setValue(Boolean.TRUE); } } catch (Exception ignored) {}
                    countdown.setText(Integer.toString(cnt[0]));
                    // animate scale+fade
                    ScaleTransition st = new ScaleTransition(Duration.millis(600), countdown);
                    st.setFromX(0.2); st.setFromY(0.2); st.setToX(1.0); st.setToY(1.0);
                    FadeTransition ft = new FadeTransition(Duration.millis(600), countdown);
                    ft.setFromValue(0.0); ft.setToValue(1.0);
                    ParallelTransition pt = new ParallelTransition(st, ft);
                    pt.play();
                } else if (cnt[0] == 0) {
                    countdown.setText("Start");
                    ScaleTransition st = new ScaleTransition(Duration.millis(800), countdown);
                    st.setFromX(0.5); st.setFromY(0.5); st.setToX(1.2); st.setToY(1.2);
                    FadeTransition ft = new FadeTransition(Duration.millis(800), countdown);
                    ft.setFromValue(0.0); ft.setToValue(1.0);
                    ParallelTransition pt = new ParallelTransition(st, ft);
                    pt.play();
                } else {
                    // finished: remove overlay and start game
                    javafx.application.Platform.runLater(() -> {
                        try {
                            if (overlay.getParent() instanceof javafx.scene.layout.Pane) {
                                ((javafx.scene.layout.Pane) overlay.getParent()).getChildren().remove(overlay);
                            } else {
                                groupNotification.getChildren().remove(countdown);
                            }
                        } catch (Exception ignored) {}
                    });
                    if (timeLine != null) timeLine.play();
                    resetClock();
                    startClock();
                    isPause.setValue(Boolean.FALSE);
                    // signal that countdown completed and gameplay started
                    try { countdownFinished.setValue(Boolean.TRUE); } catch (Exception ignored) {}
                    try { if (currentViewData != null) doRefreshBrick(currentViewData); } catch (Exception ignored) {}
                    javafx.application.Platform.runLater(() -> {
                        try {
                            if (brickPanel != null) brickPanel.setVisible(true);
                            if (ghostPanel != null) ghostPanel.setVisible(true);
                        } catch (Exception ignored) {}
                    });
                    // stop timeline properly
                    try { stopCountdownMusic(); } catch (Exception ignored) {}
                    try { countdownStarted.setValue(Boolean.FALSE); } catch (Exception ignored) {}
                    cd.stop();
                    // restore keyboard focus so scene-level handlers continue receiving key events
                    try { gamePanel.requestFocus(); } catch (Exception ignored) {}
                }
                cnt[0] = cnt[0] - 1;
            }
        });
        cd.getKeyFrames().add(kf);
    cd.setCycleCount(seconds + 2);
    cd.playFromStart();
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
            // snap to whole pixels (boardToPixel returns base pixel coordinates)
            ghostPanel.setTranslateX(Math.round(pt.getX()));
            ghostPanel.setTranslateY(Math.round(pt.getY()));
    
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

    public void showNextBricks(java.util.List<com.comp2042.logic.bricks.Brick> upcoming) {
    if (nextContent == null) return;
    nextContent.getChildren().clear();
        if (upcoming == null) return;
        upcomingCache = new java.util.ArrayList<>(upcoming);
        javafx.scene.layout.VBox built = buildNextPreview(upcoming);
        if (built != null) {
            nextContent.getChildren().addAll(built.getChildren());
        }
    }

    public javafx.scene.layout.VBox buildNextPreview(java.util.List<com.comp2042.logic.bricks.Brick> upcoming) {
        javafx.scene.layout.VBox container = new javafx.scene.layout.VBox(8);
        container.setAlignment(Pos.TOP_CENTER);
        if (upcoming == null || upcoming.isEmpty()) return container;

        double pW = Math.max(4.0, cellW);
        double pH = Math.max(4.0, cellH);

        int count = Math.min(upcoming.size(), 3);
        for (int i = 0; i < count; i++) {
            com.comp2042.logic.bricks.Brick b = upcoming.get(i);
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
                    rect.setFill(getFillColor(val));
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

    private Paint getFillColor(int i) {
        Paint returnPaint;
        switch (i) {
            case 0:
                returnPaint = Color.TRANSPARENT;
                break;
            case 1:
                returnPaint = Color.AQUA;
                break;
            case 2:
                returnPaint = Color.BLUEVIOLET;
                break;
            case 3:
                returnPaint = Color.DARKGREEN;
                break;
            case 4:
                returnPaint = Color.YELLOW;
                break;
            case 5:
                returnPaint = Color.RED;
                break;
            case 6:
                returnPaint = Color.BEIGE;
                break;
            case 7:
                returnPaint = Color.BURLYWOOD;
                break;
            case 8:
                // garbage rows - use a neutral grey so they're visually distinct
                returnPaint = Color.DARKGRAY;
                break;
            default:
                returnPaint = Color.WHITE;
                break;
        }
        return returnPaint;
    }

    private void refreshBrick(ViewData brick) {
        this.currentViewData = brick;
        if (isPause.getValue() == Boolean.FALSE) {
            doRefreshBrick(brick);
        }
    }

    private void doRefreshBrick(ViewData brick) {
        if (brick == null) return;
        int offsetX = brick.getxPosition();
        int offsetY = brick.getyPosition() - 2;

        javafx.geometry.Point2D pt = boardToPixel(offsetX, offsetY);
        double tx = Math.round(pt.getX());
        double ty = Math.round(pt.getY());

        brickPanel.setTranslateX(tx);
        brickPanel.setTranslateY(ty);

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
        double x = baseOffsetX + (boardX * cellW);
        double y = baseOffsetY + (boardY * cellH);
        return new javafx.geometry.Point2D(x, y);
    }
    
    public void refreshGameBackground(int[][] board) {
        this.currentBoardMatrix = board;
        for (int i = 2; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                setRectangleData(board[i][j], displayMatrix[i][j]);
            }
        }
    }

    private void setRectangleData(int color, Rectangle rectangle) {
        rectangle.setFill(getFillColor(color));
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
        if (start == null || end == null || particlePane == null) return;
        try {
            int[][] shape = start.getBrickData();
            if (shape == null) return;

            int startBoardY = start.getyPosition() - 2; // visible offset
            int endBoardY = end.getyPosition() - 2;

            double cellWpx = cellW;
            double cellHpx = cellH;

            double travelMs = intense ? 420.0 : 560.0; 
            double fadeMs = intense ? 620.0 : 800.0;   

            java.util.List<javafx.animation.ParallelTransition> running = new java.util.ArrayList<>();

            int minR = Integer.MAX_VALUE;
            for (int rr = 0; rr < shape.length; rr++) {
                for (int cc = 0; cc < shape[rr].length; cc++) if (shape[rr][cc] != 0) { if (rr < minR) minR = rr; }
            }
            if (minR == Integer.MAX_VALUE) minR = 0;

            javafx.geometry.Point2D topParentPt = boardToPixel(start.getxPosition(), startBoardY + minR);
            javafx.geometry.Point2D topScenePt = (brickPanel != null && brickPanel.getParent() != null)
                    ? brickPanel.getParent().localToScene(topParentPt)
                    : new javafx.geometry.Point2D(topParentPt.getX(), topParentPt.getY());
            javafx.geometry.Point2D topLocal = (particlePane != null) ? particlePane.sceneToLocal(topScenePt) : topScenePt;

            for (int r = 0; r < shape.length; r++) {
                for (int c = 0; c < shape[r].length; c++) {
                    if (shape[r][c] == 0) continue;
                    int boardX = start.getxPosition() + c;
                    int boardYEnd = endBoardY + r;

                    javafx.geometry.Point2D cellStartParent = boardToPixel(boardX, startBoardY + r);
                    javafx.geometry.Point2D cellStartScene = (brickPanel != null && brickPanel.getParent() != null)
                            ? brickPanel.getParent().localToScene(cellStartParent)
                            : new javafx.geometry.Point2D(cellStartParent.getX(), cellStartParent.getY());
                    javafx.geometry.Point2D cellStartLocal = (particlePane != null) ? particlePane.sceneToLocal(cellStartScene) : cellStartScene;

                    javafx.geometry.Point2D cellEndParent = boardToPixel(boardX, boardYEnd);
                    javafx.geometry.Point2D cellEndScene = (brickPanel != null && brickPanel.getParent() != null)
                            ? brickPanel.getParent().localToScene(cellEndParent)
                            : new javafx.geometry.Point2D(cellEndParent.getX(), cellEndParent.getY());
                    javafx.geometry.Point2D cellEndLocal = (particlePane != null) ? particlePane.sceneToLocal(cellEndScene) : cellEndScene;

                    double x = Math.round(cellStartLocal.getX());
                    double y = Math.round(topLocal.getY());

                    Rectangle cellRect = new Rectangle(Math.round(cellWpx), Math.round(cellHpx));
                    cellRect.setArcWidth(6);
                    cellRect.setArcHeight(6);
                    cellRect.setFill(Color.web("#ffffff"));
                    cellRect.setOpacity(intense ? 0.95 : 0.85);
                    cellRect.setMouseTransparent(true);
                    cellRect.setLayoutX(x);
                    cellRect.setLayoutY(y);
                    cellRect.setTranslateX(0);
                    cellRect.setTranslateY(0);

                    DropShadow ds = new DropShadow(BlurType.GAUSSIAN, Color.web("#ffffff"), intense ? 14.0 : 9.0, 0.35, 0.0, 0.0);
                    ds.setSpread(intense ? 0.7 : 0.45);
                    cellRect.setEffect(ds);

                    if (particlePane != null) particlePane.getChildren().add(cellRect);

                    TranslateTransition tt = new TranslateTransition(Duration.millis(travelMs), cellRect);
                    double deltaY = Math.round(cellEndLocal.getY() - topLocal.getY());
                    tt.setByY(deltaY);
                    tt.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
                    tt.setDelay(Duration.millis(r * 18));

                    FadeTransition ft = new FadeTransition(Duration.millis(fadeMs), cellRect);
                    ft.setFromValue(cellRect.getOpacity());
                    ft.setToValue(0.0);

                    ParallelTransition pt = new ParallelTransition(tt, ft);
                    pt.setOnFinished(e -> { try { e.consume(); if (particlePane != null) particlePane.getChildren().remove(cellRect); } catch (Exception ignored) {} });
                    running.add(pt);
                }
            }

            javafx.application.Platform.runLater(() -> {
                for (ParallelTransition pt : running) pt.play();
            });

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
                    for (int r : rows) {
                        double flashY = Math.round(baseOffsetY + (r - 2) * cellH);
                        flashRow(flashY, cellW * displayMatrix[0].length, cellH);
                    }
                    // shake the board once when rows are removed
                    shakeBoard();
                    // spawn per-cell falling square particles for each cleared cell
                    try { spawnRowClearParticles(clearRow); } catch (Exception ignored) {}
                    // spawn a burst for each cleared row using its center Y coordinate
                    for (int r : rows) {
                        // r is absolute board row index (0 = top including hidden rows)
                        double centerY = Math.round(baseOffsetY + (r - 2 + 0.5) * cellH); // center of the row in visible coords
                        // centerX => middle of the board horizontally
                        double centerX = Math.round(baseOffsetX + (displayMatrix[0].length * 0.5) * cellW);
                        spawnParticlesAt(centerX, centerY, v != null ? v.getBrickData() : null);
                    }
                    return;
                }
            }
        } catch (Exception ignored) {}
        // fallback: spawn at brick landing position
        spawnExplosion(v);
    }

    // Spawn small square particles for each brick in the cleared rows that then fall down and fade out.
    protected void spawnRowClearParticles(ClearRow clearRow) {
        if (clearRow == null || particlePane == null || displayMatrix == null) return;
        try {
            int[] rows = clearRow.getClearedRows();
            if (rows == null || rows.length == 0) return;
            int cols = displayMatrix[0].length;
            for (int r : rows) {
                // compute top-left Y for this board row (visible coords)
                double rowTopY = Math.round(baseOffsetY + (r - 2) * cellH);
                for (int c = 0; c < cols; c++) {
                    try {
                        Rectangle boardCell = null;
                        if (displayMatrix != null && displayMatrix.length > r && r >= 0) boardCell = displayMatrix[r][c];
                        Paint fill = (boardCell != null) ? boardCell.getFill() : null;
                        if (fill == null) continue;
                        if (fill == Color.TRANSPARENT) continue;
                        Color color = (fill instanceof Color) ? (Color) fill : Color.WHITE;

                        // spawn a handful of small square particles for this brick
                        int particles = 4 + (int)(Math.random() * 6); // 4..9
                        for (int p = 0; p < particles; p++) {
                            double pw = Math.max(3.0, Math.round(cellW / 3.0));
                            double ph = Math.max(3.0, Math.round(cellH / 3.0));
                            Rectangle sq = new Rectangle(pw, ph);
                            sq.setArcWidth(2);
                            sq.setArcHeight(2);
                            sq.setFill(color);
                            sq.setMouseTransparent(true);

                            // initial position: somewhere within the original brick cell
                            double cellX = Math.round(baseOffsetX + c * cellW);
                            double cellY = rowTopY;
                            double jitterX = (Math.random() - 0.5) * (cellW * 0.4);
                            double jitterY = (Math.random() - 0.5) * (cellH * 0.4);
                            double startX = Math.round(cellX + cellW * 0.5 + jitterX - pw * 0.5);
                            double startY = Math.round(cellY + cellH * 0.5 + jitterY - ph * 0.5);

                            sq.setTranslateX(startX);
                            sq.setTranslateY(startY);
                            particlePane.getChildren().add(sq);

                            // falling distance (to bottom of scene or a generous amount)
                            double sceneHeight = 800.0;
                            try { if (gameBoard != null && gameBoard.getScene() != null) sceneHeight = gameBoard.getScene().getHeight(); } catch (Exception ignored) {}
                            double fallBy = sceneHeight - startY + 80 + Math.random() * 120;

                            // duration variation (increased so particles fall longer and remain visible)
                            double durationMs = 2000 + Math.random() * 1500; // 2000..3500ms

                            TranslateTransition tt = new TranslateTransition(Duration.millis(durationMs), sq);
                            tt.setByY(fallBy);
                            tt.setInterpolator(javafx.animation.Interpolator.EASE_IN);

                            FadeTransition ft = new FadeTransition(Duration.millis(durationMs), sq);
                            ft.setFromValue(1.0);
                            ft.setToValue(0.0);

                            // slight outward motion for explosion feel
                            double sideBy = (Math.random() - 0.5) * 40.0;
                            // prolong sideways/outward motion so explosion feels stretched
                            TranslateTransition ttx = new TranslateTransition(Duration.millis(durationMs * 0.75), sq);
                            ttx.setByX(sideBy);
                            ttx.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

                            ParallelTransition pt = new ParallelTransition(ttx, tt, ft);
                            final Rectangle node = sq;
                            pt.setOnFinished(e -> { try { if (e != null) e.consume(); particlePane.getChildren().remove(node); } catch (Exception ignored) {} });
                            pt.play();
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }

    // Show a brief flash rectangle at vertical position y (top of the row) with given width/height
    protected void flashRow(double topY, double width, double height) {
        if (particlePane == null) return;
        try {
            Rectangle flash = new Rectangle(Math.round(width), Math.round(height));
            flash.setTranslateX(Math.round(baseOffsetX));
            flash.setTranslateY(Math.round(topY));
            flash.setFill(Color.web("#ffffff"));
            flash.setOpacity(0.0);
            flash.setMouseTransparent(true);
            particlePane.getChildren().add(flash);

            FadeTransition in = new FadeTransition(Duration.millis(80), flash);
            in.setFromValue(0.0);
            in.setToValue(0.85);
            FadeTransition out = new FadeTransition(Duration.millis(220), flash);
            out.setFromValue(0.85);
            out.setToValue(0.0);
            out.setDelay(Duration.millis(80));
            out.setOnFinished(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
                @Override
                public void handle(javafx.event.ActionEvent event) {
                    particlePane.getChildren().remove(flash);
                }
            });
            in.play();
            out.play();
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
        final int PARTICLE_COUNT = 18;
        final double MAX_SPEED = 220.0; // px/sec
        final double DURATION_MS = 600.0;
        java.util.List<javafx.scene.Node> particles = new java.util.ArrayList<>();

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            Circle c = new Circle(4 + Math.random() * 4);
            // random color sampled from brick colors (if available) or default gold
            Paint p = Color.web("#ffd166");
            if (brickShape != null) {
                // pick a random filled cell color from the brick shape
                java.util.List<Paint> fills = new java.util.ArrayList<>();
                for (int r = 0; r < brickShape.length; r++) for (int col = 0; col < brickShape[r].length; col++) if (brickShape[r][col] != 0) fills.add(getFillColor(brickShape[r][col]));
                if (!fills.isEmpty()) p = fills.get((int)(Math.random() * fills.size()));
            }
            c.setFill(p);
            c.setOpacity(1.0);
            c.setTranslateX(centerX + (Math.random() - 0.5) * 6);
            c.setTranslateY(centerY + (Math.random() - 0.5) * 6);
            particlePane.getChildren().add(c);
            particles.add(c);

            // random direction
            double angle = Math.random() * Math.PI * 2.0;
            double speed = 40 + Math.random() * MAX_SPEED; // px/sec
            double dx = Math.cos(angle) * speed;
            double dy = Math.sin(angle) * speed;

            // animate translation and fade
            TranslateTransition tt = new TranslateTransition(Duration.millis(DURATION_MS), c);
            tt.setByX(dx);
            tt.setByY(dy);
            tt.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

            FadeTransition ft = new FadeTransition(Duration.millis(DURATION_MS), c);
            ft.setFromValue(1.0);
            ft.setToValue(0.0);

            ScaleTransition st = new ScaleTransition(Duration.millis(DURATION_MS), c);
            st.setToX(0.3);
            st.setToY(0.3);

            ParallelTransition pt = new ParallelTransition(tt, ft, st);
            final javafx.scene.Node node = c;
            pt.setOnFinished(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
                @Override
                public void handle(javafx.event.ActionEvent event) {
                    particlePane.getChildren().remove(node);
                }
            });
            pt.play();
        }
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

                StackPane overlay = new StackPane();
                overlay.setPickOnBounds(true);
                overlay.setStyle("-fx-background-color: transparent;");

                Rectangle dark = new Rectangle();
                dark.widthProperty().bind(scene.widthProperty());
                dark.heightProperty().bind(scene.heightProperty());
                dark.setFill(Color.rgb(0,0,0,0.95));

                VBox dialog = new VBox(14);
                dialog.setAlignment(Pos.CENTER);
                dialog.setMouseTransparent(false);
                dialog.setStyle("-fx-background-color: rgba(0,0,0,1.0); -fx-padding: 18px; -fx-background-radius: 8px;");

                Text title = new Text("Match Over");
                title.setStyle("-fx-font-weight: 700;");
                title.setFill(javafx.scene.paint.LinearGradient.valueOf("from 0% 0% to 100% 0% , #ffd166 0%, #ff7b7b 100%"));
                title.setOpacity(1.0);
                title.setFont(Font.font(72));
                DropShadow ds = new DropShadow(BlurType.GAUSSIAN, Color.rgb(0,0,0,0.8), 18, 0.25, 0, 6);
                title.setEffect(ds);

        try {
            javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
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

                String scoreStr = "";
                int currentScore = -1;
                try {
                    if (currentScoreProperty != null) currentScore = currentScoreProperty.get();
                    if (currentScore < 0 && scoreValue != null) {
                        // fallback: parse score from scoreValue text which is like "Current: X"
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

                Text scoreText = new Text("Score: " + (scoreStr.isEmpty() ? (scoreValue != null ? scoreValue.getText() : "0") : scoreStr));
                scoreText.setStyle("-fx-font-size: 28px; -fx-fill: white; -fx-opacity: 0.95;");
                Text timeText = new Text("Time: " + timePlayed);
                timeText.setStyle("-fx-font-size: 22px; -fx-fill: #dddddd; -fx-opacity: 0.95;");

                Text compareText = new Text();
                try {
                    if (prevHighBeforeGame <= 0) {
                        // no previous record
                        compareText.setText("No previous record");
                        compareText.setStyle("-fx-font-size: 18px; -fx-fill: #cccccc;");
                    } else if (currentScore >= 0 && currentScore > prevHighBeforeGame) {
                        compareText.setText("New High Score! Previous: " + prevHighBeforeGame);
                        compareText.setStyle("-fx-font-size: 20px; -fx-fill: #ffd166; -fx-font-weight: bold;");
                    } else {
                        compareText.setText("Previous Best: " + prevHighBeforeGame);
                        compareText.setStyle("-fx-font-size: 18px; -fx-fill: #cccccc;");
                    }
                } catch (Exception ignored) {
                    compareText.setText("");
                }

                VBox subtitleBox = new VBox(6);
                subtitleBox.setAlignment(Pos.CENTER);
                subtitleBox.getChildren().addAll(scoreText, timeText, compareText);

                HBox buttons = new HBox(12);
                buttons.setAlignment(Pos.CENTER);

                Button btnRestart = new Button("Restart");
                Button btnMenu = new Button("Main Menu");
                btnRestart.getStyleClass().add("menu-button");
                btnMenu.getStyleClass().add("menu-button");
                try { attachButtonSoundHandlers(btnRestart); } catch (Exception ignored) {}
                try { attachButtonSoundHandlers(btnMenu); } catch (Exception ignored) {}

                btnRestart.setOnAction(ev -> {
                    ev.consume();
            try {
                try { stopGameOverMusic(); } catch (Exception ignored) {}
                try { stopCountdownMusic(); } catch (Exception ignored) {}
                        // stop any running title animation
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

                btnMenu.setOnAction(ev -> {
                    ev.consume();
                    try {
                        try { stopGameOverMusic(); } catch (Exception ignored) {}
                            try { stopCountdownMusic(); } catch (Exception ignored) {}
                            // stop any running title animation
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
                                String css = getClass().getClassLoader().getResource("menu.css").toExternalForm();
                                if (!stage.getScene().getStylesheets().contains(css)) stage.getScene().getStylesheets().add(css);
                            } catch (Exception ignored) {}
                        } else {
                            Scene s2 = new Scene(menuRoot, Math.max(420, stage.getWidth()), Math.max(700, stage.getHeight()));
                            try {
                                String css = getClass().getClassLoader().getResource("menu.css").toExternalForm();
                                s2.getStylesheets().add(css);
                            } catch (Exception ignored) {}
                            stage.setScene(s2);
                        }
                    } catch (Exception ex) { ex.printStackTrace(); }
                });

                buttons.getChildren().addAll(btnRestart, btnMenu);
                dialog.getChildren().addAll(title, subtitleBox, buttons);
                dialog.setTranslateY(0);

                overlay.setOnMouseClicked(event -> event.consume());
                overlay.getChildren().addAll(dark, dialog);

                if (scene.getRoot() instanceof javafx.scene.layout.Pane) {
                    javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) scene.getRoot();
                    root.getChildren().add(overlay);
                } else if (groupNotification != null) {
                    groupNotification.getChildren().add(overlay);
                }

                // subtle entrance animation
                javafx.animation.FadeTransition f = new javafx.animation.FadeTransition(Duration.millis(420), dialog);
                dialog.setOpacity(0.0);
                f.setFromValue(0.0);
                f.setToValue(1.0);
                f.play();

            } catch (Exception ignored) {}
        });
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
