package com.comp2042.controller.mainMenu;

import java.util.prefs.Preferences;
import javafx.scene.input.KeyCode;

/**
 * Helper that loads and saves main-menu related preferences using the
 * {@link java.util.prefs.Preferences} API.
 *
 * <p>Responsibilities:
 * - Read and write control key bindings (single-player and two-player keys).
 * - Read and write handling settings (timings and gameplay toggles).
 *
 * <p>Notes:
 * - All persistence is stored in a {@code Preferences} node scoped to
 *   {@code MainMenuController.class}.
 * - {@code load*} methods always return a non-null settings object; individual
 *   key fields may be null if no binding or an invalid value is stored.
 * - When saving key bindings, a null field is persisted as an empty string.
 * - This class does not perform FX-thread marshaling because it only accesses
 *   the user preferences store; callers should perform any required UI-thread
 *   actions themselves.
 */
public class MainMenuPreferences {

    private final Preferences prefs;

    public MainMenuPreferences() {
        this.prefs = Preferences.userNodeForPackage(MainMenuController.class);
    }

    /**
     * Load and return the persisted control key bindings.
     *
     * @return a freshly allocated {@link MainMenuControlSettings} populated from
     *         preferences. The returned object is never null; individual
     *         {@link javafx.scene.input.KeyCode} fields may be null if no
     *         value was stored or the stored value could not be parsed.
     */
    public MainMenuControlSettings loadControlSettings() {
        MainMenuControlSettings cs = new MainMenuControlSettings();
        String s;
        s = prefs.get("spLeft", ""); cs.spLeft = s.isEmpty() ? null : safeKeyCodeOf(s);
        s = prefs.get("spRight", ""); cs.spRight = s.isEmpty() ? null : safeKeyCodeOf(s);
        s = prefs.get("spRotate", ""); cs.spRotate = s.isEmpty() ? null : safeKeyCodeOf(s);
        s = prefs.get("spDown", ""); cs.spDown = s.isEmpty() ? null : safeKeyCodeOf(s);
        s = prefs.get("spHard", ""); cs.spHard = s.isEmpty() ? null : safeKeyCodeOf(s);
        s = prefs.get("spSwitch", ""); cs.spSwitch = s.isEmpty() ? null : safeKeyCodeOf(s);

        s = prefs.get("mpLeft_left", ""); cs.mpLeft_left = s.isEmpty() ? null : safeKeyCodeOf(s);
        s = prefs.get("mpLeft_right", ""); cs.mpLeft_right = s.isEmpty() ? null : safeKeyCodeOf(s);
        s = prefs.get("mpLeft_rotate", ""); cs.mpLeft_rotate = s.isEmpty() ? null : safeKeyCodeOf(s);
        s = prefs.get("mpLeft_down", ""); cs.mpLeft_down = s.isEmpty() ? null : safeKeyCodeOf(s);
        s = prefs.get("mpLeft_hard", ""); cs.mpLeft_hard = s.isEmpty() ? null : safeKeyCodeOf(s);
        s = prefs.get("mpLeft_switch", ""); cs.mpLeft_switch = s.isEmpty() ? null : safeKeyCodeOf(s);

        s = prefs.get("mpRight_left", ""); cs.mpRight_left = s.isEmpty() ? null : safeKeyCodeOf(s);
        s = prefs.get("mpRight_right", ""); cs.mpRight_right = s.isEmpty() ? null : safeKeyCodeOf(s);
        s = prefs.get("mpRight_rotate", ""); cs.mpRight_rotate = s.isEmpty() ? null : safeKeyCodeOf(s);
        s = prefs.get("mpRight_down", ""); cs.mpRight_down = s.isEmpty() ? null : safeKeyCodeOf(s);
        s = prefs.get("mpRight_hard", ""); cs.mpRight_hard = s.isEmpty() ? null : safeKeyCodeOf(s);
        s = prefs.get("mpRight_switch", ""); cs.mpRight_switch = s.isEmpty() ? null : safeKeyCodeOf(s);

        return cs;
    }

