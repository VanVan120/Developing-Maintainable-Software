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
/**
 * Context holder passed into the countdown helper to avoid long parameter lists.
 *
 * <p>The fields are intentionally package-visible and mutable; the controller
 * populates this object immediately before calling the countdown helper.</p>
 */
public class GuiCountdownContext {
    /** Reference to the main game board node (used to attach overlays). */
    public BorderPane gameBoard;
    public Pane brickPanel;
    public Pane ghostPanel;
    public Group groupNotification;
    public Timeline timeLine;
    /** Runnable to reset the internal clock display. */
    public Runnable resetClock;
    /** Runnable to start the internal clock. */
    public Runnable startClock;
    public BooleanProperty isPause;
    public BooleanProperty countdownFinished;
    public BooleanProperty countdownStarted;
    public ViewData currentViewData;
    public int[][] currentBoardMatrix;
    /** Grid node representing the game panel; used to return focus after the countdown. */
    public GridPane gamePanelNode;
    /** Optional callback to request countdown audio playback. */
    public Runnable playCountdownMusic;
    /** Optional callback to stop countdown audio. */
    public Runnable stopCountdownMusic;
    // Callbacks used by CountdownUI to control panel visibility and refresh behavior
    public Runnable hidePanels;
    public Runnable showPanels;
    public Runnable refreshAndSnap;
    public Runnable refreshVisible;

}
