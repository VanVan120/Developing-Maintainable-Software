package com.comp2042.logic;

import java.util.List;

/**
 * Produces bricks for gameplay and exposes a small preview API.
 *
 * <p>Implementations are responsible for providing the current brick
 * (consuming it when {@link #getBrick()} is called) and offering look-ahead
 * information to support preview UI. Implementations may use a bag/queue or
 * other strategy; callers should rely only on the documented method contracts.
 *
 * <p>Threading: typical implementations are used from a single game loop
 * thread. If an implementation is shared across threads it must provide its
 * own synchronization; callers should not assume thread-safety unless
 * documented by the concrete implementation.
 */
public interface BrickGenerator {

    /**
     * Consume and return the next Brick for placement.
     *
     * @return a non-null {@link Brick} instance (implementations should ensure
     *         there is always a brick available)
     */
    Brick getBrick();

    /**
     * Return (without consuming) the immediate next Brick that will be
     * returned by {@link #getBrick()}.
     *
     * @return the next {@link Brick} or {@code null} if none is available
     *         (implementations should prefer returning a non-null value).
     */
    Brick getNextBrick();

    /**
     * Peek upcoming bricks without consuming them. Default implementation
     * returns an empty list.
     *
     * @param count maximum number of upcoming bricks to return
     * @return a list (possibly empty) of upcoming {@link Brick} instances
     */
    default List<Brick> getUpcomingBricks(int count) {
        return java.util.Collections.emptyList();
    }

    /**
     * Replace the head 'next' brick with the provided {@code replacement}
     * without altering the remainder of the queue.
     *
     * @param replacement the brick to place at the head of the upcoming queue
     * @return {@code true} if the replacement succeeded, {@code false}
     *         otherwise (e.g. when the generator has no next element)
     */
    default boolean replaceNext(Brick replacement) { return false; }
}
