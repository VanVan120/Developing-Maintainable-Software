package com.comp2042.controller.cooperateBattle.coopGUI;

import javafx.scene.input.KeyCode;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates multiplayer key bindings and preference persistence.
 * Keeps default values in one place and provides load/save helpers.
 */
public class CoopKeyBindings {
    private static final Logger LOGGER = Logger.getLogger(CoopKeyBindings.class.getName());

    // Left defaults
    public static final KeyCode DEFAULT_LEFT_LEFT = KeyCode.A;
    public static final KeyCode DEFAULT_LEFT_RIGHT = KeyCode.D;
    public static final KeyCode DEFAULT_LEFT_ROTATE = KeyCode.W;
    public static final KeyCode DEFAULT_LEFT_DOWN = KeyCode.S;
    public static final KeyCode DEFAULT_LEFT_HARD = KeyCode.SHIFT;
    public static final KeyCode DEFAULT_LEFT_SWAP = KeyCode.Q;

    // Right defaults
    public static final KeyCode DEFAULT_RIGHT_LEFT = KeyCode.LEFT;
    public static final KeyCode DEFAULT_RIGHT_RIGHT = KeyCode.RIGHT;
    public static final KeyCode DEFAULT_RIGHT_ROTATE = KeyCode.UP;
    public static final KeyCode DEFAULT_RIGHT_DOWN = KeyCode.DOWN;
    public static final KeyCode DEFAULT_RIGHT_HARD = KeyCode.SPACE;
    public static final KeyCode DEFAULT_RIGHT_SWAP = KeyCode.C;

    private KeyCode leftMoveLeft = DEFAULT_LEFT_LEFT;
    private KeyCode leftMoveRight = DEFAULT_LEFT_RIGHT;
    private KeyCode leftRotate = DEFAULT_LEFT_ROTATE;
    private KeyCode leftDown = DEFAULT_LEFT_DOWN;
    private KeyCode leftHard = DEFAULT_LEFT_HARD;
    private KeyCode leftSwap = DEFAULT_LEFT_SWAP;

    private KeyCode rightMoveLeft = DEFAULT_RIGHT_LEFT;
    private KeyCode rightMoveRight = DEFAULT_RIGHT_RIGHT;
    private KeyCode rightRotate = DEFAULT_RIGHT_ROTATE;
    private KeyCode rightDown = DEFAULT_RIGHT_DOWN;
    private KeyCode rightHard = DEFAULT_RIGHT_HARD;
    private KeyCode rightSwap = DEFAULT_RIGHT_SWAP;

    public CoopKeyBindings() {}

    /**
     * Load multiplayer key bindings from a preferences node. Missing entries
     * will leave the current values unchanged.
     *
     * @param prefs preferences node to read from
     */
    public void loadFromPreferences(java.util.prefs.Preferences prefs) {
        if (prefs == null) return;
        try {
            leftMoveLeft = load(prefs, "mpLeft_left", leftMoveLeft);
            leftMoveRight = load(prefs, "mpLeft_right", leftMoveRight);
            leftRotate = load(prefs, "mpLeft_rotate", leftRotate);
            leftDown = load(prefs, "mpLeft_down", leftDown);
            leftHard = load(prefs, "mpLeft_hard", leftHard);
            leftSwap = load(prefs, "mpLeft_switch", leftSwap);

            rightMoveLeft = load(prefs, "mpRight_left", rightMoveLeft);
            rightMoveRight = load(prefs, "mpRight_right", rightMoveRight);
            rightRotate = load(prefs, "mpRight_rotate", rightRotate);
            rightDown = load(prefs, "mpRight_down", rightDown);
            rightHard = load(prefs, "mpRight_hard", rightHard);
            rightSwap = load(prefs, "mpRight_switch", rightSwap);
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Failed to load key preferences", e);
        }
    }

    public void saveToPreferences(java.util.prefs.Preferences prefs) {
        /**
         * Persist the current multiplayer key bindings to the provided
         * preferences node.
         *
         * @param prefs preferences node to write to
         */
        if (prefs == null) return;
        try {
            prefs.put("mpLeft_left", leftMoveLeft != null ? leftMoveLeft.name() : "");
            prefs.put("mpLeft_right", leftMoveRight != null ? leftMoveRight.name() : "");
            prefs.put("mpLeft_rotate", leftRotate != null ? leftRotate.name() : "");
            prefs.put("mpLeft_down", leftDown != null ? leftDown.name() : "");
            prefs.put("mpLeft_hard", leftHard != null ? leftHard.name() : "");
            prefs.put("mpLeft_switch", leftSwap != null ? leftSwap.name() : "");

            prefs.put("mpRight_left", rightMoveLeft != null ? rightMoveLeft.name() : "");
            prefs.put("mpRight_right", rightMoveRight != null ? rightMoveRight.name() : "");
            prefs.put("mpRight_rotate", rightRotate != null ? rightRotate.name() : "");
            prefs.put("mpRight_down", rightDown != null ? rightDown.name() : "");
            prefs.put("mpRight_hard", rightHard != null ? rightHard.name() : "");
            prefs.put("mpRight_switch", rightSwap != null ? rightSwap.name() : "");
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Failed to save key preferences", e);
        }
    }

    private KeyCode load(java.util.prefs.Preferences prefs, String key, KeyCode fallback) {
        try {
            String s = prefs.get(key, "");
            if (s != null && !s.isEmpty()) return KeyCode.valueOf(s);
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Failed to load key preference: " + key, e);
        }
        return fallback;
    }

    // getters/setters
    public KeyCode getLeftMoveLeft() { return leftMoveLeft; }
    public KeyCode getLeftMoveRight() { return leftMoveRight; }
    public KeyCode getLeftRotate() { return leftRotate; }
    public KeyCode getLeftDown() { return leftDown; }
    public KeyCode getLeftHard() { return leftHard; }
    public KeyCode getLeftSwap() { return leftSwap; }

    public void setLeftKeys(KeyCode left, KeyCode right, KeyCode rotate, KeyCode down, KeyCode hard, KeyCode swap) {
        if (left != null) leftMoveLeft = left;
        if (right != null) leftMoveRight = right;
        if (rotate != null) leftRotate = rotate;
        if (down != null) leftDown = down;
        if (hard != null) leftHard = hard;
        if (swap != null) leftSwap = swap;
    }

    public KeyCode getRightMoveLeft() { return rightMoveLeft; }
    public KeyCode getRightMoveRight() { return rightMoveRight; }
    public KeyCode getRightRotate() { return rightRotate; }
    public KeyCode getRightDown() { return rightDown; }
    public KeyCode getRightHard() { return rightHard; }
    public KeyCode getRightSwap() { return rightSwap; }

    public void setRightKeys(KeyCode left, KeyCode right, KeyCode rotate, KeyCode down, KeyCode hard, KeyCode swap) {
        if (left != null) rightMoveLeft = left;
        if (right != null) rightMoveRight = right;
        if (rotate != null) rightRotate = rotate;
        if (down != null) rightDown = down;
        if (hard != null) rightHard = hard;
        if (swap != null) rightSwap = swap;
    }
}
