package com.comp2042.controller.controls;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the controls UI. UI wiring and presentation live here.
 * Core key mapping logic has been moved to {@link KeyBindings} to follow SRP and improve testability.
 */
public class ControlsController {

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

    private KeyBindings keyBindings = new KeyBindings();
    private final Map<Button, Action> buttonToAction = new HashMap<>();
    private ControlsView view = null;
    private Button capturing = null;
    private java.util.function.Consumer<Boolean> closeHandler = null;
    
    private java.util.function.BiPredicate<javafx.scene.input.KeyCode, javafx.scene.control.Button> keyAvailabilityChecker = null;
    private static final Logger LOGGER = Logger.getLogger(ControlsController.class.getName());

    @FXML
    public void initialize() {
        // initialize view helper (requires FXML-injected fields)
        view = new ControlsView(lblInfo, lblHeader, btnReset, btnSave, btnCancel);

        // set default (non-interactive) labels
        view.setButtonKey(btnLeftDefault, keyBindings.getPanelDefault(Action.LEFT), false);
        view.setButtonKey(btnRightDefault, keyBindings.getPanelDefault(Action.RIGHT), false);
        view.setButtonKey(btnSoftDefault, keyBindings.getPanelDefault(Action.SOFT_DROP), false);
        view.setButtonKey(btnHardDefault, keyBindings.getPanelDefault(Action.HARD_DROP), false);
        view.setButtonKey(btnRotateDefault, keyBindings.getPanelDefault(Action.ROTATE), false);
        view.setButtonKey(btnSwitchDefault, keyBindings.getPanelDefault(Action.SWITCH), false);

        // set current (interactive) labels
        view.setButtonKey(btnLeftCurrent, keyBindings.get(Action.LEFT), true);
        view.setButtonKey(btnRightCurrent, keyBindings.get(Action.RIGHT), true);
        view.setButtonKey(btnSoftCurrent, keyBindings.get(Action.SOFT_DROP), true);
        view.setButtonKey(btnHardCurrent, keyBindings.get(Action.HARD_DROP), true);
        view.setButtonKey(btnRotateCurrent, keyBindings.get(Action.ROTATE), true);
        view.setButtonKey(btnSwitchCurrent, keyBindings.get(Action.SWITCH), true);

        // map buttons to actions
        buttonToAction.put(btnLeftCurrent, Action.LEFT);
        buttonToAction.put(btnRightCurrent, Action.RIGHT);
        buttonToAction.put(btnSoftCurrent, Action.SOFT_DROP);
        buttonToAction.put(btnHardCurrent, Action.HARD_DROP);
        buttonToAction.put(btnRotateCurrent, Action.ROTATE);
        buttonToAction.put(btnSwitchCurrent, Action.SWITCH);

        setupCapture(btnLeftCurrent);
        setupCapture(btnRightCurrent);
        setupCapture(btnSoftCurrent);
        setupCapture(btnHardCurrent);
        setupCapture(btnRotateCurrent);
        setupCapture(btnSwitchCurrent);

        view.addHoverEffect(btnLeftCurrent);
        view.addHoverEffect(btnRightCurrent);
        view.addHoverEffect(btnSoftCurrent);
        view.addHoverEffect(btnHardCurrent);
        view.addHoverEffect(btnRotateCurrent);
        view.addHoverEffect(btnSwitchCurrent);

        if (btnReset != null) {
            btnReset.setOnAction(ev -> resetAllToDefaults());
        }

        if (btnSave != null) {
            btnSave.setOnAction(ev -> {
                if (capturing != null) {
                    view.setButtonKey(capturing, getCurrentKeyCodeForButton(capturing), true); // Restore original text
                    capturing = null;
                    view.setInfoText("");
                }
                if (closeHandler != null) closeHandler.accept(Boolean.TRUE);
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnAction(ev -> {
                if (capturing != null) {
                    view.setButtonKey(capturing, getCurrentKeyCodeForButton(capturing), true); // Restore original text
                    capturing = null;
                    view.setInfoText("");
                }
                if (closeHandler != null) closeHandler.accept(Boolean.FALSE);
            });
        }

        if (btnSave != null) {
            btnSave.sceneProperty().addListener((obs, oldS, newS) -> {
                if (newS != null) {
                    newS.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
                }
            });
        }

        view.setInfoText("");
    }

    public void setDefaultKeys(KeyCode left, KeyCode right, KeyCode rotate, KeyCode down, KeyCode hard) {
        try {
            if (left != null) keyBindings.setPanelDefault(Action.LEFT, left);
            if (right != null) keyBindings.setPanelDefault(Action.RIGHT, right);
            if (rotate != null) keyBindings.setPanelDefault(Action.ROTATE, rotate);
            if (down != null) keyBindings.setPanelDefault(Action.SOFT_DROP, down);
            if (hard != null) keyBindings.setPanelDefault(Action.HARD_DROP, hard);
            if (keyBindings.getPanelDefault(Action.SWITCH) == null) keyBindings.setPanelDefault(Action.SWITCH, KeyCode.C);

            view.setButtonKey(btnLeftDefault, keyBindings.getPanelDefault(Action.LEFT), false);
            view.setButtonKey(btnRightDefault, keyBindings.getPanelDefault(Action.RIGHT), false);
            view.setButtonKey(btnSoftDefault, keyBindings.getPanelDefault(Action.SOFT_DROP), false);
            view.setButtonKey(btnHardDefault, keyBindings.getPanelDefault(Action.HARD_DROP), false);
            view.setButtonKey(btnRotateDefault, keyBindings.getPanelDefault(Action.ROTATE), false);
            view.setButtonKey(btnSwitchDefault, keyBindings.getPanelDefault(Action.SWITCH), false);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set default keys", e);
        }
    }

    public void setDefaultKeys(KeyCode left, KeyCode right, KeyCode rotate, KeyCode down, KeyCode hard, KeyCode sw) {
        try {
            if (left != null) keyBindings.setPanelDefault(Action.LEFT, left);
            if (right != null) keyBindings.setPanelDefault(Action.RIGHT, right);
            if (rotate != null) keyBindings.setPanelDefault(Action.ROTATE, rotate);
            if (down != null) keyBindings.setPanelDefault(Action.SOFT_DROP, down);
            if (hard != null) keyBindings.setPanelDefault(Action.HARD_DROP, hard);
            if (sw != null) keyBindings.setPanelDefault(Action.SWITCH, sw);

            view.setButtonKey(btnLeftDefault, keyBindings.getPanelDefault(Action.LEFT), false);
            view.setButtonKey(btnRightDefault, keyBindings.getPanelDefault(Action.RIGHT), false);
            view.setButtonKey(btnSoftDefault, keyBindings.getPanelDefault(Action.SOFT_DROP), false);
            view.setButtonKey(btnHardDefault, keyBindings.getPanelDefault(Action.HARD_DROP), false);
            view.setButtonKey(btnRotateDefault, keyBindings.getPanelDefault(Action.ROTATE), false);
            view.setButtonKey(btnSwitchDefault, keyBindings.getPanelDefault(Action.SWITCH), false);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set default keys (with switch)", e);
        }
    }

    public void setKeyAvailabilityChecker(java.util.function.BiPredicate<KeyCode, Button> checker) {
        this.keyAvailabilityChecker = checker;
    }

    // hover effect handled by ControlsView

    private void resetAllToDefaults() {
        keyBindings.resetToDefaults();
        view.setButtonKey(btnLeftCurrent, keyBindings.get(Action.LEFT), true);
        view.setButtonKey(btnRightCurrent, keyBindings.get(Action.RIGHT), true);
        view.setButtonKey(btnSoftCurrent, keyBindings.get(Action.SOFT_DROP), true);
        view.setButtonKey(btnHardCurrent, keyBindings.get(Action.HARD_DROP), true);
        view.setButtonKey(btnRotateCurrent, keyBindings.get(Action.ROTATE), true);
        view.setButtonKey(btnSwitchCurrent, keyBindings.get(Action.SWITCH), true);

        view.setInfoText("Reset to defaults");
    }

    // UI updates delegated to ControlsView

    private void setupCapture(Button b) {
        if (b == null) return;
        b.setOnAction(e -> {
            if (capturing != null && capturing != b) {
                view.setButtonKey(capturing, getCurrentKeyCodeForButton(capturing), true); // Restore text
            }

            capturing = b;
            view.setInfoText("Press a key to assign...");
            b.setText("...");
            b.requestFocus();
        });
    }

    private void onKeyPressed(KeyEvent ev) {
        if (capturing == null) return;

        KeyCode code = ev.getCode();
        if (code == KeyCode.SHIFT || code == KeyCode.CONTROL || code == KeyCode.ALT || code == KeyCode.META || code == KeyCode.ESCAPE) {
            view.setInfoText("Cannot assign modifier or Escape key.");
            view.setButtonKey(capturing, getCurrentKeyCodeForButton(capturing), true);
            capturing = null;
            ev.consume();
            return;
        }

        Action existing = keyBindings.findActionForKey(code);
        if (existing != null && !isSameActionAsCapturing(existing, capturing)) {
            String msg = "The key '" + code.getName() + "' is already assigned to '" + existing.getDisplayName() + "'. Please choose a different key.";
            view.showInlineWarning(msg);
            view.setButtonKey(capturing, getCurrentKeyCodeForButton(capturing), true);
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
                        view.showInlineWarning("Key already assigned to the other player. Please choose a different key.");
                        view.setButtonKey(capturing, getCurrentKeyCodeForButton(capturing), true);
                        capturing = null;
                        ev.consume();
                        return;
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error while checking key availability", e);
            }
        }

        assignToCurrentSlot(capturing, code);
        view.setInfoText("");
        capturing = null;
        ev.consume();
    }

    private void assignToCurrentSlot(Button b, KeyCode code) {
        if (b == null || code == null) return;
        Action a = buttonToAction.get(b);
        if (a == null) return;
        keyBindings.set(a, code);
        view.setButtonKey(b, code, true);
    }

    private KeyCode getCurrentKeyCodeForButton(Button b) {
        Action a = buttonToAction.get(b);
        return a != null ? keyBindings.get(a) : null;
    }



    private boolean isSameActionAsCapturing(Action existingAction, Button capturingButton) {
        if (existingAction == null || capturingButton == null) return false;
        Action expected = buttonToAction.get(capturingButton);
        return expected == existingAction;
    }

    public void init(KeyCode left, KeyCode right, KeyCode rotate, KeyCode down, KeyCode hard) {
        if (left != null) { keyBindings.set(Action.LEFT, left); view.setButtonKey(btnLeftCurrent, left, true); }
        if (right != null) { keyBindings.set(Action.RIGHT, right); view.setButtonKey(btnRightCurrent, right, true); }
        if (rotate != null) { keyBindings.set(Action.ROTATE, rotate); view.setButtonKey(btnRotateCurrent, rotate, true); }
        if (down != null) { keyBindings.set(Action.SOFT_DROP, down); view.setButtonKey(btnSoftCurrent, down, true); }
        if (hard != null) { keyBindings.set(Action.HARD_DROP, hard); view.setButtonKey(btnHardCurrent, hard, true); }
    }

    public void init(KeyCode left, KeyCode right, KeyCode rotate, KeyCode down, KeyCode hard, KeyCode sw) {
        init(left, right, rotate, down, hard);
        if (sw != null) { keyBindings.set(Action.SWITCH, sw); view.setButtonKey(btnSwitchCurrent, sw, true); }
    }

    public void setCloseHandler(java.util.function.Consumer<Boolean> handler) {
        this.closeHandler = handler;
    }

    public KeyCode getLeft() { return keyBindings.get(Action.LEFT); }
    public KeyCode getRight() { return keyBindings.get(Action.RIGHT); }
    public KeyCode getRotate() { return keyBindings.get(Action.ROTATE); }
    public KeyCode getDown() { return keyBindings.get(Action.SOFT_DROP); }
    public KeyCode getHard() { return keyBindings.get(Action.HARD_DROP); }
    public KeyCode getSwitch() { return keyBindings.get(Action.SWITCH); }

    public void hideActionButtons() {
        if (view != null) {
            view.hideActionButtons();
            return;
        }
        try {
            if (btnReset != null) { btnReset.setVisible(false); btnReset.setManaged(false); }
            if (btnSave != null) { btnSave.setVisible(false); btnSave.setManaged(false); }
            if (btnCancel != null) { btnCancel.setVisible(false); btnCancel.setManaged(false); }
            if (lblInfo != null) { lblInfo.setVisible(false); lblInfo.setManaged(false); }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to hide action buttons", e);
        }
    }

    public void resetToDefaults() {
        try {
            resetAllToDefaults();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to reset to defaults", e);
        }
    }

    public void resetToPanelDefaults() {
        try {
            keyBindings.resetToPanelDefaults();

            view.setButtonKey(btnLeftCurrent, keyBindings.get(Action.LEFT), true);
            view.setButtonKey(btnRightCurrent, keyBindings.get(Action.RIGHT), true);
            view.setButtonKey(btnSoftCurrent, keyBindings.get(Action.SOFT_DROP), true);
            view.setButtonKey(btnHardCurrent, keyBindings.get(Action.HARD_DROP), true);
            view.setButtonKey(btnRotateCurrent, keyBindings.get(Action.ROTATE), true);
            view.setButtonKey(btnSwitchCurrent, keyBindings.get(Action.SWITCH), true);
            view.setInfoText("Reset to panel defaults");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to reset to panel defaults", e);
        }
    }

    public void setHeaderText(String text) {
        try {
            if (lblHeader != null) lblHeader.setText(text != null ? text : "");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set header text", e);
        }
    }

    
}