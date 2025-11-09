package com.comp2042.model;

import com.comp2042.utils.MatrixOperations;
import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable snapshot of the current view state needed by the renderer.
 *
 * <p>Contracts:
 * - Arrays passed into the constructor are copied defensively.
 * - Getters return copies of internal matrices to preserve immutability.
 */
public final class ViewData {

    private final int[][] brickData;
    private final int xPosition;
    private final int yPosition;
    private final int[][] nextBrickData;

    public ViewData(int[][] brickData, int xPosition, int yPosition, int[][] nextBrickData) {
        // defensive copies to avoid retaining references to mutable caller-owned arrays
        this.brickData = MatrixOperations.copy(brickData);
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.nextBrickData = MatrixOperations.copy(nextBrickData);
    }

    /** Returns a defensive copy of the current brick matrix. */
    public int[][] getBrickData() {
        return MatrixOperations.copy(brickData);
    }

    /** Legacy-style getter retained for compatibility. */
    public int getxPosition() {
        return xPosition;
    }

    /** Legacy-style getter retained for compatibility. */
    public int getyPosition() {
        return yPosition;
    }

    /** Alternative getter using conventional Java naming. */
    public int getXPosition() { return getxPosition(); }

    /** Alternative getter using conventional Java naming. */
    public int getYPosition() { return getyPosition(); }

    /** Returns a defensive copy of the next-brick preview matrix. */
    public int[][] getNextBrickData() {
        return MatrixOperations.copy(nextBrickData);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ViewData)) return false;
        ViewData that = (ViewData) o;
        return xPosition == that.xPosition
                && yPosition == that.yPosition
                && Arrays.deepEquals(brickData, that.brickData)
                && Arrays.deepEquals(nextBrickData, that.nextBrickData);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(xPosition, yPosition);
        result = 31 * result + Arrays.deepHashCode(brickData);
        result = 31 * result + Arrays.deepHashCode(nextBrickData);
        return result;
    }

    @Override
    public String toString() {
        return "ViewData{" +
                "x=" + xPosition +
                ", y=" + yPosition +
                ", brickDataHash=" + Arrays.deepHashCode(brickData) +
                ", nextBrickDataHash=" + Arrays.deepHashCode(nextBrickData) +
                '}';
    }
}
