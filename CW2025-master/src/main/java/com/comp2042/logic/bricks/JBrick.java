package com.comp2042.logic.bricks;

import com.comp2042.logic.Brick;
import com.comp2042.utils.MatrixOperations;

import java.util.ArrayList;
import java.util.List;

/**
 * J-shaped tetromino implementation (left-oriented variant).
 *
 * <p>Provides the 4 rotation states as 4x4 integer matrices. Non-zero values
 * indicate occupied cells; the concrete integer may be used as an index or
 * colour identifier by rendering code. Callers receive defensive copies via
 * {@link #getShapeMatrix()}.
 */
public final class JBrick implements Brick {

    private final List<int[][]> brickMatrix = new ArrayList<>();

    public JBrick() {
        brickMatrix.add(new int[][]{
                {0, 0, 0, 0},
                {2, 2, 2, 0},
                {0, 0, 2, 0},
                {0, 0, 0, 0}
        });
        brickMatrix.add(new int[][]{
                {0, 0, 0, 0},
                {0, 2, 2, 0},
                {0, 2, 0, 0},
                {0, 2, 0, 0}
        });
        brickMatrix.add(new int[][]{
                {0, 0, 0, 0},
                {0, 2, 0, 0},
                {0, 2, 2, 2},
                {0, 0, 0, 0}
        });
        brickMatrix.add(new int[][]{
                {0, 0, 2, 0},
                {0, 0, 2, 0},
                {0, 2, 2, 0},
                {0, 0, 0, 0}
        });
    }

    @Override
    public List<int[][]> getShapeMatrix() {
        return MatrixOperations.deepCopyList(brickMatrix);
    }
}
