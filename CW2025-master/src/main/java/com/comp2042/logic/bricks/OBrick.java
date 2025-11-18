package com.comp2042.logic.bricks;

import com.comp2042.logic.Brick;
import com.comp2042.utils.MatrixOperations;

import java.util.ArrayList;
import java.util.List;

/**
 * O-shaped (square) tetromino.
 *
 * <p>The O-piece is rotation-symmetric; therefore only a single 4x4 matrix
 * is provided. The returned matrices from {@link #getShapeMatrix()} are deep
 * copies and safe for callers to modify.
 */
public final class OBrick implements Brick {

    private final List<int[][]> brickMatrix = new ArrayList<>();

    public OBrick() {
        brickMatrix.add(new int[][]{
                {0, 0, 0, 0},
                {0, 4, 4, 0},
                {0, 4, 4, 0},
                {0, 0, 0, 0}
        });
    }

    @Override
    public List<int[][]> getShapeMatrix() {
        return MatrixOperations.deepCopyList(brickMatrix);
    }

}
