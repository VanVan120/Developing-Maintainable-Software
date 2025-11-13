package com.comp2042.controller.guiControl;

import javafx.beans.property.BooleanProperty;
import javafx.scene.Group;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.GridPane;
import javafx.animation.Timeline;

import com.comp2042.model.ViewData;

/**
 * Simple context holder used by CountdownUI to avoid passing a very long parameter list.
 */
public class GuiCountdownContext {
    public BorderPane gameBoard;
    public Pane brickPanel;
    public Pane ghostPanel;
    public Group groupNotification;
    public Timeline timeLine;
    public Runnable resetClock;
    public Runnable startClock;
    public BooleanProperty isPause;
    public BooleanProperty countdownFinished;
    public BooleanProperty countdownStarted;
    public ViewData currentViewData;
    public int[][] currentBoardMatrix;
    public GridPane gamePanelNode;
    public Runnable playCountdownMusic;
    public Runnable stopCountdownMusic;
    // Callbacks used by CountdownUI to control panel visibility and refresh behavior
    public Runnable hidePanels;
    public Runnable showPanels;
    public Runnable refreshAndSnap;
    public Runnable refreshVisible;

}
