package com.tetris.controller.gameControl;

import com.comp2042.input.MoveEvent;
import com.comp2042.input.EventSource;
import com.comp2042.input.EventType;
import com.comp2042.controller.gameControl.GameController;
import com.comp2042.controller.guiControl.GuiController;
import com.comp2042.model.Board;
import com.comp2042.model.ClearRow;
import com.comp2042.model.Score;
import com.comp2042.model.ViewData;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameControllerTest {

    static class StubGui extends GuiController {
        boolean gameOverCalled = false;
        MoveEvent lastEvent;

    @Override public void setEventListener(com.comp2042.input.InputEventListener listener) {}
    @Override public void initGameView(int[][] matrix, ViewData viewData) {}
    @Override public void bindScore(javafx.beans.property.IntegerProperty score) {}
    @Override public void setSwapKey(javafx.scene.input.KeyCode keyCode) {}
    @Override public void refreshGameBackground(int[][] matrix) {}
    @Override public void refreshCurrentView(ViewData viewData) {}
    @Override public void showNextBricks(List<com.comp2042.logic.Brick> bricks) {}
    @Override public void gameOver() { gameOverCalled = true; }
    }

    static class FakeBoard implements Board {
        boolean moveDownResult = false;
        final Score score = new Score();

        @Override public boolean moveBrickDown() { return moveDownResult; }
        @Override public boolean moveBrickLeft() { return false; }
        @Override public boolean moveBrickRight() { return false; }
        @Override public boolean rotateLeftBrick() { return false; }
        @Override public boolean createNewBrick() { return true; }
        @Override public int[][] getBoardMatrix() { return new int[20][10]; }
        @Override public ViewData getViewData() { return new ViewData(new int[][]{{1}},0,0,new int[][]{{0}}); }
        @Override public void mergeBrickToBackground() {}
        @Override public ClearRow clearRows() { return null; }
        @Override public Score getScore() { return score; }
        @Override public void newGame() { score.reset(); }
        @Override public List<com.comp2042.logic.Brick> getUpcomingBricks(int count) { return List.of(); }
    }

    @Test
    void onDownEvent_triggersGameOverWhenEngineReportsGameOver() {
        StubGui gui = new StubGui();
        FakeBoard board = new FakeBoard();
        // create controller with a board that will cause createNewBrick() == true -> gameOver
        GameController controller = new GameController(gui, board);

    MoveEvent evt = new MoveEvent(EventType.DOWN, EventSource.USER);
        controller.onDownEvent(evt);

        assertTrue(gui.gameOverCalled, "gameOver should be called when engine signals game over");
    }

    @Test
    void clearRowHandler_isInvokedWhenForwardCountPositive() {
        StubGui gui = new StubGui();
        FakeBoard board = new FakeBoard() {
            @Override
            public ClearRow clearRows() {
                int[][] m = new int[20][10];
                int[] cleared = new int[] { 19 };
                return new ClearRow(1, m, 50, cleared);
            }
            @Override
            public boolean moveBrickDown() { return false; }
            @Override
            public boolean createNewBrick() { return false; }
        };

        GameController controller = new GameController(gui, board);
        final int[] captured = new int[1];
        controller.setClearRowHandler(i -> captured[0] = i);

    controller.onDownEvent(new MoveEvent(EventType.DOWN, EventSource.THREAD));
        assertEquals(1, captured[0]);
    }
}
