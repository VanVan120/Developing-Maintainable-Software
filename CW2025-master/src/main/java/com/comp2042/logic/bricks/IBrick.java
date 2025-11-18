package com.comp2042.logic.bricks;

import com.comp2042.logic.Brick;
import com.comp2042.utils.MatrixOperations;

import java.util.ArrayList;
import java.util.List;

/**
 * I-shaped tetromino implementation.
 *
 * <p>This class supplies the canonical rotation matrices for the I piece.
 * The internal representation is kept immutable after construction; callers
 * receive defensive deep copies from {@link #getShapeMatrix()} so it is safe
 * to call from multiple threads for read-only purposes.
 *
 * <p>The matrix values are integers where non-zero entries indicate block
 * presence and the integer value can be used by rendering code to select a
 * colour/index. Consumers should treat these matrices as read-only.
 */
public final class IBrick implements Brick {

    private final List<int[][]> brickMatrix = new ArrayList<>();

    public IBrick() {
        brickMatrix.add(new int[][]{
                {0, 0, 0, 0},
                {1, 1, 1, 1},
                {0, 0, 0, 0},
                {0, 0, 0, 0}
        });
        brickMatrix.add(new int[][]{
                {0, 1, 0, 0},
                {0, 1, 0, 0},
                {0, 1, 0, 0},
                {0, 1, 0, 0}
        });
    }

    @Override
    public List<int[][]> getShapeMatrix() {
        return MatrixOperations.deepCopyList(brickMatrix);
    }

}
