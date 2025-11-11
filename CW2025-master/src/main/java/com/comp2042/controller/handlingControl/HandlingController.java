package com.comp2042.controller.handlingControl;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;

import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HandlingController {

    // Defaults
    private static final int DEFAULT_ARR = 50;
    private static final int DEFAULT_DAS = 120;
    private static final int DEFAULT_DCD = 20;
    private static final double DEFAULT_SDF = 1.0;
    private static final boolean DEFAULT_HARD = true;

    private static final Logger LOGGER = Logger.getLogger(HandlingController.class.getName());

    @FXML private Label lblHeader;
    @FXML private Label lblInfo;
    @FXML private Slider sldArr;
    @FXML private TextField tfArr;
    @FXML private Slider sldDas;
    @FXML private TextField tfDas;
    @FXML private Slider sldDcd;
    @FXML private TextField tfDcd;
    @FXML private Slider sldSdf;
    @FXML private TextField tfSdf;
    @FXML private CheckBox chkHardDrop;
    @FXML private Button btnReset;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private int arrMs = DEFAULT_ARR;
    private int dasMs = DEFAULT_DAS;
    private int dcdMs = DEFAULT_DCD;
    private double sdf = DEFAULT_SDF;
    private boolean hardDropEnabled = DEFAULT_HARD;

    @FXML
    public void initialize() {
        // Configure integer sliders (min, max, initial)
        configureIntSliderWithText(sldArr, tfArr, 0, 500, arrMs, v -> {
            arrMs = v;
        });

        configureIntSliderWithText(sldDas, tfDas, 0, 500, dasMs, v -> {
            dasMs = v;
        });

        configureIntSliderWithText(sldDcd, tfDcd, 0, 500, dcdMs, v -> {
            dcdMs = v;
        });

        // Configure double slider for SDF (0.1 - 5.0)
        configureDoubleSliderWithText(sldSdf, tfSdf, 0.1, 5.0, sdf, v -> {
            // round to 1 decimal place to keep original behavior
            sdf = Math.round(v * 10.0) / 10.0;
        });

        if (chkHardDrop != null) {
            chkHardDrop.setSelected(hardDropEnabled);
            chkHardDrop.setOnAction(ev -> hardDropEnabled = chkHardDrop.isSelected());
        }

        // Default handlers for action buttons (if visible)
        if (btnReset != null) btnReset.setOnAction(ev -> resetToDefaults());
        if (btnCancel != null) btnCancel.setOnAction(ev -> {});
        if (btnSave != null) btnSave.setOnAction(ev -> {});
    }

    private void configureIntSliderWithText(Slider slider, TextField tf, int min, int max, int initialValue, IntConsumer onChange) {
        if (slider != null) {
            slider.setMin(min);
            slider.setMax(max);
            slider.setValue(initialValue);
            slider.valueProperty().addListener((obs, oldV, newV) -> {
                int v = newV.intValue();
                try {
                    if (onChange != null) onChange.accept(v);
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Error in int slider onChange", ex);
                }
                if (tf != null) tf.setText(String.valueOf(v));
            });
        }
        if (tf != null) tf.setText(String.valueOf(initialValue));

        // When user edits text field, update slider value (validate numeric)
        setupTextField(tf, v -> {
            try {
                int x = Integer.parseInt(v);
                int clamped = Math.max(min, Math.min(max, x));
                if (slider != null) slider.setValue(clamped);
            } catch (NumberFormatException ex) {
                // invalid input: restore displayed value
                if (tf != null) tf.setText(String.valueOf((int) (slider != null ? slider.getValue() : initialValue)));
                LOGGER.log(Level.FINER, "Invalid integer entered in text field", ex);
            }
        });
    }

    private void configureDoubleSliderWithText(Slider slider, TextField tf, double min, double max, double initialValue, DoubleConsumer onChange) {
        if (slider != null) {
            slider.setMin(min);
            slider.setMax(max);
            slider.setValue(initialValue);
            slider.valueProperty().addListener((obs, oldV, newV) -> {
                double v = newV.doubleValue();
                try {
                    if (onChange != null) onChange.accept(v);
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Error in double slider onChange", ex);
                }
                if (tf != null) tf.setText(String.valueOf(Math.round(v * 10.0) / 10.0));
            });
        }
        if (tf != null) tf.setText(String.valueOf(Math.round(initialValue * 10.0) / 10.0));

        setupTextField(tf, v -> {
            try {
                double x = Double.parseDouble(v);
                double clamped = Math.max(min, Math.min(max, x));
                if (slider != null) slider.setValue(clamped);
            } catch (NumberFormatException ex) {
                if (tf != null) tf.setText(String.valueOf(Math.round((slider != null ? slider.getValue() : initialValue) * 10.0) / 10.0));
                LOGGER.log(Level.FINER, "Invalid double entered in text field", ex);
            }
        });
    }

    private void setupTextField(TextField tf, Consumer<String> onEnter) {
        if (tf == null) return;
        tf.setOnAction(ev -> { if (onEnter != null) onEnter.accept(tf.getText()); });
        tf.focusedProperty().addListener((obs, oldV, newV) -> { if (!newV) { if (onEnter != null) onEnter.accept(tf.getText()); }});
    }

    public void init(int arrMs, int dasMs, int dcdMs, double sdf, boolean hardEnabled) {
        this.arrMs = arrMs;
        this.dasMs = dasMs;
        this.dcdMs = dcdMs;
        this.sdf = sdf;
        this.hardDropEnabled = hardEnabled;

        if (sldArr != null) sldArr.setValue(this.arrMs);
        if (sldDas != null) sldDas.setValue(this.dasMs);
        if (sldDcd != null) sldDcd.setValue(this.dcdMs);
        if (sldSdf != null) sldSdf.setValue(this.sdf);

        if (tfArr != null) tfArr.setText(String.valueOf(this.arrMs));
        if (tfDas != null) tfDas.setText(String.valueOf(this.dasMs));
        if (tfDcd != null) tfDcd.setText(String.valueOf(this.dcdMs));
        if (tfSdf != null) tfSdf.setText(String.valueOf(this.sdf));
        if (chkHardDrop != null) chkHardDrop.setSelected(this.hardDropEnabled);
    }

    public void resetToDefaults() {
        init(DEFAULT_ARR, DEFAULT_DAS, DEFAULT_DCD, DEFAULT_SDF, DEFAULT_HARD);
    }

    public void hideActionButtons() {
        if (btnReset != null) { btnReset.setVisible(false); btnReset.setManaged(false); }
        if (btnSave != null) { btnSave.setVisible(false); btnSave.setManaged(false); }
        if (btnCancel != null) { btnCancel.setVisible(false); btnCancel.setManaged(false); }
        if (lblInfo != null) { lblInfo.setVisible(false); lblInfo.setManaged(false); }
    }

    public void setHeaderText(String t) {
        if (lblHeader != null) lblHeader.setText(t != null ? t : "");
    }

    // Getters used by parent to persist
    public int getArrMs() { return arrMs; }
    public int getDasMs() { return dasMs; }
    public int getDcdMs() { return dcdMs; }
    public double getSdf() { return sdf; }
    public boolean isHardDropEnabled() { return hardDropEnabled; }
}
