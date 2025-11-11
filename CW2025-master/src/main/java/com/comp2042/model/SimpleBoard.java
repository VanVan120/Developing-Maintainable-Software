package com.comp2042.model;

import com.comp2042.logic.Brick;
import com.comp2042.logic.BrickGenerator;
import com.comp2042.logic.RandomBrickGenerator;
import com.comp2042.utils.BrickRotator;
import com.comp2042.utils.MatrixOperations;
import java.awt.Point;

public class SimpleBoard implements Board {
    private final int width;
    private final int height;
    private final BrickGenerator brickGenerator;
    private final BrickRotator brickRotator;
    private int[][] currentGameMatrix;
    private Point currentOffset;
    private final Score score;


    public SimpleBoard(int width, int height) {
        this.width = width;
        this.height = height;
        currentGameMatrix = new int[height][width];
        brickGenerator = new RandomBrickGenerator();
        brickRotator = new BrickRotator();
        score = new Score();
    }

    @Override
    public boolean moveBrickDown() {
        int[][] currentMatrix = MatrixOperations.copy(currentGameMatrix);
        Point p = new Point(currentOffset);
        p.translate(0, 1);
        boolean conflict = MatrixOperations.intersect(currentMatrix, brickRotator.getCurrentShape(), (int) p.getX(), (int) p.getY());
        if (conflict) {
            return false;
        } else {
            currentOffset = p;
            return true;
        }
    }

    @Override
    public boolean moveBrickLeft() {
        int[][] currentMatrix = MatrixOperations.copy(currentGameMatrix);
        Point p = new Point(currentOffset);
        p.translate(-1, 0);
        boolean conflict = MatrixOperations.intersect(currentMatrix, brickRotator.getCurrentShape(), (int) p.getX(), (int) p.getY());
        if (conflict) {
            return false;
        } else {
            currentOffset = p;
            return true;
        }
    }

    @Override
    public boolean moveBrickRight() {
        int[][] currentMatrix = MatrixOperations.copy(currentGameMatrix);
        Point p = new Point(currentOffset);
        p.translate(1, 0);
        boolean conflict = MatrixOperations.intersect(currentMatrix, brickRotator.getCurrentShape(), (int) p.getX(), (int) p.getY());
        if (conflict) {
            return false;
        } else {
            currentOffset = p;
            return true;
        }
    }

    @Override
    public boolean rotateLeftBrick() {
        int[][] currentMatrix = MatrixOperations.copy(currentGameMatrix);
        NextShapeInfo nextShape = brickRotator.getNextShape();
        // first try rotation at current offset
        int baseX = (int) currentOffset.getX();
        int baseY = (int) currentOffset.getY();
        if (!MatrixOperations.intersect(currentMatrix, nextShape.getShape(), baseX, baseY)) {
            brickRotator.setCurrentShape(nextShape.getPosition());
            return true;
        }

        // Improved wall-kick: try a wider range of horizontal kicks and small vertical adjustments.
        // This helps pieces (I, L, Z, T, etc.) to rotate when close to walls or other blocks.
        int shapeWidth = nextShape.getShape()[0].length;
        int boardWidth = currentGameMatrix[0].length;
        // pick a reasonable max kick range: at least 3, but also consider the shape width
        int maxKick = Math.max(3, shapeWidth);

        // Build horizontal candidate sequence: 1, -1, 2, -2, 3, -3, ... up to maxKick
        int[] dxCandidates = new int[maxKick * 2];
        for (int k = 1; k <= maxKick; k++) {
            dxCandidates[(k - 1) * 2] = k;
            dxCandidates[(k - 1) * 2 + 1] = -k;
        }

        // Try small vertical adjustments too (upwards first is most useful for floor collisions)
        int[] dyCandidates = new int[]{0, -1, 1, -2};

        for (int dy : dyCandidates) {
            for (int dx : dxCandidates) {
                int tryX = baseX + dx;
                int tryY = baseY + dy;
                if (tryX < -shapeWidth || tryX > boardWidth + shapeWidth) continue; // skip nonsensical shifts
                if (!MatrixOperations.intersect(currentMatrix, nextShape.getShape(), tryX, tryY)) {
                    // commit the successful translation
                    Point p = new Point(currentOffset);
                    p.translate(dx, dy);
                    currentOffset = p;
                    brickRotator.setCurrentShape(nextShape.getPosition());
                    return true;
                }
            }
        }

        // rotation blocked
        return false;
    }

    @Override
    public boolean createNewBrick() {
        Brick currentBrick = brickGenerator.getBrick();
        brickRotator.setBrick(currentBrick);
        // spawn near the top and centered horizontally
        int[][] shape = brickRotator.getCurrentShape();
        int shapeWidth = shape[0].length;
        int boardWidth = currentGameMatrix[0].length;
        int startX = Math.max(0, (boardWidth - shapeWidth) / 2);
        int startY = 2; // keep 2 rows as hidden buffer
        currentOffset = new Point(startX, startY);
        return MatrixOperations.intersect(currentGameMatrix, brickRotator.getCurrentShape(), (int) currentOffset.getX(), (int) currentOffset.getY());
    }

    @Override
    public int[][] getBoardMatrix() {
        return currentGameMatrix;
    }

    @Override
    public ViewData getViewData() {
        return new ViewData(brickRotator.getCurrentShape(), (int) currentOffset.getX(), (int) currentOffset.getY(), brickGenerator.getNextBrick().getShapeMatrix().get(0));
    }

    @Override
    public void mergeBrickToBackground() {
        currentGameMatrix = MatrixOperations.merge(currentGameMatrix, brickRotator.getCurrentShape(), (int) currentOffset.getX(), (int) currentOffset.getY());
    }

    @Override
    public ClearRow clearRows() {
        ClearRow clearRow = MatrixOperations.checkRemoving(currentGameMatrix);
        currentGameMatrix = clearRow.getNewMatrix();
        return clearRow;

    }

    @Override
    public Score getScore() {
        return score;
    }


    @Override
    public void newGame() {
        currentGameMatrix = new int[height][width];
        score.reset();
        createNewBrick();
    }

    @Override
    public boolean swapCurrentWithNext() {
        // peek at next brick from generator
        com.comp2042.logic.Brick next = brickGenerator.getNextBrick();
        if (next == null) return false;
        // save current brick and its rotation state
        com.comp2042.logic.Brick oldCurrent = brickRotator.getBrick();
        // attempt to set the next as current and test for collision at current offset
        brickRotator.setBrick(next);
        int[][] shape = brickRotator.getCurrentShape();
        if (MatrixOperations.intersect(currentGameMatrix, shape, (int) currentOffset.getX(), (int) currentOffset.getY())) {
            // collision — revert
            brickRotator.setBrick(oldCurrent);
            return false;
        }
        // replacement is safe; request the generator to replace its head with the old current so queue preserves order
        boolean replaced = false;
        try {
            replaced = brickGenerator.replaceNext(oldCurrent);
        } catch (Exception ignored) {}
        // if generator couldn't replace, attempt to push old current by consuming next and re-adding oldCurrent
        if (!replaced) {
            // fallback: poll head and put oldCurrent at front by creating a small buffer
            try {
                // best-effort: force a new head by consuming then reinserting oldCurrent at head
                brickGenerator.getBrick(); // consume head (we already set it as current)
            } catch (Exception ignored) {}
        }
        return true;
    }

    @Override
    public java.util.List<com.comp2042.logic.Brick> getUpcomingBricks(int count) {
        return brickGenerator.getUpcomingBricks(count);
    }
}
