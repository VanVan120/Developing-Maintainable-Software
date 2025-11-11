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
 * Encapsulates core game rules and board operations. This class is UI-agnostic so it can be
 * unit-tested without JavaFX.
 */
public class GameEngine {
    private static final Logger LOGGER = Logger.getLogger(GameEngine.class.getName());
    private final Board board;

    public GameEngine(Board board) {
        this.board = Objects.requireNonNull(board, "Board must not be null");
    }

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

    public ViewData moveLeft() {
        board.moveBrickLeft();
        return board.getViewData();
    }

    public ViewData moveRight() {
        board.moveBrickRight();
        return board.getViewData();
    }

    public ViewData rotate() {
        board.rotateLeftBrick();
        return board.getViewData();
    }

    public boolean swap() {
        try {
            return board.swapCurrentWithNext();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during swap", e);
            return false;
        }
    }

    public void newGame() {
        board.newGame();
    }

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

    public List<com.comp2042.logic.Brick> getUpcoming(int count) {
        try {
            return board.getUpcomingBricks(count);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get upcoming bricks", e);
            return Collections.emptyList();
        }
    }

    public javafx.beans.property.IntegerProperty getScoreProperty() {
        return board.getScore().scoreProperty();
    }

    public int[][] getBoardMatrix() {
        return board.getBoardMatrix();
    }

    public ViewData getViewData() {
        return board.getViewData();
    }

    public boolean createNewBrick() {
        try {
            return board.createNewBrick();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to create new brick", e);
            return false;
        }
    }
}
