package com.comp2042.controller.scoreBattle;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.embed.swing.JFXPanel;

import static org.junit.jupiter.api.Assertions.*;

public class ScoreBattleInitializerTest {

    @BeforeAll
    public static void initToolkit() {
        // initialize JavaFX toolkit
        new JFXPanel();
    }

    @Test
    public void initBothGames_setsControllerFields() throws Exception {
        ScoreBattleController ctrl = new ScoreBattleController();

        // call initializer (integration-style); should not throw
        ScoreBattleInitializer.initBothGames(ctrl, null, null);

        // after initialization, core references should be set
        assertNotNull(ctrl.leftGui, "leftGui should be set");
        assertNotNull(ctrl.rightGui, "rightGui should be set");
        assertNotNull(ctrl.leftController, "leftController should be set");
        assertNotNull(ctrl.rightController, "rightController should be set");
        assertNotNull(ctrl.matchTimer, "matchTimer should be set");
        assertNotNull(ctrl.previewPoller, "previewPoller should be set");
    }
}
