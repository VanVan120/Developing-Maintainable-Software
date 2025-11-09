package com.comp2042.logic.bricks;

import java.util.List;

/**
 * Represents a Tetris brick shape and its rotation matrices.
 * Implementations provide the set of rotation matrices for the brick.
 */
public interface Brick {

    /**
     * Return a defensive deep-copy of the brick's rotation matrices.
     */
    List<int[][]> getShapeMatrix();
}
