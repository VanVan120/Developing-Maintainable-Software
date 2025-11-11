package com.comp2042.model;

import java.util.List;

import com.comp2042.logic.Brick;

/**
 * Board model interface describing operations the controllers call to
 * manipulate and query the game board.
 *
 * Implementations are expected to be single-threaded with respect to the
 * game loop; callers should perform UI-thread / game-thread synchronization
 * externally if required.
 */
public interface Board {

    /** Move the active brick down by one cell. Returns true if moved. */
    boolean moveBrickDown();

    /** Move the active brick left. Returns true if moved. */
    boolean moveBrickLeft();

    /** Move the active brick right. Returns true if moved. */
    boolean moveBrickRight();

    /** Rotate the active brick left (counter-clockwise). Returns true if rotated. */
    boolean rotateLeftBrick();

    /** Create a new active brick on the board. Returns true on success. */
    boolean createNewBrick();

    /** Return the current board matrix (rows x cols). Implementations may return a defensive copy. */
    int[][] getBoardMatrix();

    /** Return non-graphical view data used by the renderer. */
    ViewData getViewData();

    /** Merge the active brick into the background matrix (lock it in place). */
    void mergeBrickToBackground();

    /** Clear full rows and return information about cleared rows. */
    ClearRow clearRows();

    /** Return the current score. */
    Score getScore();

    /** Reset the board to start a new game. */
    void newGame();

    /**
     * Return up to {@code count} upcoming bricks (preview queue). Implementations
     * should return a stable snapshot (defensive copy) suitable for rendering.
     */
    List<Brick> getUpcomingBricks(int count);

    /**
     * Optional: swap the currently active brick with the head of the next queue.
     * Default implementation returns false (operation not supported).
     */
    default boolean swapCurrentWithNext() { return false; }
}
