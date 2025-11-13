package com.comp2042.controller.guiControl;

import com.comp2042.model.ClearRow;
import com.comp2042.model.ViewData;
import com.comp2042.view.BoardView;
import com.comp2042.view.ParticleHelper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * Helper class for particle/visual effects previously living in GuiController.
 */
public class GuiParticleHelpers {

    public static void spawnExplosion(GuiController controller, ClearRow clearRow, ViewData v) {
        if (controller.particlePane == null) return;
        try {
            if (clearRow != null && clearRow.getLinesRemoved() > 0) {
                int[] rows = clearRow.getClearedRows();
                if (rows != null && rows.length > 0) {
                    BoardView bv = controller.getBoardView();
                    for (int r : rows) {
                        if (bv != null) {
                            javafx.geometry.Point2D sceneLeft = bv.boardCellScenePoint(0, r);
                            javafx.geometry.Point2D leftLocal = (controller.particlePane != null && sceneLeft != null)
                                    ? controller.particlePane.sceneToLocal(sceneLeft)
                                    : new javafx.geometry.Point2D(0, 0);
                            double width = bv.getCellWidth() * bv.getColumns();
                            flashRowAt(controller, Math.round(leftLocal.getX()), Math.round(leftLocal.getY()), width, bv.getCellHeight());
                        } else {
                            double flashY = Math.round(controller.baseOffsetY + (r - 2) * controller.cellH);
                            javafx.geometry.Point2D flashLocal = boardCoordsToParticleLocal(controller, Math.round(controller.baseOffsetX), flashY);
                            flashRowAt(controller, Math.round(flashLocal.getX()), Math.round(flashLocal.getY()), controller.cellW * controller.displayMatrix[0].length, controller.cellH);
                        }
                    }
                    controller.shakeBoard();
                    try { spawnRowClearParticles(controller, clearRow); } catch (Exception ignored) {}
                    for (int r : rows) {
                        if (bv != null) {
                            int midCol = Math.max(0, bv.getColumns() / 2);
                            javafx.geometry.Point2D sceneCenter = bv.boardCellScenePoint(midCol, r);
                            javafx.geometry.Point2D centerLocal = (controller.particlePane != null && sceneCenter != null) ? controller.particlePane.sceneToLocal(sceneCenter) : new javafx.geometry.Point2D(0,0);
                            spawnParticlesAt(controller, centerLocal.getX(), centerLocal.getY(), v != null ? v.getBrickData() : null);
                        } else {
                            double centerY = Math.round(controller.baseOffsetY + (r - 2 + 0.5) * controller.cellH);
                            double centerX = Math.round(controller.baseOffsetX + (controller.displayMatrix[0].length * 0.5) * controller.cellW);
                            javafx.geometry.Point2D centerLocal = boardCoordsToParticleLocal(controller, centerX, centerY);
                            spawnParticlesAt(controller, centerLocal.getX(), centerLocal.getY(), v != null ? v.getBrickData() : null);
                        }
                    }
                    return;
                }
            }
        } catch (Exception ignored) {}
        // fallback: spawn at brick landing position
        spawnExplosionAtLanding(controller, v);
    }

    public static void spawnExplosionAtLanding(GuiController controller, ViewData v) {
        if (v == null || controller.particlePane == null) return;
        int brickX = v.getxPosition();
        int brickY = v.getyPosition() - 2; // visible offset
        double centerX = Math.round(controller.baseOffsetX + brickX * controller.cellW + (controller.cellW * 2));
        double centerY = Math.round(controller.baseOffsetY + brickY * controller.cellH + (controller.cellH * 2));
        spawnParticlesAt(controller, centerX, centerY, v.getBrickData());
    }

    public static javafx.geometry.Point2D boardCoordsToParticleLocal(GuiController controller, double x, double y) {
        javafx.geometry.Point2D parentPt = new javafx.geometry.Point2D(x, y);
        javafx.geometry.Point2D scenePt = (controller.brickPanel != null && controller.brickPanel.getParent() != null)
                ? controller.brickPanel.getParent().localToScene(parentPt)
                : parentPt;
        return (controller.particlePane != null) ? controller.particlePane.sceneToLocal(scenePt) : scenePt;
    }

    public static void flashRowAt(GuiController controller, double leftXLocal, double topYLocal, double width, double height) {
        if (controller.particlePane == null) return;
        try { ParticleHelper.flashRowAt(controller.particlePane, leftXLocal, topYLocal, width, height); } catch (Exception ignored) {}
    }

    public static void spawnRowClearParticles(GuiController controller, ClearRow clearRow) {
        try { ParticleHelper.spawnRowClearParticles(controller.particlePane, clearRow, controller.displayMatrix, controller.getBoardView(), controller.baseOffsetX, controller.baseOffsetY, controller.cellW, controller.cellH, (controller.gameBoard != null) ? controller.gameBoard.getScene() : null); } catch (Exception ignored) {}
    }

    public static void flashRow(GuiController controller, double topY, double width, double height) {
        if (controller.particlePane == null) return;
        try { ParticleHelper.flashRow(controller.particlePane, controller.baseOffsetX, topY, width, height); } catch (Exception ignored) {}
    }

    public static void spawnParticlesAt(GuiController controller, double centerX, double centerY, int[][] brickShape) {
        try { ParticleHelper.spawnParticlesAt(controller.particlePane, centerX, centerY, brickShape); } catch (Exception ignored) {}
    }

    /**
     * Shake the provided controller's gameBoard to emphasize a row clear.
     */
    public static void shakeBoard(GuiController controller) {
        if (controller.gameBoard == null) return;
        try {
            final int magnitude = 8; // px
            final int shakes = 6;
            Timeline t = new Timeline();
            for (int i = 0; i < shakes; i++) {
                final int dir = (i % 2 == 0) ? 1 : -1;
                KeyFrame kf = new KeyFrame(Duration.millis((i * 30)), new javafx.event.EventHandler<javafx.event.ActionEvent>() {
                    @Override
                    public void handle(javafx.event.ActionEvent event) {
                        try { controller.gameBoard.setTranslateX(dir * magnitude); } catch (Exception ignored) {}
                    }
                });
                t.getKeyFrames().add(kf);
            }
            // final frame: reset to 0
            t.getKeyFrames().add(new KeyFrame(Duration.millis(shakes * 30), new javafx.event.EventHandler<javafx.event.ActionEvent>() {
                @Override
                public void handle(javafx.event.ActionEvent event) {
                    try { controller.gameBoard.setTranslateX(0); } catch (Exception ignored) {}
                }
            }));
            t.play();
        } catch (Exception ignored) {}
    }
}
