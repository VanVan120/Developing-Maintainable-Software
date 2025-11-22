package com.comp2042.model;

import com.comp2042.utils.MatrixOperations;

/**
 * Small immutable holder describing a preview shape and its horizontal
 * position.
 *
 * The internal matrix is defensively copied on construction and when
 * returned to callers to avoid external mutation. Instances are immutable
 * and safe to share between game logic and rendering code.
 */
public final class NextShapeInfo {

    private final int[][] shape;
    private final int position;

    /**
     * Create a new preview holder.
     *
     * @param shape a 2D shape matrix (rows x cols). The matrix is copied on
     *              construction so callers may retain or mutate their own
     *              reference without affecting this instance.
     * @param position horizontal offset (x position) where the shape should
     *                 be rendered relative to the board.
     */
    public NextShapeInfo(final int[][] shape, final int position) {
        // store a defensive copy so callers cannot mutate our internal matrix
        this.shape = MatrixOperations.copy(shape);
        this.position = position;
    }

    /**
     * Return a defensive copy of the shape matrix.
     *
     * @return a copy of the internal shape matrix; callers MUST NOT assume
     *         they can mutate the returned array and affect the board.
     */
    public int[][] getShape() {
        return MatrixOperations.copy(shape);
    }

    /**
     * @return the horizontal position (x offset) for the preview shape.
     */
    public int getPosition() {
        return position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NextShapeInfo that = (NextShapeInfo) o;
        return position == that.position && java.util.Arrays.deepEquals(this.shape, that.shape);
    }

    @Override
    public int hashCode() {
        int result = java.util.Arrays.deepHashCode(shape);
        result = 31 * result + position;
        return result;
    }

    @Override
    public String toString() {
        return "NextShapeInfo{" +
                "position=" + position +
                ", shapeHash=" + java.util.Arrays.deepHashCode(shape) +
                '}';
    }
}
