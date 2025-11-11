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
 * Holds per-player runtime state for cooperative mode.
 */
public class CoopPlayerState {

    public final RandomBrickGenerator generator = new RandomBrickGenerator();
    public final BrickRotator rotator = new BrickRotator();
    public Point offset = null;
    public final Score score = new Score();

    public void setBrick(Brick b) { rotator.setBrick(b); }
    public Brick getBrick() { return rotator.getBrick(); }
    public Brick getBrickFromGenerator() { return generator.getBrick(); }
    public Brick getNextBrick() { return generator.getNextBrick(); }
    public boolean replaceNext(Brick b) { return generator.replaceNext(b); }
    public java.util.List<Brick> getUpcoming(int count) { return generator.getUpcomingBricks(count); }
    public int[][] getCurrentShape() { return rotator.getCurrentShape(); }
    public NextShapeInfo getNextShape() { return rotator.getNextShape(); }
    public void setCurrentShape(int pos) { rotator.setCurrentShape(pos); }

    /**
     * Try to translate this player's offset by (dx,dy). Returns true if move succeeded.
     * Merges the other player's shape into a temporary board to check collisions.
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

    public boolean canMoveDown(int[][] boardMatrix) {
        if (offset == null) return false;
        Point p = new Point(offset);
        p.translate(0,1);
        return !MatrixOperations.intersect(boardMatrix, getCurrentShape(), (int) p.getX(), (int) p.getY());
    }

    public int[][] mergeIntoBoard(int[][] boardMatrix) {
        return MatrixOperations.merge(boardMatrix, getCurrentShape(), (int) offset.getX(), (int) offset.getY());
    }

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
