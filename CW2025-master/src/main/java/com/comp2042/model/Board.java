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

    /**
     * Move the active brick down by one cell.
     *
     * @return {@code true} if the brick was moved successfully; {@code false}
     *         if the movement was blocked (collision or floor) and the caller
     *         should typically lock the piece.
     */
    boolean moveBrickDown();

    /**
     * Move the active brick left by one cell.
     *
     * @return {@code true} when the move succeeded, {@code false} when blocked.
     */
    boolean moveBrickLeft();

    /**
     * Move the active brick right by one cell.
     *
     * @return {@code true} when the move succeeded, {@code false} when blocked.
     */
    boolean moveBrickRight();

    /**
     * Rotate the active brick left (counter-clockwise). Implementations may
     * apply wall-kick adjustments when a rotation would otherwise collide.
     *
     * @return {@code true} if rotation was applied, {@code false} when blocked.
     */
    boolean rotateLeftBrick();

    /**
     * Spawn a new active brick from the generator and place it on the board.
     *
     * @return {@code true} on successful spawn (no immediate collision); if
     *         {@code false} is returned the caller may interpret it as a game
     *         over condition.
     */
    boolean createNewBrick();

    /**
     * Return the current board matrix (rows x cols). Implementations should
     * return either a defensive copy or document that the caller must not
     * mutate the returned array.
     *
     * @return current board matrix; may be a defensive copy
     */
    int[][] getBoardMatrix();

    /**
     * Return a snapshot of the data required by the renderer to draw the
     * current state (current piece, position and next piece preview).
     *
     * @return a {@link ViewData} instance describing the view state; callers
     *         may assume this is immutable or a defensive snapshot.
     */
    ViewData getViewData();

    /**
     * Merge (lock) the active brick into the background matrix so its cells
     * become part of the static board.
     */
    void mergeBrickToBackground();

    /**
     * Clear any full rows and return a {@link ClearRow} result describing
     * the number of lines removed, the new matrix and any score bonus.
     */
    ClearRow clearRows();

    /**
     * Return the {@link Score} holder for this board. The returned object is
     * typically a UI-bindable wrapper (JavaFX property backed).
     */
    Score getScore();

    /**
     * Reset the board to the initial state to start a new game. This will
     * typically clear the matrix, reset score and spawn a new active brick.
     */
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
