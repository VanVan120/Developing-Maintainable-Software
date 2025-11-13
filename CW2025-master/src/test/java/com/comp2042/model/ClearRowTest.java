package com.comp2042.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ClearRowTest {

    @Test
    void gettersAndDefensiveCopyMatrixAndClearedRows() {
        int[][] matrix = new int[][] { {1,2}, {3,4} };
        int[] cleared = new int[] {0,1};
        ClearRow cr = new ClearRow(2, matrix, 100, cleared);

        // getters
        assertEquals(2, cr.getLinesRemoved());
        assertEquals(100, cr.getScoreBonus());

        // defensive copy: modify original inputs should not affect stored values
        matrix[0][0] = 99;
        cleared[0] = 9;

        int[][] returned = cr.getNewMatrix();
        assertNotEquals(99, returned[0][0]);

        int[] returnedCleared = cr.getClearedRows();
        assertNotEquals(9, returnedCleared[0]);

        // modifying returned copies should not affect subsequent calls
        returned[0][0] = 77;
        int[][] returned2 = cr.getNewMatrix();
        assertNotEquals(77, returned2[0][0]);

        returnedCleared[0] = 8;
        int[] returnedCleared2 = cr.getClearedRows();
        assertNotEquals(8, returnedCleared2[0]);
    }

    @Test
    void nullClearedRowsHandledAsEmpty() {
        int[][] matrix = new int[][] { {0} };
        ClearRow cr = new ClearRow(0, matrix, 0, null);
        int[] cleared = cr.getClearedRows();
        assertNotNull(cleared);
        assertEquals(0, cleared.length);
    }
}
