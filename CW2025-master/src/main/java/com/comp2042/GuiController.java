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

    private int highScore = 0;
    private static final String HIGHSCORE_FILE = System.getProperty("user.home") + File.separator + ".tetris_highscore";

    private final BooleanProperty isPause = new SimpleBooleanProperty();

    private final BooleanProperty isGameOver = new SimpleBooleanProperty();

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
            if (gameBoard.getScene() != null) {
                gameBoard.layoutXProperty().bind(gameBoard.getScene().widthProperty().subtract(gameBoard.widthProperty()).divide(2));
                gameBoard.layoutYProperty().bind(gameBoard.getScene().heightProperty().subtract(gameBoard.heightProperty()).divide(2));
            }

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

        // Pause button handler (if present in FXML)
        try {
            if (pauseBtn != null) {
                pauseBtn.setOnAction(ev -> togglePauseOverlay());
            }
        } catch (Exception ignored) {}
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
                        // TODO: open settings dialog; for now hide overlay
                        hidePauseOverlay();
                    });

                    buttons.getChildren().addAll(resume, settings);
                    dialog.getChildren().addAll(title, buttons);

                    pauseOverlay.getChildren().addAll(dark, dialog);

                    if (scene.getRoot() instanceof javafx.scene.layout.Pane) {
                        javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) scene.getRoot();
                        root.getChildren().add(pauseOverlay);
                    } else if (groupNotification != null) {
                        groupNotification.getChildren().add(pauseOverlay);
                    }

                    // stop gameplay timeline and block input
                    try { if (timeLine != null) timeLine.pause(); } catch (Exception ignored) {}
                    isPause.setValue(Boolean.TRUE);
                    isPauseOverlayVisible = true;
                } catch (Exception ignored) {}
            });
        } else {
            hidePauseOverlay();
        }
    }

    private void hidePauseOverlay() {
        javafx.application.Platform.runLater(() -> {
            try {
                if (pauseOverlay != null) {
                    if (pauseOverlay.getParent() instanceof javafx.scene.layout.Pane) {
                        ((javafx.scene.layout.Pane) pauseOverlay.getParent()).getChildren().remove(pauseOverlay);
                    } else if (groupNotification != null) {
                        groupNotification.getChildren().remove(pauseOverlay);
                    }
                    pauseOverlay = null;
                }
                // resume timeline and input
                try { if (timeLine != null) timeLine.play(); } catch (Exception ignored) {}
                isPause.setValue(Boolean.FALSE);
                isPauseOverlayVisible = false;
            } catch (Exception ignored) {}
        });
    }

    // Key processing helpers attached to the Scene to ensure they receive events
    private void processKeyPressed(KeyEvent keyEvent) {
        if (isPause.getValue() == Boolean.FALSE && isGameOver.getValue() == Boolean.FALSE) {
            if (keyEvent.getCode() == KeyCode.LEFT || keyEvent.getCode() == KeyCode.A) {
                refreshBrick(eventListener.onLeftEvent(new MoveEvent(EventType.LEFT, EventSource.USER)));
                keyEvent.consume();
            } else if (keyEvent.getCode() == KeyCode.RIGHT || keyEvent.getCode() == KeyCode.D) {
                refreshBrick(eventListener.onRightEvent(new MoveEvent(EventType.RIGHT, EventSource.USER)));
                keyEvent.consume();
            } else if (keyEvent.getCode() == KeyCode.UP || keyEvent.getCode() == KeyCode.W) {
                refreshBrick(eventListener.onRotateEvent(new MoveEvent(EventType.ROTATE, EventSource.USER)));
                keyEvent.consume();
            } else if (keyEvent.getCode() == KeyCode.DOWN || keyEvent.getCode() == KeyCode.S) {
                // soft-drop: speed up timeline while key is held
                if (timeLine != null) timeLine.setRate(SOFT_DROP_RATE);
                moveDown(new MoveEvent(EventType.DOWN, EventSource.USER));
                keyEvent.consume();
            } else if (keyEvent.getCode() == KeyCode.SPACE) {
                // hard drop: immediately drop to the bottom
                hardDrop();
                keyEvent.consume();
            }
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
                    if (d.getClearRow() != null && d.getClearRow().getLinesRemoved() > 0) {
                            try { spawnExplosion(d.getClearRow(), d.getViewData()); } catch (Exception ignored) {}
                        }
                    if (d.getClearRow() != null) {
                        break;
                    }
            }
            if (timeLine != null) timeLine.play();
        }
    }

    private void processKeyReleased(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.DOWN || keyEvent.getCode() == KeyCode.S) {
            if (timeLine != null) timeLine.setRate(NORMAL_RATE);
            keyEvent.consume();
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
        // prevent any user input until countdown completes
        isPause.setValue(Boolean.TRUE);
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
                }
                cnt[0] = cnt[0] - 1;
            }
        });
        cd.getKeyFrames().add(kf);
        cd.setCycleCount(seconds + 2);
        cd.playFromStart();
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

    // Use the same cell size as the main board so preview blocks match exactly.
    // Avoid premature rounding when computing offsets so previews can be centered precisely.
    double pW = Math.max(4.0, cellW);
    double pH = Math.max(4.0, cellH);

        int count = Math.min(upcoming.size(), 3);
        for (int i = 0; i < count; i++) {
            com.comp2042.logic.bricks.Brick b = upcoming.get(i);
            int[][] shape = b.getShapeMatrix().get(0); // default orientation for preview
            int rows = shape.length;
            int cols = shape[0].length;
            javafx.scene.layout.StackPane slot = new javafx.scene.layout.StackPane();
            // keep the slot size based on full matrix so layout remains stable
            slot.setPrefWidth(cols * pW + 8.0);
            slot.setPrefHeight(rows * pH + 8.0);
            slot.setStyle("-fx-background-color: transparent;");
            // compute bounding box of filled cells so we can center the visible piece
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
                // empty shape? place at center
                minR = 0; minC = 0; maxR = rows - 1; maxC = cols - 1;
            }
            int visibleCols = maxC - minC + 1;
            int visibleRows = maxR - minR + 1;
            // inner pane sized to the visible bounding box; StackPane will center it inside slot
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
            nextContent.getChildren().add(slot);
        }
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
            DownData downData = eventListener.onDownEvent(event);
            if (downData.getClearRow() != null && downData.getClearRow().getLinesRemoved() > 0) {
                NotificationPanel notificationPanel = new NotificationPanel("+" + downData.getClearRow().getScoreBonus());
                groupNotification.getChildren().add(notificationPanel);
                notificationPanel.showScore(groupNotification.getChildren());
            }
            refreshBrick(downData.getViewData());
                // spawn explosion only when the landing cleared >=1 full rows
                if (downData.getClearRow() != null && downData.getClearRow().getLinesRemoved() > 0) {
                    try { spawnExplosion(downData.getClearRow(), downData.getViewData()); } catch (Exception ignored) {}
                }
        }
        gamePanel.requestFocus();
    }

    // Spawn explosions for cleared rows. If ClearRow contains explicit cleared row indices,
    // spawn one particle burst per cleared absolute board row. If none provided, fall back to
    // spawning at the landed brick position using ViewData.
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

    public void bindScore(IntegerProperty integerProperty) {
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
        try { if (timeLine != null) timeLine.stop(); } catch (Exception ignored) {}
    // mark game over state and stop the clock
    // keep legacy FXML GameOverPanel hidden so we use the new animated overlay instead
    try { if (gameOverPanel != null) gameOverPanel.setVisible(false); } catch (Exception ignored) {}
        isGameOver.setValue(Boolean.TRUE);
        stopClock();

        // Show an animated modal overlay: fade-to-black background + centered dialog
        javafx.application.Platform.runLater(() -> {
            try {
                if (gameBoard == null || gameBoard.getScene() == null) return;
                javafx.scene.Scene scene = gameBoard.getScene();
                // overlay root that fills the scene
                StackPane overlay = new StackPane();
                overlay.setPickOnBounds(true);
                overlay.setStyle("-fx-background-color: transparent;");

                // dark background that will fade in
                Rectangle dark = new Rectangle();
                dark.widthProperty().bind(scene.widthProperty());
                dark.heightProperty().bind(scene.heightProperty());
                dark.setFill(Color.BLACK);
                dark.setOpacity(0.0);

                // dialog content
                VBox dialog = new VBox(18);
                dialog.setAlignment(Pos.CENTER);

                Label title = new Label("GAME OVER");
                title.getStyleClass().add("gameOverStyle");
                title.setStyle("-fx-font-size: 64px; -fx-text-fill: white; -fx-font-weight: bold;");
                title.setOpacity(0.0);

                HBox buttons = new HBox(12);
                buttons.setAlignment(Pos.CENTER);

                Button btnMenu = new Button("Return to Main Menu");
                Button btnExit = new Button("Exit");
                // reuse menu-button styling if available for a consistent look
                btnMenu.getStyleClass().add("menu-button");
                btnExit.getStyleClass().add("menu-button");

                // Return to main menu: load mainMenu.fxml and replace the scene root
                btnMenu.setOnAction(ev -> {
                    ev.consume();
                    try {
                        URL loc = getClass().getClassLoader().getResource("mainMenu.fxml");
                        if (loc == null) return;
                        FXMLLoader loader = new FXMLLoader(loc);
                        Parent menuRoot = loader.load();
                        javafx.stage.Stage stage = (javafx.stage.Stage) scene.getWindow();
                        if (stage.getScene() != null) {
                            stage.getScene().setRoot(menuRoot);
                        } else {
                            Scene s2 = new Scene(menuRoot, Math.max(420, stage.getWidth()), Math.max(700, stage.getHeight()));
                            stage.setScene(s2);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });

                // Exit the application
                btnExit.setOnAction(ev -> {
                    ev.consume();
                    try { javafx.application.Platform.exit(); } catch (Exception ignored) {}
                });

                buttons.getChildren().addAll(btnMenu, btnExit);
                dialog.getChildren().addAll(title, buttons);

                // make overlay consume input so underlying game is inaccessible
                overlay.setOnMouseClicked(event -> event.consume());
                overlay.getChildren().addAll(dark, dialog);

                // attach to scene root if it's a Pane
                if (scene.getRoot() instanceof javafx.scene.layout.Pane) {
                    javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) scene.getRoot();
                    root.getChildren().add(overlay);
                } else if (groupNotification != null) {
                    // fallback: add to notification group
                    groupNotification.getChildren().add(overlay);
                }

                // animations: fade-in background then pop the title and buttons
                FadeTransition bgFade = new FadeTransition(Duration.millis(400), dark);
                bgFade.setFromValue(0.0);
                bgFade.setToValue(0.85);

                ScaleTransition titleScale = new ScaleTransition(Duration.millis(600), title);
                titleScale.setFromX(0.3); titleScale.setFromY(0.3);
                titleScale.setToX(1.0); titleScale.setToY(1.0);
                FadeTransition titleFade = new FadeTransition(Duration.millis(600), title);
                titleFade.setFromValue(0.0); titleFade.setToValue(1.0);

                FadeTransition buttonsFade = new FadeTransition(Duration.millis(400), buttons);
                buttonsFade.setFromValue(0.0); buttonsFade.setToValue(1.0);

                // sequence: background then title+buttons
                bgFade.play();
                bgFade.setOnFinished(e -> {
                    e.consume();
                    // play title + buttons together
                    titleScale.play();
                    titleFade.play();
                    buttonsFade.play();
                });
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
        startTimeMs = System.currentTimeMillis();
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
