package com.comp2042.controller.controls;

import javafx.scene.input.KeyCode;
import java.util.EnumMap;
import java.util.Map;

/**
 * Pure model class that stores key mappings for game actions.
 * This separates assignment logic from JavaFX UI code so it can be unit tested.
 */
public class KeyBindings {
    private final EnumMap<Action, KeyCode> current = new EnumMap<>(Action.class);
    private final EnumMap<Action, KeyCode> panelDefaults = new EnumMap<>(Action.class);
    private final EnumMap<Action, KeyCode> defaults = new EnumMap<>(Action.class);

    public KeyBindings() {
        defaults.put(Action.LEFT, KeyCode.LEFT);
        defaults.put(Action.RIGHT, KeyCode.RIGHT);
        defaults.put(Action.SOFT_DROP, KeyCode.DOWN);
        defaults.put(Action.HARD_DROP, KeyCode.SPACE);
        defaults.put(Action.ROTATE, KeyCode.UP);
        defaults.put(Action.SWITCH, KeyCode.C);

        panelDefaults.putAll(defaults);
        current.putAll(defaults);
    }

    public KeyCode get(Action a) {
        if (a == null) return null;
        return current.get(a);
    }

    public void set(Action a, KeyCode code) {
        if (a == null) return;
        current.put(a, code);
    }

    public Action findActionForKey(KeyCode code) {
        if (code == null) return null;
        for (Map.Entry<Action, KeyCode> e : current.entrySet()) {
            if (code.equals(e.getValue())) return e.getKey();
        }
        return null;
    }

    public void resetToDefaults() {
        current.clear();
        current.putAll(defaults);
    }

    public void resetToPanelDefaults() {
        current.clear();
        current.putAll(panelDefaults);
    }

    public void setPanelDefault(Action a, KeyCode code) {
        if (a == null || code == null) return;
        panelDefaults.put(a, code);
    }

    public KeyCode getPanelDefault(Action a) {
        if (a == null) return null;
        return panelDefaults.get(a);
    }

    public void setPanelDefaults(Map<Action, KeyCode> map) {
        if (map == null) return;
        panelDefaults.putAll(map);
    }
}
