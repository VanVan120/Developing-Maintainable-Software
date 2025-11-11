package com.tetris.controller.gameControl;

import com.comp2042.controller.gameControl.GameEngine;
import com.comp2042.controller.gameControl.MoveDownResult;
import com.comp2042.input.EventSource;
import com.comp2042.model.ClearRow;
import com.comp2042.model.Score;
import com.comp2042.model.ViewData;
import com.comp2042.model.Board;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;

class GameEngineTest {

    static class FakeBoard implements Board {
        boolean moveDownResult;
        boolean createNewBrickResult = false;
        final Score score = new Score();

        public FakeBoard(boolean moveDownResult) {
            this.moveDownResult = moveDownResult;
        }

        @Override
        public boolean moveBrickDown() { return moveDownResult; }

        @Override
        public boolean moveBrickLeft() { return false; }

        @Override
        public boolean moveBrickRight() { return false; }

        @Override
        public boolean rotateLeftBrick() { return false; }

        @Override
        public boolean createNewBrick() { return createNewBrickResult; }

        @Override
        public int[][] getBoardMatrix() { return new int[20][10]; }

    @Override
    public ViewData getViewData() { return new ViewData(new int[][]{{1}}, 0, 0, new int[][]{{0}}); }

        @Override
        public void mergeBrickToBackground() { /* no-op */ }

        @Override
        public ClearRow clearRows() { return null; }

        @Override
        public Score getScore() { return score; }

        @Override
        public void newGame() { score.reset(); }

        @Override
        public List<com.comp2042.logic.Brick> getUpcomingBricks(int count) { return Collections.emptyList(); }
    }

    @Test
    void moveDown_userAddsScoreWhenCanMove() {
        FakeBoard board = new FakeBoard(true);
        GameEngine engine = new GameEngine(board);

        assertEquals(0, engine.getScoreProperty().get());
        MoveDownResult res = engine.moveDown(EventSource.USER);
        assertFalse(res.isGameOver());
        assertNull(res.getClearRow());
        assertEquals(1, engine.getScoreProperty().get());
    }

    @Test
    void moveDown_mergeClearsRowsAndAddsBonus() {
        // create a board that will indicate landing and clearing rows
        FakeBoard board = new FakeBoard(false) {
            @Override
            public ClearRow clearRows() {
                int[][] newMatrix = new int[20][10];
                int[] cleared = new int[] {18, 19};
                return new ClearRow(2, newMatrix, 200, cleared);
            }
            @Override
            public boolean createNewBrick() { return false; }
        };

        GameEngine engine = new GameEngine(board);
        MoveDownResult res = engine.moveDown(EventSource.THREAD);

        assertFalse(res.isGameOver());
        assertNotNull(res.getClearRow());
        // forwardCount should be equal to cleared rows since no garbage values present
        assertEquals(2, res.getForwardCount());
        assertEquals(200, engine.getScoreProperty().get());
    }
}
