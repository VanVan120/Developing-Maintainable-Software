package com.comp2042.view;

import com.comp2042.model.ViewData;
import com.comp2042.utils.MatrixOperations;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;



/**
 * Small helper that owns board rendering/layout concerns extracted from GuiController.
 * This is a focused, low-risk extraction: it mirrors the existing behaviour but
 * encapsulates the grid, ghost, and coordinate math so the controller can delegate to it.
 */
public class BoardView {
    public static final int BRICK_SIZE = 24;

    private final GridPane gamePanel;
    private final Pane brickPanel;
    private final Pane ghostPanel;
    private final Canvas bgCanvas;

    private Rectangle[][] displayMatrix;
    private Rectangle[][] rectangles;
    private Rectangle[][] ghostRectangles;
    private int[][] currentBoardMatrix;
    

    private double cellW = BRICK_SIZE;
    private double cellH = BRICK_SIZE;
    private double baseOffsetX = 0.0;
    private double baseOffsetY = 0.0;

    public BoardView(GridPane gamePanel, Pane brickPanel, Pane ghostPanel, Canvas bgCanvas) {
        this.gamePanel = gamePanel;
        this.brickPanel = brickPanel;
        this.ghostPanel = ghostPanel;
        this.bgCanvas = bgCanvas;
    }

