package com.tetris.controller.cooperateBattle.coopController;

import com.comp2042.controller.cooperateBattle.coopController.CoopPlayerState;
import com.comp2042.logic.Brick;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CoopPlayerState behaviors: translation, merge and down detection.
 */
public class CoopPlayerStateTest {

    static class TestBrick implements Brick {
        private final List<int[][]> shapes = new ArrayList<>();
        TestBrick(int[][] shape) { shapes.add(shape); }
        @Override public List<int[][]> getShapeMatrix() { return shapes; }
    }

    @Test
    public void tryTranslate_movesWhenNoCollision() {
        CoopPlayerState p = new CoopPlayerState();
        p.setBrick(new TestBrick(new int[][]{{1}}));
        p.offset = new java.awt.Point(1,1);

        int[][] board = new int[6][6];
        boolean moved = p.tryTranslate(1, 0, board, null, "ctx");
        assertTrue(moved, "Translation should succeed on empty board");
        assertEquals(2, p.offset.x);
    }

    @Test
    public void tryTranslate_blocksWhenCollisionWithBoard() {
        CoopPlayerState p = new CoopPlayerState();
        p.setBrick(new TestBrick(new int[][]{{1}}));
        p.offset = new java.awt.Point(1,1);

        int[][] board = new int[6][6];
        // place a block at the target location (2,1)
        board[1][2] = 9;
        boolean moved = p.tryTranslate(1, 0, board, null, "ctx");
        assertFalse(moved, "Translation should fail when board cell occupied");
        assertEquals(1, p.offset.x);
    }

    @Test
    public void tryTranslate_blocksWhenCollisionWithOtherPlayer() {
        CoopPlayerState a = new CoopPlayerState();
        CoopPlayerState b = new CoopPlayerState();
        a.setBrick(new TestBrick(new int[][]{{1}}));
        b.setBrick(new TestBrick(new int[][]{{1}}));
        a.offset = new java.awt.Point(1,1);
        b.offset = new java.awt.Point(2,1); // occupies (2,1)

        int[][] board = new int[6][6];
        boolean moved = a.tryTranslate(1, 0, board, b, "ctx");
        assertFalse(moved, "Translation should fail when other player's piece occupies target");
        assertEquals(1, a.offset.x);
    }

    @Test
    public void mergeIntoBoard_placesShape() {
        CoopPlayerState p = new CoopPlayerState();
        p.setBrick(new TestBrick(new int[][]{{2}}));
        p.offset = new java.awt.Point(3,2);
        int[][] board = new int[6][6];
        int[][] merged = p.mergeIntoBoard(board);
        assertEquals(2, merged[2][3]);
    }

    @Test
    public void canMoveDown_detectsGround() {
        CoopPlayerState p = new CoopPlayerState();
        p.setBrick(new TestBrick(new int[][]{{1}}));
        p.offset = new java.awt.Point(2,0);
        int[][] board = new int[6][6];
        board[1][2] = 7; // block directly below
        assertFalse(p.canMoveDown(board));
        board[1][2] = 0;
        assertTrue(p.canMoveDown(board));
    }
}
