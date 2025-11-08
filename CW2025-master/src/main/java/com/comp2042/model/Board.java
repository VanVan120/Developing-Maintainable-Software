package com.comp2042.model;

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

    java.util.List<com.comp2042.logic.bricks.Brick> getUpcomingBricks(int count);

    default boolean swapCurrentWithNext() { return false; }
}
