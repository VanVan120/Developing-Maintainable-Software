package com.comp2042.input;

import com.comp2042.model.DownData;
import com.comp2042.model.ViewData;

public interface InputEventListener {

    /**
     * Called when a "down" input occurs (soft/hard drop or repeated down).
     *
     * @param event details about the move
     * @return DownData produced by handling the event; may be null if not used
     */
    DownData onDownEvent(MoveEvent event);

    /**
     * Called when a left movement input is requested.
     *
     * @param event details about the move
     * @return a ViewData describing how the view should change (may be null)
     */
    ViewData onLeftEvent(MoveEvent event);

    /**
     * Called when a right movement input is requested.
     *
     * @param event details about the move
     * @return a ViewData describing how the view should change (may be null)
     */
    ViewData onRightEvent(MoveEvent event);

    /**
     * Called when a rotation input is requested.
     *
     * @param event details about the move
     * @return a ViewData describing how the view should change (may be null)
     */
    ViewData onRotateEvent(MoveEvent event);

    void createNewGame();

    default void onSwapEvent() {}
}
