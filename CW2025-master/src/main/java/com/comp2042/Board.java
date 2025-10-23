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

    /**
     * Swap the currently-falling piece with the next piece from the generator.
     * Return true if the swap was performed, false otherwise (e.g. would cause collision).
     */
    default boolean swapCurrentWithNext() { return false; }
}
