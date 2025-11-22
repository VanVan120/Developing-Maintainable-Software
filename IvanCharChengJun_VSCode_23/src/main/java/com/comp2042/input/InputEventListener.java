package com.comp2042.input;

import com.comp2042.model.DownData;
import com.comp2042.model.ViewData;

/**
 * Listener interface for input events produced by the input subsystem.
 *
 * <p>Implementations (typically controllers or game model adapters) receive
 * higher-level move events encapsulated in {@link MoveEvent} and return
 * model/view updates as required. Implementations should be resilient to
 * {@code null} fields inside {@link MoveEvent}.
 */
public interface InputEventListener {

    /**
     * Handle a down-type input (soft/hard drop or repeated down).
     *
     * @param event details about the input; event may be non-null
     * @return a {@link DownData} instance describing the down action results
     *         (may be {@code null} if not applicable)
     */
    DownData onDownEvent(MoveEvent event);

    /**
     * Handle a left move request.
     *
     * @param event details about the input request
     * @return a {@link ViewData} describing view updates (may be {@code null})
     */
    ViewData onLeftEvent(MoveEvent event);

    /**
     * Handle a right move request.
     *
     * @param event details about the input request
     * @return a {@link ViewData} describing view updates (may be {@code null})
     */
    ViewData onRightEvent(MoveEvent event);

    /**
     * Handle a rotation request.
     *
     * @param event details about the input request
     * @return a {@link ViewData} describing view updates (may be {@code null})
     */
    ViewData onRotateEvent(MoveEvent event);

    /**
     * Signal to the listener that a new game should be created/initialized.
     */
    void createNewGame();

    /**
     * Optional callback triggered when a swap piece action is requested.
     * Default implementation does nothing.
     */
    default void onSwapEvent() {}
}
