package com.comp2042.controller.guiControl;

import javafx.animation.Timeline;

/**
 * Helper to encapsulate the simple wiring required to start the game
 * countdown overlay.
 *
 * <p>This small utility prepares a {@link GuiCountdownContext} populated from
 * the provided {@link GuiController} and then delegates to
 * {@link GuiCountdown#startCountdown(int, GuiCountdownContext)}. The helper
 * centralises the mapping between controller internals and the countdown
 * context so the controller itself remains focused on lifecycle and event
 * handling.</p>
 */
public final class GuiCountdownController {
    private GuiCountdownController() {}

    /**
     * Prepare the countdown context and return a configured {@link Timeline}.
     *
     * <p>The returned timeline is <em>not</em> started by this method; callers
     * should call {@code play()} or {@code playFromStart()} on the result.
     * The method is best-effort and tolerates missing helper instances on the
     * controller by wrapping calls in try/catch blocks.</p>
     *
     * @param controller the GUI controller providing UI nodes and callbacks
    * @param seconds number of seconds to count down (3 if {@code <= 0})
     * @return a {@link Timeline} configured to run the visual countdown, or
     *         {@code null} if the context could not be prepared
     */
    public static Timeline startCountdown(GuiController controller, int seconds) {
        try { controller.setPrevHighBeforeGame((controller.highScoreManager != null) ? controller.highScoreManager.getHighScore() : 0); } catch (Exception ignored) {}
        GuiCountdownContext ctx = new GuiCountdownContext();
        ctx.gameBoard = controller.gameBoard;
        ctx.brickPanel = controller.brickPanel;
        ctx.ghostPanel = controller.ghostPanel;
        ctx.groupNotification = controller.groupNotification;
        ctx.timeLine = controller.timeLine;
        ctx.resetClock = () -> { try { if (controller.clockManager != null) controller.clockManager.resetClock(); } catch (Exception ignored) {}; };
        ctx.startClock = () -> { try { if (controller.clockManager != null) controller.clockManager.startClock(); } catch (Exception ignored) {}; };
        ctx.isPause = controller.isPause;
        ctx.countdownFinished = controller.countdownFinished;
        ctx.countdownStarted = controller.countdownStarted;
        ctx.currentViewData = controller.currentViewData;
        ctx.currentBoardMatrix = controller.currentBoardMatrix;
        ctx.gamePanelNode = controller.gamePanel;
        ctx.playCountdownMusic = controller::playCountdownMusicInternal;
        ctx.stopCountdownMusic = controller::stopCountdownMusicInternal;
        // wire panel/refresh callbacks so the countdown can hide visuals and restore them after finishing
        ctx.refreshAndSnap = controller::refreshAndSnapBrickAsyncInternal;
        ctx.hidePanels = controller::hideBrickAndGhostPanelsAsyncInternal;
        ctx.showPanels = () -> javafx.application.Platform.runLater(() -> {
            try { if (controller.brickPanel != null) controller.brickPanel.setVisible(true); } catch (Exception ignored) {}
            try { if (controller.ghostPanel != null) controller.ghostPanel.setVisible(true); } catch (Exception ignored) {}
        });
        ctx.refreshVisible = () -> {
            try { if (controller.currentViewData != null) controller.doRefreshBrick(controller.currentViewData); } catch (Exception ignored) {}
        };

        Timeline cd = GuiCountdown.startCountdown(seconds, ctx);
        return cd;
    }
}
