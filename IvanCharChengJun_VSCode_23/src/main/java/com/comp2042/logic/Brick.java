package com.comp2042.logic;

import java.util.List;

/**
 * Represents a Tetris brick shape and its rotation matrices.
 *
 * <p>Implementations provide the set of rotation matrices for the brick.
 * Each matrix is a 4x4 integer array where non-zero entries indicate the
 * presence of a block; specific non-zero values may be used by rendering
 * code as colour or index identifiers.
 *
 * <p>Contract:
 * - {@link #getShapeMatrix()} returns a defensive deep-copy of the internal
 *   matrices so callers may safely mutate the returned arrays without
 *   affecting shared state.
 * - Implementations are effectively immutable after construction and are
 *   safe for concurrent read-only use.
 */
public interface Brick {

    /**
     * Return a defensive deep-copy of the brick's rotation matrices.
     *
     * @return a non-null {@link List} of 4x4 {@code int[][]} matrices; callers
     *         may mutate the returned arrays without affecting the provider.
     */
    List<int[][]> getShapeMatrix();
}
