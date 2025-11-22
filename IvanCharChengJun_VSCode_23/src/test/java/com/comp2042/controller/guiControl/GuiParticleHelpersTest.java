package com.comp2042.controller.guiControl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiParticleHelpersTest {
    @Test
    void basicNoOpWhenNoPane() {
        GuiController c = new GuiController(){};
        // particlePane is null by default — these calls should be safe no-ops
        GuiParticleHelpers.spawnExplosion(c, null, null);
        GuiParticleHelpers.spawnExplosionAtLanding(c, null);
        GuiParticleHelpers.boardCoordsToParticleLocal(c, 0, 0);
        GuiParticleHelpers.flashRowAt(c, 0, 0, 10, 10);
        GuiParticleHelpers.spawnRowClearParticles(c, null);
        GuiParticleHelpers.flashRow(c, 0, 1, 1);
        GuiParticleHelpers.spawnParticlesAt(c, 0, 0, null);
        GuiParticleHelpers.shakeBoard(c);
        assertTrue(true);
    }
}
