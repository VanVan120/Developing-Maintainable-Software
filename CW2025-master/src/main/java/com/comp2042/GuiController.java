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
    private Group groupNotification;

    @FXML
    private GameOverPanel gameOverPanel;

    @FXML
    private Text scoreValue;

    @FXML
    private Text highScoreValue;

    @FXML
    private VBox scoreBox;

    private Rectangle[][] displayMatrix;

    private InputEventListener eventListener;

    private Rectangle[][] rectangles;

    // ghost piece rectangles (same dimensions as moving brick)
    private Rectangle[][] ghostRectangles;
    // latest background matrix (including merged bricks) for ghost calculation
    private int[][] currentBoardMatrix;

    private Timeline timeLine;

    // actual cell size measured from the background grid after layout
    private double cellW = BRICK_SIZE;
    private double cellH = BRICK_SIZE;

    private int highScore = 0;
    private static final String HIGHSCORE_FILE = System.getProperty("user.home") + File.separator + ".tetris_highscore";

    private final BooleanProperty isPause = new SimpleBooleanProperty();

    private final BooleanProperty isGameOver = new SimpleBooleanProperty();

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
            } else {
                gamePanel.sceneProperty().addListener(new javafx.beans.value.ChangeListener<>() {
                    @Override
                    public void changed(javafx.beans.value.ObservableValue<? extends javafx.scene.Scene> observable, javafx.scene.Scene oldScene, javafx.scene.Scene newScene) {
                        if (newScene != null) {
                            newScene.addEventHandler(KeyEvent.KEY_PRESSED, pressHandler);
                            newScene.addEventHandler(KeyEvent.KEY_RELEASED, releaseHandler);
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
                } else {
                    cellW = initialCellW;
                    cellH = initialCellH;
                }
            } catch (Exception ex) {
                cellW = initialCellW;
                cellH = initialCellH;
            }
            // reposition brick and ghost rectangles to the measured grid
            for (int i = 0; i < rectangles.length; i++) {
                for (int j = 0; j < rectangles[i].length; j++) {
                    Rectangle rect = rectangles[i][j];
                    if (rect != null) {
                        rect.setLayoutX(j * cellW);
                        rect.setLayoutY(i * cellH);
                    }
                }
            }
            for (int i = 0; i < ghostRectangles.length; i++) {
                for (int j = 0; j < ghostRectangles[i].length; j++) {
                    Rectangle rect = ghostRectangles[i][j];
                    if (rect != null) {
                        rect.setLayoutX(j * cellW);
                        rect.setLayoutY(i * cellH);
                    }
                }
            }
        });


    timeLine = new Timeline(new KeyFrame(
        Duration.millis(DROP_INTERVAL_MS),
        ae -> {
            ae.consume();
            moveDown(new MoveEvent(EventType.DOWN, EventSource.THREAD));
        }
    ));
        timeLine.setCycleCount(Timeline.INDEFINITE);
        timeLine.play();
        // initial ghost render
        updateGhost(brick, currentBoardMatrix);
    }

    // compute landing position and update ghostPanel visibility/translate
    private void updateGhost(ViewData brick, int[][] boardMatrix) {
        if (brick == null || boardMatrix == null) return;
    int startX = brick.getxPosition();
    int startY = brick.getyPosition();
    int[][] shape = brick.getBrickData();
        // simulate dropping: find the smallest y >= startY such that intersectForGhost becomes true
        int landingY = startY;
        // The maximum y we can try is board rows - brick height (so brick bottom doesn't go past board)
        int maxY = boardMatrix.length - shape.length;
        for (int y = startY; y <= maxY; y++) {
            boolean conflict = MatrixOperations.intersectForGhost(boardMatrix, shape, startX, y);
            if (conflict) {
                landingY = y - 1;
                break;
            }
            // if no conflict all the way to maxY, land at maxY
            if (y == maxY) landingY = y;
        }

        // place ghostPanel at landingY (account for hidden rows)
        double cellW = BRICK_SIZE + gamePanel.getHgap();
        double cellH = BRICK_SIZE + gamePanel.getVgap();
    double tx = startX * cellW;
        double ty = (landingY - 2) * cellH;
        // Ensure ghostPanel translation doesn't produce fractional pixel offsets that misalign
        ghostPanel.setTranslateX(Math.round(tx));
        ghostPanel.setTranslateY(Math.round(ty));
    System.out.println("updateGhost start=(" + startX + "," + startY + ") landingY=" + landingY + " translate=(" + tx + "," + ty + ")");

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
        if (isPause.getValue() == Boolean.FALSE) {
            int offsetX = brick.getxPosition();
            int offsetY = brick.getyPosition() - 2; // account for hidden rows
            double cellW = BRICK_SIZE + gamePanel.getHgap();
            double cellH = BRICK_SIZE + gamePanel.getVgap();
            double tx = offsetX * cellW;
            double ty = offsetY * cellH;
            // move the whole brickPanel by full cell offsets so movement is crisp
            brickPanel.setTranslateX(tx);
            brickPanel.setTranslateY(ty);
            System.out.println("refreshBrick view(x,y)=" + brick.getxPosition() + "," + brick.getyPosition() + " -> translate=" + tx + "," + ty);
            for (int i = 0; i < brick.getBrickData().length; i++) {
                for (int j = 0; j < brick.getBrickData()[i].length; j++) {
                    Rectangle r = rectangles[i][j];
                    // update fill and visibility based on brick matrix cell
                    int val = brick.getBrickData()[i][j];
                    setRectangleData(val, r);
                    r.setVisible(val != 0);
                }
            }
            // update ghost using the latest background matrix
            updateGhost(brick, currentBoardMatrix);
        }
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
        }
        gamePanel.requestFocus();
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
        timeLine.stop();
        gameOverPanel.setVisible(true);
        isGameOver.setValue(Boolean.TRUE);
    }

    public void newGame(ActionEvent actionEvent) {
        timeLine.stop();
        gameOverPanel.setVisible(false);
        eventListener.createNewGame();
        gamePanel.requestFocus();
        timeLine.play();
        isPause.setValue(Boolean.FALSE);
        isGameOver.setValue(Boolean.FALSE);
    }

    public void pauseGame(ActionEvent actionEvent) {
        gamePanel.requestFocus();
    }
}
