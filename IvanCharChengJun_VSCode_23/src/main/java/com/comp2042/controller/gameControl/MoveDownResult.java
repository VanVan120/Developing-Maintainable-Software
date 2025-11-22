package com.comp2042.controller.gameControl;

import com.comp2042.model.ClearRow;
import com.comp2042.model.ViewData;

/**
 * Data transfer object returned by {@link GameEngine#moveDown(com.comp2042.input.EventSource)}.
 *
 * <p>Contains the cleared-row information (if any), the view data for the
 * falling piece after the move, a snapshot of the board matrix, the number
 * of forward cleared rows (used by scoring/attack logic), and a boolean
 * flag indicating whether a spawn-collision (game over) occurred.</p>
 */
public class MoveDownResult {
    private final ClearRow clearRow;
    private final ViewData viewData;
    private final int[][] boardMatrix;
    private final int forwardCount;
    private final boolean gameOver;

    public MoveDownResult(ClearRow clearRow, ViewData viewData, int[][] boardMatrix, int forwardCount, boolean gameOver) {
        this.clearRow = clearRow;
        this.viewData = viewData;
        this.boardMatrix = boardMatrix;
        this.forwardCount = forwardCount;
        this.gameOver = gameOver;
    }

    public ClearRow getClearRow() {
        return clearRow;
    }

    public ViewData getViewData() {
        return viewData;
    }

    public int[][] getBoardMatrix() {
        return boardMatrix;
    }

    public int getForwardCount() {
        return forwardCount;
    }

    public boolean isGameOver() {
        return gameOver;
    }
}
