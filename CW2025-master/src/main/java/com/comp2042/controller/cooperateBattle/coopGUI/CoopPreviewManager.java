package com.comp2042.controller.cooperateBattle.coopGUI;

import java.util.List;
import com.comp2042.controller.cooperateBattle.coopController.CoopGameController;
import com.comp2042.logic.Brick;

import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/**
 * Small helper responsible for building and refreshing the "next" previews for
 * both players. Delegates to the controller's existing buildNextPreview(...) so
 * behaviour remains unchanged.
 */
public class CoopPreviewManager {

    private final CoopGuiController controller;
    private final CoopGameController coop;
    private final VBox leftNextBox;
    private final Pane nextContent;

    public CoopPreviewManager(CoopGuiController controller, CoopGameController coop, VBox leftNextBox, Pane nextContent) {
        this.controller = controller;
        this.coop = coop;
        this.leftNextBox = leftNextBox;
        this.nextContent = nextContent;
    }

    public void refreshPreviews() {
        try {
            if (coop == null) return;

            if (leftNextBox != null) {
                leftNextBox.getChildren().removeIf(n -> !(n instanceof Text));
                List<Brick> leftUp = coop.getUpcomingLeft(3);
                VBox built = controller.buildNextPreview(leftUp);
                if (built != null) leftNextBox.getChildren().addAll(built.getChildren());
            }

            if (nextContent != null) {
                nextContent.getChildren().clear();
                List<Brick> rightUp = coop.getUpcomingRight(3);
                VBox built2 = controller.buildNextPreview(rightUp);
                if (built2 != null) nextContent.getChildren().addAll(built2.getChildren());
            }
        } catch (Exception ignored) {}
    }
}
