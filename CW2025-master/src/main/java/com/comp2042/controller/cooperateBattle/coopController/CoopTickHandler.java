package com.comp2042.controller.cooperateBattle.coopController;

import com.comp2042.model.ViewData;
import com.comp2042.model.ClearRow;
// DownData / CoopTickResult are created in the controller after spawn; not needed here
import com.comp2042.model.CoopScore;
import com.comp2042.utils.MatrixOperations;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates tick processing logic for cooperative mode.
 * Returns an outcome containing merged state, landing views, clear row and the updated board matrix.
 */
public class CoopTickHandler {
    private static final Logger LOGGER = Logger.getLogger(CoopTickHandler.class.getName());

    public static class TickOutcome {
        public final boolean merged;
        public final ViewData leftLandingView;
        public final ViewData rightLandingView;
        public final ClearRow clearRow;
        public final int[][] newBoard;

        public TickOutcome(boolean merged, ViewData leftLandingView, ViewData rightLandingView, ClearRow clearRow, int[][] newBoard) {
            this.merged = merged;
            this.leftLandingView = leftLandingView;
            this.rightLandingView = rightLandingView;
            this.clearRow = clearRow;
            this.newBoard = newBoard;
        }
    }

    public static TickOutcome processTick(int[][] boardMatrix, CoopPlayerState leftPlayer, CoopPlayerState rightPlayer, CoopScore totalScore, boolean debug) {
        boolean leftCan = leftPlayer.canMoveDown(boardMatrix);
        boolean rightCan = rightPlayer.canMoveDown(boardMatrix);
        boolean mergedThisTick = false;
        if (debug) System.out.println("[COOP TICK] leftOff=" + leftPlayer.offset + " rightOff=" + rightPlayer.offset + " leftCan=" + leftCan + " rightCan=" + rightCan);

        if (leftCan && rightCan) {
            if (!nextPositionsOverlap(leftPlayer, rightPlayer)) {
                leftPlayer.offset.translate(0,1);
                totalScore.add(1);
                rightPlayer.offset.translate(0,1);
                totalScore.add(1);
            } else {
                int ly = (int) leftPlayer.offset.getY();
                int ry = (int) rightPlayer.offset.getY();
                if (ly > ry) {
                    if (debug) System.out.println("[COOP OVERLAP] moving LOWER=LEFT only");
                    leftPlayer.offset.translate(0,1);
                } else if (ry > ly) {
                    if (debug) System.out.println("[COOP OVERLAP] moving LOWER=RIGHT only");
                    rightPlayer.offset.translate(0,1);
                } else {
                    int lx = (int) leftPlayer.offset.getX();
                    int rx = (int) rightPlayer.offset.getX();
                    if (lx <= rx) {
                        if (debug) System.out.println("[COOP OVERLAP] tie-break move LEFT");
                        leftPlayer.offset.translate(0,1);
                    } else {
                        if (debug) System.out.println("[COOP OVERLAP] tie-break move RIGHT");
                        rightPlayer.offset.translate(0,1);
                    }
                }
            }
        } else {
            if (leftCan) { leftPlayer.offset.translate(0,1); totalScore.add(1); }
            if (rightCan) { rightPlayer.offset.translate(0,1); totalScore.add(1); }
        }

        ViewData leftLandingView = null;
        ViewData rightLandingView = null;
        if (!leftCan) leftLandingView = leftPlayer.getViewData();
        if (!rightCan) rightLandingView = rightPlayer.getViewData();

        int[][] newBoard = boardMatrix;
        if (!leftCan) {
            if (debug) System.out.println("[COOP MERGE] merging LEFT at " + leftPlayer.offset);
            newBoard = MatrixOperations.merge(newBoard, leftPlayer.getCurrentShape(), (int) leftPlayer.offset.getX(), (int) leftPlayer.offset.getY());
            mergedThisTick = true;
        }

        if (!rightCan) {
            if (debug) System.out.println("[COOP MERGE] merging RIGHT at " + rightPlayer.offset);
            newBoard = MatrixOperations.merge(newBoard, rightPlayer.getCurrentShape(), (int) rightPlayer.offset.getX(), (int) rightPlayer.offset.getY());
            mergedThisTick = true;
        }

        if (mergedThisTick) {
            ClearRow clearRow = MatrixOperations.checkRemoving(newBoard);
            newBoard = clearRow.getNewMatrix();
            if (clearRow.getLinesRemoved() > 0) {
                int bonus = clearRow.getScoreBonus();
                try { totalScore.add(bonus); } catch (Exception e) { LOGGER.log(Level.FINER, "Failed to add score bonus", e); }
            }
            return new TickOutcome(true, leftLandingView, rightLandingView, clearRow, newBoard);
        }

        return new TickOutcome(false, null, null, null, newBoard);
    }

    private static boolean nextPositionsOverlap(CoopPlayerState leftPlayer, CoopPlayerState rightPlayer) {
        int[][] leftShape = leftPlayer.getCurrentShape();
        int[][] rightShape = rightPlayer.getCurrentShape();
        int lx = (int) leftPlayer.offset.getX();
        int ly = (int) leftPlayer.offset.getY() + 1;
        int rx = (int) rightPlayer.offset.getX();
        int ry = (int) rightPlayer.offset.getY() + 1;

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
}
