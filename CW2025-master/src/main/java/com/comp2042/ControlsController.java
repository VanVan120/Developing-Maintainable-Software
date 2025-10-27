package com.comp2042;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class ControlsController {

    // --- Define Default Keys ---
    private static final KeyCode DEFAULT_LEFT = KeyCode.LEFT;
    private static final KeyCode DEFAULT_RIGHT = KeyCode.RIGHT;
    private static final KeyCode DEFAULT_SOFT = KeyCode.DOWN;
    private static final KeyCode DEFAULT_HARD = KeyCode.SPACE;
    private static final KeyCode DEFAULT_ROTATE = KeyCode.UP;
    private static final KeyCode DEFAULT_SWITCH = KeyCode.C;

    // --- FXML Bindings ---

    // Default Buttons (Display Only)
    @FXML private Button btnLeftDefault;
    @FXML private Button btnRightDefault;
    @FXML private Button btnSoftDefault;
    @FXML private Button btnHardDefault;
    @FXML private Button btnRotateDefault;
    @FXML private Button btnSwitchDefault;

    // Current Buttons (Interactive)
    @FXML private Button btnLeftCurrent;
    @FXML private Button btnRightCurrent;
    @FXML private Button btnSoftCurrent;
    @FXML private Button btnHardCurrent;
    @FXML private Button btnRotateCurrent;
    @FXML private Button btnSwitchCurrent;

    // Other Controls
    @FXML private Button btnReset;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private Label lblInfo;
    @FXML private Label lblHeader;

    // --- Current KeyCode Storage ---
    // Initialize current keys to defaults
    private KeyCode currentLeft = DEFAULT_LEFT;
    private KeyCode currentRight = DEFAULT_RIGHT;
    private KeyCode currentSoft = DEFAULT_SOFT;
    private KeyCode currentHard = DEFAULT_HARD;
    private KeyCode currentRotate = DEFAULT_ROTATE;
    private KeyCode currentSwitch = DEFAULT_SWITCH;
    // Panel-specific "default" values (used when embedding controls for other players)
    private KeyCode panelDefaultLeft = DEFAULT_LEFT;
    private KeyCode panelDefaultRight = DEFAULT_RIGHT;
    private KeyCode panelDefaultSoft = DEFAULT_SOFT;
    private KeyCode panelDefaultHard = DEFAULT_HARD;
    private KeyCode panelDefaultRotate = DEFAULT_ROTATE;
    private KeyCode panelDefaultSwitch = DEFAULT_SWITCH;

    // --- State Variables ---
    private Button capturing = null; // Track which button is waiting for key input
    private java.util.function.Consumer<Boolean> closeHandler = null; // Callback for MainMenuController
    private Timeline warningTimeline = null; // used to clear inline warnings after a delay

    @FXML
    public void initialize() {
        // 1. Populate Default Buttons (and make them non-interactive)
    setButtonKey(btnLeftDefault, panelDefaultLeft, false);
    setButtonKey(btnRightDefault, panelDefaultRight, false);
    setButtonKey(btnSoftDefault, panelDefaultSoft, false);
    setButtonKey(btnHardDefault, panelDefaultHard, false);
    setButtonKey(btnRotateDefault, panelDefaultRotate, false);
    setButtonKey(btnSwitchDefault, panelDefaultSwitch, false);

        // 2. Populate Current Buttons with initial values (defaults or loaded)
    setButtonKey(btnLeftCurrent, currentLeft, true);
    setButtonKey(btnRightCurrent, currentRight, true);
    setButtonKey(btnSoftCurrent, currentSoft, true);
    setButtonKey(btnHardCurrent, currentHard, true);
    setButtonKey(btnRotateCurrent, currentRotate, true);
    setButtonKey(btnSwitchCurrent, currentSwitch, true);

        // 3. Setup Capture Handlers for CURRENT buttons only
    setupCapture(btnLeftCurrent);
    setupCapture(btnRightCurrent);
    setupCapture(btnSoftCurrent);
    setupCapture(btnHardCurrent);
    setupCapture(btnRotateCurrent);
    setupCapture(btnSwitchCurrent);

        // 3b. Add hover visual feedback for current buttons
    addHoverEffect(btnLeftCurrent);
    addHoverEffect(btnRightCurrent);
    addHoverEffect(btnSoftCurrent);
    addHoverEffect(btnHardCurrent);
    addHoverEffect(btnRotateCurrent);
    addHoverEffect(btnSwitchCurrent);

        // Reset handler: restore defaults
        if (btnReset != null) {
            btnReset.setOnAction(ev -> {
                resetAllToDefaults();
            });
        }

        // 4. Setup Save/Cancel Handlers
        btnSave.setOnAction(ev -> {
            if (capturing != null) { // If capturing, cancel it first
                setButtonKey(capturing, getCurrentKeyCodeForButton(capturing), true); // Restore original text
                capturing = null;
                setInfoText("");
            }
            if (closeHandler != null) closeHandler.accept(Boolean.TRUE); // Signal save
        });

        btnCancel.setOnAction(ev -> {
             if (capturing != null) { // If capturing, cancel it
                setButtonKey(capturing, getCurrentKeyCodeForButton(capturing), true); // Restore original text
                capturing = null;
                setInfoText("");
            }
            if (closeHandler != null) closeHandler.accept(Boolean.FALSE); // Signal cancel
        });

        // 5. Add Scene-Level Key Listener for capturing input
        // Use btnSave as it's guaranteed to be part of the scene initially
        if (btnSave != null) {
            btnSave.sceneProperty().addListener((obs, oldS, newS) -> {
                if (newS != null) {
                    newS.addEventHandler(KeyEvent.KEY_PRESSED, this::onKeyPressed);
                }
                // Optional: Remove handler from old scene if needed
                // if (oldS != null) {
                //    oldS.removeEventHandler(KeyEvent.KEY_PRESSED, this::onKeyPressed);
                // }
            });
        }
        
        setInfoText(""); // Initial info text
    }

    /**
     * Override the default column values for this Controls panel.
     * Useful when embedding two Controls panes (eg. left/right players) so the "Default" column
     * shows per-player defaults (WASD, arrows, etc) instead of the global application defaults.
     */
    public void setDefaultKeys(KeyCode left, KeyCode right, KeyCode rotate, KeyCode down, KeyCode hard) {
        try {
            if (left != null) panelDefaultLeft = left;
            if (right != null) panelDefaultRight = right;
            if (rotate != null) panelDefaultRotate = rotate;
            if (down != null) panelDefaultSoft = down;
            if (hard != null) panelDefaultHard = hard;
            // Keep switch default as-is when caller doesn't provide one
            if (panelDefaultSwitch == null) panelDefaultSwitch = DEFAULT_SWITCH;

            // Update the UI default buttons to show these values
            setButtonKey(btnLeftDefault, panelDefaultLeft, false);
            setButtonKey(btnRightDefault, panelDefaultRight, false);
            setButtonKey(btnSoftDefault, panelDefaultSoft, false);
            setButtonKey(btnHardDefault, panelDefaultHard, false);
            setButtonKey(btnRotateDefault, panelDefaultRotate, false);
            setButtonKey(btnSwitchDefault, panelDefaultSwitch, false);
        } catch (Exception ignored) {}
    }

    /** Overloaded setDefaultKeys that also accepts a Switch key value. */
    public void setDefaultKeys(KeyCode left, KeyCode right, KeyCode rotate, KeyCode down, KeyCode hard, KeyCode sw) {
        try {
            if (left != null) panelDefaultLeft = left;
            if (right != null) panelDefaultRight = right;
            if (rotate != null) panelDefaultRotate = rotate;
            if (down != null) panelDefaultSoft = down;
            if (hard != null) panelDefaultHard = hard;
            if (sw != null) panelDefaultSwitch = sw;

            // Update the UI default buttons to show these values
            setButtonKey(btnLeftDefault, panelDefaultLeft, false);
            setButtonKey(btnRightDefault, panelDefaultRight, false);
            setButtonKey(btnSoftDefault, panelDefaultSoft, false);
            setButtonKey(btnHardDefault, panelDefaultHard, false);
            setButtonKey(btnRotateDefault, panelDefaultRotate, false);
            setButtonKey(btnSwitchDefault, panelDefaultSwitch, false);
        } catch (Exception ignored) {}
    }

    /** Adds a subtle hover effect by toggling a helper CSS class. */
    private void addHoverEffect(Button b) {
        if (b == null) return;
        b.setOnMouseEntered(e -> {
            if (!b.getStyleClass().contains("keybind-hover")) b.getStyleClass().add("keybind-hover");
        });
        b.setOnMouseExited(e -> {
            b.getStyleClass().remove("keybind-hover");
        });
    }

    /** Resets all current keybindings to the application's defaults and updates UI. */
    private void resetAllToDefaults() {
        currentLeft = DEFAULT_LEFT;
        currentRight = DEFAULT_RIGHT;
        currentSoft = DEFAULT_SOFT;
        currentHard = DEFAULT_HARD;
        currentRotate = DEFAULT_ROTATE;
        currentSwitch = DEFAULT_SWITCH;

        // Update current column buttons
        setButtonKey(btnLeftCurrent, currentLeft, true);
        setButtonKey(btnRightCurrent, currentRight, true);
        setButtonKey(btnSoftCurrent, currentSoft, true);
        setButtonKey(btnHardCurrent, currentHard, true);
        setButtonKey(btnRotateCurrent, currentRotate, true);
    setButtonKey(btnSwitchCurrent, currentSwitch, true);

        setInfoText("Reset to defaults");
    }

    /**
     * Sets the text of a button to the KeyCode name and optionally makes it non-interactive.
     */
    private void setButtonKey(Button b, KeyCode code, boolean isInteractive) {
        if (b == null) return;
        b.setText(code != null ? code.getName() : "[NOT SET]");
        if (!isInteractive) {
            b.setDisable(true); // Make default buttons non-clickable
            // Optional: Add a specific style class via CSS or inline style
            // b.getStyleClass().add("keybind-button-default"); // Or use the one from FXML
        } else {
             b.setDisable(false);
             // b.getStyleClass().add("keybind-button-current"); // Or use the one from FXML
        }
    }

    /**
     * Sets up a button to enter "capture mode" when clicked.
     */
    private void setupCapture(Button b) {
        if (b == null) return;
        b.setOnAction(e -> {
            // If already capturing another button, cancel it first
             if (capturing != null && capturing != b) {
                 setButtonKey(capturing, getCurrentKeyCodeForButton(capturing), true); // Restore text
             }

            capturing = b; // Set this button as the one waiting for input
            setInfoText("Press a key to assign..."); // Update info label
            b.setText("..."); // Indicate waiting state
            // Request focus so key events are likely captured by the scene
            b.requestFocus(); 
        });
    }

    /**
     * Handles key presses when a button is in "capture mode".
     */
    private void onKeyPressed(KeyEvent ev) {
        if (capturing == null) return; // Only act if a button is waiting

        KeyCode code = ev.getCode();

        // Prevent assigning common modifier keys or Escape if desired
        if (code == KeyCode.SHIFT || code == KeyCode.CONTROL || code == KeyCode.ALT || code == KeyCode.META || code == KeyCode.ESCAPE) {
             setInfoText("Cannot assign modifier or Escape key.");
             // Restore original text
             setButtonKey(capturing, getCurrentKeyCodeForButton(capturing), true); 
             capturing = null;
             ev.consume();
             return;
        }

        // Prevent duplicate assignment across different actions
        String existing = findActionForKey(code);
        if (existing != null && !isSameActionAsCapturing(existing, capturing)) {
            // show inline warning in the overlay and do not assign
            String msg = "The key '" + code.getName() + "' is already assigned to '" + existing + "'. Please choose a different key.";
            showInlineWarning(msg);
            // restore button's label to previously assigned key
            setButtonKey(capturing, getCurrentKeyCodeForButton(capturing), true);
            capturing = null;
            ev.consume();
            return;
        }

        assignToCurrentSlot(capturing, code); // Update the internal variable and button text
        setInfoText(""); // Clear info text
        capturing = null; // Exit capture mode
        ev.consume(); // Prevent the key press from triggering other actions
    }

    /**
     * Updates the corresponding 'current' KeyCode variable based on the button pressed.
     */
    private void assignToCurrentSlot(Button b, KeyCode code) {
        if (b == null || code == null) return;

        // Determine which action this button corresponds to and update the variable
        if (b == btnLeftCurrent) { currentLeft = code; }
        else if (b == btnRightCurrent) { currentRight = code; }
        else if (b == btnSoftCurrent) { currentSoft = code; }
        else if (b == btnHardCurrent) { currentHard = code; }
        else if (b == btnRotateCurrent) { currentRotate = code; }
        else if (b == btnSwitchCurrent) { currentSwitch = code; }
        
        // Update the button's text
        setButtonKey(b, code, true);
    }

    /** Helper to get the currently assigned KeyCode for a given button */
    private KeyCode getCurrentKeyCodeForButton(Button b) {
        if (b == btnLeftCurrent) return currentLeft;
        if (b == btnRightCurrent) return currentRight;
        if (b == btnSoftCurrent) return currentSoft;
        if (b == btnHardCurrent) return currentHard;
        if (b == btnRotateCurrent) return currentRotate;
        if (b == btnSwitchCurrent) return currentSwitch;
        return null; // Should not happen for current buttons
    }

    /** Show an inline, temporary warning message in the overlay (lblInfo) instead of a modal dialog. */
    private void showInlineWarning(String message) {
        if (lblInfo == null) return;
        setInfoText(message);
        if (!lblInfo.getStyleClass().contains("warning")) lblInfo.getStyleClass().add("warning");

        // Restart/replace any existing timeline
        if (warningTimeline != null) warningTimeline.stop();
        warningTimeline = new Timeline(new KeyFrame(Duration.seconds(3), ev -> {
            lblInfo.getStyleClass().remove("warning");
            setInfoText("");
        }));
        warningTimeline.setCycleCount(1);
        warningTimeline.play();
    }

    /** Find which action (if any) is already using the given KeyCode. Returns a human-readable action name or null. */
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

    /**
     * Returns true if the provided existing action name maps to the same button that is currently capturing.
     * This lets us allow pressing the same key again for the same action without raising a duplicate warning.
     */
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


    /**
     * Called by MainMenuController to initialize with previously saved keys.
     * Overrides the defaults set in initialize().
     */
    public void init(KeyCode left, KeyCode right, KeyCode rotate, KeyCode down, KeyCode hard) {
        if (left != null) { currentLeft = left; setButtonKey(btnLeftCurrent, left, true); }
        if (right != null) { currentRight = right; setButtonKey(btnRightCurrent, right, true); }
        if (rotate != null) { currentRotate = rotate; setButtonKey(btnRotateCurrent, rotate, true); }
        if (down != null) { currentSoft = down; setButtonKey(btnSoftCurrent, down, true); }
        if (hard != null) { currentHard = hard; setButtonKey(btnHardCurrent, hard, true); }
    }

    /** Overloaded init to accept Switch key as well. */
    public void init(KeyCode left, KeyCode right, KeyCode rotate, KeyCode down, KeyCode hard, KeyCode sw) {
        init(left, right, rotate, down, hard);
        if (sw != null) { currentSwitch = sw; setButtonKey(btnSwitchCurrent, sw, true); }
    }

    /**
     * Sets the callback for when the Save/Cancel buttons are clicked.
     */
    public void setCloseHandler(java.util.function.Consumer<Boolean> handler) {
        this.closeHandler = handler;
    }

    // --- Getters for MainMenuController to retrieve the saved values ---
    public KeyCode getLeft() { return currentLeft; }
    public KeyCode getRight() { return currentRight; }
    public KeyCode getRotate() { return currentRotate; }
    public KeyCode getDown() { return currentSoft; }
    public KeyCode getHard() { return currentHard; }
    public KeyCode getSwitch() { return currentSwitch; }

    /**
     * Hide the action buttons (Reset/Save/Cancel) and info label.
     * Useful when embedding this controller inside another overlay that provides its own actions.
     */
    public void hideActionButtons() {
        try {
            if (btnReset != null) { btnReset.setVisible(false); btnReset.setManaged(false); }
            if (btnSave != null) { btnSave.setVisible(false); btnSave.setManaged(false); }
            if (btnCancel != null) { btnCancel.setVisible(false); btnCancel.setManaged(false); }
            if (lblInfo != null) { lblInfo.setVisible(false); lblInfo.setManaged(false); }
        } catch (Exception ignored) {}
    }

    /** Public wrapper to reset all current keybindings to application defaults. */
    public void resetToDefaults() {
        try {
            resetAllToDefaults();
        } catch (Exception ignored) {}
    }

    /** Set the top header text in the controls panel (e.g. Single Player Controls). */
    public void setHeaderText(String text) {
        try {
            if (lblHeader != null) lblHeader.setText(text != null ? text : "");
        } catch (Exception ignored) {}
    }

     /** Helper to safely set the info label text */
    private void setInfoText(String text) {
        if (lblInfo != null) {
            lblInfo.setText(text != null ? text : "");
        }
    }
}