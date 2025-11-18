package com.comp2042.logic.bricks;

import com.comp2042.logic.Brick;
import com.comp2042.utils.MatrixOperations;

import java.util.ArrayList;
import java.util.List;

/**
 * Z-shaped tetromino implementation.
 *
 * <p>Provides the rotation matrices for the Z piece. Callers should not
 * rely on the returned arrays being shared — use {@link #getShapeMatrix()}
 * which returns deep copies suitable for modification.
 */
public final class ZBrick implements Brick {

    private final List<int[][]> brickMatrix = new ArrayList<>();

    public ZBrick() {
        brickMatrix.add(new int[][]{
                {0, 0, 0, 0},
                {7, 7, 0, 0},
                {0, 7, 7, 0},
                {0, 0, 0, 0}
        });
        brickMatrix.add(new int[][]{
                {0, 7, 0, 0},
                {7, 7, 0, 0},
                {7, 0, 0, 0},
                {0, 0, 0, 0}
        });
    }

    @Override
    public List<int[][]> getShapeMatrix() {
        return MatrixOperations.deepCopyList(brickMatrix);
    }
}
