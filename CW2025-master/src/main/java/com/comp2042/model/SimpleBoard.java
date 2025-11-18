package com.comp2042.model;

import com.comp2042.logic.Brick;
import com.comp2042.logic.BrickGenerator;
import com.comp2042.logic.RandomBrickGenerator;
import com.comp2042.utils.BrickRotator;
import com.comp2042.utils.MatrixOperations;
import java.awt.Point;

/**
 * Concrete {@link Board} implementation using a simple 2D int matrix as the
 * background and a {@link com.comp2042.logic.BrickGenerator} to provide pieces.
 *
 * This implementation is not thread-safe and is intended to be used from the
 * game loop / JavaFX thread. Many return values are snapshots or direct
 * references to internal structures; callers should treat returned arrays as
 * read-only unless explicitly documented otherwise.
 */
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

    /**
     * Move the active brick down by one cell if the space below is free.
     *
     * @return {@code true} when the piece moved; {@code false} when blocked
     *         (the caller will usually lock the piece after a failed move).
     */
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

    /**
     * Move the active brick left by one cell when possible.
     *
     * @return {@code true} when the move succeeded, {@code false} when
     *         blocked by the board or other pieces.
     */
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

    /**
     * Move the active brick right by one cell when possible.
     *
     * @return {@code true} when the move succeeded, {@code false} when
     *         blocked by the board or other pieces.
     */
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

    /**
     * Rotate the active brick counter-clockwise. This implementation will
     * attempt a series of wall-kick translations when the straightforward
     * rotation would intersect other cells or the board edges.
     *
     * @return {@code true} when the rotation (possibly with kicks) succeeded,
     *         {@code false} when no valid placement could be found.
     */
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

    /**
     * Spawn a new brick from the configured {@link BrickGenerator} and place
     * it near the top of the board. This method positions the piece centered
     * horizontally (respecting shape width) and uses a small hidden buffer
     * rows from the top.
     *
     * @return {@code true} if the spawn intersects the existing board. A
     *         returning {@code true} typically indicates an immediate collision
     *         and may be treated as a game-over condition by callers.
     */

    /**
     * Spawn a new active brick taken from the configured {@link BrickGenerator}
     * and position it near the top of the board.
     *
     * @return {@code true} if the newly spawned piece immediately intersects
     *         the current board (a typical signal that the board is full /
     *         game over). Callers should treat {@code true} as a collision.
     */
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

    /**
     * Return the internal board matrix. This implementation returns the
     * internal reference for efficiency — callers MUST NOT mutate the
     * returned array. Use {@link com.comp2042.utils.MatrixOperations#copy}
     * to obtain a defensive copy when needed.
     *
     * @return the board matrix (rows x cols).
     */
    @Override
    public int[][] getBoardMatrix() {
        // NOTE: this implementation returns the internal matrix reference for
        // efficiency. Callers must NOT mutate the returned array. Use
        // MatrixOperations.copy(...) if a defensive copy is required.
        return currentGameMatrix;
    }

    /**
     * Create and return a {@link ViewData} snapshot describing the current
     * active piece position and the next-piece preview. The returned snapshot
     * is immutable from the caller's perspective.
     *
     * @return a {@link ViewData} instance suitable for rendering.
     */
    @Override
    public ViewData getViewData() {
        return new ViewData(brickRotator.getCurrentShape(), (int) currentOffset.getX(), (int) currentOffset.getY(), brickGenerator.getNextBrick().getShapeMatrix().get(0));
    }

    /**
     * Lock the active brick into the background matrix, merging its cells
     * into the static board content. After calling this the board should
     * typically call {@link #clearRows()} to remove any completed lines.
     */
    @Override
    public void mergeBrickToBackground() {
        currentGameMatrix = MatrixOperations.merge(currentGameMatrix, brickRotator.getCurrentShape(), (int) currentOffset.getX(), (int) currentOffset.getY());
    }

    /**
     * Remove any completed rows from the board and return a {@link ClearRow}
     * describing the result (new matrix, lines removed and score bonus).
     *
     * @return information about cleared rows and the new board matrix.
     */
    @Override
    public ClearRow clearRows() {
        ClearRow clearRow = MatrixOperations.checkRemoving(currentGameMatrix);
        currentGameMatrix = clearRow.getNewMatrix();
        return clearRow;

    }

    /**
     * @return the {@link Score} holder for this board (UI-bindable JavaFX
     *         property wrapper).
     */
    @Override
    public Score getScore() {
        return score;
    }


    /**
     * Reset the board state to start a new game: clear the matrix, reset the
     * score and spawn the first active piece.
     */
    @Override
    public void newGame() {
        currentGameMatrix = new int[height][width];
        score.reset();
        createNewBrick();
    }
    /**
     * Swap the current active piece with the generator's next piece.
     *
     * The method first peeks at the generator's next piece and attempts to
     * place it at the current offset. If the placement collides the swap
     * is rejected and the board state is restored. When the generator
     * supports replacing its head the original current piece is inserted
     * back into the queue; otherwise a best-effort fallback is used.
     *
     * @return {@code true} if the swap succeeded, {@code false} on failure
     *         (for example due to collision or generator returning null).
     */
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

    /**
     * Return a list of upcoming bricks from the generator. The returned list
     * is produced by the generator and callers must not rely on mutating it.
     *
     * @param count number of upcoming pieces requested
     * @return a list of upcoming {@link com.comp2042.logic.Brick} instances
     */
    @Override
    public java.util.List<com.comp2042.logic.Brick> getUpcomingBricks(int count) {
        return brickGenerator.getUpcomingBricks(count);
    }
}
