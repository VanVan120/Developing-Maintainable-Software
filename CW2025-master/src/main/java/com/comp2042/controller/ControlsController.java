package com.comp2042.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.stage.Modality;
import javafx.util.Duration;

public class ControlsController {

    private static final KeyCode DEFAULT_LEFT = KeyCode.LEFT;
    private static final KeyCode DEFAULT_RIGHT = KeyCode.RIGHT;
    private static final KeyCode DEFAULT_SOFT = KeyCode.DOWN;
    private static final KeyCode DEFAULT_HARD = KeyCode.SPACE;
    private static final KeyCode DEFAULT_ROTATE = KeyCode.UP;
    private static final KeyCode DEFAULT_SWITCH = KeyCode.C;

    @FXML private Button btnLeftDefault;
    @FXML private Button btnRightDefault;
    @FXML private Button btnSoftDefault;
    @FXML private Button btnHardDefault;
    @FXML private Button btnRotateDefault;
    @FXML private Button btnSwitchDefault;

    @FXML private Button btnLeftCurrent;
    @FXML private Button btnRightCurrent;
    @FXML private Button btnSoftCurrent;
    @FXML private Button btnHardCurrent;
    @FXML private Button btnRotateCurrent;
    @FXML private Button btnSwitchCurrent;

    @FXML private Button btnReset;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private Label lblInfo;
    @FXML private Label lblHeader;

    private KeyCode currentLeft = DEFAULT_LEFT;
    private KeyCode currentRight = DEFAULT_RIGHT;
    private KeyCode currentSoft = DEFAULT_SOFT;
    private KeyCode currentHard = DEFAULT_HARD;
    private KeyCode currentRotate = DEFAULT_ROTATE;
    private KeyCode currentSwitch = DEFAULT_SWITCH;
    private KeyCode panelDefaultLeft = DEFAULT_LEFT;
    private KeyCode panelDefaultRight = DEFAULT_RIGHT;
    private KeyCode panelDefaultSoft = DEFAULT_SOFT;
    private KeyCode panelDefaultHard = DEFAULT_HARD;
    private KeyCode panelDefaultRotate = DEFAULT_ROTATE;
    private KeyCode panelDefaultSwitch = DEFAULT_SWITCH;

    private Button capturing = null;
    private java.util.function.Consumer<Boolean> closeHandler = null; 
    private Timeline warningTimeline = null;
    private java.util.function.BiPredicate<javafx.scene.input.KeyCode, javafx.scene.control.Button> keyAvailabilityChecker = null;

    @FXML
    public void initialize() {
    setButtonKey(btnLeftDefault, panelDefaultLeft, false);
    setButtonKey(btnRightDefault, panelDefaultRight, false);
    setButtonKey(btnSoftDefault, panelDefaultSoft, false);
    setButtonKey(btnHardDefault, panelDefaultHard, false);
    setButtonKey(btnRotateDefault, panelDefaultRotate, false);
    setButtonKey(btnSwitchDefault, panelDefaultSwitch, false);

    setButtonKey(btnLeftCurrent, currentLeft, true);
    setButtonKey(btnRightCurrent, currentRight, true);
    setButtonKey(btnSoftCurrent, currentSoft, true);
    setButtonKey(btnHardCurrent, currentHard, true);
    setButtonKey(btnRotateCurrent, currentRotate, true);
    setButtonKey(btnSwitchCurrent, currentSwitch, true);

    setupCapture(btnLeftCurrent);
    setupCapture(btnRightCurrent);
    setupCapture(btnSoftCurrent);
    setupCapture(btnHardCurrent);
    setupCapture(btnRotateCurrent);
    setupCapture(btnSwitchCurrent);

    addHoverEffect(btnLeftCurrent);
    addHoverEffect(btnRightCurrent);
    addHoverEffect(btnSoftCurrent);
    addHoverEffect(btnHardCurrent);
    addHoverEffect(btnRotateCurrent);
    addHoverEffect(btnSwitchCurrent);

        if (btnReset != null) {
            btnReset.setOnAction(ev -> {
                resetAllToDefaults();
            });
        }

        btnSave.setOnAction(ev -> {
            if (capturing != null) {
                setButtonKey(capturing, getCurrentKeyCodeForButton(capturing), true); // Restore original text
                capturing = null;
                setInfoText("");
            }
            if (closeHandler != null) closeHandler.accept(Boolean.TRUE);
        });

        btnCancel.setOnAction(ev -> {
             if (capturing != null) { 
                setButtonKey(capturing, getCurrentKeyCodeForButton(capturing), true); // Restore original text
                capturing = null;
                setInfoText("");
            }
            if (closeHandler != null) closeHandler.accept(Boolean.FALSE); 
        });

        if (btnSave != null) {
            btnSave.sceneProperty().addListener((obs, oldS, newS) -> {
                if (newS != null) {
                    newS.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
                }
            });
        }
        
        setInfoText(""); 
    }

