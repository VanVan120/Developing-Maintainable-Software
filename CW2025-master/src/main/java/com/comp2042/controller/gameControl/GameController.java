package com.comp2042.controller.gameControl;

import com.comp2042.controller.GuiController;

import com.comp2042.input.InputEventListener;
import com.comp2042.input.MoveEvent;
import com.comp2042.model.DownData;
import com.comp2042.model.ViewData;
import com.comp2042.model.Board;
import com.comp2042.model.SimpleBoard;

import javafx.beans.property.IntegerProperty;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameController implements InputEventListener {
    private final GameEngine engine;
    private final GuiController viewGuiController;
    private static final Logger LOGGER = Logger.getLogger(GameController.class.getName());
    private Consumer<Integer> clearRowHandler = null;

    /**
     * Primary constructor that uses a default SimpleBoard. Keeps compatibility with callers
     * that only pass a GuiController.
     */
    public GameController(GuiController c) {
        this(c, new SimpleBoard(10, 25));
    }

    /**
     * Constructor that accepts a Board instance — enables dependency injection for testing.
     */
    public GameController(GuiController c, Board board) {
        this.viewGuiController = Objects.requireNonNull(c, "GuiController must not be null");
        this.engine = new GameEngine(Objects.requireNonNull(board, "Board must not be null"));

        // initialize board and view in small helper methods for clarity and testability
        engine.createNewBrick();
        viewGuiController.setEventListener(this);
        viewGuiController.initGameView(engine.getBoardMatrix(), engine.getViewData());
        viewGuiController.bindScore(engine.getScoreProperty());

        setSwapKeySafe();
        safeRefreshUpcomingBricks();
    }

    public void setClearRowHandler(java.util.function.Consumer<Integer> handler) {
        this.clearRowHandler = handler;
    }

    public void addGarbageRows(int count, int holeColumn) {
        try {
            int[][] matrix = engine.addGarbageRows(count, holeColumn);
            refreshGameBackgroundSafe(matrix);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unexpected error while adding garbage rows", e);
        }
    }

    public IntegerProperty getScoreProperty() {
        return engine.getScoreProperty();
    }

    public java.util.List<com.comp2042.logic.Brick> getUpcomingBricks(int count) {
        return engine.getUpcoming(count);
    }

    @Override
    public DownData onDownEvent(MoveEvent event) {
        LOGGER.fine("onDownEvent source=" + event.getEventSource());
        // delegate main game logic to the engine
        MoveDownResult result = engine.moveDown(event.getEventSource());
        try {
            LOGGER.fine("moveDown processed; offset=" + engine.getViewData().getxPosition() + "," + engine.getViewData().getyPosition());
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Unable to log moveDown offset", e);
        }
        // update UI based on engine result
        if (result.isGameOver()) {
            viewGuiController.gameOver();
        }

        if (result.getForwardCount() > 0 && clearRowHandler != null) {
            try {
                clearRowHandler.accept(result.getForwardCount());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Clear row handler threw", e);
            }
        }

        refreshGameBackgroundSafe(result.getBoardMatrix());
        safeRefreshUpcomingBricks();

        return new DownData(result.getClearRow(), result.getViewData());
    }

    @Override
    public ViewData onLeftEvent(MoveEvent event) {
        LOGGER.fine("onLeftEvent");
        boolean moved = engine.moveLeft() != null;
        try {
            LOGGER.fine("moveLeft result=" + moved + " offset=" + engine.getViewData().getxPosition() + "," + engine.getViewData().getyPosition());
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Unable to log moveLeft offset", e);
        }
        return engine.getViewData();
    }

    @Override
    public ViewData onRightEvent(MoveEvent event) {
        LOGGER.fine("onRightEvent");
        boolean moved = engine.moveRight() != null;
        try {
            LOGGER.fine("moveRight result=" + moved + " offset=" + engine.getViewData().getxPosition() + "," + engine.getViewData().getyPosition());
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Unable to log moveRight offset", e);
        }
        return engine.getViewData();
    }

    @Override
    public ViewData onRotateEvent(MoveEvent event) {
        LOGGER.fine("onRotateEvent");
        boolean rotated = engine.rotate() != null;
        try {
            LOGGER.fine("rotate result=" + rotated + " offset=" + engine.getViewData().getxPosition() + "," + engine.getViewData().getyPosition());
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Unable to log rotate offset", e);
        }
        return engine.getViewData();
    }

    @Override
    public void createNewGame() {
        engine.newGame();
        viewGuiController.refreshGameBackground(engine.getBoardMatrix());
    }

    @Override
    public void onSwapEvent() {
        try {
            boolean swapped = engine.swap();
            if (swapped) {
                viewGuiController.refreshGameBackground(engine.getBoardMatrix());
                viewGuiController.refreshCurrentView(engine.getViewData());
                safeRefreshUpcomingBricks();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during swap event", e);
        }
    }
    private void safeRefreshUpcomingBricks() {
        try {
            List<com.comp2042.logic.Brick> upcoming = engine.getUpcoming(3);
            viewGuiController.showNextBricks(upcoming);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to update upcoming bricks preview", e);
        }
    }

    private void setSwapKeySafe() {
        try {
            viewGuiController.setSwapKey(javafx.scene.input.KeyCode.C);
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Failed to set swap key", e);
        }
    }

    private void refreshGameBackgroundSafe(int[][] matrix) {
        try {
            viewGuiController.refreshGameBackground(matrix);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to refresh game background after update", e);
        }
    }

    

    
}
