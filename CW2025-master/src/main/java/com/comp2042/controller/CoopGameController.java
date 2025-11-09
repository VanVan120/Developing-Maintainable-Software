package com.comp2042.controller;

import com.comp2042.logic.bricks.Brick;
import com.comp2042.logic.bricks.RandomBrickGenerator;
import com.comp2042.model.ViewData;
import com.comp2042.model.DownData;
import com.comp2042.model.CoopTickResult;
import com.comp2042.model.ClearRow;
import com.comp2042.model.NextShapeInfo;
import com.comp2042.model.Score;
import com.comp2042.model.CoopScore;
import com.comp2042.utils.MatrixOperations;
import com.comp2042.utils.BrickRotator;

import java.awt.Point;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class CoopGameController {

    private final int width;
    private final int height;
    private int[][] boardMatrix;
    private final RandomBrickGenerator leftGenerator = new RandomBrickGenerator();
    private final RandomBrickGenerator rightGenerator = new RandomBrickGenerator();
    private final BrickRotator leftRotator = new BrickRotator();
    private final BrickRotator rightRotator = new BrickRotator();
    private Point leftOffset;
    private Point rightOffset;
    private final Score leftScore = new Score();
    private final Score rightScore = new Score();
    private final CoopScore totalScore = new CoopScore();
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(CoopGameController.class.getName());
    private final BooleanProperty isGameOver = new SimpleBooleanProperty(false);

    public CoopGameController(int width, int height) {
        this.width = width;
        this.height = height;
        this.boardMatrix = new int[height][width];
    }

    public int[][] getBoardMatrix() { return MatrixOperations.copy(boardMatrix); }
    public javafx.beans.property.IntegerProperty getLeftScoreProperty() { return leftScore.scoreProperty(); }
    public javafx.beans.property.IntegerProperty getRightScoreProperty() { return rightScore.scoreProperty(); }
    public java.util.List<com.comp2042.logic.bricks.Brick> getUpcomingLeft(int count) { return leftGenerator.getUpcomingBricks(count); }
    public java.util.List<com.comp2042.logic.bricks.Brick> getUpcomingRight(int count) { return rightGenerator.getUpcomingBricks(count); }
    public javafx.beans.property.IntegerProperty getTotalScoreProperty() { return totalScore.scoreProperty(); }
    public javafx.beans.property.IntegerProperty getTotalHighScoreProperty() { return totalScore.highScoreProperty(); }

    public ViewData getViewDataLeft() {
        if (leftOffset == null) return null;
        return new ViewData(leftRotator.getCurrentShape(), (int) leftOffset.getX(), (int) leftOffset.getY(), leftGenerator.getNextBrick().getShapeMatrix().get(0));
    }

    public ViewData getViewDataRight() {
        if (rightOffset == null) return null;
        return new ViewData(rightRotator.getCurrentShape(), (int) rightOffset.getX(), (int) rightOffset.getY(), rightGenerator.getNextBrick().getShapeMatrix().get(0));
    }

    public void createNewGame() {
        boardMatrix = new int[height][width];
        leftScore.reset();
        rightScore.reset();
        totalScore.reset();
        try { isGameOver.set(false); } catch (Exception e) { LOGGER.log(Level.FINER, "Failed to reset isGameOver property", e); }
        spawnLeft();
        spawnRight();
    }

    private boolean spawnLeft() {
        Brick b = leftGenerator.getBrick();
        leftRotator.setBrick(b);
        int[][] shape = leftRotator.getCurrentShape();
        int shapeWidth = shape[0].length;
        int startX = Math.max(0, (width - shapeWidth) / 2) - 3; 
        if (startX < 0) startX = 0;
        int startY = 2;
        leftOffset = new Point(startX, startY);
        int[][] tmp = MatrixOperations.copy(boardMatrix);
        if (rightOffset != null) {
            tmp = MatrixOperations.merge(tmp, rightRotator.getCurrentShape(), (int) rightOffset.getX(), (int) rightOffset.getY());
        }
        boolean coll = MatrixOperations.intersect(tmp, shape, (int) leftOffset.getX(), (int) leftOffset.getY());
        if (DEBUG) System.out.println("[COOP SPAWN] spawnLeft at " + leftOffset + " coll=" + coll);
        if (coll) {
            try { isGameOver.set(true); } catch (Exception e) { LOGGER.log(Level.FINER, "Failed to set isGameOver=true in spawnLeft", e); }
        }
        return coll;
    }

    private boolean spawnRight() {
        Brick b = rightGenerator.getBrick();
        rightRotator.setBrick(b);
        int[][] shape = rightRotator.getCurrentShape();
        int shapeWidth = shape[0].length;
        int startX = Math.max(0, (width - shapeWidth) / 2) + 3;
        if (startX + shapeWidth > width) startX = Math.max(0, width - shapeWidth);
        int startY = 2;
        rightOffset = new Point(startX, startY);
        int[][] tmp = MatrixOperations.copy(boardMatrix);
        if (leftOffset != null) {
            tmp = MatrixOperations.merge(tmp, leftRotator.getCurrentShape(), (int) leftOffset.getX(), (int) leftOffset.getY());
        }
        boolean coll = MatrixOperations.intersect(tmp, shape, (int) rightOffset.getX(), (int) rightOffset.getY());
        if (DEBUG) System.out.println("[COOP SPAWN] spawnRight at " + rightOffset + " coll=" + coll);
        if (coll) {
            try { isGameOver.set(true); } catch (Exception e) { LOGGER.log(Level.FINER, "Failed to set isGameOver=true in spawnRight", e); }
        }
        return coll;
    }

    public boolean moveLeftPlayerLeft() {
        Point p = new Point(leftOffset);
        p.translate(-1,0);
        int[][] tmp = MatrixOperations.copy(boardMatrix);
        tmp = safeMerge(tmp, rightRotator.getCurrentShape(), rightOffset, "moveLeftPlayerLeft");
        boolean conflict = MatrixOperations.intersect(tmp, leftRotator.getCurrentShape(), (int) p.getX(), (int) p.getY());
        if (conflict) return false;
        leftOffset = p; return true;
    }

    public boolean moveLeftPlayerRight() {
        Point p = new Point(leftOffset);
        p.translate(1,0);
        int[][] tmp = MatrixOperations.copy(boardMatrix);
        tmp = safeMerge(tmp, rightRotator.getCurrentShape(), rightOffset, "moveLeftPlayerRight");
        boolean conflict = MatrixOperations.intersect(tmp, leftRotator.getCurrentShape(), (int) p.getX(), (int) p.getY());
        if (conflict) return false;
        leftOffset = p; return true;
    }

    public boolean rotateLeftPlayer() {
        int[][] tmp = MatrixOperations.copy(boardMatrix);
        tmp = safeMerge(tmp, rightRotator.getCurrentShape(), rightOffset, "rotateLeftPlayer");
        NextShapeInfo next = leftRotator.getNextShape();
        int baseX = (int) leftOffset.getX();
        int baseY = (int) leftOffset.getY();
        if (!MatrixOperations.intersect(tmp, next.getShape(), baseX, baseY)) {
            leftRotator.setCurrentShape(next.getPosition());
            return true;
        }

        int shapeWidth = next.getShape()[0].length;
        int boardWidth = boardMatrix[0].length;
        int maxKick = Math.max(3, shapeWidth);
        int[] dxCandidates = new int[maxKick * 2];
        for (int k = 1; k <= maxKick; k++) {
            dxCandidates[(k - 1) * 2] = k;
            dxCandidates[(k - 1) * 2 + 1] = -k;
        }
        int[] dyCandidates = new int[]{0, -1, 1, -2};
        for (int dy : dyCandidates) {
            for (int dx : dxCandidates) {
                int tryX = baseX + dx;
                int tryY = baseY + dy;
                if (tryX < -shapeWidth || tryX > boardWidth + shapeWidth) continue;
                if (!MatrixOperations.intersect(tmp, next.getShape(), tryX, tryY)) {
                    Point p = new Point(leftOffset);
                    p.translate(dx, dy);
                    leftOffset = p;
                    leftRotator.setCurrentShape(next.getPosition());
                    return true;
                }
            }
        }
        return false;
    }

    public boolean moveRightPlayerLeft() {
        Point p = new Point(rightOffset);
        p.translate(-1,0);
        int[][] tmp = MatrixOperations.copy(boardMatrix);
        tmp = safeMerge(tmp, leftRotator.getCurrentShape(), leftOffset, "moveRightPlayerLeft");
        boolean conflict = MatrixOperations.intersect(tmp, rightRotator.getCurrentShape(), (int) p.getX(), (int) p.getY());
        if (conflict) return false;
        rightOffset = p; return true;
    }

    public boolean moveRightPlayerRight() {
        Point p = new Point(rightOffset);
        p.translate(1,0);
        int[][] tmp = MatrixOperations.copy(boardMatrix);
        tmp = safeMerge(tmp, leftRotator.getCurrentShape(), leftOffset, "moveRightPlayerRight");
        boolean conflict = MatrixOperations.intersect(tmp, rightRotator.getCurrentShape(), (int) p.getX(), (int) p.getY());
        if (conflict) return false;
        rightOffset = p; return true;
    }

    public boolean rotateRightPlayer() {
        int[][] tmp = MatrixOperations.copy(boardMatrix);
        tmp = safeMerge(tmp, leftRotator.getCurrentShape(), leftOffset, "rotateRightPlayer");
        NextShapeInfo next = rightRotator.getNextShape();
        int baseX = (int) rightOffset.getX();
        int baseY = (int) rightOffset.getY();
        if (!MatrixOperations.intersect(tmp, next.getShape(), baseX, baseY)) {
            rightRotator.setCurrentShape(next.getPosition());
            return true;
        }
        int shapeWidth = next.getShape()[0].length;
        int boardWidth = boardMatrix[0].length;
        int maxKick = Math.max(3, shapeWidth);
        int[] dxCandidates = new int[maxKick * 2];
        for (int k = 1; k <= maxKick; k++) {
            dxCandidates[(k - 1) * 2] = k;
            dxCandidates[(k - 1) * 2 + 1] = -k;
        }
        int[] dyCandidates = new int[]{0, -1, 1, -2};
        for (int dy : dyCandidates) {
            for (int dx : dxCandidates) {
                int tryX = baseX + dx;
                int tryY = baseY + dy;
                if (tryX < -shapeWidth || tryX > boardWidth + shapeWidth) continue;
                if (!MatrixOperations.intersect(tmp, next.getShape(), tryX, tryY)) {
                    Point p = new Point(rightOffset);
                    p.translate(dx, dy);
                    rightOffset = p;
                    rightRotator.setCurrentShape(next.getPosition());
                    return true;
                }
            }
        }
        return false;
    }

    public CoopTickResult tick() {
        boolean leftCan = canMoveDownLeft();
        boolean rightCan = canMoveDownRight();
        boolean mergedThisTick = false;
        if (DEBUG) System.out.println("[COOP TICK] leftOff=" + leftOffset + " rightOff=" + rightOffset + " leftCan=" + leftCan + " rightCan=" + rightCan);
        if (leftCan && rightCan) {
            if (!nextPositionsOverlap()) {
                leftOffset.translate(0,1);
                totalScore.add(1);
                rightOffset.translate(0,1);
                totalScore.add(1);
            } else {
                int ly = (int) leftOffset.getY();
                int ry = (int) rightOffset.getY();
                if (ly > ry) {
                    if (DEBUG) System.out.println("[COOP OVERLAP] moving LOWER=LEFT only");
                    leftOffset.translate(0,1);
                } else if (ry > ly) {
                    if (DEBUG) System.out.println("[COOP OVERLAP] moving LOWER=RIGHT only");
                    rightOffset.translate(0,1);
                } else {
                    int lx = (int) leftOffset.getX();
                    int rx = (int) rightOffset.getX();
                    if (lx <= rx) {
                        if (DEBUG) System.out.println("[COOP OVERLAP] tie-break move LEFT");
                        leftOffset.translate(0,1);
                    } else {
                        if (DEBUG) System.out.println("[COOP OVERLAP] tie-break move RIGHT");
                        rightOffset.translate(0,1);
                    }
                }
            }
        } else {
                if (leftCan) { leftOffset.translate(0,1); totalScore.add(1); }
            if (rightCan) { rightOffset.translate(0,1); totalScore.add(1); }
        }

        ViewData leftLandingView = null;
        ViewData rightLandingView = null;
        if (!leftCan) leftLandingView = getViewDataLeft();
        if (!rightCan) rightLandingView = getViewDataRight();

        if (!leftCan) {
            if (DEBUG) System.out.println("[COOP MERGE] merging LEFT at " + leftOffset);
            boardMatrix = MatrixOperations.merge(boardMatrix, leftRotator.getCurrentShape(), (int) leftOffset.getX(), (int) leftOffset.getY());
            mergedThisTick = true;
        }

        if (!rightCan) {
            if (DEBUG) System.out.println("[COOP MERGE] merging RIGHT at " + rightOffset);
            boardMatrix = MatrixOperations.merge(boardMatrix, rightRotator.getCurrentShape(), (int) rightOffset.getX(), (int) rightOffset.getY());
            mergedThisTick = true;
        }

        if (mergedThisTick) {
            ClearRow clearRow = MatrixOperations.checkRemoving(boardMatrix);
            boardMatrix = clearRow.getNewMatrix();
            if (clearRow.getLinesRemoved() > 0) {
                int bonus = clearRow.getScoreBonus();
                totalScore.add(bonus);
            }
            try { spawnLeft(); } catch (Exception e) { LOGGER.log(Level.WARNING, "spawnLeft failed after merge", e); }
            try { spawnRight(); } catch (Exception e) { LOGGER.log(Level.WARNING, "spawnRight failed after merge", e); }

            DownData leftData = null;
            DownData rightData = null;
            if (leftLandingView != null) leftData = new DownData(clearRow.getLinesRemoved() > 0 ? clearRow : null, leftLandingView);
            if (rightLandingView != null) rightData = new DownData(clearRow.getLinesRemoved() > 0 ? clearRow : null, rightLandingView);
            return new CoopTickResult(true, leftData, rightData, clearRow.getLinesRemoved() > 0 ? clearRow : null);
        }

        return new CoopTickResult(false, null, null, null);
    }

    private boolean canMoveDownLeft() {
        Point p = new Point(leftOffset);
        p.translate(0,1);
        return !MatrixOperations.intersect(boardMatrix, leftRotator.getCurrentShape(), (int) p.getX(), (int) p.getY());
    }

    private boolean canMoveDownRight() {
        Point p = new Point(rightOffset);
        p.translate(0,1);
        return !MatrixOperations.intersect(boardMatrix, rightRotator.getCurrentShape(), (int) p.getX(), (int) p.getY());
    }

    private boolean nextPositionsOverlap() {
        int[][] leftShape = leftRotator.getCurrentShape();
        int[][] rightShape = rightRotator.getCurrentShape();
        int lx = (int) leftOffset.getX();
        int ly = (int) leftOffset.getY() + 1;
        int rx = (int) rightOffset.getX();
        int ry = (int) rightOffset.getY() + 1;

        for (int i = 0; i < leftShape.length; i++) {
            for (int j = 0; j < leftShape[i].length; j++) {
                if (leftShape[i][j] == 0) continue;
                int ax = lx + j;
                int ay = ly + i;
                int ri = ay - ry;
                int rj = ax - rx;
                if (ri >= 0 && ri < rightShape.length && rj >= 0 && rj < rightShape[ri].length) {
                    if (rightShape[ri][rj] != 0) return true;
                }
            }
        }
        return false;
    }

    public DownData onLeftDown() {
        boolean can = canMoveDownLeft();
        if (can) { leftOffset.translate(0,1); totalScore.add(1); return new DownData(null, getViewDataLeft()); }
    boardMatrix = MatrixOperations.merge(boardMatrix, leftRotator.getCurrentShape(), (int) leftOffset.getX(), (int) leftOffset.getY());
        ClearRow cr = MatrixOperations.checkRemoving(boardMatrix);
        boardMatrix = cr.getNewMatrix();
    if (cr.getLinesRemoved() > 0) totalScore.add(cr.getScoreBonus());
    try { spawnLeft(); } catch (Exception e) { LOGGER.log(Level.WARNING, "spawnLeft failed in onLeftDown", e); }
    return new DownData(cr, getViewDataLeft());
    }

    public DownData onRightDown() {
        boolean can = canMoveDownRight();
        if (can) { rightOffset.translate(0,1); totalScore.add(1); return new DownData(null, getViewDataRight()); }
        boardMatrix = MatrixOperations.merge(boardMatrix, rightRotator.getCurrentShape(), (int) rightOffset.getX(), (int) rightOffset.getY());
        ClearRow cr = MatrixOperations.checkRemoving(boardMatrix);
        boardMatrix = cr.getNewMatrix();
    if (cr.getLinesRemoved() > 0) totalScore.add(cr.getScoreBonus());
    try { spawnRight(); } catch (Exception e) { LOGGER.log(Level.WARNING, "spawnRight failed in onRightDown", e); }
    return new DownData(cr, getViewDataRight());
    }

    public BooleanProperty gameOverProperty() { return isGameOver; }

    public boolean swapLeft() {
        Brick next = leftGenerator.getNextBrick();
        if (next == null) return false;
        Brick old = leftRotator.getBrick();
        leftRotator.setBrick(next);
        if (MatrixOperations.intersect(boardMatrix, leftRotator.getCurrentShape(), (int) leftOffset.getX(), (int) leftOffset.getY())) {
            leftRotator.setBrick(old);
            return false;
        }
        boolean replaced = leftGenerator.replaceNext(old);
        if (!replaced) {
            try { leftGenerator.getBrick(); } catch (Exception e) { LOGGER.log(Level.FINER, "leftGenerator.getBrick() failed in swapLeft", e); }
        }
        return true;
    }

    public boolean swapRight() {
        Brick next = rightGenerator.getNextBrick();
        if (next == null) return false;
        Brick old = rightRotator.getBrick();
        rightRotator.setBrick(next);
        if (MatrixOperations.intersect(boardMatrix, rightRotator.getCurrentShape(), (int) rightOffset.getX(), (int) rightOffset.getY())) {
            rightRotator.setBrick(old);
            return false;
        }
        boolean replaced = rightGenerator.replaceNext(old);
        if (!replaced) {
            try { rightGenerator.getBrick(); } catch (Exception e) { LOGGER.log(Level.FINER, "rightGenerator.getBrick() failed in swapRight", e); }
        }
        return true;
    }

    private int[][] safeMerge(int[][] base, int[][] shape, Point offset, String ctx) {
        if (shape == null || offset == null) return base;
        try {
            return MatrixOperations.merge(base, shape, (int) offset.getX(), (int) offset.getY());
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "safeMerge failed (" + ctx + ")", e);
            return base;
        }
    }

}