package com.comp2042;

public class GameController implements InputEventListener {

    // SimpleBoard(width, height) — we want a tall board: height=25 rows, width=10 columns
    private Board board = new SimpleBoard(10, 25);

    private final GuiController viewGuiController;

    public GameController(GuiController c) {
        viewGuiController = c;
        board.createNewBrick();
        viewGuiController.setEventListener(this);
        viewGuiController.initGameView(board.getBoardMatrix(), board.getViewData());
        viewGuiController.bindScore(board.getScore().scoreProperty());
        // show upcoming bricks preview (up to 3)
        try {
            java.util.List<com.comp2042.logic.bricks.Brick> upcoming = board.getUpcomingBricks(3);
            viewGuiController.showNextBricks(upcoming);
        } catch (Exception ignored) {}
    }

    @Override
    public DownData onDownEvent(MoveEvent event) {
        System.out.println("onDownEvent source=" + event.getEventSource());
        boolean canMove = board.moveBrickDown();
        System.out.println("moveDown result=" + canMove + " offset=" + board.getViewData().getxPosition() + "," + board.getViewData().getyPosition());
        ClearRow clearRow = null;
        if (!canMove) {
            board.mergeBrickToBackground();
            clearRow = board.clearRows();
            if (clearRow.getLinesRemoved() > 0) {
                board.getScore().add(clearRow.getScoreBonus());
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
}