    /**
     * Persist control key bindings from the provided settings object.
     *
     * <p>Null key fields are persisted as an empty string. This method has the
     * side-effect of writing to the platform preferences store; callers should
     * be prepared for platform-specific behavior and possible SecurityExceptions
     * in restricted environments.
     *
     * @param cs the settings to persist; may be the same instance previously
     *           returned by {@link #loadControlSettings()} but must not be null.
     */
    public void saveControlSettings(MainMenuControlSettings cs) {
        prefs.put("spLeft", cs.spLeft != null ? cs.spLeft.name() : "");
        prefs.put("spRight", cs.spRight != null ? cs.spRight.name() : "");
        prefs.put("spRotate", cs.spRotate != null ? cs.spRotate.name() : "");
        prefs.put("spDown", cs.spDown != null ? cs.spDown.name() : "");
        prefs.put("spHard", cs.spHard != null ? cs.spHard.name() : "");
        prefs.put("spSwitch", cs.spSwitch != null ? cs.spSwitch.name() : "");

        prefs.put("mpLeft_left", cs.mpLeft_left != null ? cs.mpLeft_left.name() : "");
        prefs.put("mpLeft_right", cs.mpLeft_right != null ? cs.mpLeft_right.name() : "");
        prefs.put("mpLeft_rotate", cs.mpLeft_rotate != null ? cs.mpLeft_rotate.name() : "");
        prefs.put("mpLeft_down", cs.mpLeft_down != null ? cs.mpLeft_down.name() : "");
        prefs.put("mpLeft_hard", cs.mpLeft_hard != null ? cs.mpLeft_hard.name() : "");
        prefs.put("mpLeft_switch", cs.mpLeft_switch != null ? cs.mpLeft_switch.name() : "");

        prefs.put("mpRight_left", cs.mpRight_left != null ? cs.mpRight_left.name() : "");
        prefs.put("mpRight_right", cs.mpRight_right != null ? cs.mpRight_right.name() : "");
        prefs.put("mpRight_rotate", cs.mpRight_rotate != null ? cs.mpRight_rotate.name() : "");
        prefs.put("mpRight_down", cs.mpRight_down != null ? cs.mpRight_down.name() : "");
        prefs.put("mpRight_hard", cs.mpRight_hard != null ? cs.mpRight_hard.name() : "");
        prefs.put("mpRight_switch", cs.mpRight_switch != null ? cs.mpRight_switch.name() : "");
    }

    /**
     * Load persisted handling (timing and gameplay) settings.
     *
     * @return a newly created {@link MainMenuHandlingSettings} populated from
     *         preferences. Returns default values when no preference exists.
     */
    public MainMenuHandlingSettings loadHandlingSettings() {
        MainMenuHandlingSettings hs = new MainMenuHandlingSettings();
        hs.settingArrMs = prefs.getInt("settingArrMs", hs.settingArrMs);
        hs.settingDasMs = prefs.getInt("settingDasMs", hs.settingDasMs);
        hs.settingDcdMs = prefs.getInt("settingDcdMs", hs.settingDcdMs);
        hs.settingSdf = prefs.getDouble("settingSdf", hs.settingSdf);
        hs.settingHardDropEnabled = prefs.getBoolean("settingHardDropEnabled", hs.settingHardDropEnabled);
        return hs;
    }

    /**
     * Persist handling settings into the preferences store.
     *
     * @param hs the handling settings to persist; must not be null.
     */
    public void saveHandlingSettings(MainMenuHandlingSettings hs) {
        prefs.putInt("settingArrMs", hs.settingArrMs);
        prefs.putInt("settingDasMs", hs.settingDasMs);
        prefs.putInt("settingDcdMs", hs.settingDcdMs);
        prefs.putDouble("settingSdf", hs.settingSdf);
        prefs.putBoolean("settingHardDropEnabled", hs.settingHardDropEnabled);
    }

    /**
     * Parse a {@link KeyCode} from its name in a null-safe way.
     *
     * @param name the name of the key (as returned by {@code KeyCode.name()});
     *             may be null or empty.
     * @return the corresponding {@link KeyCode} or {@code null} if the input is
     *         null, empty, or does not match any known {@code KeyCode} constant.
     */
    private KeyCode safeKeyCodeOf(String name) {
        if (name == null || name.isEmpty()) return null;
        try { return KeyCode.valueOf(name); } catch (IllegalArgumentException ex) { return null; }
    }
}
