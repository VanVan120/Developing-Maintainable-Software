package com.comp2042;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;

public class HandlingController {

    // Defaults
    private static final int DEFAULT_ARR = 50;
    private static final int DEFAULT_DAS = 120;
    private static final int DEFAULT_DCD = 20;
    private static final double DEFAULT_SDF = 1.0;
    private static final boolean DEFAULT_HARD = true;

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

    // current values
    private int arrMs = DEFAULT_ARR;
    private int dasMs = DEFAULT_DAS;
    private int dcdMs = DEFAULT_DCD;
    private double sdf = DEFAULT_SDF;
    private boolean hardDropEnabled = DEFAULT_HARD;

    @FXML
    public void initialize() {
        try {
            // ARR slider (0-500 ms)
            if (sldArr != null) {
                sldArr.setMin(0); sldArr.setMax(500); sldArr.setValue(arrMs);
                sldArr.valueProperty().addListener((obs, oldV, newV) -> {
                    arrMs = newV.intValue();
                    if (tfArr != null) tfArr.setText(String.valueOf(arrMs));
                });
            }
            if (tfArr != null) tfArr.setText(String.valueOf(arrMs));

            // DAS slider
            if (sldDas != null) {
                sldDas.setMin(0); sldDas.setMax(500); sldDas.setValue(dasMs);
                sldDas.valueProperty().addListener((obs, oldV, newV) -> {
                    dasMs = newV.intValue();
                    if (tfDas != null) tfDas.setText(String.valueOf(dasMs));
                });
            }
            if (tfDas != null) tfDas.setText(String.valueOf(dasMs));

            // DCD slider
            if (sldDcd != null) {
                sldDcd.setMin(0); sldDcd.setMax(500); sldDcd.setValue(dcdMs);
                sldDcd.valueProperty().addListener((obs, oldV, newV) -> {
                    dcdMs = newV.intValue();
                    if (tfDcd != null) tfDcd.setText(String.valueOf(dcdMs));
                });
            }
            if (tfDcd != null) tfDcd.setText(String.valueOf(dcdMs));

            // SDF slider (0.1 - 5.0)
            if (sldSdf != null) {
                sldSdf.setMin(0.1); sldSdf.setMax(5.0); sldSdf.setValue(sdf);
                sldSdf.valueProperty().addListener((obs, oldV, newV) -> {
                    sdf = Math.round(newV.doubleValue() * 10.0) / 10.0;
                    if (tfSdf != null) tfSdf.setText(String.valueOf(sdf));
                });
            }
            if (tfSdf != null) tfSdf.setText(String.valueOf(sdf));

            if (chkHardDrop != null) chkHardDrop.setSelected(hardDropEnabled);

            // Wire text fields to allow typing values
            setupTextField(tfArr, v -> {
                try { int x = Integer.parseInt(v); sldArr.setValue(Math.max(0, Math.min(500, x))); } catch (Exception ignored) {}
            });
            setupTextField(tfDas, v -> {
                try { int x = Integer.parseInt(v); sldDas.setValue(Math.max(0, Math.min(500, x))); } catch (Exception ignored) {}
            });
            setupTextField(tfDcd, v -> {
                try { int x = Integer.parseInt(v); sldDcd.setValue(Math.max(0, Math.min(500, x))); } catch (Exception ignored) {}
            });
            setupTextField(tfSdf, v -> {
                try { double x = Double.parseDouble(v); sldSdf.setValue(Math.max(0.1, Math.min(5.0, x))); } catch (Exception ignored) {}
            });

            if (chkHardDrop != null) chkHardDrop.setOnAction(ev -> { hardDropEnabled = chkHardDrop.isSelected(); });

            // Default handlers for action buttons (if visible)
            if (btnReset != null) btnReset.setOnAction(ev -> resetToDefaults());
            if (btnCancel != null) btnCancel.setOnAction(ev -> { /* will be wired by parent overlay */ });
            if (btnSave != null) btnSave.setOnAction(ev -> { /* parent overlay wires */ });

        } catch (Exception ignored) {}
    }

    private void setupTextField(TextField tf, java.util.function.Consumer<String> onEnter) {
        if (tf == null) return;
        tf.setOnAction(ev -> { if (onEnter != null) onEnter.accept(tf.getText()); });
        tf.focusedProperty().addListener((obs, oldV, newV) -> { if (!newV) { if (onEnter != null) onEnter.accept(tf.getText()); }});
    }

    public void init(int arrMs, int dasMs, int dcdMs, double sdf, boolean hardEnabled) {
        try {
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
        } catch (Exception ignored) {}
    }

    public void resetToDefaults() {
        init(DEFAULT_ARR, DEFAULT_DAS, DEFAULT_DCD, DEFAULT_SDF, DEFAULT_HARD);
    }

    public void hideActionButtons() {
        try {
            if (btnReset != null) { btnReset.setVisible(false); btnReset.setManaged(false); }
            if (btnSave != null) { btnSave.setVisible(false); btnSave.setManaged(false); }
            if (btnCancel != null) { btnCancel.setVisible(false); btnCancel.setManaged(false); }
            if (lblInfo != null) { lblInfo.setVisible(false); lblInfo.setManaged(false); }
        } catch (Exception ignored) {}
    }

    public void setHeaderText(String t) { try { if (lblHeader != null) lblHeader.setText(t != null ? t : ""); } catch (Exception ignored) {} }

    // Getters used by parent to persist
    public int getArrMs() { return arrMs; }
    public int getDasMs() { return dasMs; }
    public int getDcdMs() { return dcdMs; }
    public double getSdf() { return sdf; }
    public boolean isHardDropEnabled() { return hardDropEnabled; }
}
