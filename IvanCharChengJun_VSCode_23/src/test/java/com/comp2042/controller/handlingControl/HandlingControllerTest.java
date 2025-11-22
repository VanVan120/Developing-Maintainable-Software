package com.comp2042.controller.handlingControl;

import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class HandlingControllerTest {

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @BeforeAll
    public static void initToolkit() {
        // Start JavaFX toolkit for tests that touch JavaFX objects
        new JFXPanel();
    }

    @Test
    public void init_updatesControlsAndGetters() throws Exception {
        HandlingController ctrl = new HandlingController();

        // create controls and inject
        Slider sldArr = new Slider();
        TextField tfArr = new TextField();
        Slider sldDas = new Slider();
        TextField tfDas = new TextField();
        Slider sldDcd = new Slider();
        TextField tfDcd = new TextField();
        Slider sldSdf = new Slider();
        TextField tfSdf = new TextField();
        CheckBox chkHard = new CheckBox();

        setField(ctrl, "sldArr", sldArr);
        setField(ctrl, "tfArr", tfArr);
        setField(ctrl, "sldDas", sldDas);
        setField(ctrl, "tfDas", tfDas);
        setField(ctrl, "sldDcd", sldDcd);
        setField(ctrl, "tfDcd", tfDcd);
        setField(ctrl, "sldSdf", sldSdf);
        setField(ctrl, "tfSdf", tfSdf);
        setField(ctrl, "chkHardDrop", chkHard);

        // Initialize controller to wire up listeners
        ctrl.initialize();

        // Provide values
        ctrl.init(10, 20, 30, 1.2, false);

        // Verify getters
        assertEquals(10, ctrl.getArrMs());
        assertEquals(20, ctrl.getDasMs());
        assertEquals(30, ctrl.getDcdMs());
        assertEquals(1.2, ctrl.getSdf(), 0.0001);
        assertFalse(ctrl.isHardDropEnabled());

        // Verify controls were updated
        assertEquals(10, (int) sldArr.getValue());
        assertEquals("10", tfArr.getText());
        assertEquals(20, (int) sldDas.getValue());
        assertEquals("20", tfDas.getText());
        assertEquals(30, (int) sldDcd.getValue());
        assertEquals("30", tfDcd.getText());
        // SDF text uses rounding to 1 decimal
        assertEquals(1.2, Math.round(sldSdf.getValue() * 10.0) / 10.0, 0.0001);
        assertEquals(String.valueOf(Math.round(1.2 * 10.0) / 10.0), tfSdf.getText());
        assertFalse(chkHard.isSelected());
    }

    @Test
    public void resetToDefaults_restoresDefaultValues() throws Exception {
        HandlingController ctrl = new HandlingController();

        Slider sldArr = new Slider();
        TextField tfArr = new TextField();
        CheckBox chkHard = new CheckBox();

        setField(ctrl, "sldArr", sldArr);
        setField(ctrl, "tfArr", tfArr);
        setField(ctrl, "chkHardDrop", chkHard);

        ctrl.initialize();

        // Change values then reset
        ctrl.init(5, 5, 5, 0.5, false);
        ctrl.resetToDefaults();

        // Defaults as declared in the class
        assertEquals(50, ctrl.getArrMs());
        assertEquals(120, ctrl.getDasMs());
        assertEquals(20, ctrl.getDcdMs());
        assertEquals(1.0, ctrl.getSdf(), 0.0001);
        assertTrue(ctrl.isHardDropEnabled());
    }

    @Test
    public void hideActionButtons_hidesButtonsAndInfo() throws Exception {
        HandlingController ctrl = new HandlingController();

        Button btnReset = new Button();
        Button btnSave = new Button();
        Button btnCancel = new Button();
        Label lblInfo = new Label();

        setField(ctrl, "btnReset", btnReset);
        setField(ctrl, "btnSave", btnSave);
        setField(ctrl, "btnCancel", btnCancel);
        setField(ctrl, "lblInfo", lblInfo);

        ctrl.hideActionButtons();

        assertFalse(btnReset.isVisible());
        assertFalse(btnReset.isManaged());
        assertFalse(btnSave.isVisible());
        assertFalse(btnSave.isManaged());
        assertFalse(btnCancel.isVisible());
        assertFalse(btnCancel.isManaged());
        assertFalse(lblInfo.isVisible());
        assertFalse(lblInfo.isManaged());
    }

    @Test
    public void setHeaderText_updatesLabel_or_clearsOnNull() throws Exception {
        HandlingController ctrl = new HandlingController();
        Label lbl = new Label();
        setField(ctrl, "lblHeader", lbl);

        ctrl.setHeaderText("Hello");
        assertEquals("Hello", lbl.getText());

        ctrl.setHeaderText(null);
        assertEquals("", lbl.getText());
    }
}
