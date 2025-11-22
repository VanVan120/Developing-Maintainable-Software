package com.comp2042.controller.guiControl;

import com.comp2042.logic.Brick;
import com.comp2042.view.BoardView;
import javafx.geometry.Pos;

import java.util.List;

/**
 * Small companion helper for GUI view construction utilities extracted from GuiController.
 */
public class GuiViewHelpers {

    /**
     * Build the 'next pieces' preview VBox. This was extracted from GuiController.buildNextPreview
     * to keep the controller slimmer. It uses controller-visible fields for sizing.
     */
    public static javafx.scene.layout.VBox buildNextPreview(GuiController controller, List<Brick> upcoming) {
        /**
         * Build the 'next pieces' preview VBox. This was extracted from
         * {@link GuiController#showNextBricks} to keep the controller slimmer.
         * The returned {@link javafx.scene.layout.VBox} contains up to three
         * preview slots sized from the controller's configured cell dimensions.
         */
        javafx.scene.layout.VBox container = new javafx.scene.layout.VBox(8);
        container.setAlignment(Pos.TOP_CENTER);
        if (upcoming == null || upcoming.isEmpty()) return container;

        double pW = Math.max(4.0, controller.cellW);
        double pH = Math.max(4.0, controller.cellH);

        int count = Math.min(upcoming.size(), 3);
        for (int i = 0; i < count; i++) {
            Brick b = upcoming.get(i);
            int[][] shape = b.getShapeMatrix().get(0); // default orientation for preview
            int rows = shape.length;
            int cols = shape[0].length;
            javafx.scene.layout.StackPane slot = new javafx.scene.layout.StackPane();
            slot.setPrefWidth(cols * pW + 8.0);
            slot.setPrefHeight(rows * pH + 8.0);
            slot.setStyle("-fx-background-color: transparent;");

            int minR = Integer.MAX_VALUE, minC = Integer.MAX_VALUE, maxR = Integer.MIN_VALUE, maxC = Integer.MIN_VALUE;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (shape[r][c] != 0) {
                        minR = Math.min(minR, r);
                        minC = Math.min(minC, c);
                        maxR = Math.max(maxR, r);
                        maxC = Math.max(maxC, c);
                    }
                }
            }
            if (minR == Integer.MAX_VALUE) {
                minR = 0; minC = 0; maxR = rows - 1; maxC = cols - 1;
            }
            int visibleCols = maxC - minC + 1;
            int visibleRows = maxR - minR + 1;

            javafx.scene.layout.Pane inner = new javafx.scene.layout.Pane();
            inner.setPrefWidth(visibleCols * pW);
            inner.setPrefHeight(visibleRows * pH);
            inner.setMinWidth(visibleCols * pW);
            inner.setMinHeight(visibleRows * pH);
            inner.setMaxWidth(visibleCols * pW);
            inner.setMaxHeight(visibleRows * pH);

            for (int r = minR; r <= maxR; r++) {
                for (int c = minC; c <= maxC; c++) {
                    int v = shape[r][c];
                    javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle(Math.round(pW), Math.round(pH));
                    rect.setLayoutX(Math.round((c - minC) * pW));
                    rect.setLayoutY(Math.round((r - minR) * pH));
                    rect.setFill(BoardView.mapCodeToPaint(v));
                    rect.setArcHeight(6);
                    rect.setArcWidth(6);
                    rect.setVisible(v != 0);
                    inner.getChildren().add(rect);
                }
            }
            slot.getChildren().add(inner);
            container.getChildren().add(slot);
        }
        return container;
    }
}