    public void initGameView(int[][] boardMatrix, ViewData brick) {
        if (boardMatrix == null || gamePanel == null) return;
        this.currentBoardMatrix = boardMatrix;

        displayMatrix = new Rectangle[boardMatrix.length][boardMatrix[0].length];
        for (int i = 2; i < boardMatrix.length; i++) {
            for (int j = 0; j < boardMatrix[i].length; j++) {
                Rectangle rectangle = new Rectangle(BRICK_SIZE, BRICK_SIZE);
                // show a faint cell border so the grid is visible behind bricks
                rectangle.setFill(Color.TRANSPARENT);
                rectangle.setStroke(Color.rgb(255, 255, 255, 0.06));
                rectangle.setStrokeWidth(0.8);
                rectangle.setArcWidth(8);
                rectangle.setArcHeight(8);
                displayMatrix[i][j] = rectangle;
                gamePanel.add(rectangle, j, i - 2);
            }
        }

        if (brick == null) return;

        rectangles = new Rectangle[brick.getBrickData().length][brick.getBrickData()[0].length];
        double initialCellW = BRICK_SIZE + (gamePanel == null ? 0 : gamePanel.getHgap());
        double initialCellH = BRICK_SIZE + (gamePanel == null ? 0 : gamePanel.getVgap());
        for (int i = 0; i < brick.getBrickData().length; i++) {
            for (int j = 0; j < brick.getBrickData()[i].length; j++) {
                Rectangle rectangle = new Rectangle(BRICK_SIZE, BRICK_SIZE);
                // bricks get a solid-ish fill and a subtle stroke to pop against the grid
                rectangle.setFill(getFillColor(brick.getBrickData()[i][j]));
                rectangle.setStroke(Color.rgb(0,0,0,0.35));
                rectangle.setStrokeWidth(1.0);
                rectangles[i][j] = rectangle;
                rectangle.setLayoutX(j * initialCellW);
                rectangle.setLayoutY(i * initialCellH);
                if (brickPanel != null) brickPanel.getChildren().add(rectangle);
            }
        }

        ghostRectangles = new Rectangle[brick.getBrickData().length][brick.getBrickData()[0].length];
        for (int i = 0; i < ghostRectangles.length; i++) {
            for (int j = 0; j < ghostRectangles[i].length; j++) {
                Rectangle r = new Rectangle(BRICK_SIZE, BRICK_SIZE);
                r.setFill(Color.rgb(200, 200, 200, 0.25));
                r.setStroke(Color.rgb(0,0,0,0.25));
                r.setStrokeWidth(0.8);
                r.setVisible(false);
                ghostRectangles[i][j] = r;
                r.setLayoutX(j * initialCellW);
                r.setLayoutY(i * initialCellH);
                if (ghostPanel != null) ghostPanel.getChildren().add(r);
            }
        }

        Platform.runLater(() -> {
            try {
                // Measure grid origin using first non-null reference cell if possible
                Rectangle ref = null;
                for (int r = 2; r < displayMatrix.length; r++) {
                    for (int c = 0; c < displayMatrix[r].length; c++) {
                        if (displayMatrix[r][c] != null) { ref = displayMatrix[r][c]; break; }
                    }
                    if (ref != null) break;
                }
                if (ref != null) {
                    baseOffsetX = ref.getBoundsInParent().getMinX();
                    baseOffsetY = ref.getBoundsInParent().getMinY();
                } else {
                    baseOffsetX = 0;
                    baseOffsetY = 0;
                }

                // Recompute cell sizes if possible
                // Keep previous values if measurement fails
                try {
                    double measuredW = (rectangles != null && rectangles.length > 0 && rectangles[0].length > 0)
                            ? rectangles[0][0].getBoundsInParent().getWidth() : BRICK_SIZE;
                    double measuredH = (rectangles != null && rectangles.length > 0 && rectangles[0].length > 0)
                            ? rectangles[0][0].getBoundsInParent().getHeight() : BRICK_SIZE;
                    cellW = Math.max(4.0, measuredW + (gamePanel == null ? 0 : gamePanel.getHgap()));
                    cellH = Math.max(4.0, measuredH + (gamePanel == null ? 0 : gamePanel.getVgap()));
                } catch (Exception ex) {
                    // leave defaults
                }

                // snap rectangles to measured grid
                if (rectangles != null) {
                    for (int i = 0; i < rectangles.length; i++) {
                        for (int j = 0; j < rectangles[i].length; j++) {
                            Rectangle rect = rectangles[i][j];
                            if (rect != null) {
                                rect.setLayoutX(Math.round(j * cellW));
                                rect.setLayoutY(Math.round(i * cellH));
                            }
                        }
                    }
                }

                if (ghostRectangles != null) {
                    for (int i = 0; i < ghostRectangles.length; i++) {
                        for (int j = 0; j < ghostRectangles[i].length; j++) {
                            Rectangle rect = ghostRectangles[i][j];
                            if (rect != null) {
                                rect.setLayoutX(Math.round(j * cellW));
                                rect.setLayoutY(Math.round(i * cellH));
                            }
                        }
                    }
                }

                if (bgCanvas != null) {
                    bgCanvas.setTranslateX(Math.round(baseOffsetX));
                    bgCanvas.setTranslateY(Math.round(baseOffsetY));
                    // draw subtle background and grid lines on the canvas
                    try {
                        drawBackgroundGrid(bgCanvas, boardMatrix);
                    } catch (Throwable t) {
                        // non-fatal
                    }
                }

                if (brick != null) refreshBrick(brick);
            } catch (Throwable t) {
                // defensive: don't let layout measurement break the app
                t.printStackTrace();
            }
        });
    }

    private void drawBackgroundGrid(Canvas canvas, int[][] boardMatrix) {
        if (canvas == null || boardMatrix == null) return;
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        // clear
        g.clearRect(0,0,w,h);

        int cols = boardMatrix[0].length;
        int rows = Math.max(0, boardMatrix.length - 2);

        // ensure canvas size roughly matches measured grid; if cell sizes known, resize canvas
        double desiredW = cols * cellW;
        double desiredH = rows * cellH;
        if (desiredW > 0 && desiredH > 0) {
            if (Math.abs(canvas.getWidth() - desiredW) > 1.0) canvas.setWidth(desiredW);
            if (Math.abs(canvas.getHeight() - desiredH) > 1.0) canvas.setHeight(desiredH);
            w = canvas.getWidth();
            h = canvas.getHeight();
        }

        // background fill: dark, slightly gradient-like using two fills
        g.setFill(Color.rgb(18, 30, 33, 0.95));
        g.fillRoundRect(0,0,w,h,14,14);

        // inner darker overlay to make the grid subtle
        g.setFill(Color.rgb(8, 16, 18, 0.18));
        g.fillRoundRect(4,4,w-8,h-8,10,10);

        // no per-cell canvas lines: the grid is rendered by the individual cell Rectangles
        // (this avoids double/darker lines when both canvas and rectangles draw the grid)

        // subtle inner border
        g.setStroke(Color.rgb(0,0,0,0.45));
        g.setLineWidth(2.0);
        g.strokeRoundRect(0.5,0.5,w-1,h-1,14,14);
    }

