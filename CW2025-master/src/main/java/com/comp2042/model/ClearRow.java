package com.comp2042.model;

import com.comp2042.utils.MatrixOperations;

/**
 * Result object returned after clearing rows from the board.
 *
 * <p>Contains the number of lines removed, the new board matrix (a defensive
 * copy is returned by {@link #getNewMatrix()}), any score bonus awarded, and
 * an array of indices of cleared rows.</p>
 */
public final class ClearRow {

    private final int linesRemoved;
    private final int[][] newMatrix;
    private final int[] clearedRows;
    private final int scoreBonus;

    public ClearRow(int linesRemoved, int[][] newMatrix, int scoreBonus, int[] clearedRows) {
        this.linesRemoved = linesRemoved;
        // store a defensive copy of the matrix to avoid external mutation
        this.newMatrix = MatrixOperations.copy(newMatrix);
        this.scoreBonus = scoreBonus;
        this.clearedRows = (clearedRows == null) ? new int[0] : clearedRows.clone();
    }

    /** Number of lines removed by this clear operation. */
    public int getLinesRemoved() {
        return linesRemoved;
    }

    /**
     * Return a defensive copy of the new board matrix after rows were cleared.
     */
    public int[][] getNewMatrix() {
        return MatrixOperations.copy(newMatrix);
    }

    /** Score bonus awarded for this clear operation. */
    public int getScoreBonus() {
        return scoreBonus;
    }
    
    /** Return a defensive copy of the cleared row indices (may be empty). */
    public int[] getClearedRows() {
        return clearedRows == null ? new int[0] : clearedRows.clone();
    }
}
