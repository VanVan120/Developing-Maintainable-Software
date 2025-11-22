package com.comp2042.controller.cooperateBattle.coopGUI;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;

/**
 * Utility to load and manage the coop UI stylesheet and provide safe helpers
 * to add/remove style classes on JavaFX nodes.
 */
public final class CoopStyleManager {
    private static final Logger LOGGER = Logger.getLogger(CoopStyleManager.class.getName());

    private CoopStyleManager() {}

    /**
     * Ensure the coop stylesheet (/css/coop.css) is added to the given scene.
     * This is safe to call multiple times.
     */
    public static void ensureStylesheet(Scene scene) {
        if (scene == null) return;
        try {
            URL res = CoopStyleManager.class.getResource("/css/coop.css");
            if (res == null) return;
            String css = res.toExternalForm();
            ObservableList<String> sheets = scene.getStylesheets();
            if (!sheets.contains(css)) sheets.add(css);
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Failed to load coop stylesheet", e);
        }
    }

    /**
     * Add a style class to a node if not already present.
     */
    public static void addStyleClass(Node node, String styleClass) {
        if (node == null || styleClass == null) return;
        try {
            ObservableList<String> classes = node.getStyleClass();
            if (!classes.contains(styleClass)) classes.add(styleClass);
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Failed to add style class: " + styleClass, e);
        }
    }

    /**
     * Remove a style class from a node if present.
     */
    public static void removeStyleClass(Node node, String styleClass) {
        if (node == null || styleClass == null) return;
        try {
            ObservableList<String> classes = node.getStyleClass();
            classes.remove(styleClass);
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Failed to remove style class: " + styleClass, e);
        }
    }
}
