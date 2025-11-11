package com.comp2042.controller.cooperateBattle.coopGUI;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.comp2042.model.ViewData;
import com.comp2042.utils.MatrixOperations;
import com.comp2042.view.BoardView;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Encapsulates the second player's brick and ghost panels and
 * provides build/refresh methods formerly present in the controller.
 */
public class CoopSecondPlayerView {
    private static final Logger LOGGER = Logger.getLogger(CoopSecondPlayerView.class.getName());

    private final Pane secondBrickPanel;
    private final Pane secondGhostPanel;
    private Rectangle[][] rectangles;
    private Rectangle[][] ghostRectangles;

    public CoopSecondPlayerView(Pane secondBrickPanel, Pane secondGhostPanel) {
        this.secondBrickPanel = secondBrickPanel;
        this.secondGhostPanel = secondGhostPanel;
    }

    /**
     * Build the visual rectangles for the given preview/brick shape.
     * hgap and vgap are added to the provided cell dimensions to match layout spacing.
     */
    public void build(ViewData rightView, double cellW, double cellH, double hgap, double vgap) {
        if (rightView == null || secondBrickPanel == null || secondGhostPanel == null) return;
        try {
            int[][] shape = rightView.getBrickData();
            if (shape == null || shape.length == 0) return;
            int rows = shape.length;
            int cols = shape[0].length;
            rectangles = new Rectangle[rows][cols];
            ghostRectangles = new Rectangle[rows][cols];
            double initialCellW = cellW + (hgap);
            double initialCellH = cellH + (vgap);
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    Rectangle r = new Rectangle(Math.max(4.0, initialCellW - 2), Math.max(4.0, initialCellH - 2));
                    r.setFill(com.comp2042.view.BoardView.mapCodeToPaint(shape[i][j]));
                    r.setLayoutX(j * initialCellW);
                    r.setLayoutY(i * initialCellH);
                    rectangles[i][j] = r;
                    secondBrickPanel.getChildren().add(r);

                    Rectangle g = new Rectangle(Math.max(4.0, initialCellW - 2), Math.max(4.0, initialCellH - 2));
                    g.setFill(Color.rgb(200,200,200,0.25));
                    g.setVisible(false);
                    g.setLayoutX(j * initialCellW);
                    g.setLayoutY(i * initialCellH);
                    ghostRectangles[i][j] = g;
                    secondGhostPanel.getChildren().add(g);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Failed to build second player visuals", e);
        }
    }

    /**
     * Refresh the second player's view. All external state required is passed in.
     * - v: the ViewData for the second player
     * - boardMatrix: the current board matrix
     * - boardView: optional BoardView to compute scene coordinates (can be null)
     * - currentViewData: the current primary view data (used when computing ghost intersection)
     * - cellW/cellH: single-cell dimensions in pixels
     * - baseOffsetX/baseOffsetY: fallback base offset used when scene coordinates are not available
     */
    public void refresh(ViewData v, int[][] boardMatrix, BoardView boardView, ViewData currentViewData, double cellW, double cellH, double baseOffsetX, double baseOffsetY) {
        if (v == null || rectangles == null) return;
        try {
            int offsetX = v.getxPosition();
            int offsetY = v.getyPosition() - 2;

            // Position second player's brick panel using precise scene coordinates when possible
            try {
                javafx.geometry.Point2D scenePt = null;
                if (boardView != null) scenePt = boardView.boardCellScenePoint(offsetX, offsetY + 2);
                if (scenePt != null && secondBrickPanel != null && secondBrickPanel.getParent() != null) {
                    javafx.geometry.Point2D parentLocal = secondBrickPanel.getParent().sceneToLocal(scenePt);
                    secondBrickPanel.setTranslateX(Math.round(parentLocal.getX()));
                    secondBrickPanel.setTranslateY(Math.round(parentLocal.getY()));
                } else {
                    javafx.geometry.Point2D pt = boardToPixelLocal(offsetX, offsetY, cellW, cellH, baseOffsetX, baseOffsetY);
                    secondBrickPanel.setTranslateX(Math.round(pt.getX()));
                    secondBrickPanel.setTranslateY(Math.round(pt.getY()));
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINER, "Failed to position secondBrickPanel using scene point, falling back to boardToPixelLocal", e);
                try {
                    javafx.geometry.Point2D pt = boardToPixelLocal(offsetX, offsetY, cellW, cellH, baseOffsetX, baseOffsetY);
                    secondBrickPanel.setTranslateX(Math.round(pt.getX()));
                    secondBrickPanel.setTranslateY(Math.round(pt.getY()));
                } catch (Exception ignored2) {
                    LOGGER.log(Level.FINER, "Fallback positioning of secondBrickPanel failed", ignored2);
                }
            }

            int[][] data = v.getBrickData();
            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < data[i].length; j++) {
                    Rectangle r = rectangles[i][j];
                    int val = data[i][j];
                    r.setFill(com.comp2042.view.BoardView.mapCodeToPaint(val));
                    r.setVisible(val != 0);
                    r.setLayoutX(Math.round(j * cellW));
                    r.setLayoutY(Math.round(i * cellH));
                }
            }