    public void setDefaultKeys(KeyCode left, KeyCode right, KeyCode rotate, KeyCode down, KeyCode hard) {
        try {
            if (left != null) panelDefaultLeft = left;
            if (right != null) panelDefaultRight = right;
            if (rotate != null) panelDefaultRotate = rotate;
            if (down != null) panelDefaultSoft = down;
            if (hard != null) panelDefaultHard = hard;
            if (panelDefaultSwitch == null) panelDefaultSwitch = DEFAULT_SWITCH;

            setButtonKey(btnLeftDefault, panelDefaultLeft, false);
            setButtonKey(btnRightDefault, panelDefaultRight, false);
            setButtonKey(btnSoftDefault, panelDefaultSoft, false);
            setButtonKey(btnHardDefault, panelDefaultHard, false);
            setButtonKey(btnRotateDefault, panelDefaultRotate, false);
            setButtonKey(btnSwitchDefault, panelDefaultSwitch, false);
        } catch (Exception ignored) {}
    }

    public void setDefaultKeys(KeyCode left, KeyCode right, KeyCode rotate, KeyCode down, KeyCode hard, KeyCode sw) {
        try {
            if (left != null) panelDefaultLeft = left;
            if (right != null) panelDefaultRight = right;
            if (rotate != null) panelDefaultRotate = rotate;
            if (down != null) panelDefaultSoft = down;
            if (hard != null) panelDefaultHard = hard;
            if (sw != null) panelDefaultSwitch = sw;

            setButtonKey(btnLeftDefault, panelDefaultLeft, false);
            setButtonKey(btnRightDefault, panelDefaultRight, false);
            setButtonKey(btnSoftDefault, panelDefaultSoft, false);
            setButtonKey(btnHardDefault, panelDefaultHard, false);
            setButtonKey(btnRotateDefault, panelDefaultRotate, false);
            setButtonKey(btnSwitchDefault, panelDefaultSwitch, false);
        } catch (Exception ignored) {}
    }

    public void setKeyAvailabilityChecker(java.util.function.BiPredicate<KeyCode, Button> checker) {
        this.keyAvailabilityChecker = checker;
    }

    private void addHoverEffect(Button b) {
        if (b == null) return;
        b.setOnMouseEntered(e -> {
            if (!b.getStyleClass().contains("keybind-hover")) b.getStyleClass().add("keybind-hover");
        });
        b.setOnMouseExited(e -> {
            b.getStyleClass().remove("keybind-hover");
        });
    }

    private void resetAllToDefaults() {
        currentLeft = DEFAULT_LEFT;
        currentRight = DEFAULT_RIGHT;
        currentSoft = DEFAULT_SOFT;
        currentHard = DEFAULT_HARD;
        currentRotate = DEFAULT_ROTATE;
        currentSwitch = DEFAULT_SWITCH;

        setButtonKey(btnLeftCurrent, currentLeft, true);
        setButtonKey(btnRightCurrent, currentRight, true);
        setButtonKey(btnSoftCurrent, currentSoft, true);
        setButtonKey(btnHardCurrent, currentHard, true);
        setButtonKey(btnRotateCurrent, currentRotate, true);
    setButtonKey(btnSwitchCurrent, currentSwitch, true);

        setInfoText("Reset to defaults");
    }

    private void setButtonKey(Button b, KeyCode code, boolean isInteractive) {
        if (b == null) return;
        b.setText(code != null ? code.getName() : "[NOT SET]");
        if (!isInteractive) {
            b.setDisable(true); 
        } else {
             b.setDisable(false);
        }
    }

