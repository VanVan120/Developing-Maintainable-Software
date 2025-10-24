package com.comp2042;

import javafx.beans.property.IntegerProperty;

public class GameController implements InputEventListener {

    // SimpleBoard(width, height) — we want a tall board: height=25 rows, width=10 columns
    private Board board = new SimpleBoard(10, 25);

    private final GuiController viewGuiController;
    // optional handler to notify an external multiplayer coordinator when rows are cleared
    // Now accepts Integer = number of lines to forward to opponent (excludes cleared garbage-only rows)
    private java.util.function.Consumer<Integer> clearRowHandler = null;

    public GameController(GuiController c) {
        viewGuiController = c;
        board.createNewBrick();
        viewGuiController.setEventListener(this);
        viewGuiController.initGameView(board.getBoardMatrix(), board.getViewData());
        viewGuiController.bindScore(board.getScore().scoreProperty());
        // enable swap key for single-player: use 'C' as default swap key (matches multiplayer right player)
        try {
            viewGuiController.setSwapKey(javafx.scene.input.KeyCode.C);
        } catch (Exception ignored) {}
        // show upcoming bricks preview (up to 3)
        try {
            java.util.List<com.comp2042.logic.bricks.Brick> upcoming = board.getUpcomingBricks(3);
            viewGuiController.showNextBricks(upcoming);
        } catch (Exception ignored) {}
    }

    /**
     * Register a handler that will be invoked when this controller clears rows.
     * The handler receives the ClearRow object describing what was removed.
     */
    public void setClearRowHandler(java.util.function.Consumer<Integer> handler) {
        this.clearRowHandler = handler;
    }

    /**
     * Add garbage rows to the bottom of this controller's board. Existing rows are
     * shifted upward by 'count'. The 'holeColumn' parameter selects which column
     * is left empty in each garbage row; pass a negative value to use the rightmost
     * column.
     */
    public void addGarbageRows(int count, int holeColumn) {
        try {
            if (count <= 0) return;
            int[][] matrix = board.getBoardMatrix();
            if (matrix == null || matrix.length == 0) return;
            int h = matrix.length;
            int w = matrix[0].length;
            if (holeColumn < 0) holeColumn = w - 1; // default: rightmost column
            if (holeColumn < 0 || holeColumn >= w) holeColumn = w - 1;

            // build a temporary matrix representing the new board after garbage insertion
            int[][] tmp = new int[h][w];
            // shift existing rows upward by 'count'
            for (int r = 0; r < h - count; r++) {
                System.arraycopy(matrix[r + count], 0, tmp[r], 0, w);
            }
            // bottom 'count' rows become garbage rows with a single hole
            for (int r = h - count; r < h; r++) {
                for (int c = 0; c < w; c++) {
                    tmp[r][c] = (c == holeColumn) ? 0 : 8; // use value '8' for garbage blocks (grey)
                }
            }

            // copy tmp back into the board's matrix
            for (int r = 0; r < h; r++) {
                System.arraycopy(tmp[r], 0, matrix[r], 0, w);
            }

            // notify the view to refresh
            try { viewGuiController.refreshGameBackground(matrix); } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    /**
     * Expose the score IntegerProperty for external observers (e.g. multiplayer UI).
     */
    public IntegerProperty getScoreProperty() {
        return board.getScore().scoreProperty();
    }

    /**
     * Return a snapshot list of upcoming bricks (up to 'count').
     */
    public java.util.List<com.comp2042.logic.bricks.Brick> getUpcomingBricks(int count) {
        try {
            return board.getUpcomingBricks(count);
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    @Override
    public DownData onDownEvent(MoveEvent event) {
        System.out.println("onDownEvent source=" + event.getEventSource());
        boolean canMove = board.moveBrickDown();
        System.out.println("moveDown result=" + canMove + " offset=" + board.getViewData().getxPosition() + "," + board.getViewData().getyPosition());
        ClearRow clearRow = null;
        if (!canMove) {
            board.mergeBrickToBackground();
            // snapshot the matrix after merging the piece but before row removal so we can
            // inspect which cleared rows contained original (non-garbage) blocks.
            int[][] matrixBeforeClear = MatrixOperations.copy(board.getBoardMatrix());
            clearRow = board.clearRows();
            if (clearRow.getLinesRemoved() > 0) {
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
                                        if (v == 8) { // 8 == garbage block value
                                            hasGarbage = true;
                                            break;
                                        }
                                    }
                                    // Only forward rows that contained NO garbage (pure original filled rows)
                                    if (!hasGarbage) forwardCount++;
                                }
                            }
                        }
                        clearRowHandler.accept(Integer.valueOf(forwardCount));
                    }
                } catch (Exception ignored) {}
            }
            if (board.createNewBrick()) {
                viewGuiController.gameOver();
            }

            viewGuiController.refreshGameBackground(board.getBoardMatrix());
            // update upcoming preview after a brick is consumed and new one created
            try {
                java.util.List<com.comp2042.logic.bricks.Brick> upcoming = board.getUpcomingBricks(3);
                viewGuiController.showNextBricks(upcoming);
            } catch (Exception ignored) {}

        } else {
            if (event.getEventSource() == EventSource.USER) {
                board.getScore().add(1);
            }
        }
        return new DownData(clearRow, board.getViewData());
    }

    @Override
    public ViewData onLeftEvent(MoveEvent event) {
        System.out.println("onLeftEvent");
        boolean moved = board.moveBrickLeft();
        System.out.println("moveLeft result=" + moved + " offset=" + board.getViewData().getxPosition() + "," + board.getViewData().getyPosition());
        return board.getViewData();
    }

    @Override
    public ViewData onRightEvent(MoveEvent event) {
        System.out.println("onRightEvent");
        boolean moved = board.moveBrickRight();
        System.out.println("moveRight result=" + moved + " offset=" + board.getViewData().getxPosition() + "," + board.getViewData().getyPosition());
        return board.getViewData();
    }

    @Override
    public ViewData onRotateEvent(MoveEvent event) {
        System.out.println("onRotateEvent");
        boolean rotated = board.rotateLeftBrick();
        System.out.println("rotate result=" + rotated + " offset=" + board.getViewData().getxPosition() + "," + board.getViewData().getyPosition());
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
                // refresh the whole board and current brick view
                viewGuiController.refreshGameBackground(board.getBoardMatrix());
                viewGuiController.refreshCurrentView(board.getViewData());
                java.util.List<com.comp2042.logic.bricks.Brick> upcoming = board.getUpcomingBricks(3);
                viewGuiController.showNextBricks(upcoming);
            }
        } catch (Exception ignored) {}
    }
}
