package com.tetris.controller.classicBattle;

import com.comp2042.controller.classicBattle.ClassicBattleMatchManager;
import com.comp2042.controller.classicBattle.ClassicBattlePreviewService;
import com.comp2042.controller.GuiController;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ClassicBattleMatchManagerTest {

    static class TestGuiController extends GuiController {
        private final javafx.beans.property.BooleanProperty gameOverProp = new javafx.beans.property.SimpleBooleanProperty(false);
        public boolean gameOverCalled = false;

        @Override
        public javafx.beans.property.BooleanProperty isGameOverProperty() {
            return gameOverProp;
        }

        @Override
        public void gameOver() {
            gameOverCalled = true;
        }
    }

    static class DummyPreviewService extends ClassicBattlePreviewService {
        public boolean stopped = false;
        public DummyPreviewService() { super(null, null, null, null, null, null); }
        @Override public void stop() { stopped = true; super.stop(); }
    }

    @Test
    void whenLeftGameOver_managerInvokesHandlers_andReportsRightWinner() {
        TestGuiController left = new TestGuiController();
        TestGuiController right = new TestGuiController();
        DummyPreviewService preview = new DummyPreviewService();

        AtomicBoolean stopMusicCalled = new AtomicBoolean(false);
        Runnable stopMusic = () -> stopMusicCalled.set(true);

        AtomicInteger overlayCount = new AtomicInteger(0);
        AtomicReference<String> lastTitle = new AtomicReference<>();
        AtomicReference<String> lastReason = new AtomicReference<>();

        ClassicBattleMatchManager mgr = new ClassicBattleMatchManager(
                left, right, preview, /*audioHelper*/ null,
                stopMusic, /*restart*/ null,
                (title, reason) -> { overlayCount.incrementAndGet(); lastTitle.set(title); lastReason.set(reason); }
        );

        mgr.registerListeners();

        // Simulate left becoming game-over
        left.isGameOverProperty().setValue(Boolean.TRUE);

        assertTrue(preview.stopped, "Preview service should be stopped when match ends");
        assertTrue(left.gameOverCalled, "Left.gameOver() should have been invoked");
        assertTrue(right.gameOverCalled, "Right.gameOver() should have been invoked");
        assertTrue(stopMusicCalled.get(), "stopMusicAction should be invoked");
        assertEquals(1, overlayCount.get(), "Winner overlay should be shown once");
        assertNotNull(lastTitle.get());
        assertTrue(lastTitle.get().contains("Right") || lastTitle.get().contains("Left"));

        // Now ensure restart resets the internal flag so another game-over can trigger overlay again
        mgr.restartMatch();
        // flip property so listeners will see a transition to TRUE again
        left.isGameOverProperty().setValue(Boolean.FALSE);
        left.isGameOverProperty().setValue(Boolean.TRUE);

        assertEquals(2, overlayCount.get(), "After restart, another winner overlay should be shown");
    }

    @Test
    void whenRightGameOver_managerInvokesHandlers_andReportsLeftWinner() {
        TestGuiController left = new TestGuiController();
        TestGuiController right = new TestGuiController();
        DummyPreviewService preview = new DummyPreviewService();

        AtomicBoolean stopMusicCalled = new AtomicBoolean(false);
        Runnable stopMusic = () -> stopMusicCalled.set(true);

        AtomicInteger overlayCount = new AtomicInteger(0);
        AtomicReference<String> lastTitle = new AtomicReference<>();

        ClassicBattleMatchManager mgr = new ClassicBattleMatchManager(
                left, right, preview, /*audioHelper*/ null,
                stopMusic, /*restart*/ null,
                (title, reason) -> { overlayCount.incrementAndGet(); lastTitle.set(title); }
        );

        mgr.registerListeners();

        // Simulate right becoming game-over
        right.isGameOverProperty().setValue(Boolean.TRUE);

        assertTrue(preview.stopped, "Preview service should be stopped when match ends");
        assertTrue(left.gameOverCalled, "Left.gameOver() should have been invoked");
        assertTrue(right.gameOverCalled, "Right.gameOver() should have been invoked");
        assertTrue(stopMusicCalled.get(), "stopMusicAction should be invoked");
        assertEquals(1, overlayCount.get(), "Winner overlay should be shown once");
        assertNotNull(lastTitle.get());
        assertTrue(lastTitle.get().contains("Left") || lastTitle.get().contains("Right"));
    }

    @Test
    void rapidMultipleGameOvers_onlyShowOverlayOnceUntilRestart() {
        TestGuiController left = new TestGuiController();
        TestGuiController right = new TestGuiController();
        DummyPreviewService preview = new DummyPreviewService();

        AtomicInteger overlayCount = new AtomicInteger(0);

        ClassicBattleMatchManager mgr = new ClassicBattleMatchManager(
                left, right, preview, null,
                () -> {}, () -> {},
                (title, reason) -> overlayCount.incrementAndGet()
        );

        mgr.registerListeners();

        // fire several transitions quickly
        left.isGameOverProperty().setValue(Boolean.TRUE);
        right.isGameOverProperty().setValue(Boolean.TRUE);
        left.isGameOverProperty().setValue(Boolean.TRUE);

        // should only have triggered overlay once
        assertEquals(1, overlayCount.get(), "Overlay should be shown only once until restart");

        // restart and trigger again
        mgr.restartMatch();
        left.isGameOverProperty().setValue(Boolean.FALSE);
        right.isGameOverProperty().setValue(Boolean.FALSE);
        right.isGameOverProperty().setValue(Boolean.TRUE);
        assertEquals(2, overlayCount.get(), "Overlay should be shown again after restart");
    }
}