    public Point2D boardToPixel(int boardX, int boardY) {
        double x = baseOffsetX + (boardX * cellW);
        double y = baseOffsetY + (boardY * cellH);
        return new Point2D(x, y);
    }

    public void refreshGameBackground(int[][] board) {
        this.currentBoardMatrix = board;
        if (board == null || displayMatrix == null) return;
        for (int i = 2; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                setRectangleData(board[i][j], displayMatrix[i][j]);
            }
        }
    }

    /**
     * Map a numeric board color code to a Paint instance. Public so other controllers
     * can reuse the canonical mapping and avoid duplication.
     */
    public static Paint mapCodeToPaint(int i) {
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

    // keep an instance-level helper for internal usage that delegates to the static mapper
    private Paint getFillColor(int i) {
        return mapCodeToPaint(i);
    }

    private void setRectangleData(int color, Rectangle rectangle) {
        if (rectangle == null) return;
        rectangle.setFill(getFillColor(color));
        rectangle.setArcHeight(9);
        rectangle.setArcWidth(9);
    }

    public void refreshBrick(ViewData brick) {
        if (brick == null || brickPanel == null) return;
        doRefreshBrick(brick);
    }

    private void doRefreshBrick(ViewData brick) {
        if (brick == null) return;
        int offsetX = brick.getxPosition();
        int offsetY = brick.getyPosition() - 2;
        // prefer converting using an actual cell node if available to avoid coordinate drift
        Point2D scenePt = null;
        try {
            if (offsetY + 2 >= 0 && offsetY + 2 < displayMatrix.length && offsetX >= 0 && offsetX < displayMatrix[0].length) {
                Rectangle ref = displayMatrix[offsetY + 2][offsetX];
                if (ref != null) scenePt = ref.localToScene(0.0, 0.0);
            }
        } catch (Exception ignored) {}
        if (scenePt == null) scenePt = (gamePanel != null && gamePanel.getParent() != null) ? gamePanel.localToScene(boardToPixel(offsetX, offsetY)) : boardToPixel(offsetX, offsetY);

        try {
            if (brickPanel != null && brickPanel.getParent() != null) {
                Point2D parentLocal = brickPanel.getParent().sceneToLocal(scenePt);
                brickPanel.setTranslateX(Math.round(parentLocal.getX()));
                brickPanel.setTranslateY(Math.round(parentLocal.getY()));
            } else {
                Point2D pt = boardToPixel(offsetX, offsetY);
                brickPanel.setTranslateX(Math.round(pt.getX()));
                brickPanel.setTranslateY(Math.round(pt.getY()));
            }
        } catch (Exception ex) {
            Point2D pt = boardToPixel(offsetX, offsetY);
            brickPanel.setTranslateX(Math.round(pt.getX()));
            brickPanel.setTranslateY(Math.round(pt.getY()));
        }

        int[][] data = brick.getBrickData();
        if (rectangles == null) return;
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                Rectangle r = rectangles[i][j];
                int val = data[i][j];
                setRectangleData(val, r);
                if (r != null) r.setVisible(val != 0);
                if (r != null) r.setLayoutX(Math.round(j * cellW));
                if (r != null) r.setLayoutY(Math.round(i * cellH));
            }
        }

        updateGhost(brick, currentBoardMatrix);
    }

    public void updateGhost(ViewData brick, int[][] boardMatrix) {
        if (brick == null || boardMatrix == null || ghostPanel == null || ghostRectangles == null) return;
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
        int maxY = boardMatrix.length - effectiveBrickHeight;
        for (int y = startY; y <= maxY; y++) {
            boolean conflict = MatrixOperations.intersectForGhost(boardMatrix, shape, startX, y);
            if (conflict) { landingY = y - 1; break; }
            if (y == maxY) landingY = y;
        }

        // compute a scene point using the precise display cell when possible to avoid rounding/offset errors
        Point2D scenePt = null;
        try {
            int refRow = landingY; // landingY already matches the boardMatrix row index used by displayMatrix
            if (refRow >= 0 && refRow < displayMatrix.length && startX >= 0 && startX < displayMatrix[0].length) {
                Rectangle ref = displayMatrix[refRow][startX];
                if (ref != null) scenePt = ref.localToScene(0.0, 0.0);
            }
        } catch (Exception ignored) {}
        if (scenePt == null) scenePt = (gamePanel != null && gamePanel.getParent() != null) ? gamePanel.localToScene(boardToPixel(startX, landingY - 2)) : boardToPixel(startX, landingY - 2);

        try {
            if (ghostPanel != null && ghostPanel.getParent() != null) {
                Point2D parentLocal = ghostPanel.getParent().sceneToLocal(scenePt);
                ghostPanel.setTranslateX(Math.round(parentLocal.getX()));
                ghostPanel.setTranslateY(Math.round(parentLocal.getY()));
            } else {
                Point2D pt = boardToPixel(startX, landingY - 2);
                ghostPanel.setTranslateX(Math.round(pt.getX()));
                ghostPanel.setTranslateY(Math.round(pt.getY()));
            }
        } catch (Exception ex) {
            Point2D pt = boardToPixel(startX, landingY - 2);
            ghostPanel.setTranslateX(Math.round(pt.getX()));
            ghostPanel.setTranslateY(Math.round(pt.getY()));
        }

        for (int i = 0; i < shape.length; i++) {
            for (int j = 0; j < shape[i].length; j++) {
                Rectangle r = ghostRectangles[i][j];
                if (r == null) continue;
                if (shape[i][j] == 0) { r.setVisible(false); continue; }
                int boardY = landingY + i;
                int boardX = startX + j;
                boolean visible = true;
                if (boardX < 0 || boardX >= boardMatrix[0].length) visible = false;
                if (boardY < 2) visible = false;
                if (boardY >= 0 && boardY < boardMatrix.length && boardMatrix[boardY][boardX] != 0) visible = false;
                r.setVisible(visible);
            }
        }
    }

    // small utility used by callers that may need measured sizes
    public double getCellWidth() { return cellW; }
    public double getCellHeight() { return cellH; }
    public double getBaseOffsetX() { return baseOffsetX; }
    public double getBaseOffsetY() { return baseOffsetY; }

    // number of columns in the board (visible grid width)
    public int getColumns() {
        try { return (displayMatrix != null && displayMatrix.length > 0) ? displayMatrix[0].length : 10; } catch (Exception ignored) { return 10; }
    }

    // safely return the fill Paint for a given board cell (row, col)
    public Paint getCellFill(int boardRow, int boardCol) {
        try {
            if (displayMatrix != null && boardRow >= 0 && boardRow < displayMatrix.length && boardCol >= 0 && boardCol < displayMatrix[0].length) {
                Rectangle r = displayMatrix[boardRow][boardCol];
                if (r != null) return r.getFill();
            }
        } catch (Exception ignored) {}
        return Color.TRANSPARENT;
    }

    /**
     * Returns the scene coordinates of the top-left corner of the given board cell.
     * If an actual display Rectangle is available it's used for exact positioning,
     * otherwise falls back to converting the computed boardToPixel point via the gamePanel.
     */
    public javafx.geometry.Point2D boardCellScenePoint(int boardX, int boardY) {
        try {
            if (displayMatrix != null && boardY >= 0 && boardY < displayMatrix.length && boardX >= 0 && boardX < displayMatrix[0].length) {
                Rectangle ref = displayMatrix[boardY][boardX];
                if (ref != null) return ref.localToScene(0.0, 0.0);
            }
        } catch (Exception ignored) {}
        try {
            javafx.geometry.Point2D pt = boardToPixel(boardX, boardY);
            return (gamePanel != null) ? gamePanel.localToScene(pt) : pt;
        } catch (Exception ignored) {
            return new javafx.geometry.Point2D(baseOffsetX + boardX * cellW, baseOffsetY + boardY * cellH);
        }
    }

}
