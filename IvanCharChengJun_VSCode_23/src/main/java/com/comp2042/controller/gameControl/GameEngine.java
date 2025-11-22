package com.comp2042.controller.gameControl;

import com.comp2042.input.EventSource;
import com.comp2042.model.Board;
import com.comp2042.model.ClearRow;
import com.comp2042.model.ViewData;
import com.comp2042.utils.MatrixOperations;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates core game rules and board operations.
 *
 * <p>This class is UI-agnostic and can be unit-tested without JavaFX. It
 * operates on the provided {@link com.comp2042.model.Board} instance and
 * exposes pure-ish methods used by the UI adapter {@link GameController}.
 */
public class GameEngine {
    private static final Logger LOGGER = Logger.getLogger(GameEngine.class.getName());
    private final Board board;

    public GameEngine(Board board) {
        this.board = Objects.requireNonNull(board, "Board must not be null");
    }

    /**
     * Attempt to move the current falling brick down by one row.
     *
     * <p>If the brick cannot move down it is merged into the background, row
     * clears are computed and the next brick is created. The returned
     * {@link MoveDownResult} describes any cleared rows, the resulting view
     * data and whether a spawn collision (game over) occurred.
     *
     * @param source event source (USER or SYSTEM) used to decide whether to
     *               award a soft-drop point
     * @return result object describing what happened during the move
     */
    public MoveDownResult moveDown(EventSource source) {
        boolean canMove = board.moveBrickDown();
        ClearRow clearRow = null;
        int forwardCount = 0;
        boolean gameOver = false;

        try {
            if (!canMove) {
                board.mergeBrickToBackground();
                int[][] matrixBeforeClear = MatrixOperations.copy(board.getBoardMatrix());
                clearRow = board.clearRows();

                if (clearRow != null && clearRow.getLinesRemoved() > 0) {
                    board.getScore().add(clearRow.getScoreBonus());
                    int[] cleared = clearRow.getClearedRows();
                    if (cleared != null && cleared.length > 0) {
                        for (int r : cleared) {
                            if (r >= 0 && r < matrixBeforeClear.length) {
                                boolean hasGarbage = false;
                                for (int c = 0; c < matrixBeforeClear[r].length; c++) {
                                    if (matrixBeforeClear[r][c] == 8) { // preserve existing semantics
                                        hasGarbage = true;
                                        break;
                                    }
                                }
                                if (!hasGarbage) forwardCount++;
                            }
                        }
                    }
                }

                if (board.createNewBrick()) {
                    gameOver = true;
                }
            } else {
                if (source == EventSource.USER) {
                    board.getScore().add(1);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error while processing moveDown", e);
        }

        return new MoveDownResult(clearRow, board.getViewData(), board.getBoardMatrix(), forwardCount, gameOver);
    }

    /**
     * Move the current brick one cell to the left and return the updated view data.
     */
    public ViewData moveLeft() {
        board.moveBrickLeft();
        return board.getViewData();
    }

    /**
     * Move the current brick one cell to the right and return the updated view data.
     */
    public ViewData moveRight() {
        board.moveBrickRight();
        return board.getViewData();
    }

    /**
     * Rotate the current brick (left rotation) and return updated view data.
     */
    public ViewData rotate() {
        board.rotateLeftBrick();
        return board.getViewData();
    }

    /**
     * Swap the current brick with the next brick (if supported by the board).
     *
     * @return {@code true} if the swap succeeded
     */
    public boolean swap() {
        try {
            return board.swapCurrentWithNext();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during swap", e);
            return false;
        }
    }

    /**
     * Reset the board and start a new game.
     */
    public void newGame() {
        board.newGame();
    }

    /**
     * Add garbage rows to the bottom of the board. Existing rows are shifted
     * upwards and the bottom rows are filled with garbage (value 8) except for
     * the specified hole column.
     *
     * @param count number of garbage rows to add
     * @param holeColumn column index to leave empty in garbage rows
     * @return the board matrix after modification
     */
    public int[][] addGarbageRows(int count, int holeColumn) {
        if (count <= 0) return board.getBoardMatrix();
        int[][] matrix = board.getBoardMatrix();
        if (matrix == null || matrix.length == 0) return matrix;

        int h = matrix.length;
        int w = matrix[0].length;
        if (holeColumn < 0 || holeColumn >= w) holeColumn = w - 1;

        int[][] tmp = new int[h][w];
        for (int r = 0; r < h - count; r++) {
            System.arraycopy(matrix[r + count], 0, tmp[r], 0, w);
        }
        for (int r = h - count; r < h; r++) {
            for (int c = 0; c < w; c++) {
                tmp[r][c] = (c == holeColumn) ? 0 : 8;
            }
        }

        // copy back
        for (int r = 0; r < h; r++) {
            System.arraycopy(tmp[r], 0, matrix[r], 0, w);
        }

        return matrix;
    }

    /**
     * Return a list of upcoming bricks from the board's generator for preview.
     *
     * @param count how many bricks to request
     * @return list of upcoming bricks or an empty list on error
     */
    public List<com.comp2042.logic.Brick> getUpcoming(int count) {
        try {
            return board.getUpcomingBricks(count);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get upcoming bricks", e);
            return Collections.emptyList();
        }
    }

    /**
     * Expose the board's score property for binding by the UI.
     */
    public javafx.beans.property.IntegerProperty getScoreProperty() {
        return board.getScore().scoreProperty();
    }

    /**
     * Return a defensive copy of the board matrix used for rendering.
     */
    public int[][] getBoardMatrix() {
        return board.getBoardMatrix();
    }

    /**
     * Return current view data describing the falling piece and its position.
     */
    public ViewData getViewData() {
        return board.getViewData();
    }

    /**
     * Create a new brick via the board and return whether the creation succeeded
     * (note: some boards return {@code true} to signal that a spawn collision
     * occurred and the game should end).
     */
    public boolean createNewBrick() {
        try {
            return board.createNewBrick();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to create new brick", e);
            return false;
        }
    }
}
