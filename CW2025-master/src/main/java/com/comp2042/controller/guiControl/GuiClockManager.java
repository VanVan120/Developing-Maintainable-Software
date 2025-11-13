package com.comp2042.controller.guiControl;

/**
 * Lightweight clock manager stub. The original GuiController still contains
 * full clock methods; this class exists to centralize future refactoring and
 * to satisfy type references created during refactor.
 */
class GuiClockManager {
    private final GuiController owner;
    private long startTimeMs = 0;
    private long pausedElapsedMs = 0;
    private javafx.animation.Timeline clockTimeline = null;

    GuiClockManager(GuiController owner) {
        this.owner = owner;
    }

    // Start the clock (runs on JavaFX thread)
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

    void stopClock() {
        if (clockTimeline != null) clockTimeline.stop();
    }

    void resetClock() {
        pausedElapsedMs = 0;
        startTimeMs = System.currentTimeMillis();
        try { if (owner.timeValue != null) owner.timeValue.setText("00:00"); } catch (Exception ignored) {}
    }

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

    // Pause helper used when pausing from overlays
    void pauseAndRecord() {
        if (clockTimeline != null && clockTimeline.getStatus() == javafx.animation.Animation.Status.RUNNING) {
            pausedElapsedMs = System.currentTimeMillis() - startTimeMs;
            clockTimeline.pause();
        }
    }
}
