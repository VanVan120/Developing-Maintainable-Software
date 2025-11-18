package com.comp2042.controller.classicBattle;

import java.util.Objects;
import java.util.function.BiConsumer;

import com.comp2042.controller.guiControl.GuiController;

/**
 * Responsible for match lifecycle: watching for game-over, coordinating preview/audio
 * stop, and invoking callbacks to show overlays or restart the match.
 */
public class ClassicBattleMatchManager {

    private final GuiController leftGui;
    private final GuiController rightGui;
    private final ClassicBattlePreviewService previewService;
    private final ClassicBattleAudioHelper audioHelper;

    private final Runnable stopMusicAction;
    private final Runnable restartAction;
    private final BiConsumer<String, String> showWinnerOverlay;
    private volatile boolean matchEnded = false;

    public ClassicBattleMatchManager(GuiController leftGui,
                                     GuiController rightGui,
                                     ClassicBattlePreviewService previewService,
                                     ClassicBattleAudioHelper audioHelper,
                                     Runnable stopMusicAction,
                                     Runnable restartAction,
                                     BiConsumer<String, String> showWinnerOverlay) {
        this.leftGui = leftGui;
        this.rightGui = rightGui;
        this.previewService = previewService;
        this.audioHelper = audioHelper;
        this.stopMusicAction = stopMusicAction;
        this.restartAction = restartAction;
        this.showWinnerOverlay = showWinnerOverlay;
    }

    /**
     * Register listeners on both GUI controllers to detect game-over events.
     * When a game-over is observed the manager will coordinate stopping the
     * preview, invoking audio hooks and showing the winner overlay via the
     * provided {@code showWinnerOverlay} callback.
     */
    public void registerListeners() {
        try {
            if (leftGui != null) {
                leftGui.isGameOverProperty().addListener((obs, oldV, newV) -> handleGameOver(true, obs, oldV, newV));
            }
        } catch (Exception ignored) {}
        try {
            if (rightGui != null) {
                rightGui.isGameOverProperty().addListener((obs, oldV, newV) -> handleGameOver(false, obs, oldV, newV));
            }
        } catch (Exception ignored) {}
    }

    private void handleGameOver(boolean isLeft, javafx.beans.Observable obs, Boolean oldV, Boolean newV) {
        try {
            Objects.requireNonNull(obs);
            Objects.requireNonNull(oldV);
            if (newV == Boolean.TRUE && !matchEnded) {
                matchEnded = true;
                try { if (previewService != null) previewService.stop(); } catch (Exception ignored) {}
                try { if (leftGui != null) leftGui.gameOver(); } catch (Exception ignored) {}
                try { if (rightGui != null) rightGui.gameOver(); } catch (Exception ignored) {}
                try { if (stopMusicAction != null) stopMusicAction.run(); } catch (Exception ignored) {}
                try { if (audioHelper != null) audioHelper.playMatchGameOverSound(); } catch (Exception ignored) {}

                boolean winnerIsLeft = !isLeft;
                String title = winnerIsLeft ? "Left Player Wins!" : "Right Player Wins!";
                String reason = "Winner by survival (opponent lost)";
                try {
                    if (showWinnerOverlay != null) showWinnerOverlay.accept(title, reason);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    /**
     * Trigger a match restart by invoking the configured restart action and
     * clearing the internal match-ended flag.
     */
    public void restartMatch() {
        try {
            if (restartAction != null) restartAction.run();
            matchEnded = false;
        } catch (Exception ignored) {}
    }

    /**
     * Clear the internal match-ended flag (used when the owner restarts a match).
     */
    public void clearMatchEnded() {
        this.matchEnded = false;
    }

}
