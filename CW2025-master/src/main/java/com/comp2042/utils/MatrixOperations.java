package com.comp2042.utils;

import com.comp2042.model.ClearRow;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Small collection of matrix helpers used by the game logic and renderer.
 *
 * <p>All methods here are static and intended to be side-effect free: callers
 * receive copies where appropriate. The class intentionally keeps behaviour
 * simple and predictable so higher-level code can reason about collisions,
 * merges and row-clearing.
 */
public class MatrixOperations {

    // We don't want to instantiate this utility class
    private MatrixOperations() {
    }

    /**
     * Returns true if any non-zero cell of {@code brick} would collide with
     * filled cells in {@code matrix} or would fall outside the visible board.
     */
    public static boolean intersect(final int[][] matrix, final int[][] brick, final int x, final int y) {
        Objects.requireNonNull(matrix, "matrix");
        Objects.requireNonNull(brick, "brick");
        // brick is an array of rows (i) and columns (j). matrix is matrix[row][col].
        for (int i = 0; i < brick.length; i++) {
            for (int j = 0; j < brick[i].length; j++) {
                int targetY = y + i; // row index
                int targetX = x + j; // column index
                if (brick[i][j] != 0) {
                    if (isOutOfBounds(matrix, targetX, targetY) || matrix[targetY][targetX] != 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Like {@link #intersect(int[][], int[][], int, int)} but treats cells above
     * the top of the board (targetY &lt; 0) as non-colliding. Used for ghost/landing
     * calculations.
     */
    public static boolean intersectForGhost(final int[][] matrix, final int[][] brick, final int x, final int y) {
        Objects.requireNonNull(matrix, "matrix");
        Objects.requireNonNull(brick, "brick");
        for (int i = 0; i < brick.length; i++) {
            for (int j = 0; j < brick[i].length; j++) {
                int targetY = y + i; // row index
                int targetX = x + j; // column index
                if (brick[i][j] == 0) continue;
                // horizontal out-of-bounds is a collision
                if (targetX < 0 || targetX >= matrix[0].length) return true;
                // below the board is a collision
                if (targetY >= matrix.length) return true;
                // if within the visible matrix, check filled cells. If targetY < 0 (above top), ignore.
                if (targetY >= 0 && matrix[targetY][targetX] != 0) return true;
            }
        }
        return false;
    }

    private static boolean isOutOfBounds(final int[][] matrix, final int targetX, final int targetY) {
        return targetX < 0 || targetY < 0 || targetY >= matrix.length || targetX >= matrix[targetY].length;
    }

    /**
     * Deep-copy a rectangular int matrix. Caller retains ownership of the returned
     * array and may mutate it safely.
     */
    public static int[][] copy(final int[][] original) {
        Objects.requireNonNull(original, "original");
        int[][] myInt = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            int[] aMatrix = original[i];
            int aLength = aMatrix.length;
            myInt[i] = new int[aLength];
            System.arraycopy(aMatrix, 0, myInt[i], 0, aLength);
        }
        return myInt;
    }

    /**
     * Return a new matrix with the brick merged into the filledFields matrix.
     * The original filledFields is not modified.
     */
    public static int[][] merge(final int[][] filledFields, final int[][] brick, final int x, final int y) {
        Objects.requireNonNull(filledFields, "filledFields");
        Objects.requireNonNull(brick, "brick");
        int[][] copy = copy(filledFields);
        for (int i = 0; i < brick.length; i++) {
            for (int j = 0; j < brick[i].length; j++) {
                int targetY = y + i; // row
                int targetX = x + j; // column
                if (brick[i][j] != 0) {
                    copy[targetY][targetX] = brick[i][j];
                }
            }
        }
        return copy;
    }

    /**
     * Scan the board for full rows, return a ClearRow containing the number of
     * cleared rows, the new matrix (with rows shifted down), the score bonus
     * and the absolute indices of cleared rows.
     */
    public static ClearRow checkRemoving(final int[][] matrix) {
        Objects.requireNonNull(matrix, "matrix");
        int[][] tmp = new int[matrix.length][matrix[0].length];
        Deque<int[]> newRows = new ArrayDeque<>();
        List<Integer> clearedRows = new ArrayList<>();

        for (int i = 0; i < matrix.length; i++) {
            int[] tmpRow = new int[matrix[i].length];
            boolean rowToClear = true;
            for (int j = 0; j < matrix[0].length; j++) {
                if (matrix[i][j] == 0) {
                    rowToClear = false;
                }
                tmpRow[j] = matrix[i][j];
            }
            if (rowToClear) {
                clearedRows.add(i);
            } else {
                newRows.add(tmpRow);
            }
        }
        for (int i = matrix.length - 1; i >= 0; i--) {
            int[] row = newRows.pollLast();
            if (row != null) {
                tmp[i] = row;
            } else {
                break;
            }
        }
        int scoreBonus = 50 * clearedRows.size() * clearedRows.size();
        // convert clearedRows list to int[] of absolute row indices
        int[] cleared = new int[clearedRows.size()];
        for (int i = 0; i < clearedRows.size(); i++) cleared[i] = clearedRows.get(i);
        return new ClearRow(clearedRows.size(), tmp, scoreBonus, cleared);
    }

    public static List<int[][]> deepCopyList(final List<int[][]> list) {
        Objects.requireNonNull(list, "list");
        return list.stream().map(MatrixOperations::copy).collect(Collectors.toList());
    }

}
