package com.comp2042;

public interface Board {

    boolean moveBrickDown();

    boolean moveBrickLeft();

    boolean moveBrickRight();

    boolean rotateLeftBrick();

    boolean createNewBrick();

    int[][] getBoardMatrix();

    ViewData getViewData();

    void mergeBrickToBackground();

    ClearRow clearRows();

    Score getScore();

    void newGame();

    /**
     * Peek at the upcoming bricks without consuming them. Returns at most {@code count} items.
     */
    java.util.List<com.comp2042.logic.bricks.Brick> getUpcomingBricks(int count);
}
