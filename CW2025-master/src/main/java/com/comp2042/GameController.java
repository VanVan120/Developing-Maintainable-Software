package com.comp2042;

import javafx.beans.property.IntegerProperty;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameController implements InputEventListener {
    private Board board = new SimpleBoard(10, 25);
    private final GuiController viewGuiController;
    private static final Logger LOGGER = Logger.getLogger(GameController.class.getName());
    private java.util.function.Consumer<Integer> clearRowHandler = null;

    public GameController(GuiController c) {
        viewGuiController = c;
        board.createNewBrick();
        viewGuiController.setEventListener(this);
        viewGuiController.initGameView(board.getBoardMatrix(), board.getViewData());
        viewGuiController.bindScore(board.getScore().scoreProperty());
        try {
            viewGuiController.setSwapKey(javafx.scene.input.KeyCode.C);
        } catch (Exception ignored) {}
        try {
            java.util.List<com.comp2042.logic.bricks.Brick> upcoming = board.getUpcomingBricks(3);
            viewGuiController.showNextBricks(upcoming);
        } catch (Exception ignored) {}
    }

    public void setClearRowHandler(java.util.function.Consumer<Integer> handler) {
        this.clearRowHandler = handler;
    }

    public void addGarbageRows(int count, int holeColumn) {
        try {
            if (count <= 0) return;
            int[][] matrix = board.getBoardMatrix();
            if (matrix == null || matrix.length == 0) return;
            int h = matrix.length;
            int w = matrix[0].length;
            if (holeColumn < 0) holeColumn = w - 1;
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

            for (int r = 0; r < h; r++) {
                System.arraycopy(tmp[r], 0, matrix[r], 0, w);
            }

            try {
                viewGuiController.refreshGameBackground(matrix);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to refresh game background after adding garbage rows", e);
            }
        } catch (Exception ignored) {}
    }

    public IntegerProperty getScoreProperty() {
        return board.getScore().scoreProperty();
    }

    public java.util.List<com.comp2042.logic.bricks.Brick> getUpcomingBricks(int count) {
        try {
            return board.getUpcomingBricks(count);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get upcoming bricks", e);
            return java.util.Collections.emptyList();
        }
    }

    @Override
    public DownData onDownEvent(MoveEvent event) {
        LOGGER.fine(() -> "onDownEvent source=" + event.getEventSource());
        boolean canMove = board.moveBrickDown();
        try {
            LOGGER.fine(() -> "moveDown result=" + canMove + " offset=" + board.getViewData().getxPosition() + "," + board.getViewData().getyPosition());
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Unable to log moveDown offset", e);
        }
        ClearRow clearRow = null;
        if (!canMove) {
            board.mergeBrickToBackground();
            int[][] matrixBeforeClear = MatrixOperations.copy(board.getBoardMatrix());
            clearRow = board.clearRows();
            if (clearRow != null && clearRow.getLinesRemoved() > 0) {
                board.getScore().add(clearRow.getScoreBonus());
                try {
                    if (clearRowHandler != null) {
                        int[] cleared = clearRow.getClearedRows();
                        int forwardCount = 0;
                        if (cleared != null && cleared.length > 0) {
                            for (int r : cleared) {
                                if (r >= 0 && r < matrixBeforeClear.length) {
                                    boolean hasGarbage = false;
                                    for (int c = 0; c < matrixBeforeClear[r].length; c++) {
                                        int v = matrixBeforeClear[r][c];
                                        if (v == 8) { 
                                            hasGarbage = true;
                                            break;
                                        }
                                    }
                                    if (!hasGarbage) forwardCount++;
                                }
                            }
                        }
                        clearRowHandler.accept(Integer.valueOf(forwardCount));
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Clear row handler threw", e);
                }
            }
            if (board.createNewBrick()) {
                viewGuiController.gameOver();
            }

            viewGuiController.refreshGameBackground(board.getBoardMatrix());
            try {
                java.util.List<com.comp2042.logic.bricks.Brick> upcoming = board.getUpcomingBricks(3);
                viewGuiController.showNextBricks(upcoming);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to update upcoming bricks preview", e);
            }

        } else {
            if (event.getEventSource() == EventSource.USER) {
                board.getScore().add(1);
            }
        }
        return new DownData(clearRow, board.getViewData());
    }

    @Override
    public ViewData onLeftEvent(MoveEvent event) {
        LOGGER.fine("onLeftEvent");
        boolean moved = board.moveBrickLeft();
        try {
            LOGGER.fine(() -> "moveLeft result=" + moved + " offset=" + board.getViewData().getxPosition() + "," + board.getViewData().getyPosition());
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Unable to log moveLeft offset", e);
        }
        return board.getViewData();
    }

    @Override
    public ViewData onRightEvent(MoveEvent event) {
        LOGGER.fine("onRightEvent");
        boolean moved = board.moveBrickRight();
        try {
            LOGGER.fine(() -> "moveRight result=" + moved + " offset=" + board.getViewData().getxPosition() + "," + board.getViewData().getyPosition());
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Unable to log moveRight offset", e);
        }
        return board.getViewData();
    }

    @Override
    public ViewData onRotateEvent(MoveEvent event) {
        LOGGER.fine("onRotateEvent");
        boolean rotated = board.rotateLeftBrick();
        try {
            LOGGER.fine(() -> "rotate result=" + rotated + " offset=" + board.getViewData().getxPosition() + "," + board.getViewData().getyPosition());
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Unable to log rotate offset", e);
        }
        return board.getViewData();
    }


    @Override
    public void createNewGame() {
        board.newGame();
        viewGuiController.refreshGameBackground(board.getBoardMatrix());
    }

    @Override
    public void onSwapEvent() {
        try {
            boolean swapped = board.swapCurrentWithNext();
            if (swapped) {
                viewGuiController.refreshGameBackground(board.getBoardMatrix());
                viewGuiController.refreshCurrentView(board.getViewData());
                java.util.List<com.comp2042.logic.bricks.Brick> upcoming = board.getUpcomingBricks(3);
                viewGuiController.showNextBricks(upcoming);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during swap event", e);
        }
    }
}
