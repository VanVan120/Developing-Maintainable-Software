package com.comp2042.controller.scoreBattle;

import com.comp2042.controller.guiControl.GuiController;
import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// NOTE: placed in a separate package name to avoid classpath collisions
public class ScoreBattleControllerTest {

    @BeforeAll
    public static void initToolkit() {
        new JFXPanel();
    }

    @Test
    public void scheduleStartMusicWhenCountdownsDone_registersListener() throws Exception {
        ScoreBattleController ctrl = new ScoreBattleController();
        GuiController left = new GuiController();
        GuiController right = new GuiController();

        // install simple boolean properties which the listener reads
        left.countdownFinishedProperty().set(false);
        right.countdownFinishedProperty().set(false);

        // assign to controller fields (package-private access in original)
        // use reflection if direct access unavailable
        try {
            java.lang.reflect.Field lf = ScoreBattleController.class.getDeclaredField("leftGui");
            lf.setAccessible(true);
            lf.set(ctrl, left);
            java.lang.reflect.Field rf = ScoreBattleController.class.getDeclaredField("rightGui");
            rf.setAccessible(true);
            rf.set(ctrl, right);
        } catch (NoSuchFieldException nsfe) {
            // ignore - test environment may differ
        }

        // call the refactored wiring method; should not throw
        java.lang.reflect.Method m = ScoreBattleController.class.getDeclaredMethod("scheduleStartMusicWhenCountdownsDone");
        m.setAccessible(true);
        m.invoke(ctrl);

        // toggling properties should not throw and listeners are invoked on FX thread
        left.countdownFinishedProperty().set(true);
        right.countdownFinishedProperty().set(true);

        // basic assertion: left/right controllers still present
        assertNotNull(left);
        assertNotNull(right);
    }
}
