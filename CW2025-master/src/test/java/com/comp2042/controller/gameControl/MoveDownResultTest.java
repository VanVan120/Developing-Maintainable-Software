package com.comp2042.controller.gameControl;

import com.comp2042.model.ClearRow;
import com.comp2042.model.ViewData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MoveDownResultTest {

    @Test
    void gettersReturnConstructionValues() {
        int[][] board = new int[20][10];
        int[][] view = new int[][]{{1}};
        int[] cleared = new int[]{19};
        ClearRow cr = new ClearRow(1, board, 100, cleared);
        ViewData vd = new ViewData(view, 0, 0, new int[][]{{0}});

        MoveDownResult r = new MoveDownResult(cr, vd, board, 2, true);

        assertSame(cr, r.getClearRow(), "clearRow should be the same instance passed to constructor");
        assertSame(vd, r.getViewData(), "viewData should be the same instance passed to constructor");
        assertArrayEquals(board, r.getBoardMatrix(), "boardMatrix should match");
        assertEquals(2, r.getForwardCount(), "forwardCount should match");
        assertTrue(r.isGameOver(), "gameOver should match");
    }
}
