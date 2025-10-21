package com.comp2042;

public final class ClearRow {

    private final int linesRemoved;
    private final int[][] newMatrix;
    private final int[] clearedRows;
    private final int scoreBonus;

    public ClearRow(int linesRemoved, int[][] newMatrix, int scoreBonus, int[] clearedRows) {
        this.linesRemoved = linesRemoved;
        this.newMatrix = newMatrix;
        this.scoreBonus = scoreBonus;
        this.clearedRows = (clearedRows == null) ? new int[0] : clearedRows.clone();
    }

    public int getLinesRemoved() {
        return linesRemoved;
    }

    public int[][] getNewMatrix() {
        return MatrixOperations.copy(newMatrix);
    }

    public int getScoreBonus() {
        return scoreBonus;
    }

    /** Returns a copy of the absolute board row indices that were cleared (may be empty).
     *  Rows are indexed in the same coordinate system as the board matrix (0 = top including hidden rows).
     */
    public int[] getClearedRows() {
        return clearedRows == null ? new int[0] : clearedRows.clone();
    }
}
