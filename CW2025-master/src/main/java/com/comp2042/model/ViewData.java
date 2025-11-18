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

    /**
     * Create a new immutable snapshot describing the view state.
     *
     * @param brickData   matrix for the active piece (rows x cols). A defensive
     *                    copy is taken; callers may continue to mutate their
     *                    reference without affecting this instance.
     * @param xPosition   horizontal position (x offset) of the active piece.
     * @param yPosition   vertical position (y offset) of the active piece.
     * @param nextBrickData preview matrix for the next piece; also copied
     *                      defensively.
     */
    public ViewData(int[][] brickData, int xPosition, int yPosition, int[][] nextBrickData) {
        // defensive copies to avoid retaining references to mutable caller-owned arrays
        this.brickData = MatrixOperations.copy(brickData);
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.nextBrickData = MatrixOperations.copy(nextBrickData);
    }

    /**
     * @return a defensive copy of the current brick matrix (active piece).
     */
    public int[][] getBrickData() {
        return MatrixOperations.copy(brickData);
    }

    /**
     * Legacy-style getter retained for compatibility.
     *
     * @return the legacy x position value.
     */
    public int getxPosition() {
        return xPosition;
    }

    /**
     * Legacy-style getter retained for compatibility.
     *
     * @return the legacy y position value.
     */
    public int getyPosition() {
        return yPosition;
    }

    /**
     * @return the horizontal position (x offset) of the active piece.
     */
    public int getXPosition() { return getxPosition(); }

    /**
     * @return the vertical position (y offset) of the active piece.
     */
    public int getYPosition() { return getyPosition(); }

    /**
     * @return a defensive copy of the next-piece preview matrix.
     */
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
