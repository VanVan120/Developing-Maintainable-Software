package com.comp2042.controller.cooperateBattle.coopController;

import com.comp2042.model.ViewData;
import com.comp2042.model.DownData;
import com.comp2042.logic.Brick;
import com.comp2042.model.ClearRow;
import com.comp2042.utils.MatrixOperations;

import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Controller coordinating the cooperative two-player game mode.
 *
 * <p>This class maintains a shared board matrix and two {@link CoopPlayerState}
 * instances (left and right). It exposes operations used by the UI layer to
 * move/rotate players, trigger drops, swaps and advance the game by one tick.
 * The class is intentionally lightweight and delegates per-player logic to
 * {@link CoopPlayerState} and tick merging/clearing logic to
 * {@link CoopTickHandler}.
 */
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

    /**
     * Create a new controller for a board of the given dimensions.
     *
     * @param width board width in cells
     * @param height board height in cells
     */
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

    /**
     * Reset the board and player state and spawn the initial pieces for both players.
     */
    public void createNewGame() {
        boardMatrix = new int[height][width];
        leftPlayer.score.reset();
        rightPlayer.score.reset();
        totalScore.reset();
        try { isGameOver.set(false); } catch (Exception e) { LOGGER.log(Level.FINER, "Failed to reset isGameOver property", e); }
        spawnLeft();
        spawnRight();
    }

    /**
     * Spawn a new brick for the left player. Marks the game over property if the
     * spawn immediately collides (indicating the board is filled).
     *
     * @return {@code true} if a collision occurred on spawn
     */
    private boolean spawnLeft() {
        boolean coll = leftPlayer.spawn(width, boardMatrix, rightPlayer, -3);
        if (DEBUG) System.out.println("[COOP SPAWN] spawnLeft at " + leftPlayer.offset + " coll=" + coll);
        if (coll) {
            try { isGameOver.set(true); } catch (Exception e) { LOGGER.log(Level.FINER, "Failed to set isGameOver=true in spawnLeft", e); }
        }
        return coll;
        }

    /**
     * Spawn a new brick for the right player. Marks the game over property if the
     * spawn immediately collides.
     *
     * @return {@code true} if a collision occurred on spawn
     */
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

    /**
     * Advance the game state by one tick. This attempts to move pieces down for
     * both players, merges landed pieces into the board, checks for cleared rows
     * and adjusts scores. When a merge occurs the method will spawn new pieces
     * for the affected players.
     *
     * @return a {@link CoopTickResult} describing what happened this tick
     */
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

    

    /**
     * Handle an explicit soft-drop action for the left player.
     * <p>If the piece can move down this will translate the piece and award one
     * point to the shared score. If the piece cannot move down it is merged into
     * the board, row clears are processed and a new piece is spawned.
     *
     * @return {@link DownData} describing the result of the drop (landing view and any clear)
     */
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

    /**
     * Handle an explicit soft-drop action for the right player. Behavior mirrors
     * {@link #onLeftDown()} but for the right player.
     *
     * @return {@link DownData} describing the result of the drop
     */
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

    /**
     * Property indicating whether the game is over (spawn collision detected).
     */
    public BooleanProperty gameOverProperty() { return isGameOver; }

    /**
     * Swap the left player's current brick with their next brick (if available).
     * If the swap would cause immediate collision the swap is undone and
     * {@code false} is returned.
     *
     * @return {@code true} if the swap succeeded
     */
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

    /**
     * Swap the right player's current brick with their next brick (if available).
     * If the swap would cause immediate collision the swap is undone and
     * {@code false} is returned.
     *
     * @return {@code true} if the swap succeeded
     */
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
