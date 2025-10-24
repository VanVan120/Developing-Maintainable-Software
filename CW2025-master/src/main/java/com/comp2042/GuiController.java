package com.comp2042;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.beans.binding.Bindings;
import javafx.event.EventHandler;
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
import java.util.ResourceBundle;

public class GuiController implements Initializable {

    private static final int BRICK_SIZE = 24;

    // base drop interval (ms). Increase to make falling slower.
    private static final int DROP_INTERVAL_MS = 1000;
    // Mode-adjustable drop interval (ms). Default uses DROP_INTERVAL_MS; can be changed at runtime for Easy/Hard modes.
    private int dropIntervalMs = DROP_INTERVAL_MS;
    // soft-drop multiplier while DOWN is held (how many times faster)
    private static final double SOFT_DROP_RATE = 4.0;
    // normal rate
    private static final double NORMAL_RATE = 1.0;

    private static final double SCORE_BOX_OFFSET_X = 250.0;           // px from gameBoard left edge (increase -> nearer to left)
    private static final double SCORE_BOX_OFFSET_FROM_BOTTOM = 120.0; // px from gameBoard bottom  (increase -> higher above bottom)


    @FXML
    private GridPane gamePanel;

    @FXML
    private BorderPane gameBoard;

    @FXML
    private Pane brickPanel;

    @FXML
    private Pane ghostPanel;

    @FXML
    private Canvas bgCanvas;

    @FXML
    private Group groupNotification;

    @FXML
    private GameOverPanel gameOverPanel;

    @FXML
    private Text scoreValue;

    @FXML
    private Text highScoreValue;

    @FXML
    private VBox scoreBox;

    @FXML
    private javafx.scene.control.Button pauseBtn;

    @FXML
    private VBox nextBox; // right-hand preview container for upcoming bricks
    @FXML
    private VBox nextContent; // inner container that holds only the preview bricks
    @FXML
    private Rectangle gameBoardFrame;
    @FXML
    private Rectangle nextBoxFrame;
    @FXML
    private Pane particlePane;
    @FXML
    private VBox timeBox;
    @FXML
    private Text timeValue;
    @FXML
    private VBox levelBox;
    @FXML
    private Text levelValue;

    // cache of upcoming bricks so we can re-render after grid measurement completes
    private java.util.List<com.comp2042.logic.bricks.Brick> upcomingCache = null;

    private Rectangle[][] displayMatrix;

    private InputEventListener eventListener;

    private Rectangle[][] rectangles;

    // ghost piece rectangles (same dimensions as moving brick)
    private Rectangle[][] ghostRectangles;
    // latest background matrix (including merged bricks) for ghost calculation
    private int[][] currentBoardMatrix;
    // most recent ViewData used to render the falling brick (kept so we can realign before start)
    private ViewData currentViewData;

    private Timeline timeLine;
    private Timeline clockTimeline;
    private long startTimeMs = 0;
    // accumulated elapsed milliseconds when clock is paused
    private long pausedElapsedMs = 0;

    // actual cell size measured from the background grid after layout
    private double cellW = BRICK_SIZE;
    private double cellH = BRICK_SIZE;
    // measured origin (top-left) of the visible grid within the containing StackPane
    private double baseOffsetX = 0;
    private double baseOffsetY = 0;
    // small adjustable nudge to compensate for border/stroke/device pixel differences
    // set these to +/-0.5 or +/-1.0 as needed on your display (change at runtime by editing these values)
    private double nudgeX = 0.0;
    private double nudgeY = 0.0;

    // block-only micro-adjust (affects only the real falling blocks)
    // change these to move the real pieces without moving the ghost
    private double blockNudgeX = 0.0;
    private double blockNudgeY = 0.0;

    // multiplayer mode flag - when true, only show lock effect for explicit hard-drops by the player
    private boolean isMultiplayer = false;
    // whether the last input was a hard drop (pressed by user)
    private boolean lastWasHardDrop = false;

    // Optional callback provided by a multiplayer manager (ScoreBattleController) so a Retry
    // in multiplayer mode can request a full match restart (both players) instead of just
    // restarting this single player's board.
    private Runnable multiplayerRestartHandler = null;
    // Optional callback to notify a multiplayer coordinator when this GUI is paused/unpaused.
    // Accepts a Boolean: true => paused, false => resumed.
    private java.util.function.Consumer<Boolean> multiplayerPauseHandler = null;
    // When applying a pause change that originated from the multiplayer coordinator we
    // set this flag to avoid re-notifying the coordinator and causing reentrant loops.
    private boolean suppressMultiplayerPauseNotify = false;

    private int highScore = 0;
    private static final String HIGHSCORE_FILE = System.getProperty("user.home") + File.separator + ".tetris_highscore";

    // keep a reference to the bound score property so we can read the numeric score at game over
    private IntegerProperty currentScoreProperty = null;
    // record the previous high score value at the start of the current game so we can compare at game over
    private int prevHighBeforeGame = 0;

    private final BooleanProperty isPause = new SimpleBooleanProperty();

    private final BooleanProperty isGameOver = new SimpleBooleanProperty();
    // becomes true when the start countdown (3..1..Start) finishes and gameplay begins
    private final BooleanProperty countdownFinished = new SimpleBooleanProperty(false);

