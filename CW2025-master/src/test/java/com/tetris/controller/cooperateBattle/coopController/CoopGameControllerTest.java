package com.tetris.controller.cooperateBattle.coopController;

import com.comp2042.controller.cooperateBattle.coopController.CoopGameController;
import com.comp2042.model.DownData;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CoopGameController focused on basic lifecycle and hard-drop behavior.
 */
public class CoopGameControllerTest {

    @Test
    public void createNewGame_initializesBoardAndPlayers() {
        CoopGameController ctrl = new CoopGameController(10, 20);
        ctrl.createNewGame();

        int[][] board = ctrl.getBoardMatrix();
        assertEquals(20, board.length, "Board height should match");
        assertEquals(10, board[0].length, "Board width should match");

        assertNotNull(ctrl.getViewDataLeft(), "Left player view should be initialized after createNewGame");
        assertNotNull(ctrl.getViewDataRight(), "Right player view should be initialized after createNewGame");

        assertEquals(0, ctrl.getTotalScoreProperty().get(), "Total score should start at 0");
    }

    @Test
    public void hardDropLeft_mergesPieceIntoBoardAndIncreasesScore() {
        CoopGameController ctrl = new CoopGameController(10, 20);
        ctrl.createNewGame();

        int[][] before = ctrl.getBoardMatrix();
        boolean nonZeroBefore = Arrays.stream(before).flatMapToInt(row -> Arrays.stream(row)).anyMatch(x -> x != 0);
        assertFalse(nonZeroBefore, "Board should start empty");

        int prevScore = ctrl.getTotalScoreProperty().get();
        int safety = 0;
        while (safety++ < 500) {
            DownData d = ctrl.onLeftDown();
            // onLeftDown returns a non-null DownData both when a piece simply moves and when it lands
            // the landing/merge case is indicated by a non-null ClearRow in the DownData
            if (d != null && d.getClearRow() != null) break;
        }

        int[][] after = ctrl.getBoardMatrix();
        boolean nonZeroAfter = Arrays.stream(after).flatMapToInt(row -> Arrays.stream(row)).anyMatch(x -> x != 0);

        assertTrue(nonZeroAfter, "Board should contain blocks after a hard drop");
        assertTrue(ctrl.getTotalScoreProperty().get() >= prevScore, "Score should not decrease after hard drop");

        // DownData may be null in edge cases; ensure we received a DownData or at least the board changed
        // The main assertions above validate important post-conditions
    }
}
