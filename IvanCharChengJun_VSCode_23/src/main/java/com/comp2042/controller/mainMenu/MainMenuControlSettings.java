package com.comp2042.controller.mainMenu;

import javafx.scene.input.KeyCode;

/**
 * Simple data holder for control key bindings used by the main menu.
 *
 * <p>Fields are publicly mutable for easy copy/assign semantics when loading
 * and saving preferences. A {@code null} KeyCode indicates "no binding".
 */
public class MainMenuControlSettings {
    public KeyCode spLeft;
    public KeyCode spRight;
    public KeyCode spRotate;
    public KeyCode spDown;
    public KeyCode spHard;
    public KeyCode spSwitch;

    public KeyCode mpLeft_left;
    public KeyCode mpLeft_right;
    public KeyCode mpLeft_rotate;
    public KeyCode mpLeft_down;
    public KeyCode mpLeft_hard;
    public KeyCode mpLeft_switch;

    public KeyCode mpRight_left;
    public KeyCode mpRight_right;
    public KeyCode mpRight_rotate;
    public KeyCode mpRight_down;
    public KeyCode mpRight_hard;
    public KeyCode mpRight_switch;
}