    private void setupCapture(Button b) {
        if (b == null) return;
        b.setOnAction(e -> {
             if (capturing != null && capturing != b) {
                 setButtonKey(capturing, getCurrentKeyCodeForButton(capturing), true); // Restore text
             }

            capturing = b; 
            setInfoText("Press a key to assign..."); 
            b.setText("..."); 
            b.requestFocus(); 
        });
    }

    private void onKeyPressed(KeyEvent ev) {
        if (capturing == null) return;

        KeyCode code = ev.getCode();
        if (code == KeyCode.SHIFT || code == KeyCode.CONTROL || code == KeyCode.ALT || code == KeyCode.META || code == KeyCode.ESCAPE) {
             setInfoText("Cannot assign modifier or Escape key.");
             setButtonKey(capturing, getCurrentKeyCodeForButton(capturing), true); 
             capturing = null;
             ev.consume();
             return;
        }

        String existing = findActionForKey(code);
        if (existing != null && !isSameActionAsCapturing(existing, capturing)) {
            String msg = "The key '" + code.getName() + "' is already assigned to '" + existing + "'. Please choose a different key.";
            showInlineWarning(msg);
            setButtonKey(capturing, getCurrentKeyCodeForButton(capturing), true);
            capturing = null;
            ev.consume();
            return;
        }

        if (keyAvailabilityChecker != null) {
            try {
                KeyCode existingForThisButton = getCurrentKeyCodeForButton(capturing);
                if (!(existingForThisButton != null && existingForThisButton.equals(code))) {
                    boolean ok = keyAvailabilityChecker.test(code, capturing);
                    if (!ok) {
                        showInlineWarning("Key already assigned to the other player. Please choose a different key.");
                        setButtonKey(capturing, getCurrentKeyCodeForButton(capturing), true);
                        capturing = null;
                        ev.consume();
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        assignToCurrentSlot(capturing, code); 
        setInfoText(""); 
        capturing = null; 
        ev.consume(); 
    }

    private void assignToCurrentSlot(Button b, KeyCode code) {
        if (b == null || code == null) return;
        if (b == btnLeftCurrent) { currentLeft = code; }
        else if (b == btnRightCurrent) { currentRight = code; }
        else if (b == btnSoftCurrent) { currentSoft = code; }
        else if (b == btnHardCurrent) { currentHard = code; }
        else if (b == btnRotateCurrent) { currentRotate = code; }
        else if (b == btnSwitchCurrent) { currentSwitch = code; }
        
        setButtonKey(b, code, true);
    }

    private KeyCode getCurrentKeyCodeForButton(Button b) {
        if (b == btnLeftCurrent) return currentLeft;
        if (b == btnRightCurrent) return currentRight;
        if (b == btnSoftCurrent) return currentSoft;
        if (b == btnHardCurrent) return currentHard;
        if (b == btnRotateCurrent) return currentRotate;
        if (b == btnSwitchCurrent) return currentSwitch;
        return null; 
    }

    private void showInlineWarning(String message) {
        if (lblInfo == null) return;
        setInfoText(message);
        if (!lblInfo.getStyleClass().contains("warning")) lblInfo.getStyleClass().add("warning");
        if (warningTimeline != null) warningTimeline.stop();
        warningTimeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> {
            lblInfo.getStyleClass().remove("warning");
            setInfoText("");
        }));
        warningTimeline.setCycleCount(1);
        warningTimeline.play();

        try {
            javafx.application.Platform.runLater(() -> {
                try {
                    java.awt.EventQueue.invokeLater(() -> {}); 
                } catch (Exception ignored) {}
                try {
                    Alert a = new Alert(Alert.AlertType.WARNING);
                    try {
                        if (lblInfo.getScene() != null && lblInfo.getScene().getWindow() != null) {
                            a.initOwner(lblInfo.getScene().getWindow());
                        }
                    } catch (Exception ignored) {}
                    a.initModality(Modality.APPLICATION_MODAL);
                    a.setTitle("Key already assigned");
                    a.setHeaderText("Key already assigned");
                    a.setContentText(message);
                    a.showAndWait();
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    private String findActionForKey(KeyCode code) {
        if (code == null) return null;
        if (code.equals(currentLeft)) return "Left";
        if (code.equals(currentRight)) return "Right";
        if (code.equals(currentSoft)) return "Soft Drop";
        if (code.equals(currentHard)) return "Hard Drop";
        if (code.equals(currentRotate)) return "Rotate";
        if (code.equals(currentSwitch)) return "Switch";
        return null;
    }

    private boolean isSameActionAsCapturing(String existingActionName, Button capturingButton) {
        if (existingActionName == null || capturingButton == null) return false;
        switch (existingActionName) {
            case "Left": return capturingButton == btnLeftCurrent;
            case "Right": return capturingButton == btnRightCurrent;
            case "Soft Drop": return capturingButton == btnSoftCurrent;
            case "Hard Drop": return capturingButton == btnHardCurrent;
            case "Rotate": return capturingButton == btnRotateCurrent;
            case "Switch": return capturingButton == btnSwitchCurrent;
            default: return false;
        }
    }

    public void init(KeyCode left, KeyCode right, KeyCode rotate, KeyCode down, KeyCode hard) {
        if (left != null) { currentLeft = left; setButtonKey(btnLeftCurrent, left, true); }
        if (right != null) { currentRight = right; setButtonKey(btnRightCurrent, right, true); }
        if (rotate != null) { currentRotate = rotate; setButtonKey(btnRotateCurrent, rotate, true); }
        if (down != null) { currentSoft = down; setButtonKey(btnSoftCurrent, down, true); }
        if (hard != null) { currentHard = hard; setButtonKey(btnHardCurrent, hard, true); }
    }

    public void init(KeyCode left, KeyCode right, KeyCode rotate, KeyCode down, KeyCode hard, KeyCode sw) {
        init(left, right, rotate, down, hard);
        if (sw != null) { currentSwitch = sw; setButtonKey(btnSwitchCurrent, sw, true); }
    }

    public void setCloseHandler(java.util.function.Consumer<Boolean> handler) {
        this.closeHandler = handler;
    }

    public KeyCode getLeft() { return currentLeft; }
    public KeyCode getRight() { return currentRight; }
    public KeyCode getRotate() { return currentRotate; }
    public KeyCode getDown() { return currentSoft; }
    public KeyCode getHard() { return currentHard; }
    public KeyCode getSwitch() { return currentSwitch; }

    public void hideActionButtons() {
        try {
            if (btnReset != null) { btnReset.setVisible(false); btnReset.setManaged(false); }
            if (btnSave != null) { btnSave.setVisible(false); btnSave.setManaged(false); }
            if (btnCancel != null) { btnCancel.setVisible(false); btnCancel.setManaged(false); }
            if (lblInfo != null) { lblInfo.setVisible(false); lblInfo.setManaged(false); }
        } catch (Exception ignored) {}
    }

    public void resetToDefaults() {
        try {
            resetAllToDefaults();
        } catch (Exception ignored) {}
    }

    public void resetToPanelDefaults() {
        try {
            currentLeft = (panelDefaultLeft != null) ? panelDefaultLeft : DEFAULT_LEFT;
            currentRight = (panelDefaultRight != null) ? panelDefaultRight : DEFAULT_RIGHT;
            currentSoft = (panelDefaultSoft != null) ? panelDefaultSoft : DEFAULT_SOFT;
            currentHard = (panelDefaultHard != null) ? panelDefaultHard : DEFAULT_HARD;
            currentRotate = (panelDefaultRotate != null) ? panelDefaultRotate : DEFAULT_ROTATE;
            currentSwitch = (panelDefaultSwitch != null) ? panelDefaultSwitch : DEFAULT_SWITCH;

            setButtonKey(btnLeftCurrent, currentLeft, true);
            setButtonKey(btnRightCurrent, currentRight, true);
            setButtonKey(btnSoftCurrent, currentSoft, true);
            setButtonKey(btnHardCurrent, currentHard, true);
            setButtonKey(btnRotateCurrent, currentRotate, true);
            setButtonKey(btnSwitchCurrent, currentSwitch, true);
            setInfoText("Reset to panel defaults");
        } catch (Exception ignored) {}
    }

    public void setHeaderText(String text) {
        try {
            if (lblHeader != null) lblHeader.setText(text != null ? text : "");
        } catch (Exception ignored) {}
    }

    private void setInfoText(String text) {
        if (lblInfo != null) {
            lblInfo.setText(text != null ? text : "");
        }
    }
}