    // animation reference for the single-player game-over title so we can stop it when overlay is removed
    private javafx.animation.Animation gameOverPulse = null;

    // Optional per-instance control key mapping. If any of these are non-null then the
    // controller will ONLY respond to the configured keys. If all are null the legacy
    // behavior (accept both WASD and arrows+space) is preserved for single-player.
    private KeyCode ctrlMoveLeft = null;
    private KeyCode ctrlMoveRight = null;
    private KeyCode ctrlRotate = null;
    private KeyCode ctrlSoftDrop = null;
    private KeyCode ctrlHardDrop = null;
    private KeyCode ctrlSwap = null;

    // Configurable offsets for the timeBox relative to the game board
    private final javafx.beans.property.DoubleProperty timeBoxOffsetX = new javafx.beans.property.SimpleDoubleProperty(-100.0);
    private final javafx.beans.property.DoubleProperty timeBoxOffsetY = new javafx.beans.property.SimpleDoubleProperty(12.0);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Font.loadFont(getClass().getClassLoader().getResource("digital.ttf").toExternalForm(), 38);
        // Make sure key events are handled even if the GridPane loses focus by listening on the Scene.
        gamePanel.setFocusTraversable(true);
        gamePanel.requestFocus();

        // Helper methods to process key events
        final EventHandler<KeyEvent> pressHandler = new EventHandler<>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                processKeyPressed(keyEvent);
            }
        };

        final EventHandler<KeyEvent> releaseHandler = new EventHandler<>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                processKeyReleased(keyEvent);
            }
        };

        // Attach to the scene when available, otherwise listen for scene property
        javafx.application.Platform.runLater(() -> {
            if (gamePanel.getScene() != null) {
                gamePanel.getScene().addEventHandler(KeyEvent.KEY_PRESSED, pressHandler);
                gamePanel.getScene().addEventHandler(KeyEvent.KEY_RELEASED, releaseHandler);
                // also listen for Esc to toggle pause
                gamePanel.getScene().addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                    if (e.getCode() == KeyCode.ESCAPE) {
                        togglePauseOverlay();
                        e.consume();
                    }
                });
            } else {
                gamePanel.sceneProperty().addListener(new javafx.beans.value.ChangeListener<>() {
                    @Override
                    public void changed(javafx.beans.value.ObservableValue<? extends javafx.scene.Scene> observable, javafx.scene.Scene oldScene, javafx.scene.Scene newScene) {
                        if (newScene != null) {
                            newScene.addEventHandler(KeyEvent.KEY_PRESSED, pressHandler);
                            newScene.addEventHandler(KeyEvent.KEY_RELEASED, releaseHandler);
                            newScene.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                                if (e.getCode() == KeyCode.ESCAPE) {
                                    togglePauseOverlay();
                                    e.consume();
                                }
                            });
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
            try {
                // prefer binding to the immediate parent region so the board centers inside its container (works for SubScene or holder panes)
                if (gameBoard.getParent() instanceof javafx.scene.layout.Region) {
                    javafx.scene.layout.Region parent = (javafx.scene.layout.Region) gameBoard.getParent();
                    gameBoard.layoutXProperty().bind(parent.widthProperty().subtract(gameBoard.widthProperty()).divide(2));
                    gameBoard.layoutYProperty().bind(parent.heightProperty().subtract(gameBoard.heightProperty()).divide(2));
                } else if (gameBoard.getScene() != null) {
                    // fallback to centering within the Scene
                    gameBoard.layoutXProperty().bind(gameBoard.getScene().widthProperty().subtract(gameBoard.widthProperty()).divide(2));
                    gameBoard.layoutYProperty().bind(gameBoard.getScene().heightProperty().subtract(gameBoard.heightProperty()).divide(2));
                }
            } catch (Exception ignored) {}

            // Position score box a little to the left of the gameBoard so it stays near the board
            if (scoreBox != null) {
                scoreBox.layoutXProperty().bind(gameBoard.layoutXProperty().subtract(SCORE_BOX_OFFSET_X));
                scoreBox.layoutYProperty().bind(gameBoard.layoutYProperty().add(gameBoard.heightProperty().subtract(SCORE_BOX_OFFSET_FROM_BOTTOM)));
            }

            // Bind the rectangle frames to the corresponding controls so they match size and position
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

            // Position timeBox to the left of the gameBoard (above scoreBox)
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
                // remove the old smaller class and apply the high-score style class
                scoreValue.getStyleClass().remove("scoreClass");
                scoreValue.getStyleClass().add("highScoreClass");
            }

            // brickPanel declared in FXML stacked over gamePanel; no runtime move required

            // Center notification/group (game over) relative to gameBoard using bindings
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
        // If game is already over, ignore pause
        if (isGameOver.getValue() == Boolean.TRUE) return;

        if (!isPauseOverlayVisible) {
            // show overlay
            javafx.application.Platform.runLater(() -> {
                try {
                    Scene scene = gameBoard.getScene();
                    if (scene == null) return;
                    // create overlay root
                    pauseOverlay = new StackPane();
                    // mark overlay with well-known id so multiplayer mode can remove duplicates
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

                    resume.setOnAction(ev -> {
                        ev.consume();
                        hidePauseOverlay();
                    });

                    // Settings placeholder: for now just close overlay (or could open settings dialog)
                    settings.setOnAction(ev -> {
                        ev.consume();
                        hidePauseOverlay();
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

                    // stop gameplay timeline and block input
                    try { if (timeLine != null) timeLine.pause(); } catch (Exception ignored) {}
                        // pause clock and remember elapsed time so we don't count paused duration
                        try {
                            if (clockTimeline != null && clockTimeline.getStatus() == Timeline.Status.RUNNING) {
                                // capture elapsed so far
                                pausedElapsedMs = System.currentTimeMillis() - startTimeMs;
                                clockTimeline.pause();
                            }
                        } catch (Exception ignored) {}
                    isPause.setValue(Boolean.TRUE);
                    isPauseOverlayVisible = true;
                    // notify multiplayer coordinator (if any) that this player paused
                    try {
                        if (!suppressMultiplayerPauseNotify && multiplayerPauseHandler != null) multiplayerPauseHandler.accept(Boolean.TRUE);
                    } catch (Exception ignored) {}
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
                try {
                    Scene scene = gameBoard.getScene();
                    if (scene != null && scene.getRoot() instanceof javafx.scene.layout.Pane) {
                        javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) scene.getRoot();
                        java.util.List<javafx.scene.Node> toRemove = new java.util.ArrayList<>();
                        for (javafx.scene.Node n : root.getChildren()) {
                            if (n != null && "GLOBAL_PAUSE_OVERLAY".equals(n.getId())) toRemove.add(n);
                        }
                        root.getChildren().removeAll(toRemove);
                    }
                } catch (Exception ignored) {}
                try {
                    if (groupNotification != null) {
                        java.util.List<javafx.scene.Node> toRemove2 = new java.util.ArrayList<>();
                        for (javafx.scene.Node n : groupNotification.getChildren()) {
                            if (n != null && "GLOBAL_PAUSE_OVERLAY".equals(n.getId())) toRemove2.add(n);
                        }
                        groupNotification.getChildren().removeAll(toRemove2);
                    }
                } catch (Exception ignored) {}
                pauseOverlay = null;
                // resume timeline and input
                try { if (timeLine != null) timeLine.play(); } catch (Exception ignored) {}
                isPause.setValue(Boolean.FALSE);
                isPauseOverlayVisible = false;
                // notify multiplayer coordinator (if any) that this player resumed
                try {
                    if (!suppressMultiplayerPauseNotify && multiplayerPauseHandler != null) multiplayerPauseHandler.accept(Boolean.FALSE);
                } catch (Exception ignored) {}
                // resume clock but account for paused elapsed time
                try {
                    // startClock uses pausedElapsedMs to resume without counting paused duration
                    startClock();
                } catch (Exception ignored) {}
                // restore keyboard focus so Scene-level handlers continue receiving key events
                try { gamePanel.requestFocus(); } catch (Exception ignored) {}
            } catch (Exception ignored) {}
        });
    }

    // Key processing helpers attached to the Scene to ensure they receive events
    private void processKeyPressed(KeyEvent keyEvent) {
        if (isPause.getValue() == Boolean.FALSE && isGameOver.getValue() == Boolean.FALSE) {
            KeyCode code = keyEvent.getCode();
            boolean handled = false;
            boolean hasCustom = (ctrlMoveLeft != null || ctrlMoveRight != null || ctrlRotate != null || ctrlSoftDrop != null || ctrlHardDrop != null);
            if (hasCustom) {
                if (ctrlMoveLeft != null && code == ctrlMoveLeft) {
                    refreshBrick(eventListener.onLeftEvent(new MoveEvent(EventType.LEFT, EventSource.USER)));
                    handled = true;
                } else if (ctrlMoveRight != null && code == ctrlMoveRight) {
                    refreshBrick(eventListener.onRightEvent(new MoveEvent(EventType.RIGHT, EventSource.USER)));
                    handled = true;
                } else if (ctrlRotate != null && code == ctrlRotate) {
                    refreshBrick(eventListener.onRotateEvent(new MoveEvent(EventType.ROTATE, EventSource.USER)));
                    handled = true;
                } else if (ctrlSoftDrop != null && code == ctrlSoftDrop) {
                    if (timeLine != null) timeLine.setRate(SOFT_DROP_RATE);
                    moveDown(new MoveEvent(EventType.DOWN, EventSource.USER));
                    handled = true;
                } else if (ctrlHardDrop != null && code == ctrlHardDrop) {
                    // mark that user used hard-drop via configured hard-drop key
                    lastWasHardDrop = true;
                    hardDrop();
                    handled = true;
                } else if (ctrlSwap != null && code == ctrlSwap) {
                    try { if (eventListener != null) eventListener.onSwapEvent(); } catch (Exception ignored) {}
                    handled = true;
                }
            } else {
                // legacy behavior: accept both arrow keys and WASD/space
                if (code == KeyCode.LEFT || code == KeyCode.A) {
                    refreshBrick(eventListener.onLeftEvent(new MoveEvent(EventType.LEFT, EventSource.USER)));
                    handled = true;
                } else if (code == KeyCode.RIGHT || code == KeyCode.D) {
                    refreshBrick(eventListener.onRightEvent(new MoveEvent(EventType.RIGHT, EventSource.USER)));
                    handled = true;
                } else if (code == KeyCode.UP || code == KeyCode.W) {
                    refreshBrick(eventListener.onRotateEvent(new MoveEvent(EventType.ROTATE, EventSource.USER)));
                    handled = true;
                } else if (code == KeyCode.DOWN || code == KeyCode.S) {
                    if (timeLine != null) timeLine.setRate(SOFT_DROP_RATE);
                    moveDown(new MoveEvent(EventType.DOWN, EventSource.USER));
                    handled = true;
                } else if (code == KeyCode.SPACE || code == KeyCode.SHIFT) {
                    // mark that user used hard-drop via space or shift
                    lastWasHardDrop = true;
                    hardDrop();
                    handled = true;
                }
                // even in legacy mode allow a dedicated swap key if configured
                if (!handled && ctrlSwap != null && code == ctrlSwap) {
                    try { if (eventListener != null) eventListener.onSwapEvent(); } catch (Exception ignored) {}
                    handled = true;
                }
            }
            if (handled) keyEvent.consume();
        }
        if (keyEvent.getCode() == KeyCode.N) {
            newGame(null);
        }
    }

    /**
     * Hard drop the current piece to the bottom (like repeated DOWN presses).
     * This calls the model via eventListener.onDownEvent and updates UI until the piece lands.
     */
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
                // update view
                refreshBrick(v);
                    // if clearRow is non-null it means the piece could not move and was merged -> landing occurred
                    if (d.getClearRow() != null) {
                            // play an intense hard-drop/lock visual effect only when user explicitly hard-dropped
                            try {
                                if (lastWasHardDrop) playLockEffect(startViewForEffect, d.getViewData(), true);
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

        // After layout, measure actual cell size from the background rectangles and reposition overlays
        javafx.application.Platform.runLater(() -> {
            try {
                // find a reference rectangle from displayMatrix (first visible row)
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
                    // compute the actual top-left origin of the first visible cell using scene->local conversion
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
            // draw procedural background grid on the canvas so it exactly matches measured cell sizes
            try {
                if (bgCanvas != null) {
                    double width = cellW * boardMatrix[0].length;
                    double height = cellH * (boardMatrix.length - 2); // visible rows only
                    bgCanvas.setWidth(Math.round(width));
                    bgCanvas.setHeight(Math.round(height));
                    GraphicsContext gc = bgCanvas.getGraphicsContext2D();
                    // clear
                    gc.clearRect(0, 0, bgCanvas.getWidth(), bgCanvas.getHeight());
                    // draw a subtle inner panel and a clearer grid
                    // inner translucent background slightly lighter so grid lines contrast better
                    gc.setFill(new javafx.scene.paint.LinearGradient(
                        0, 0, 0, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop[] {
                            new javafx.scene.paint.Stop(0, javafx.scene.paint.Color.rgb(28,28,30,0.30)),
                            new javafx.scene.paint.Stop(1, javafx.scene.paint.Color.rgb(12,12,14,0.48))
                        }
                    ));
                    gc.fillRect(0, 0, bgCanvas.getWidth(), bgCanvas.getHeight());

                    // minor grid lines (brighter for gameplay clarity)
                    javafx.scene.paint.Color minorCol = javafx.scene.paint.Color.rgb(85, 90, 92, 0.60);
                    // major grid lines previously were brighter (divider). Use same color as minor to avoid a strong divider
                    // majorCol intentionally same as minor to avoid a strong divider

                    // draw vertical lines
                    for (int c = 0; c <= boardMatrix[0].length; c++) {
                        double x = Math.round(c * cellW) + 0.5; // 0.5 to draw crisp 1px lines
                        // draw all lines with the same appearance so no standout divider exists
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
                    bgCanvas.setTranslateX(Math.round(baseOffsetX + nudgeX));
                    bgCanvas.setTranslateY(Math.round(baseOffsetY + nudgeY));
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
            // ensure nextBox has a minimum size so the frame is visible. Visual styling is handled
            // by CSS and the Rectangle frame nodes to avoid compositing artifacts.
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

    /**
     * Start a countdown overlay (e.g. 3,2,1,Start) then begin the game timeline and clock.
     * This leaves the board visible but frozen until the countdown completes.
     */
    public void startCountdown(int seconds) {
        if (seconds <= 0) seconds = 3;
        // capture the high score snapshot at the start of this game so we can compare at game over
        try { prevHighBeforeGame = highScore; } catch (Exception ignored) {}
    // prevent any user input until countdown completes
        isPause.setValue(Boolean.TRUE);
    // mark countdown as not finished yet
    countdownFinished.setValue(Boolean.FALSE);
        final Text countdown = new Text();
        countdown.getStyleClass().add("gameOverStyle");
        countdown.setStyle("-fx-font-size: 96px; -fx-fill: yellow; -fx-stroke: black; -fx-stroke-width:2;");

        // ensure panels are hidden during countdown even if view data isn't available yet
        try {
            javafx.application.Platform.runLater(() -> {
                try {
                    if (brickPanel != null) brickPanel.setVisible(false);
                    if (ghostPanel != null) ghostPanel.setVisible(false);
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}

        // realign visible falling brick to match ghost position (if available) while paused
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
                        System.out.println("DEBUG ALIGN: brick translate=(" + brickPanel.getTranslateX() + "," + brickPanel.getTranslateY() + ") ghost translate=(" + ghostPanel.getTranslateX() + "," + ghostPanel.getTranslateY() + ")");
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
        final Timeline cd = new Timeline();
        KeyFrame kf = new KeyFrame(Duration.seconds(1), new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                if (cnt[0] > 0) {
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
                    // restore brick position (in case we snapped it to ghost during countdown)
                    try { if (currentViewData != null) doRefreshBrick(currentViewData); } catch (Exception ignored) {}
                    // restore visibility of brick and ghost after countdown
                    javafx.application.Platform.runLater(() -> {
                        try {
                            if (brickPanel != null) brickPanel.setVisible(true);
                            if (ghostPanel != null) ghostPanel.setVisible(true);
                        } catch (Exception ignored) {}
                    });
                    // stop timeline properly
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

    /**
     * Observable property that becomes true when the start countdown finishes and gameplay begins.
     */
    public BooleanProperty countdownFinishedProperty() {
        return countdownFinished;
    }

    /**
     * Expose the game-over property so external coordinators (multiplayer) can listen
     * for when this player's board becomes game over.
     */
    public BooleanProperty isGameOverProperty() {
        return isGameOver;
    }

    /**
     * Adjust the automatic drop interval (milliseconds) used by the game timeline.
     * Call before starting the game to affect falling speed.
     */
    public void setDropIntervalMs(int ms) {
        if (ms <= 0) return;
        this.dropIntervalMs = ms;
        // if timeline already exists, recreate it with new interval
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

    // compute landing position and update ghostPanel visibility/translate
    private void updateGhost(ViewData brick, int[][] boardMatrix) {
        if (brick == null || boardMatrix == null) return;
        int startX = brick.getxPosition();
        int startY = brick.getyPosition();
        int[][] shape = brick.getBrickData();
        // simulate dropping: find the smallest y >= startY such that intersectForGhost becomes true
        int landingY = startY;
        // Determine effective brick height (ignore trailing empty rows at bottom)
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
                // diagnose which cell caused the conflict for debugging
                String reason = "unknown";
                outer:
                for (int i = 0; i < shape.length; i++) {
                    for (int j = 0; j < shape[i].length; j++) {
                        if (shape[i][j] == 0) continue;
                        int tY = y + i;
                        int tX = startX + j;
                        if (tX < 0 || tX >= boardMatrix[0].length) {
                            reason = "horizontal OOB at (" + tX + "," + tY + ")";
                            break outer;
                        }
                        if (tY >= boardMatrix.length) {
                            reason = "below board at (" + tX + "," + tY + ")";
                            break outer;
                        }
                        if (tY >= 0 && boardMatrix[tY][tX] != 0) {
                            reason = "filled cell at (" + tX + "," + tY + ")";
                            break outer;
                        }
                    }
                }
                System.out.println("ghost conflict at trialY=" + y + " reason=" + reason);
                landingY = y - 1;
                break;
            }
            // if no conflict all the way to maxY, land at maxY
            if (y == maxY) landingY = y;
        }

        // place ghostPanel at landingY (account for hidden rows)
        // use measured cell size (cellW/cellH) computed after layout so background tiles and cells align
        // use helper to convert board coords (visible rows) to pixels
        javafx.geometry.Point2D pt = boardToPixel(startX, landingY - 2);
            // snap to whole pixels (boardToPixel already includes nudge)
            ghostPanel.setTranslateX(Math.round(pt.getX()));
            ghostPanel.setTranslateY(Math.round(pt.getY()));
    System.out.println("updateGhost start=(" + startX + "," + startY + ") landingY=" + landingY + " translate=(" + pt.getX() + "," + pt.getY() + ")");

        // update ghost rectangles visibility to match shape
        // Update ghost rectangle visibility. If the mapped board row < 2 (hidden area) or out of horizontal range,
        // hide that ghost cell to avoid overlap with already placed bricks or showing above the view.
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
                // hide if horizontal out-of-range
                if (boardX < 0 || boardX >= boardMatrix[0].length) visible = false;
                // hide if above the visible area (hidden rows)
                if (boardY < 2) visible = false;
                // hide if overlapping an existing cell (safety)
                if (boardY >= 0 && boardY < boardMatrix.length && boardMatrix[boardY][boardX] != 0) visible = false;
                r.setVisible(visible);
            }
        }
    }

    /**
     * Render up to three upcoming bricks in the right-hand preview VBox.
     * This uses a smaller preview cell size (half the normal cellW) so previews fit comfortably.
     */
    public void showNextBricks(java.util.List<com.comp2042.logic.bricks.Brick> upcoming) {
    if (nextContent == null) return;
    nextContent.getChildren().clear();
        if (upcoming == null) return;
        // store cache so we can re-render once actual measured sizes are available
        upcomingCache = new java.util.ArrayList<>(upcoming);
        // Delegate the actual construction to a helper so external callers can reuse the same visuals
        javafx.scene.layout.VBox built = buildNextPreview(upcoming);
        if (built != null) {
            nextContent.getChildren().addAll(built.getChildren());
        }
    }

    /**
     * Build a VBox containing the next-brick preview visuals for a given upcoming list.
     * This returns a standalone node which can be embedded either into the internal nextContent
     * or into an external container (used by multiplayer ScoreBattle layout where embedded nextBox
     * may be clipped inside a SubScene).
     */
    public javafx.scene.layout.VBox buildNextPreview(java.util.List<com.comp2042.logic.bricks.Brick> upcoming) {
        javafx.scene.layout.VBox container = new javafx.scene.layout.VBox(8);
        container.setAlignment(Pos.TOP_CENTER);
        if (upcoming == null || upcoming.isEmpty()) return container;

        // Use the same cell size as the main board so preview blocks match approximately.
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
        // always cache the most recent view data so other UI flows can realign if needed
        this.currentViewData = brick;
        if (isPause.getValue() == Boolean.FALSE) {
            doRefreshBrick(brick);
        }
    }

    // Internal UI update extracted so it can be invoked even when paused (for alignment)
    private void doRefreshBrick(ViewData brick) {
        if (brick == null) return;
        int offsetX = brick.getxPosition();
        int offsetY = brick.getyPosition() - 2; // account for hidden rows

        // Compute integer-aligned pixel positions using unified conversion helper
        javafx.geometry.Point2D pt = boardToPixel(offsetX, offsetY);
        double tx = Math.round(pt.getX() + blockNudgeX);
        double ty = Math.round(pt.getY() + blockNudgeY);

        // Debug: print board->pixel conversion inputs and outputs for diagnosis
        System.out.printf("DBG POS: baseOffset=(%.3f,%.3f) cell=(%.3f,%.3f) boardOffset=(%d,%d) -> pixel=(%.3f,%.3f) rounded=(%.1f,%.1f)%n",
                baseOffsetX, baseOffsetY, cellW, cellH, offsetX, offsetY, pt.getX(), pt.getY(), tx, ty);

        // Snap brick panel to integer pixel positions
        brickPanel.setTranslateX(tx);
        brickPanel.setTranslateY(ty);

        System.out.printf(
            "refreshBrick view(x,y)=%d,%d -> translate=(%.1f,%.1f)%n",
            brick.getxPosition(), brick.getyPosition(), tx, ty
        );

        // Update brick cell rectangles
        int[][] data = brick.getBrickData();
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                Rectangle r = rectangles[i][j];
                int val = data[i][j];
                setRectangleData(val, r);
                r.setVisible(val != 0);
                // Optional: pixel snap individual rects (extra precision on high DPI)
                r.setLayoutX(Math.round(j * cellW));
                r.setLayoutY(Math.round(i * cellH));
            }
        }

        // Update ghost alignment
        updateGhost(brick, currentBoardMatrix);
    }

    // convert board coordinates (visible-origin) to pixel coordinates using measured baseOffset and cell sizes
    // boardX, boardY should be coordinates already adjusted to visible rows (i.e. y = boardRow - 2 when needed)
    private javafx.geometry.Point2D boardToPixel(int boardX, int boardY) {
        double x = baseOffsetX + (boardX * cellW) + nudgeX;
        double y = baseOffsetY + (boardY * cellH) + nudgeY;
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

    private void moveDown(MoveEvent event) {
        if (isPause.getValue() == Boolean.FALSE) {
            // capture starting view so we can animate a lock effect if the piece lands
            ViewData startViewForEffect = this.currentViewData;
            DownData downData = eventListener.onDownEvent(event);
            if (downData.getClearRow() != null && downData.getClearRow().getLinesRemoved() > 0) {
                NotificationPanel notificationPanel = new NotificationPanel("+" + downData.getClearRow().getScoreBonus());
                groupNotification.getChildren().add(notificationPanel);
                notificationPanel.showScore(groupNotification.getChildren());
            }
            refreshBrick(downData.getViewData());
                // if the piece landed (clearRow non-null) play a softer lock effect and spawn explosion when rows removed
                if (downData.getClearRow() != null) {
                    try {
                        // only show lock visual when the user explicitly hard-dropped
                        if (lastWasHardDrop) playLockEffect(startViewForEffect, downData.getViewData(), false);
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
     * Play a vertical light/lock effect from the starting view position to the ending view position.
     * Creates one thin bright rectangle per column occupied by the piece and animates it downwards
     * while fading out. Uses the existing particlePane (if available) as the parent layer.
     *
     * @param start  the ViewData before the drop (may be null)
     * @param end    the ViewData after the drop/lock (may be null)
     * @param intense when true use shorter/faster animation and higher opacity (for hard drops)
     */
    private void playLockEffect(ViewData start, ViewData end, boolean intense) {
        if (start == null || end == null || particlePane == null) return;
        try {
            int[][] shape = start.getBrickData();
            if (shape == null) return;

            int startBoardY = start.getyPosition() - 2; // visible offset
            int endBoardY = end.getyPosition() - 2;

            double cellWpx = cellW;
            double cellHpx = cellH;

            // durations (ms) - increased to give a longer visible effect
            double travelMs = intense ? 420.0 : 560.0; 
            double fadeMs = intense ? 620.0 : 800.0;   

            java.util.List<javafx.animation.ParallelTransition> running = new java.util.ArrayList<>();

            // Determine top-most filled row in the shape so the effect starts at the top of the piece
            int minR = Integer.MAX_VALUE;
            for (int rr = 0; rr < shape.length; rr++) {
                for (int cc = 0; cc < shape[rr].length; cc++) if (shape[rr][cc] != 0) { if (rr < minR) minR = rr; }
            }
            if (minR == Integer.MAX_VALUE) minR = 0;

            // compute a common start Y (top of the brick) in particlePane local coords
            javafx.geometry.Point2D topParentPt = boardToPixel(start.getxPosition(), startBoardY + minR);
            javafx.geometry.Point2D topScenePt = (brickPanel != null && brickPanel.getParent() != null)
                    ? brickPanel.getParent().localToScene(topParentPt)
                    : new javafx.geometry.Point2D(topParentPt.getX(), topParentPt.getY());
            javafx.geometry.Point2D topLocal = (particlePane != null) ? particlePane.sceneToLocal(topScenePt) : topScenePt;

            // Create one visual rectangle per occupied cell so the falling visual matches the brick's shape.
            for (int r = 0; r < shape.length; r++) {
                for (int c = 0; c < shape[r].length; c++) {
                    if (shape[r][c] == 0) continue;
                    int boardX = start.getxPosition() + c;
                    int boardYEnd = endBoardY + r;

                    // compute per-cell X (start) and per-cell end position in particle local coords
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
                    // use the common topLocal Y so effect originates at the top of the piece
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
                    // small per-row delay so the effect cascades downward smoothly
                    tt.setDelay(Duration.millis(r * 18));

                    FadeTransition ft = new FadeTransition(Duration.millis(fadeMs), cellRect);
                    ft.setFromValue(cellRect.getOpacity());
                    ft.setToValue(0.0);

                    ParallelTransition pt = new ParallelTransition(tt, ft);
                    pt.setOnFinished(e -> { try { e.consume(); if (particlePane != null) particlePane.getChildren().remove(cellRect); } catch (Exception ignored) {} });
                    running.add(pt);
                }
            }

            // start all transitions after a layout pass to ensure positions are stable
            javafx.application.Platform.runLater(() -> {
                for (ParallelTransition pt : running) pt.play();
            });

        } catch (Exception ignored) {}
    }

    // Spawn explosions for cleared rows. If ClearRow contains explicit cleared row indices,
    private void spawnExplosion(ClearRow clearRow, ViewData v) {
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
    private void spawnRowClearParticles(ClearRow clearRow) {
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
    private void flashRow(double topY, double width, double height) {
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
    private void shakeBoard() {
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
    private void spawnExplosion(ViewData v) {
        if (v == null || particlePane == null) return;
        int brickX = v.getxPosition();
        int brickY = v.getyPosition() - 2; // visible offset
        double centerX = Math.round(baseOffsetX + brickX * cellW + (cellW * 2) );
        double centerY = Math.round(baseOffsetY + brickY * cellH + (cellH * 2) );
        spawnParticlesAt(centerX, centerY, v.getBrickData());
    }

    private void spawnParticlesAt(double centerX, double centerY, int[][] brickShape) {
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

    /**
     * Configure multiplayer mode. When true, lock visuals only appear when the user explicitly used a hard-drop key.
     */
    public void setMultiplayerMode(boolean multiplayer) {
        this.isMultiplayer = multiplayer;
    }

    /** Register a restart handler callable provided by the multiplayer controller. */
    public void setMultiplayerRestartHandler(Runnable handler) {
        this.multiplayerRestartHandler = handler;
        if (handler != null) this.isMultiplayer = true;
    }

    /** Register a pause handler provided by the multiplayer controller. The handler will be
     * notified when this GuiController pauses or resumes due to a user action. The handler
     * should typically forward the pause state to the other player's GuiController(s).
     */
    public void setMultiplayerPauseHandler(java.util.function.Consumer<Boolean> handler) {
        this.multiplayerPauseHandler = handler;
        if (handler != null) this.isMultiplayer = true;
    }

    /**
     * Apply a pause/unpause request that originated externally (from the multiplayer coordinator).
     * This prevents re-notifying the coordinator while applying the visual overlay and timeline
     * changes locally.
     */
    public void applyExternalPause(boolean paused) {
        // if requested state matches current visible overlay state, no-op
        if (paused == isPauseOverlayVisible) return;
        try {
            suppressMultiplayerPauseNotify = true;
            // togglePauseOverlay will show or hide overlay depending on current state
            // and will normally notify the multiplayer handler, but the suppress flag
            // prevents that re-notification while we're applying an externally-driven change.
            togglePauseOverlay();
        } catch (Exception ignored) {
        } finally {
            suppressMultiplayerPauseNotify = false;
        }
    }

    /** Public wrapper to refresh the visible falling brick from external controllers. */
    public void refreshCurrentView(ViewData v) {
        try { refreshBrick(v); } catch (Exception ignored) {}
    }

    /**
     * Configure per-instance control keys. If any parameter is non-null the controller
     * will only respond to the provided keys. Pass null for any parameter to leave it
     * unassigned. Order: moveLeft, moveRight, rotate, softDrop, hardDrop.
     */
    public void setControlKeys(KeyCode moveLeft, KeyCode moveRight, KeyCode rotate, KeyCode softDrop, KeyCode hardDrop) {
        this.ctrlMoveLeft = moveLeft;
        this.ctrlMoveRight = moveRight;
        this.ctrlRotate = rotate;
        this.ctrlSoftDrop = softDrop;
        this.ctrlHardDrop = hardDrop;
    }

    /** Set the per-instance swap key (pressing it requests a current/next swap). */
    public void setSwapKey(KeyCode swapKey) {
        this.ctrlSwap = swapKey;
    }

    public void bindScore(IntegerProperty integerProperty) {
        // remember the bound score property so we can report the numeric score at game over
        this.currentScoreProperty = integerProperty;

        // store a snapshot of previous high when binding occurs (will be updated at game start)
        // Load high score (UI update only if the node exists in FXML)
        loadHighScore();
        if (highScoreValue != null) {
            // initialize text
            highScoreValue.setText("Highest: " + highScore);
        }
         // show "Current: X"
         scoreValue.textProperty().bind(Bindings.createStringBinding(
                 () -> "Current: " + integerProperty.get(),
                 integerProperty
         ));
 
         // listen for changes to update high score dynamically and animate when beaten
            integerProperty.addListener((obs, oldV, newV) -> {
                // reference unused parameters weakly to satisfy static analyzers (no-op)
                java.util.Objects.requireNonNull(obs);
                java.util.Objects.requireNonNull(oldV);
             int current = newV.intValue();
             if (current > highScore) {
                highScore = current;
                 saveHighScore();
                 if (highScoreValue != null) {
                     highScoreValue.setText("Highest: " + highScore);
                     // scale animation to make it impressive
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
    // mark game over state and stop the clock
    // keep legacy FXML GameOverPanel hidden so we use the new animated overlay instead
    try { if (gameOverPanel != null) gameOverPanel.setVisible(false); } catch (Exception ignored) {}
        isGameOver.setValue(Boolean.TRUE);
        stopClock();
        if (isMultiplayer) return;

        // Show a simpler, elegant centered announcement (no large 'GAME OVER' text)
        javafx.application.Platform.runLater(() -> {
            try {
                if (gameBoard == null || gameBoard.getScene() == null) return;
                javafx.scene.Scene scene = gameBoard.getScene();

                StackPane overlay = new StackPane();
                overlay.setPickOnBounds(true);
                overlay.setStyle("-fx-background-color: transparent;");

                // dim background: make the full-screen overlay nearly-black for a crisp game-over screen
                Rectangle dark = new Rectangle();
                dark.widthProperty().bind(scene.widthProperty());
                dark.heightProperty().bind(scene.heightProperty());
                // use near-opaque black to make the game-over screen appear fully black
                dark.setFill(Color.rgb(0,0,0,0.95));

                // center announcement - use a black dialog panel so the entire screen reads as black
                VBox dialog = new VBox(14);
                dialog.setAlignment(Pos.CENTER);
                dialog.setMouseTransparent(false);
                // opaque black panel with subtle corner radius so the text remains readable
                dialog.setStyle("-fx-background-color: rgba(0,0,0,1.0); -fx-padding: 18px; -fx-background-radius: 8px;");

                // Decorative title: elegant gradient + soft glow
                Text title = new Text("Match Over");
                title.setStyle("-fx-font-weight: 700;");
                title.setFill(javafx.scene.paint.LinearGradient.valueOf("from 0% 0% to 100% 0% , #ffd166 0%, #ff7b7b 100%"));
                title.setOpacity(1.0);
                title.setFont(Font.font(72));
                DropShadow ds = new DropShadow(BlurType.GAUSSIAN, Color.rgb(0,0,0,0.8), 18, 0.25, 0, 6);
                title.setEffect(ds);

        // add a gold glow + gentle pulse + color accent to the title
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

                // build an informative subtitle showing score and time and compare with previous best
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

                // Restart behavior: if multiplayer, delegate to multiplayer restart handler, otherwise create new game and run countdown
                btnRestart.setOnAction(ev -> {
                    ev.consume();
                    try {
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
                        // stop any running title animation
                        try { if (gameOverPulse != null) { gameOverPulse.stop(); gameOverPulse = null; } } catch (Exception ignored) {}
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
        // when starting (or resuming) the clock, ensure startTimeMs is adjusted so
        // pausedElapsedMs is preserved (we want clock to exclude paused durations)
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

    /**
     * Hide the on-board score and time UI elements. Intended for use by
     * multiplayer modes (ClassicBattle) which don't display per-player score/time.
     */
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

    /**
     * Update the level text shown in the right-bottom levelBox (e.g. "Normal", "Hard").
     */
    public void setLevelText(String text) {
        if (levelValue != null) {
            javafx.application.Platform.runLater(() -> levelValue.setText(text));
        }
    }
}
