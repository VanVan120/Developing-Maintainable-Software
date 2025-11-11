package com.tetris.controller.cooperateBattle.coopController;

import com.comp2042.controller.cooperateBattle.coopController.CoopPlayerState;
import com.comp2042.controller.cooperateBattle.coopController.CoopTickHandler;
import com.comp2042.logic.Brick;
import com.comp2042.model.CoopScore;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CoopTickHandlerTest {

    static class TestBrick implements Brick {
        private final List<int[][]> shapes = new ArrayList<>();
        TestBrick(int[][] shape) { shapes.add(shape); }
        @Override public List<int[][]> getShapeMatrix() { return shapes; }
    }

    @Test
    public void processTick_movesPlayers_whenBothCanMove() {
        int width = 6, height = 6;
        int[][] board = new int[height][width];

        CoopPlayerState left = new CoopPlayerState();
        CoopPlayerState right = new CoopPlayerState();
        left.setBrick(new TestBrick(new int[][]{{1}}));
        right.setBrick(new TestBrick(new int[][]{{1}}));
        left.offset = new Point(1, 0);
        right.offset = new Point(4, 0);

        CoopScore score = new CoopScore();

        CoopTickHandler.TickOutcome out = CoopTickHandler.processTick(board, left, right, score, false);
        // no merge expected because both can move down
        assertFalse(out.merged, "No merge should occur when both can move down");
        // offsets should have moved down by 1
        assertEquals(1, (int) left.offset.getY());
        assertEquals(1, (int) right.offset.getY());
        // score should have increased by 2 (one per player)
        assertTrue(score.scoreProperty().get() >= 2);
    }

    @Test
    public void processTick_mergesWhenCannotMoveDown() {
        int width = 6, height = 6;
        int[][] board = new int[height][width];
        // place ground at row 1 so a piece at y=0 cannot move down
        for (int x = 0; x < width; x++) board[1][x] = 9;

        CoopPlayerState left = new CoopPlayerState();
        CoopPlayerState right = new CoopPlayerState();
        left.setBrick(new TestBrick(new int[][]{{1}}));
        right.setBrick(new TestBrick(new int[][]{{1}}));
        left.offset = new Point(1, 0);
        right.offset = new Point(4, 0);

        CoopScore score = new CoopScore();

        CoopTickHandler.TickOutcome out = CoopTickHandler.processTick(board, left, right, score, false);
        assertTrue(out.merged, "Merged should be true when pieces cannot move down");
        assertNotNull(out.clearRow, "ClearRow should be provided after merge check");
        // newBoard should differ from original board (merged blocks present)
        boolean boardChanged = false;
        for (int r = 0; r < height; r++) for (int c = 0; c < width; c++) if (out.newBoard[r][c] != board[r][c]) boardChanged = true;
        assertTrue(boardChanged, "Board must change after merge");
    }
}
