package com.comp2042.controller.cooperateBattle.coopController;

import com.comp2042.logic.Brick;
import com.comp2042.logic.RandomBrickGenerator;
import com.comp2042.model.NextShapeInfo;
import com.comp2042.model.ViewData;
import com.comp2042.utils.MatrixOperations;
import com.comp2042.model.Score;
import com.comp2042.utils.BrickRotator;

import java.awt.Point;

/**
 * Per-player runtime state used by the cooperative game controller.
 *
 * <p>This class stores a player's generator, rotation helper, current offset
 * and score. All shape/rotation/translation logic is performed here so the
 * higher-level controller can remain focused on coordinating both players and
 * the shared board matrix.
 */
public class CoopPlayerState {

    public final RandomBrickGenerator generator = new RandomBrickGenerator();
    public final BrickRotator rotator = new BrickRotator();
    public Point offset = null;
    public final Score score = new Score();

    /** Assign the active brick for this player. */
    public void setBrick(Brick b) { rotator.setBrick(b); }
    /** Return the active brick. */
    public Brick getBrick() { return rotator.getBrick(); }
    /** Pull a new brick from the player's generator. */
    public Brick getBrickFromGenerator() { return generator.getBrick(); }
    /** Peek the next brick without removing it from the generator. */
    public Brick getNextBrick() { return generator.getNextBrick(); }
    /** Replace the next brick in the generator (used by swap logic). */
    public boolean replaceNext(Brick b) { return generator.replaceNext(b); }
    /** Return a preview list of upcoming bricks. */
    public java.util.List<Brick> getUpcoming(int count) { return generator.getUpcomingBricks(count); }
    /** Current rotated shape matrix for the active brick. */
    public int[][] getCurrentShape() { return rotator.getCurrentShape(); }
    /** Next rotated shape information used when attempting rotations. */
    public NextShapeInfo getNextShape() { return rotator.getNextShape(); }
    /** Set the rotation position index for the current shape. */
    public void setCurrentShape(int pos) { rotator.setCurrentShape(pos); }

    /**
     * Try to translate this player's offset by (dx,dy). Returns true if move succeeded.
     * Merges the other player's shape into a temporary board to check collisions.
     */
    /**
     * Attempt to translate the player's current offset by (dx,dy).
     *
     * <p>The method builds a temporary board that includes the other player's
     * piece (if present) to perform collision checks. If the translation would
     * collide the method returns {@code false} and leaves the offset unchanged.
     *
     * @return {@code true} if the translation was applied
     */
    public boolean tryTranslate(int dx, int dy, int[][] boardMatrix, CoopPlayerState other, String ctx) {
        if (offset == null) return false;
        Point p = new Point(offset);
        p.translate(dx, dy);
        int[][] tmp = MatrixOperations.copy(boardMatrix);
        if (other != null && other.offset != null) {
            try {
                tmp = MatrixOperations.merge(tmp, other.getCurrentShape(), (int) other.offset.getX(), (int) other.offset.getY());
            } catch (Exception e) {
                // fall back to original tmp
            }
        }
        boolean conflict = MatrixOperations.intersect(tmp, getCurrentShape(), (int) p.getX(), (int) p.getY());
        if (conflict) return false;
        this.offset = p;
        return true;
    }

