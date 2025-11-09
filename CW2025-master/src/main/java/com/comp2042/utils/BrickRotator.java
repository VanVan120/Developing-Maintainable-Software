package com.comp2042.utils;

import com.comp2042.model.NextShapeInfo;
import com.comp2042.logic.bricks.Brick;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Small utility that tracks the current rotation index for a {@link Brick} and
 * provides the current and next rotation matrices.
 *
 * <p>This class is lightweight and intentionally does not mutate the underlying
 * {@link Brick} implementation. It defensively validates indices and returns
 * copies of matrices where helpful so callers cannot accidentally modify the
 * brick's shape arrays.
 */
public class BrickRotator {

    private static final Logger LOGGER = Logger.getLogger(BrickRotator.class.getName());

    private Brick brick;
    private int currentShape = 0;

    /**
     * Returns the next shape (rotation) info without advancing the current index.
     *
     * @throws IllegalStateException if no brick is set or the brick has no shapes
     */
    public NextShapeInfo getNextShape() {
        ensureBrickPresent();
        int shapes = brick.getShapeMatrix().size();
        if (shapes == 0) {
            LOGGER.log(Level.SEVERE, "Requested next shape but brick has no shapes");
            throw new IllegalStateException("Brick has no rotation shapes");
        }
        int nextShape = (currentShape + 1) % shapes;
        // NextShapeInfo makes its own defensive copy, but we still fetch the source
        return new NextShapeInfo(brick.getShapeMatrix().get(nextShape), nextShape);
    }

    /**
     * Returns a defensive copy of the current shape matrix.
     *
     * @throws IllegalStateException if no brick is set or the current index is invalid
     */
    public int[][] getCurrentShape() {
        ensureBrickPresent();
        int shapes = brick.getShapeMatrix().size();
        if (shapes == 0) {
            LOGGER.log(Level.SEVERE, "Requested current shape but brick has no shapes");
            throw new IllegalStateException("Brick has no rotation shapes");
        }
        int idx = ((currentShape % shapes) + shapes) % shapes; // normalize
        return MatrixOperations.copy(brick.getShapeMatrix().get(idx));
    }

    /**
     * Set the current rotation index. Index will be normalized into the valid range.
     *
     * @param currentShape desired rotation index
     * @throws IllegalStateException if no brick is set
     */
    public void setCurrentShape(int currentShape) {
        ensureBrickPresent();
        int shapes = brick.getShapeMatrix().size();
        if (shapes == 0) {
            LOGGER.log(Level.WARNING, "Attempt to set shape index but brick has no shapes");
            this.currentShape = 0;
            return;
        }
        // normalize to [0, shapes-1]
        this.currentShape = ((currentShape % shapes) + shapes) % shapes;
    }

    /**
     * Attach a new brick to this rotator and reset the rotation index to 0.
     */
    public void setBrick(Brick brick) {
        this.brick = Objects.requireNonNull(brick, "brick");
        this.currentShape = 0;
    }

    /** Returns the brick currently attached, may be null. */
    public Brick getBrick() {
        return this.brick;
    }

    private void ensureBrickPresent() {
        if (this.brick == null) {
            LOGGER.log(Level.SEVERE, "No brick set on BrickRotator");
            throw new IllegalStateException("No brick set for rotation operations");
        }
    }
}