package com.comp2042.controller.guiControl;

/**
 * Lightweight clock manager stub. The original GuiController still contains
 * full clock methods; this class exists to centralize future refactoring and
 * to satisfy type references created during refactor.
 */
/**
 * Lightweight clock manager used by {@link GuiController} to track and display
 * elapsed play time.
 *
 * <p>This helper drives a one-second JavaFX {@link javafx.animation.Timeline}
 * that updates the {@code GuiController.timeValue} text. It records pause
 * durations so the displayed time excludes paused intervals.</p>
 */
class GuiClockManager {
    private final GuiController owner;
    private long startTimeMs = 0;
    private long pausedElapsedMs = 0;
    private javafx.animation.Timeline clockTimeline = null;

    GuiClockManager(GuiController owner) {
        this.owner = owner;
    }

    /** Start or resume the clock. Safe to call from the JavaFX thread. */
    void startClock() {
        startTimeMs = System.currentTimeMillis() - pausedElapsedMs;
        if (clockTimeline != null) clockTimeline.stop();
        clockTimeline = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                updateClock();
            }
        }));
        clockTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        clockTimeline.play();
    }

    /** Stop the running clock timeline without resetting elapsed time. */
    void stopClock() {
        if (clockTimeline != null) clockTimeline.stop();
    }

    /** Reset the recorded time to zero and update the UI to show "00:00". */
    void resetClock() {
        pausedElapsedMs = 0;
        startTimeMs = System.currentTimeMillis();
        try { if (owner.timeValue != null) owner.timeValue.setText("00:00"); } catch (Exception ignored) {}
    }

    /**
     * Recompute the elapsed time and update the controller's time label.
     * This method is invoked by the internal timeline once per second.
     */
    void updateClock() {
        if (startTimeMs == 0) return;
        long elapsed = System.currentTimeMillis() - startTimeMs;
        long seconds = elapsed / 1000;
        long mins = seconds / 60;
        long secs = seconds % 60;
        if (owner.timeValue != null) {
            try { owner.timeValue.setText(String.format("%02d:%02d", mins, secs)); } catch (Exception ignored) {}
        }
    }

    /** Pause the clock and record the elapsed time so it can be resumed. */
    void pauseAndRecord() {
        if (clockTimeline != null && clockTimeline.getStatus() == javafx.animation.Animation.Status.RUNNING) {
            pausedElapsedMs = System.currentTimeMillis() - startTimeMs;
            clockTimeline.pause();
        }
    }
}