            int[][] shape = v.getBrickData();
            int startX = v.getxPosition();
            int startY = v.getyPosition();
            int landingY = startY;
            int effectiveBrickHeight = shape.length;
            for (int i = shape.length - 1; i >= 0; i--) {
                boolean rowHas = false;
                for (int j = 0; j < shape[i].length; j++) if (shape[i][j] != 0) { rowHas = true; break; }
                if (rowHas) { effectiveBrickHeight = i + 1; break; }
            }
            int maxY = (boardMatrix != null ? boardMatrix.length : 0) - effectiveBrickHeight;
            for (int y = startY; y <= maxY; y++) {
                int[][] tmp = MatrixOperations.copy(boardMatrix != null ? boardMatrix : new int[0][0]);
                try { tmp = MatrixOperations.merge(tmp, currentViewData != null ? currentViewData.getBrickData() : new int[0][0], currentViewData != null ? currentViewData.getxPosition() : 0, currentViewData != null ? currentViewData.getyPosition() : 0); } catch (Exception ignored) {}
                boolean conflict = MatrixOperations.intersectForGhost(tmp, shape, startX, y);
                if (conflict) { landingY = y - 1; break; }
                if (y == maxY) landingY = y;
            }

            try {
                javafx.geometry.Point2D scenePt = null;
                if (boardView != null) scenePt = boardView.boardCellScenePoint(startX, landingY);
                if (scenePt != null && secondGhostPanel != null && secondGhostPanel.getParent() != null) {
                    javafx.geometry.Point2D parentLocal = secondGhostPanel.getParent().sceneToLocal(scenePt);
                    secondGhostPanel.setTranslateX(Math.round(parentLocal.getX()));
                    secondGhostPanel.setTranslateY(Math.round(parentLocal.getY()));
                } else {
                    javafx.geometry.Point2D gpt = boardToPixelLocal(startX, landingY - 2, cellW, cellH, baseOffsetX, baseOffsetY);
                    secondGhostPanel.setTranslateX(Math.round(gpt.getX()));
                    secondGhostPanel.setTranslateY(Math.round(gpt.getY()));
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINER, "Failed to position secondGhostPanel using scene point, falling back to boardToPixelLocal", e);
                try {
                    javafx.geometry.Point2D gpt = boardToPixelLocal(startX, landingY - 2, cellW, cellH, baseOffsetX, baseOffsetY);
                    secondGhostPanel.setTranslateX(Math.round(gpt.getX()));
                    secondGhostPanel.setTranslateY(Math.round(gpt.getY()));
                } catch (Exception ignored2) {
                    LOGGER.log(Level.FINER, "Fallback positioning of secondGhostPanel failed", ignored2);
                }
            }

            for (int i = 0; i < shape.length; i++) for (int j = 0; j < shape[i].length; j++) {
                Rectangle r = ghostRectangles[i][j];
                if (r == null) continue;
                if (shape[i][j] == 0) { r.setVisible(false); continue; }
                int boardY = landingY + i;
                int boardX = startX + j;
                boolean visible = true;
                if (boardMatrix == null || boardMatrix.length == 0) visible = false;
                if (boardX < 0 || (boardMatrix != null && boardMatrix[0] != null && boardX >= boardMatrix[0].length)) visible = false;
                if (boardY < 2) visible = false;
                if (boardY >= 0 && boardMatrix != null && boardY < boardMatrix.length && boardMatrix[boardY][boardX] != 0) visible = false;
                r.setVisible(visible);
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Failed during refreshSecondView", e);
        }
    }

    private javafx.geometry.Point2D boardToPixelLocal(int boardX, int boardY, double cellW, double cellH, double baseOffsetX, double baseOffsetY) {
        double x = baseOffsetX + (boardX * cellW);
        double y = baseOffsetY + (boardY * cellH);
        return new javafx.geometry.Point2D(x, y);
    }

    public void setVisible(boolean visible) {
        try { if (secondBrickPanel != null) secondBrickPanel.setVisible(visible); } catch (Exception ignored) {}
        try { if (secondGhostPanel != null) secondGhostPanel.setVisible(visible); } catch (Exception ignored) {}
    }

    public Pane getSecondBrickPanel() { return secondBrickPanel; }
    public Pane getSecondGhostPanel() { return secondGhostPanel; }
}
