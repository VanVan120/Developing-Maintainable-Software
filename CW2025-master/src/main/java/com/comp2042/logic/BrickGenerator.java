package com.comp2042.logic;

/**
 * Produces bricks for gameplay and exposes a small preview API.
 * Implementations may provide a queue/bag; callers can request the current
 * brick and peek at upcoming bricks.
 */
public interface BrickGenerator {

    Brick getBrick();

    Brick getNextBrick();

    /**
     * Optional: peek upcoming bricks without consuming. Default implementation returns empty list.
     */
    default java.util.List<Brick> getUpcomingBricks(int count) {
        return java.util.Collections.emptyList();
    }

    /**
     * Replace the head 'next' brick with a provided Brick without altering the remainder of the queue.
     * Return true if replacement succeeded, false otherwise.
     */
    default boolean replaceNext(Brick replacement) { return false; }
}
