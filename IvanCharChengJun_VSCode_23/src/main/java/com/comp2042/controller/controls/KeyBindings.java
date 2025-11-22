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

    /**
     * Create a new KeyBindings model populated with sensible defaults.
     * <p>Defaults are copied into both the panel defaults and current mapping so
     * the UI will display values out of the box.
     */
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

    /**
     * Return the currently assigned key for the given action.
     *
     * @param a action to query
     * @return assigned {@link KeyCode} or {@code null} if none
     */
    public KeyCode get(Action a) {
        if (a == null) return null;
        return current.get(a);
    }

    /**
     * Assign a key to an action in the current mapping.
     *
     * @param a    action to assign
     * @param code key to assign
     */
    public void set(Action a, KeyCode code) {
        if (a == null) return;
        current.put(a, code);
    }

    /**
     * Find which action (if any) is bound to the given key in the current mapping.
     *
     * @param code key to search for
     * @return the {@link Action} bound to the key or {@code null} if none
     */
    public Action findActionForKey(KeyCode code) {
        if (code == null) return null;
        for (Map.Entry<Action, KeyCode> e : current.entrySet()) {
            if (code.equals(e.getValue())) return e.getKey();
        }
        return null;
    }

    /**
     * Reset the current mapping to the global defaults.
     */
    public void resetToDefaults() {
        current.clear();
        current.putAll(defaults);
    }

    /**
     * Reset the current mapping to the panel-specific defaults.
     */
    public void resetToPanelDefaults() {
        current.clear();
        current.putAll(panelDefaults);
    }

    /**
     * Set a panel default for an action.
     *
     * @param a    action to set
     * @param code default key
     */
    public void setPanelDefault(Action a, KeyCode code) {
        if (a == null || code == null) return;
        panelDefaults.put(a, code);
    }

    /**
     * Get the panel default for the given action.
     *
     * @param a action to query
     * @return panel default key or {@code null}
     */
    public KeyCode getPanelDefault(Action a) {
        if (a == null) return null;
        return panelDefaults.get(a);
    }

    /**
     * Bulk replace or add panel defaults from a map.
     *
     * @param map mapping of actions to default keys
     */
    public void setPanelDefaults(Map<Action, KeyCode> map) {
        if (map == null) return;
        panelDefaults.putAll(map);
    }
}