    /**
     * Try to rotate this player's brick (with kick attempts). Returns true if rotation applied.
     */
    /**
     * Attempt a rotation for the current brick using simple kick offsets.
     *
     * <p>If a non-colliding rotation is found the rotation and any required
     * offset (kick) is applied and {@code true} is returned. Otherwise the
     * method returns {@code false} and the rotation is not applied.
     *
     * @return {@code true} if rotation succeeded
     */
    public boolean tryRotate(int[][] boardMatrix, CoopPlayerState other, String ctx) {
        if (offset == null) return false;
        int[][] tmp = MatrixOperations.copy(boardMatrix);
        if (other != null && other.offset != null) {
            try {
                tmp = MatrixOperations.merge(tmp, other.getCurrentShape(), (int) other.offset.getX(), (int) other.offset.getY());
            } catch (Exception e) {
                // ignore and proceed with tmp
            }
        }
        NextShapeInfo next = getNextShape();
        int baseX = (int) offset.getX();
        int baseY = (int) offset.getY();
        if (!MatrixOperations.intersect(tmp, next.getShape(), baseX, baseY)) {
            setCurrentShape(next.getPosition());
            return true;
        }

        int shapeWidth = next.getShape()[0].length;
        int boardWidth = boardMatrix[0].length;
        int maxKick = Math.max(3, shapeWidth);
        int[] dxCandidates = new int[maxKick * 2];
        for (int k = 1; k <= maxKick; k++) {
            dxCandidates[(k - 1) * 2] = k;
            dxCandidates[(k - 1) * 2 + 1] = -k;
        }
        int[] dyCandidates = new int[]{0, -1, 1, -2};
        for (int dy : dyCandidates) {
            for (int dx : dxCandidates) {
                int tryX = baseX + dx;
                int tryY = baseY + dy;
                if (tryX < -shapeWidth || tryX > boardWidth + shapeWidth) continue;
                if (!MatrixOperations.intersect(tmp, next.getShape(), tryX, tryY)) {
                    Point p = new Point(offset);
                    p.translate(dx, dy);
                    this.offset = p;
                    setCurrentShape(next.getPosition());
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check whether the player's active piece can move down by one row.
     *
     * @return {@code true} if the piece can move down
     */
    public boolean canMoveDown(int[][] boardMatrix) {
        if (offset == null) return false;
        Point p = new Point(offset);
        p.translate(0,1);
        return !MatrixOperations.intersect(boardMatrix, getCurrentShape(), (int) p.getX(), (int) p.getY());
    }

    /**
     * Merge the player's current shape into the provided board matrix and
     * return the resulting matrix. Caller must ensure {@code offset} is set.
     */
    public int[][] mergeIntoBoard(int[][] boardMatrix) {
        return MatrixOperations.merge(boardMatrix, getCurrentShape(), (int) offset.getX(), (int) offset.getY());
    }

    /**
     * Create a {@link ViewData} instance representing the player's current
     * falling piece (used by the renderer).
     */
    public ViewData getViewData() {
        if (offset == null) return null;
        return new ViewData(getCurrentShape(), (int) offset.getX(), (int) offset.getY(), getNextBrick().getShapeMatrix().get(0));
    }

    /**
     * Spawn a new brick for this player from its generator and set the offset to the default spawn position
     * (centered horizontally with a small left/right bias applied by the caller). Returns true if spawn collided
     * immediately (i.e., game over condition for this player).
     *
     * The caller should pass the current boardMatrix and the other player's state so that spawn collision
     * detection can account for the other player's currently falling piece.
     */
    /**
     * Spawn a new piece for this player using the generator and position it at
     * the standard spawn coordinates (adjusted by {@code defaultXOffset}).
     *
     * @return {@code true} if the new piece immediately collides (spawn collision)
     */
    public boolean spawn(int boardWidth, int[][] boardMatrix, CoopPlayerState other, int defaultXOffset) {
        Brick b = getBrickFromGenerator();
        setBrick(b);
        int[][] shape = getCurrentShape();
        int shapeWidth = shape[0].length;
        int startX = Math.max(0, (boardWidth - shapeWidth) / 2) + defaultXOffset;
        if (startX < 0) startX = 0;
        if (startX + shapeWidth > boardWidth) startX = Math.max(0, boardWidth - shapeWidth);
        int startY = 2;
        this.offset = new Point(startX, startY);

        int[][] tmp = MatrixOperations.copy(boardMatrix);
        if (other != null && other.offset != null) {
            try { tmp = MatrixOperations.merge(tmp, other.getCurrentShape(), (int) other.offset.getX(), (int) other.offset.getY()); } catch (Exception ignored) {}
        }
        return MatrixOperations.intersect(tmp, shape, (int) this.offset.getX(), (int) this.offset.getY());
    }
}
