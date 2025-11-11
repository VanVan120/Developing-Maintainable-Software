package com.comp2042.controller.cooperateBattle.coopController;

import com.comp2042.model.ViewData;
import com.comp2042.model.DownData;
import com.comp2042.model.CoopTickResult;
import com.comp2042.logic.Brick;
import com.comp2042.model.ClearRow;
import com.comp2042.model.CoopScore;
import com.comp2042.utils.MatrixOperations;

import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class CoopGameController {
    private final int width;
    private final int height;
    private int[][] boardMatrix;
    private final CoopPlayerState leftPlayer = new CoopPlayerState();
    private final CoopPlayerState rightPlayer = new CoopPlayerState();
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
    public javafx.beans.property.IntegerProperty getLeftScoreProperty() { return leftPlayer.score.scoreProperty(); }
    public javafx.beans.property.IntegerProperty getRightScoreProperty() { return rightPlayer.score.scoreProperty(); }
    public java.util.List<com.comp2042.logic.Brick> getUpcomingLeft(int count) { return leftPlayer.getUpcoming(count); }
    public java.util.List<com.comp2042.logic.Brick> getUpcomingRight(int count) { return rightPlayer.getUpcoming(count); }
    public javafx.beans.property.IntegerProperty getTotalScoreProperty() { return totalScore.scoreProperty(); }
    public javafx.beans.property.IntegerProperty getTotalHighScoreProperty() { return totalScore.highScoreProperty(); }

    public ViewData getViewDataLeft() {
        if (leftPlayer.offset == null) return null;
        return new ViewData(leftPlayer.getCurrentShape(), (int) leftPlayer.offset.getX(), (int) leftPlayer.offset.getY(), leftPlayer.getNextBrick().getShapeMatrix().get(0));
    }

    public ViewData getViewDataRight() {
        if (rightPlayer.offset == null) return null;
        return new ViewData(rightPlayer.getCurrentShape(), (int) rightPlayer.offset.getX(), (int) rightPlayer.offset.getY(), rightPlayer.getNextBrick().getShapeMatrix().get(0));
    }

    public void createNewGame() {
        boardMatrix = new int[height][width];
        leftPlayer.score.reset();
        rightPlayer.score.reset();
        totalScore.reset();
        try { isGameOver.set(false); } catch (Exception e) { LOGGER.log(Level.FINER, "Failed to reset isGameOver property", e); }
        spawnLeft();
        spawnRight();
    }

    private boolean spawnLeft() {
        boolean coll = leftPlayer.spawn(width, boardMatrix, rightPlayer, -3);
        if (DEBUG) System.out.println("[COOP SPAWN] spawnLeft at " + leftPlayer.offset + " coll=" + coll);
        if (coll) {
            try { isGameOver.set(true); } catch (Exception e) { LOGGER.log(Level.FINER, "Failed to set isGameOver=true in spawnLeft", e); }
        }
        return coll;
        }

    private boolean spawnRight() {
        boolean coll = rightPlayer.spawn(width, boardMatrix, leftPlayer, 3);
        if (DEBUG) System.out.println("[COOP SPAWN] spawnRight at " + rightPlayer.offset + " coll=" + coll);
        if (coll) {
            try { isGameOver.set(true); } catch (Exception e) { LOGGER.log(Level.FINER, "Failed to set isGameOver=true in spawnRight", e); }
        }
        return coll;
        }

    public boolean moveLeftPlayerLeft() {
        return leftPlayer.tryTranslate(-1, 0, boardMatrix, rightPlayer, "moveLeftPlayerLeft");
    }

    public boolean moveLeftPlayerRight() {
        return leftPlayer.tryTranslate(1, 0, boardMatrix, rightPlayer, "moveLeftPlayerRight");
    }

    public boolean rotateLeftPlayer() {
        return leftPlayer.tryRotate(boardMatrix, rightPlayer, "rotateLeftPlayer");
    }

    public boolean moveRightPlayerLeft() {
        return rightPlayer.tryTranslate(-1, 0, boardMatrix, leftPlayer, "moveRightPlayerLeft");
    }

    public boolean moveRightPlayerRight() {
        return rightPlayer.tryTranslate(1, 0, boardMatrix, leftPlayer, "moveRightPlayerRight");
    }

    public boolean rotateRightPlayer() {
        return rightPlayer.tryRotate(boardMatrix, leftPlayer, "rotateRightPlayer");
    }

    public CoopTickResult tick() {
        CoopTickHandler.TickOutcome outcome = CoopTickHandler.processTick(boardMatrix, leftPlayer, rightPlayer, totalScore, DEBUG);
        // update board matrix from outcome
        boardMatrix = outcome.newBoard;

        if (outcome.merged) {
            try { spawnLeft(); } catch (Exception e) { LOGGER.log(Level.WARNING, "spawnLeft failed after merge", e); }
            try { spawnRight(); } catch (Exception e) { LOGGER.log(Level.WARNING, "spawnRight failed after merge", e); }

            DownData leftData = null;
            DownData rightData = null;
            if (outcome.leftLandingView != null) leftData = new DownData(outcome.clearRow != null && outcome.clearRow.getLinesRemoved() > 0 ? outcome.clearRow : null, outcome.leftLandingView);
            if (outcome.rightLandingView != null) rightData = new DownData(outcome.clearRow != null && outcome.clearRow.getLinesRemoved() > 0 ? outcome.clearRow : null, outcome.rightLandingView);
            return new CoopTickResult(true, leftData, rightData, outcome.clearRow != null && outcome.clearRow.getLinesRemoved() > 0 ? outcome.clearRow : null);
        }

        return new CoopTickResult(false, null, null, null);
    }

    private boolean canMoveDownLeft() {
        return leftPlayer.canMoveDown(boardMatrix);
    }

    private boolean canMoveDownRight() {
        return rightPlayer.canMoveDown(boardMatrix);
    }

    

    public DownData onLeftDown() {
        boolean can = canMoveDownLeft();
        if (can) { leftPlayer.offset.translate(0,1); totalScore.add(1); return new DownData(null, getViewDataLeft()); }
        boardMatrix = leftPlayer.mergeIntoBoard(boardMatrix);
        ClearRow cr = MatrixOperations.checkRemoving(boardMatrix);
        boardMatrix = cr.getNewMatrix();
        if (cr.getLinesRemoved() > 0) totalScore.add(cr.getScoreBonus());
        try { spawnLeft(); } catch (Exception e) { LOGGER.log(Level.WARNING, "spawnLeft failed in onLeftDown", e); }
        return new DownData(cr, leftPlayer.getViewData());
    }

    public DownData onRightDown() {
        boolean can = canMoveDownRight();
        if (can) { rightPlayer.offset.translate(0,1); totalScore.add(1); return new DownData(null, getViewDataRight()); }
        boardMatrix = rightPlayer.mergeIntoBoard(boardMatrix);
        ClearRow cr = MatrixOperations.checkRemoving(boardMatrix);
        boardMatrix = cr.getNewMatrix();
        if (cr.getLinesRemoved() > 0) totalScore.add(cr.getScoreBonus());
        try { spawnRight(); } catch (Exception e) { LOGGER.log(Level.WARNING, "spawnRight failed in onRightDown", e); }
        return new DownData(cr, rightPlayer.getViewData());
    }

    public BooleanProperty gameOverProperty() { return isGameOver; }

    public boolean swapLeft() {
        Brick next = leftPlayer.getNextBrick();
        if (next == null) return false;
        Brick old = leftPlayer.getBrick();
        leftPlayer.setBrick(next);
        if (MatrixOperations.intersect(boardMatrix, leftPlayer.getCurrentShape(), (int) leftPlayer.offset.getX(), (int) leftPlayer.offset.getY())) {
            leftPlayer.setBrick(old);
            return false;
        }
        boolean replaced = leftPlayer.replaceNext(old);
        if (!replaced) {
            try { leftPlayer.getBrickFromGenerator(); } catch (Exception e) { LOGGER.log(Level.FINER, "leftGenerator.getBrick() failed in swapLeft", e); }
        }
        return true;
    }

    public boolean swapRight() {
        Brick next = rightPlayer.getNextBrick();
        if (next == null) return false;
        Brick old = rightPlayer.getBrick();
        rightPlayer.setBrick(next);
        if (MatrixOperations.intersect(boardMatrix, rightPlayer.getCurrentShape(), (int) rightPlayer.offset.getX(), (int) rightPlayer.offset.getY())) {
            rightPlayer.setBrick(old);
            return false;
        }
        boolean replaced = rightPlayer.replaceNext(old);
        if (!replaced) {
            try { rightPlayer.getBrickFromGenerator(); } catch (Exception e) { LOGGER.log(Level.FINER, "rightGenerator.getBrick() failed in swapRight", e); }
        }
        return true;
    }
}
