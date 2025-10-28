package com.comp2042;

import com.comp2042.logic.bricks.Brick;
import com.comp2042.logic.bricks.RandomBrickGenerator;

import java.awt.Point;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Cooperative game controller managing a single shared board and two independent falling pieces.
 * Left and right players each have their own generator, rotator and score.
 */
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
    // shared cooperative score: both players contribute to this total
    private final CoopScore totalScore = new CoopScore();

    // Enable temporary debug logging for cooperative tick merge/offset events
    private static final boolean DEBUG = true;

    // Game over property: becomes true when a new brick cannot be spawned
    private final BooleanProperty isGameOver = new SimpleBooleanProperty(false);

    public CoopGameController(int width, int height) {
        this.width = width;
        this.height = height;
        this.boardMatrix = new int[height][width];
    }

    public int[][] getBoardMatrix() { return boardMatrix; }

    public javafx.beans.property.IntegerProperty getLeftScoreProperty() { return leftScore.scoreProperty(); }
    public javafx.beans.property.IntegerProperty getRightScoreProperty() { return rightScore.scoreProperty(); }

    public java.util.List<com.comp2042.logic.bricks.Brick> getUpcomingLeft(int count) { return leftGenerator.getUpcomingBricks(count); }
    public java.util.List<com.comp2042.logic.bricks.Brick> getUpcomingRight(int count) { return rightGenerator.getUpcomingBricks(count); }

    public javafx.beans.property.IntegerProperty getTotalScoreProperty() { return totalScore.scoreProperty(); }

    /** Expose cooperative-mode high score property (persisted separately from single-player). */
    public javafx.beans.property.IntegerProperty getTotalHighScoreProperty() { return totalScore.highScoreProperty(); }

    public ViewData getViewDataLeft() {
        return new ViewData(leftRotator.getCurrentShape(), (int) leftOffset.getX(), (int) leftOffset.getY(), leftGenerator.getNextBrick().getShapeMatrix().get(0));
    }

    public ViewData getViewDataRight() {
        return new ViewData(rightRotator.getCurrentShape(), (int) rightOffset.getX(), (int) rightOffset.getY(), rightGenerator.getNextBrick().getShapeMatrix().get(0));
    }

    public void createNewGame() {
        boardMatrix = new int[height][width];
        leftScore.reset();
        rightScore.reset();
        totalScore.reset();
        // reset game-over flag on new game
        try { isGameOver.set(false); } catch (Exception ignored) {}
        // seed generators already seeded in constructor; ensure offsets and rotators are set
        spawnLeft();
        spawnRight();
    }

    private boolean spawnLeft() {
        Brick b = leftGenerator.getBrick();
        leftRotator.setBrick(b);
        int[][] shape = leftRotator.getCurrentShape();
        int shapeWidth = shape[0].length;
        int startX = Math.max(0, (width - shapeWidth) / 2) - 3; // push left player a bit toward left
        if (startX < 0) startX = 0;
        int startY = 2;
        leftOffset = new Point(startX, startY);
        // collision check against board and other piece
        int[][] tmp = MatrixOperations.copy(boardMatrix);
        // merge right piece as occupied space to avoid overlap
        try { tmp = MatrixOperations.merge(tmp, rightRotator.getCurrentShape(), (int) rightOffset.getX(), (int) rightOffset.getY()); } catch (Exception ignored) {}
        boolean coll = MatrixOperations.intersect(tmp, shape, (int) leftOffset.getX(), (int) leftOffset.getY());
        if (DEBUG) System.out.println("[COOP SPAWN] spawnLeft at " + leftOffset + " coll=" + coll);
        if (coll) {
            // cannot spawn -> game over
            try { isGameOver.set(true); } catch (Exception ignored) {}
        }
        return coll;
    }

    private boolean spawnRight() {
        Brick b = rightGenerator.getBrick();
        rightRotator.setBrick(b);
        int[][] shape = rightRotator.getCurrentShape();
        int shapeWidth = shape[0].length;
        int startX = Math.max(0, (width - shapeWidth) / 2) + 3; // push right player a bit toward right
        if (startX + shapeWidth > width) startX = Math.max(0, width - shapeWidth);
        int startY = 2;
        rightOffset = new Point(startX, startY);
        int[][] tmp = MatrixOperations.copy(boardMatrix);
        try { tmp = MatrixOperations.merge(tmp, leftRotator.getCurrentShape(), (int) leftOffset.getX(), (int) leftOffset.getY()); } catch (Exception ignored) {}
        boolean coll = MatrixOperations.intersect(tmp, shape, (int) rightOffset.getX(), (int) rightOffset.getY());
        if (DEBUG) System.out.println("[COOP SPAWN] spawnRight at " + rightOffset + " coll=" + coll);
        if (coll) {
            try { isGameOver.set(true); } catch (Exception ignored) {}
        }
        return coll;
    }

    // Movement API for left player
    public boolean moveLeftPlayerLeft() {
        Point p = new Point(leftOffset);
        p.translate(-1,0);
        int[][] tmp = MatrixOperations.copy(boardMatrix);
        // occupy right piece
        try { tmp = MatrixOperations.merge(tmp, rightRotator.getCurrentShape(), (int) rightOffset.getX(), (int) rightOffset.getY()); } catch (Exception ignored) {}
        boolean conflict = MatrixOperations.intersect(tmp, leftRotator.getCurrentShape(), (int) p.getX(), (int) p.getY());
        if (conflict) return false;
        leftOffset = p; return true;
    }

    public boolean moveLeftPlayerRight() {
        Point p = new Point(leftOffset);
        p.translate(1,0);
        int[][] tmp = MatrixOperations.copy(boardMatrix);
        try { tmp = MatrixOperations.merge(tmp, rightRotator.getCurrentShape(), (int) rightOffset.getX(), (int) rightOffset.getY()); } catch (Exception ignored) {}
        boolean conflict = MatrixOperations.intersect(tmp, leftRotator.getCurrentShape(), (int) p.getX(), (int) p.getY());
        if (conflict) return false;
        leftOffset = p; return true;
    }

    public boolean rotateLeftPlayer() {
        int[][] tmp = MatrixOperations.copy(boardMatrix);
        try { tmp = MatrixOperations.merge(tmp, rightRotator.getCurrentShape(), (int) rightOffset.getX(), (int) rightOffset.getY()); } catch (Exception ignored) {}
        NextShapeInfo next = leftRotator.getNextShape();
        boolean conflict = MatrixOperations.intersect(tmp, next.getShape(), (int) leftOffset.getX(), (int) leftOffset.getY());
        if (conflict) return false;
        leftRotator.setCurrentShape(next.getPosition());
        return true;
    }

    // Movement API for right player
    public boolean moveRightPlayerLeft() {
        Point p = new Point(rightOffset);
        p.translate(-1,0);
        int[][] tmp = MatrixOperations.copy(boardMatrix);
        try { tmp = MatrixOperations.merge(tmp, leftRotator.getCurrentShape(), (int) leftOffset.getX(), (int) leftOffset.getY()); } catch (Exception ignored) {}
        boolean conflict = MatrixOperations.intersect(tmp, rightRotator.getCurrentShape(), (int) p.getX(), (int) p.getY());
        if (conflict) return false;
        rightOffset = p; return true;
    }

    public boolean moveRightPlayerRight() {
        Point p = new Point(rightOffset);
        p.translate(1,0);
        int[][] tmp = MatrixOperations.copy(boardMatrix);
        try { tmp = MatrixOperations.merge(tmp, leftRotator.getCurrentShape(), (int) leftOffset.getX(), (int) leftOffset.getY()); } catch (Exception ignored) {}
        boolean conflict = MatrixOperations.intersect(tmp, rightRotator.getCurrentShape(), (int) p.getX(), (int) p.getY());
        if (conflict) return false;
        rightOffset = p; return true;
    }

    public boolean rotateRightPlayer() {
        int[][] tmp = MatrixOperations.copy(boardMatrix);
        try { tmp = MatrixOperations.merge(tmp, leftRotator.getCurrentShape(), (int) leftOffset.getX(), (int) leftOffset.getY()); } catch (Exception ignored) {}
        NextShapeInfo next = rightRotator.getNextShape();
        boolean conflict = MatrixOperations.intersect(tmp, next.getShape(), (int) rightOffset.getX(), (int) rightOffset.getY());
        if (conflict) return false;
        rightRotator.setCurrentShape(next.getPosition());
        return true;
    }

    // Automatic drop tick: returns detailed result describing merges/clears so GUI can render effects
    public CoopTickResult tick() {
        boolean leftCan = canMoveDownLeft();
        boolean rightCan = canMoveDownRight();
        boolean mergedThisTick = false;
        if (DEBUG) System.out.println("[COOP TICK] leftOff=" + leftOffset + " rightOff=" + rightOffset + " leftCan=" + leftCan + " rightCan=" + rightCan);
        // If both can move down, ensure their *next* positions don't overlap each other.
        if (leftCan && rightCan) {
            if (!nextPositionsOverlap()) {
                leftOffset.translate(0,1);
                // award 1 point for each piece moving down
                totalScore.add(1);
                rightOffset.translate(0,1);
                totalScore.add(1);
            } else {
                // Overlap: block the later (higher) piece so it stays above and allow the lower
                // piece to move down. This prevents the higher (later) piece from being merged
                // or disappearing when both try to occupy the same cells.
                int ly = (int) leftOffset.getY();
                int ry = (int) rightOffset.getY();
                if (ly > ry) {
                    // left is lower -> move left only
                    if (DEBUG) System.out.println("[COOP OVERLAP] moving LOWER=LEFT only");
                    leftOffset.translate(0,1);
                } else if (ry > ly) {
                    if (DEBUG) System.out.println("[COOP OVERLAP] moving LOWER=RIGHT only");
                    rightOffset.translate(0,1);
                } else {
                    // same row: tie-break by x (move the one more to the left)
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
                // Note: do NOT recompute can flags here — we intentionally block the other piece
                // (it remains active above) so it should NOT be merged this tick.
            }
        } else {
                if (leftCan) { leftOffset.translate(0,1); totalScore.add(1); }
            if (rightCan) { rightOffset.translate(0,1); totalScore.add(1); }
        }

        // capture view data for any piece that will merge this tick (so GUI can animate lock effects)
        ViewData leftLandingView = null;
        ViewData rightLandingView = null;
        if (!leftCan) leftLandingView = getViewDataLeft();
        if (!rightCan) rightLandingView = getViewDataRight();

        if (!leftCan) {
            // merge left into board
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
            // After merges, clear rows once and assign score to player(s) who caused merge
            ClearRow clearRow = MatrixOperations.checkRemoving(boardMatrix);
            boardMatrix = clearRow.getNewMatrix();
            if (clearRow.getLinesRemoved() > 0) {
                int bonus = clearRow.getScoreBonus();
                // cooperative mode: add full bonus to the shared total score
                totalScore.add(bonus);
            }
            // Spawn new bricks if needed
            try { spawnLeft(); } catch (Exception ignored) {}
            try { spawnRight(); } catch (Exception ignored) {}

            // Build DownData results for each side that landed this tick. We attach the
            // overall ClearRow when rowsRemoved>0 so GUI can spawn explosions and notifications.
            DownData leftData = null;
            DownData rightData = null;
            if (leftLandingView != null) leftData = new DownData(clearRow.getLinesRemoved() > 0 ? clearRow : null, leftLandingView);
            if (rightLandingView != null) rightData = new DownData(clearRow.getLinesRemoved() > 0 ? clearRow : null, rightLandingView);
            return new CoopTickResult(true, leftData, rightData, clearRow.getLinesRemoved() > 0 ? clearRow : null);
        }

        return new CoopTickResult(false, null, null, null);
    }

    private boolean canMoveDownLeft() {
        // Only consider the static board when checking downward movement so both active
        // pieces may fall simultaneously; don't treat the other active piece as static.
        Point p = new Point(leftOffset);
        p.translate(0,1);
        return !MatrixOperations.intersect(boardMatrix, leftRotator.getCurrentShape(), (int) p.getX(), (int) p.getY());
    }

    private boolean canMoveDownRight() {
        Point p = new Point(rightOffset);
        p.translate(0,1);
        return !MatrixOperations.intersect(boardMatrix, rightRotator.getCurrentShape(), (int) p.getX(), (int) p.getY());
    }

    /**
     * Returns true if the two pieces' next positions (one row down) would occupy the same
     * board cell(s). This guards against both moving into the same space.
     */
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

    // manual down for left/right (user-initiated). Returns DownData similar to GameController
    public DownData onLeftDown() {
        boolean can = canMoveDownLeft();
        if (can) { leftOffset.translate(0,1); totalScore.add(1); return new DownData(null, getViewDataLeft()); }
        // cannot move — merge
    boardMatrix = MatrixOperations.merge(boardMatrix, leftRotator.getCurrentShape(), (int) leftOffset.getX(), (int) leftOffset.getY());
        ClearRow cr = MatrixOperations.checkRemoving(boardMatrix);
        boardMatrix = cr.getNewMatrix();
    if (cr.getLinesRemoved() > 0) totalScore.add(cr.getScoreBonus());
        boolean coll = spawnLeft();
        // spawnLeft will update isGameOver property when collision occurs
        return new DownData(cr, getViewDataLeft());
    }

    public DownData onRightDown() {
        boolean can = canMoveDownRight();
        if (can) { rightOffset.translate(0,1); totalScore.add(1); return new DownData(null, getViewDataRight()); }
        boardMatrix = MatrixOperations.merge(boardMatrix, rightRotator.getCurrentShape(), (int) rightOffset.getX(), (int) rightOffset.getY());
        ClearRow cr = MatrixOperations.checkRemoving(boardMatrix);
        boardMatrix = cr.getNewMatrix();
    if (cr.getLinesRemoved() > 0) totalScore.add(cr.getScoreBonus());
        boolean coll = spawnRight();
        // spawnRight will update isGameOver property when collision occurs
        return new DownData(cr, getViewDataRight());
    }

    /**
     * Observable game-over property. True when the controller has detected that a new brick
     * cannot be spawned (board full).
     */
    public BooleanProperty gameOverProperty() { return isGameOver; }

    // Swap current with next for left/right
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
            try { leftGenerator.getBrick(); } catch (Exception ignored) {}
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
            try { rightGenerator.getBrick(); } catch (Exception ignored) {}
        }
        return true;
    }

}
