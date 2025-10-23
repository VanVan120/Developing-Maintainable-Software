package com.comp2042;

import com.comp2042.logic.bricks.Brick;
import com.comp2042.logic.bricks.BrickGenerator;
import com.comp2042.logic.bricks.RandomBrickGenerator;

import java.awt.*;

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
        // matrix is rows (height) by columns (width)
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
        boolean conflict = MatrixOperations.intersect(currentMatrix, nextShape.getShape(), (int) currentOffset.getX(), (int) currentOffset.getY());
        if (conflict) {
            return false;
        } else {
            brickRotator.setCurrentShape(nextShape.getPosition());
            return true;
        }
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
        com.comp2042.logic.bricks.Brick next = brickGenerator.getNextBrick();
        if (next == null) return false;
        // save current brick and its rotation state
        com.comp2042.logic.bricks.Brick oldCurrent = brickRotator.getBrick();
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
    public java.util.List<com.comp2042.logic.bricks.Brick> getUpcomingBricks(int count) {
        return brickGenerator.getUpcomingBricks(count);
    }
}
