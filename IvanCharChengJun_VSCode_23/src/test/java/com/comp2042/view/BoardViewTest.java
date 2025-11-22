package com.comp2042.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for BoardView pure helpers.
 * These tests avoid JavaFX by exercising the static, pure computation methods.
 */
public class BoardViewTest {

    @Test
    public void effectiveHeight_trimsEmptyBottomRows() {
        int[][] shape = new int[][] {
            {1, 1},
            {0, 0}
        };
        int h = BoardView.computeEffectiveBrickHeight(shape);
        assertEquals(1, h, "Effective height should ignore empty bottom row");
    }

    @Test
    public void effectiveHeight_fullShapeReturnsLength() {
        int[][] shape = new int[][] {
            {1, 1},
            {1, 0}
        };
        int h = BoardView.computeEffectiveBrickHeight(shape);
        assertEquals(2, h, "Effective height should be full length when bottom row has blocks");
    }

    @Test
    public void computeLandingY_onEmptyBoardFallsToMax() {
        int rows = 6;
        int cols = 4;
        int[][] board = new int[rows][cols]; // all zeros
        int[][] shape = new int[][] {
            {1}
        };
        int startX = 0;
        int startY = 0;
        int effective = BoardView.computeEffectiveBrickHeight(shape);
        int landing = BoardView.computeLandingY(startX, startY, shape, board, effective);
        int expectedMax = rows - effective;
        assertEquals(expectedMax, landing, "Landing Y should be the max available row on an empty board");
    }

    @Test
    public void computeLandingY_stopsBeforeCollision() {
        int rows = 6;
        int cols = 4;
        int[][] board = new int[rows][cols];
        // place a blocking cell at row 3, column 0
        board[3][0] = 1;
        int[][] shape = new int[][] {
            {1}
        };
        int startX = 0;
        int startY = 0;
        int effective = BoardView.computeEffectiveBrickHeight(shape);
        int landing = BoardView.computeLandingY(startX, startY, shape, board, effective);
        // conflict at y=3 => landing should be 2
        assertEquals(2, landing, "Landing Y should be one row above the collision");
    }
}
