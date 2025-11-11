package com.comp2042.controller.gameControl;

import com.comp2042.model.ClearRow;
import com.comp2042.model.ViewData;

/**
 * Simple DTO returned by GameEngine.moveDown to carry results to the UI adapter.
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
