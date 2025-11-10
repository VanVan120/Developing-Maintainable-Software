package com.tetris.controller.classicBattle;

import com.comp2042.controller.classicBattle.ClassicBattleAudioHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClassicBattleAudioHelperTest {

    @Test
    void stopAndDispose_null_returnsNullAndDoesNotThrow() {
        assertNull(ClassicBattleAudioHelper.stopAndDispose(null));
    }

    @Test
    void stopAndClose_null_returnsNullAndDoesNotThrow() {
        assertNull(ClassicBattleAudioHelper.stopAndClose(null));
    }

    @Test
    void playAndStop_methods_doNotThrow_whenResourcesMissing() {
        ClassicBattleAudioHelper helper = new ClassicBattleAudioHelper(getClass());

        assertDoesNotThrow(() -> helper.playMatchGameOverSound());
        assertDoesNotThrow(() -> helper.stopMatchGameOverSound());

        assertDoesNotThrow(() -> helper.playMatchCountdownSound());
        assertDoesNotThrow(() -> helper.stopMatchCountdownSound());
    }
}
