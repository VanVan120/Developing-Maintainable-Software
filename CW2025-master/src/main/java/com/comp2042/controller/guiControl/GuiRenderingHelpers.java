package com.comp2042.controller.guiControl;

import com.comp2042.model.ViewData;
import com.comp2042.utils.MatrixOperations;
import com.comp2042.view.BoardView;
import javafx.geometry.Point2D;
import javafx.scene.shape.Rectangle;

/**
 * Rendering helpers extracted from GuiController to shrink the controller.
 */
class GuiRenderingHelpers {

    static void updateGhost(GuiController owner, ViewData brick, int[][] boardMatrix) {
        if (owner.getBoardView() != null) {
            owner.getBoardView().updateGhost(brick, boardMatrix);
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
        int maxY = boardMatrix.length - effectiveBrickHeight;
        for (int y = startY; y <= maxY; y++) {
            boolean conflict = MatrixOperations.intersectForGhost(boardMatrix, shape, startX, y);
            if (conflict) { landingY = y - 1; break; }
            if (y == maxY) landingY = y;
        }

        Point2D pt = boardToPixel(owner, startX, landingY - 2);
        try {
            Point2D scenePt = (owner.gamePanel != null && owner.gamePanel.getParent() != null) ? owner.gamePanel.localToScene(pt) : pt;
            if (owner.ghostPanel != null && owner.ghostPanel.getParent() != null) {
                Point2D parentLocal = owner.ghostPanel.getParent().sceneToLocal(scenePt);
                owner.ghostPanel.setTranslateX(Math.round(parentLocal.getX()));
                owner.ghostPanel.setTranslateY(Math.round(parentLocal.getY()));
            } else {
                owner.ghostPanel.setTranslateX(Math.round(pt.getX()));
                owner.ghostPanel.setTranslateY(Math.round(pt.getY()));
            }
        } catch (Exception ex) {
            owner.ghostPanel.setTranslateX(Math.round(pt.getX()));
            owner.ghostPanel.setTranslateY(Math.round(pt.getY()));
        }

        for (int i = 0; i < shape.length; i++) {
            for (int j = 0; j < shape[i].length; j++) {
                Rectangle r = owner.ghostRectangles[i][j];
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

    static void doRefreshBrick(GuiController owner, ViewData brick) {
        if (owner.getBoardView() != null) { owner.getBoardView().refreshBrick(brick); return; }
        if (brick == null) return;
        int offsetX = brick.getxPosition();
        int offsetY = brick.getyPosition() - 2;
        Point2D pt = boardToPixel(owner, offsetX, offsetY);
        try {
            Point2D scenePt = (owner.gamePanel != null && owner.gamePanel.getParent() != null) ? owner.gamePanel.localToScene(pt) : pt;
            if (owner.brickPanel != null && owner.brickPanel.getParent() != null) {
                Point2D parentLocal = owner.brickPanel.getParent().sceneToLocal(scenePt);
                owner.brickPanel.setTranslateX(Math.round(parentLocal.getX()));
                owner.brickPanel.setTranslateY(Math.round(parentLocal.getY()));
            } else {
                owner.brickPanel.setTranslateX(Math.round(pt.getX()));
                owner.brickPanel.setTranslateY(Math.round(pt.getY()));
            }
        } catch (Exception ex) {
            owner.brickPanel.setTranslateX(Math.round(pt.getX()));
            owner.brickPanel.setTranslateY(Math.round(pt.getY()));
        }

        int[][] data = brick.getBrickData();
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                Rectangle r = owner.rectangles[i][j];
                int val = data[i][j];
                setRectangleData(owner, val, r);
                r.setVisible(val != 0);
                r.setLayoutX(Math.round(j * owner.cellW));
                r.setLayoutY(Math.round(i * owner.cellH));
            }
        }

        updateGhost(owner, brick, owner.currentBoardMatrix);
    }

    static void refreshGameBackground(GuiController owner, int[][] board) {
        if (owner.getBoardView() != null) { owner.getBoardView().refreshGameBackground(board); return; }
        owner.currentBoardMatrix = board;
        for (int i = 2; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                setRectangleData(owner, board[i][j], owner.displayMatrix[i][j]);
            }
        }
    }

    static void setRectangleData(GuiController owner, int color, Rectangle rectangle) {
        rectangle.setFill(BoardView.mapCodeToPaint(color));
        rectangle.setArcHeight(9);
        rectangle.setArcWidth(9);
    }

    static Point2D boardToPixel(GuiController owner, int boardX, int boardY) {
        if (owner.getBoardView() != null) return owner.getBoardView().boardToPixel(boardX, boardY);
        double x = owner.baseOffsetX + (boardX * owner.cellW);
        double y = owner.baseOffsetY + (boardY * owner.cellH);
        return new Point2D(x, y);
    }
}
