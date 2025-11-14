package com.comp2042.controller.mainMenu;

import java.util.prefs.Preferences;
import javafx.scene.input.KeyCode;

public class MainMenuPreferences {

    private final Preferences prefs;

    public MainMenuPreferences() {
        this.prefs = Preferences.userNodeForPackage(MainMenuController.class);
    }

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

    public MainMenuHandlingSettings loadHandlingSettings() {
        MainMenuHandlingSettings hs = new MainMenuHandlingSettings();
        hs.settingArrMs = prefs.getInt("settingArrMs", hs.settingArrMs);
        hs.settingDasMs = prefs.getInt("settingDasMs", hs.settingDasMs);
        hs.settingDcdMs = prefs.getInt("settingDcdMs", hs.settingDcdMs);
        hs.settingSdf = prefs.getDouble("settingSdf", hs.settingSdf);
        hs.settingHardDropEnabled = prefs.getBoolean("settingHardDropEnabled", hs.settingHardDropEnabled);
        return hs;
    }

    public void saveHandlingSettings(MainMenuHandlingSettings hs) {
        prefs.putInt("settingArrMs", hs.settingArrMs);
        prefs.putInt("settingDasMs", hs.settingDasMs);
        prefs.putInt("settingDcdMs", hs.settingDcdMs);
        prefs.putDouble("settingSdf", hs.settingSdf);
        prefs.putBoolean("settingHardDropEnabled", hs.settingHardDropEnabled);
    }

    private KeyCode safeKeyCodeOf(String name) {
        if (name == null || name.isEmpty()) return null;
        try { return KeyCode.valueOf(name); } catch (IllegalArgumentException ex) { return null; }
    }
}
