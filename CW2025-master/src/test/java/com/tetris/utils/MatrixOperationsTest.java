package com.tetris.utils;

import com.comp2042.model.ClearRow;
import com.comp2042.utils.MatrixOperations;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatrixOperationsTest {

    @Test
    void intersectDetectsCollisionAndOutOfBounds() {
        int[][] matrix = new int[4][4];
        matrix[1][1] = 5;
        int[][] brick = new int[][]{{1}};
        // place brick at empty cell
        assertFalse(MatrixOperations.intersect(matrix, brick, 0, 0));
        // place brick overlapping filled cell
        assertTrue(MatrixOperations.intersect(matrix, brick, 1, 1));
        // out of bounds to the left
        assertTrue(MatrixOperations.intersect(matrix, brick, -1, 0));
        // out of bounds below
        assertTrue(MatrixOperations.intersect(matrix, brick, 0, 5));
    }

    @Test
    void intersectForGhostTreatsNegativeYAsNonColliding() {
        int[][] matrix = new int[3][3];
        matrix[0][1] = 1; // top row occupied
        int[][] brick = new int[][]{{1,1}}; // single row brick

        // place brick partly above the top (y = -1). For ghost, should not consider above rows colliding
        assertFalse(MatrixOperations.intersectForGhost(matrix, brick, 0, -1));

        // horizontal out of bounds remains collision
        assertTrue(MatrixOperations.intersectForGhost(matrix, brick, -2, -1));

        // below the board is collision
        assertTrue(MatrixOperations.intersectForGhost(matrix, brick, 0, 5));
    }

    @Test
    void copyReturnsDeepCopy() {
        int[][] original = new int[][]{{1,2},{3,4}};
        int[][] cp = MatrixOperations.copy(original);
        assertArrayEquals(original[0], cp[0]);
        cp[0][0] = 9;
        assertEquals(1, original[0][0]);
    }

    @Test
    void mergePlacesBrickIntoMatrix() {
        int[][] base = new int[3][3];
        int[][] brick = new int[][]{{7,0},{0,8}}; // 2x2 brick
        int[][] merged = MatrixOperations.merge(base, brick, 1, 1);
        assertEquals(7, merged[1][1]);
        assertEquals(8, merged[2][2]);
        // original base unchanged
        assertEquals(0, base[1][1]);
    }

    @Test
    void checkRemovingClearsRowsAndComputesBonus() {
        int[][] m = new int[4][3];
        // Fill row 1 completely and leave others partially empty
        for (int j = 0; j < 3; j++) m[1][j] = 1;
        m[2][0] = 1;
        ClearRow cr = MatrixOperations.checkRemoving(m);
        assertEquals(1, cr.getLinesRemoved());
        assertNotNull(cr.getNewMatrix());
        assertEquals(50, cr.getScoreBonus()); // 50 * 1 * 1
        assertEquals(1, cr.getClearedRows().length);
        assertEquals(1, cr.getClearedRows()[0]);
    }
}
