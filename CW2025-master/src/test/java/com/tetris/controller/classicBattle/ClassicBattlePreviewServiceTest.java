package com.tetris.controller.classicBattle;

import com.comp2042.controller.classicBattle.ClassicBattlePreviewService;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ClassicBattlePreviewServiceTest {

    @Test
    void stop_withoutStart_doesNotThrow() {
        ClassicBattlePreviewService svc = new ClassicBattlePreviewService(null, null, null, null, null, null);
        assertDoesNotThrow(() -> svc.stop());
    }

    @Test
    void pause_and_play_withoutPoller_doNotThrow() {
        ClassicBattlePreviewService svc = new ClassicBattlePreviewService(null, null, null, null, null, null);
        assertDoesNotThrow(() -> svc.pause());
        assertDoesNotThrow(() -> svc.play());
    }
}
