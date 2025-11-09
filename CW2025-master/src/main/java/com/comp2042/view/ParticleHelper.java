package com.comp2042.view;

import com.comp2042.model.ClearRow;
import com.comp2042.model.ViewData;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.geometry.Point2D;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.BlurType;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Small helper to centralize particle creation and flash effects so controllers can delegate to it.
 */
public class ParticleHelper {

    private static final Logger LOGGER = Logger.getLogger(ParticleHelper.class.getName());
    // Ensure the particles stylesheet is loaded once per JVM run
    private static volatile boolean PARTICLE_STYLES_LOADED = false;

    public static void flashRowAt(Pane particlePane, double leftXLocal, double topYLocal, double width, double height) {
        if (particlePane == null) return;
        ensureParticleStyles(particlePane);
        try {
            Rectangle flash = new Rectangle(Math.round(width), Math.round(height));
            flash.setTranslateX(Math.round(leftXLocal));
            flash.setTranslateY(Math.round(topYLocal));
            // default visual defined in CSS
            flash.getStyleClass().add("particle-flash");
            flash.setOpacity(0.0);
            flash.setMouseTransparent(true);
            particlePane.getChildren().add(flash);

            FadeTransition in = new FadeTransition(Duration.millis(80), flash);
            in.setFromValue(0.0);
            in.setToValue(0.85);
            FadeTransition out = new FadeTransition(Duration.millis(220), flash);
            out.setFromValue(0.85);
            out.setToValue(0.0);
            out.setDelay(Duration.millis(80));
            out.setOnFinished(event -> particlePane.getChildren().remove(flash));
            in.play();
            out.play();
        } catch (Exception ex) { LOGGER.log(Level.FINER, "flashRowAt failed", ex); }
    }

    public static void spawnRowClearParticles(Pane particlePane, ClearRow clearRow, Rectangle[][] displayMatrix, BoardView bv, double baseOffsetX, double baseOffsetY, double cellW, double cellH, javafx.scene.Scene gameScene) {
        if (clearRow == null || particlePane == null || displayMatrix == null) return;
        ensureParticleStyles(particlePane);
        try {
            int[] rows = clearRow.getClearedRows();
            if (rows == null || rows.length == 0) return;
            int cols = displayMatrix[0].length;
            for (int r : rows) {
                // compute top-left Y for this board row (visible coords)
                double rowTopY = Math.round(baseOffsetY + (r - 2) * cellH);
                for (int c = 0; c < cols; c++) {
                    try {
                        javafx.scene.paint.Paint fill = null;
                        if (bv != null) {
                            fill = bv.getCellFill(r, c);
                        } else {
                            Rectangle boardCell = null;
                            if (displayMatrix != null && displayMatrix.length > r && r >= 0) boardCell = displayMatrix[r][c];
                            fill = (boardCell != null) ? boardCell.getFill() : null;
                        }
                        if (fill == null) continue;
                        if (fill == javafx.scene.paint.Color.TRANSPARENT) continue;
                        // if the board cell fill is a concrete Color, use it; otherwise
                        // fall back to CSS-defined default color (handled by style class)
                        javafx.scene.paint.Color color = (fill instanceof javafx.scene.paint.Color) ? (javafx.scene.paint.Color) fill : null;

                        // spawn a handful of small square particles for this brick
                        int particles = 4 + (int)(Math.random() * 6); // 4..9
                        for (int p = 0; p < particles; p++) {
                            double pw = Math.max(3.0, Math.round(cellW / 3.0));
                            double ph = Math.max(3.0, Math.round(cellH / 3.0));
                            Rectangle sq = new Rectangle(pw, ph);
                            sq.setArcWidth(2);
                            sq.setArcHeight(2);
                            // if we have a concrete Color from the board cell, use it; otherwise
                            // fall back to a CSS-defined default color.
                            if (color != null) {
                                sq.setFill(color);
                            } else {
                                sq.getStyleClass().add("particle-square-default");
                            }
                            sq.setMouseTransparent(true);

                            // initial position: somewhere within the original brick cell
                            Point2D local = null;
                            if (bv != null) {
                                Point2D sceneCell = bv.boardCellScenePoint(c, r);
                                if (sceneCell != null) {
                                    double jitterX = (Math.random() - 0.5) * (bv.getCellWidth() * 0.4);
                                    double jitterY = (Math.random() - 0.5) * (bv.getCellHeight() * 0.4);
                                    Point2D jitteredScene = new Point2D(sceneCell.getX() + bv.getCellWidth() * 0.5 + jitterX, sceneCell.getY() + bv.getCellHeight() * 0.5 + jitterY);
                                    local = (particlePane != null) ? particlePane.sceneToLocal(jitteredScene) : jitteredScene;
                                }
                            }
                            if (local == null) {
                                double cellX = Math.round(baseOffsetX + c * cellW);
                                double cellY = rowTopY;
                                double jitterX = (Math.random() - 0.5) * (cellW * 0.4);
                                double jitterY = (Math.random() - 0.5) * (cellH * 0.4);
                                double startX = Math.round(cellX + cellW * 0.5 + jitterX - pw * 0.5);
                                double startY = Math.round(cellY + cellH * 0.5 + jitterY - ph * 0.5);
                                // convert starting position to particlePane local coordinates
                                javafx.geometry.Point2D parentPt = new javafx.geometry.Point2D(startX, startY);
                                javafx.geometry.Point2D scenePt = parentPt; // caller must have parent coords relative to board parent
                                local = (particlePane != null) ? particlePane.sceneToLocal(scenePt) : scenePt;
                            }
                            sq.setTranslateX(local.getX());
                            sq.setTranslateY(local.getY());
                            particlePane.getChildren().add(sq);

                            // falling distance (to bottom of scene or a generous amount)
                            double sceneHeight = 800.0;
                            try { if (gameScene != null) sceneHeight = gameScene.getHeight(); } catch (Exception ex) { LOGGER.log(Level.FINER, "Failed to get gameScene height", ex); }
                            double fallBy = sceneHeight - local.getY() + 80 + Math.random() * 120;

                            // duration variation (increased so particles fall longer and remain visible)
                            double durationMs = 2000 + Math.random() * 1500; // 2000..3500ms

                            TranslateTransition tt = new TranslateTransition(Duration.millis(durationMs), sq);
                            tt.setByY(fallBy);
                            tt.setInterpolator(javafx.animation.Interpolator.EASE_IN);

                            FadeTransition ft = new FadeTransition(Duration.millis(durationMs), sq);
                            ft.setFromValue(1.0);
                            ft.setToValue(0.0);

                            double sideBy = (Math.random() - 0.5) * 40.0;
                            TranslateTransition ttx = new TranslateTransition(Duration.millis(durationMs * 0.75), sq);
                            ttx.setByX(sideBy);
                            ttx.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

                            ParallelTransition pt = new ParallelTransition(ttx, tt, ft);
                            final Rectangle node = sq;
                            pt.setOnFinished(e -> { try { if (e != null) e.consume(); particlePane.getChildren().remove(node); } catch (Exception ex) { LOGGER.log(Level.FINER, "Failed to cleanup particle node", ex); } });
                            pt.play();
                        }
                        } catch (Exception ex) { LOGGER.log(Level.FINER, "spawnRowClearParticles particle creation failed", ex); }
                }
            }
        } catch (Exception ex) { LOGGER.log(Level.FINER, "spawnRowClearParticles failed", ex); }
    }

    public static void flashRow(Pane particlePane, double baseOffsetX, double topY, double width, double height) {
        if (particlePane == null) return;
        ensureParticleStyles(particlePane);
        try {
            Rectangle flash = new Rectangle(Math.round(width), Math.round(height));
            flash.setTranslateX(Math.round(baseOffsetX));
            flash.setTranslateY(Math.round(topY));
            flash.setFill(Color.web("#ffffff"));
            flash.setOpacity(0.0);
            flash.setMouseTransparent(true);
            particlePane.getChildren().add(flash);

            FadeTransition in = new FadeTransition(Duration.millis(80), flash);
            in.setFromValue(0.0);
            in.setToValue(0.85);
            FadeTransition out = new FadeTransition(Duration.millis(220), flash);
            out.setFromValue(0.85);
            out.setToValue(0.0);
            out.setDelay(Duration.millis(80));
            out.setOnFinished(event -> particlePane.getChildren().remove(flash));
            in.play();
            out.play();
        } catch (Exception ex) { LOGGER.log(Level.FINER, "flashRow failed", ex); }
    }

    public static void spawnParticlesAt(Pane particlePane, double centerX, double centerY, int[][] brickShape) {
        if (particlePane == null) return;
        ensureParticleStyles(particlePane);
        final int PARTICLE_COUNT = 18;
        final double MAX_SPEED = 220.0; // px/sec
        final double DURATION_MS = 600.0;

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            Circle c = new Circle(4 + Math.random() * 4);
            // random color sampled from brick colors (if available) or default gold
            Paint p = null; // default color is provided by CSS unless brickShape supplies fills
            if (brickShape != null) {
                java.util.List<Paint> fills = new java.util.ArrayList<>();
                for (int r = 0; r < brickShape.length; r++) {
                    for (int col = 0; col < brickShape[r].length; col++) {
                        int v = brickShape[r][col];
                        if (v != 0) fills.add(BoardView.mapCodeToPaint(v));
                    }
                }
                if (!fills.isEmpty()) p = fills.get((int) (Math.random() * fills.size()));
            }
            if (p != null) {
                c.setFill(p);
            } else {
                c.getStyleClass().add("particle-circle-default");
            }
            c.setOpacity(1.0);
            c.setTranslateX(centerX + (Math.random() - 0.5) * 6);
            c.setTranslateY(centerY + (Math.random() - 0.5) * 6);
            particlePane.getChildren().add(c);

            // random direction
            double angle = Math.random() * Math.PI * 2.0;
            double speed = 40 + Math.random() * MAX_SPEED; // px/sec
            double dx = Math.cos(angle) * speed;
            double dy = Math.sin(angle) * speed;

            // animate translation and fade
            TranslateTransition tt = new TranslateTransition(Duration.millis(DURATION_MS), c);
            tt.setByX(dx);
            tt.setByY(dy);
            tt.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

            FadeTransition ft = new FadeTransition(Duration.millis(DURATION_MS), c);
            ft.setFromValue(1.0);
            ft.setToValue(0.0);

            ScaleTransition st = new ScaleTransition(Duration.millis(DURATION_MS), c);
            st.setToX(0.3);
            st.setToY(0.3);

            ParallelTransition pt = new ParallelTransition(tt, ft, st);
            pt.setOnFinished(event -> { try { particlePane.getChildren().remove(c); } catch (Exception ex) { LOGGER.log(Level.FINER, "Failed to remove particle", ex); } });
            pt.play();
        }
    }

    /**
     * Play the lock effect that moves small rounded rectangles from the piece area down into
     * their landed positions. This is extracted from GuiController.playLockEffect so it can
     * be reused by other controllers.
     */
    public static void playLockEffect(Pane particlePane, ViewData start, ViewData end, boolean intense,
                                      Pane brickPanel, BoardView bv, double cellWpx, double cellHpx) {
        if (start == null || end == null || particlePane == null) return;
        ensureParticleStyles(particlePane);
        try {
            int[][] shape = start.getBrickData();
            if (shape == null) return;

            int startBoardY = start.getyPosition() - 2; // visible offset
            int endBoardY = end.getyPosition() - 2;

            double travelMs = intense ? 420.0 : 560.0;
            double fadeMs = intense ? 620.0 : 800.0;

            java.util.List<ParallelTransition> running = new java.util.ArrayList<>();

            int minR = Integer.MAX_VALUE;
            for (int rr = 0; rr < shape.length; rr++) {
                for (int cc = 0; cc < shape[rr].length; cc++) if (shape[rr][cc] != 0) { if (rr < minR) minR = rr; }
            }
            if (minR == Integer.MAX_VALUE) minR = 0;

            // compute top anchor (scene -> particlePane local)
            Point2D topParentPt = null;
            try {
                if (bv != null) {
                    Point2D scenePt = bv.boardCellScenePoint(start.getxPosition(), startBoardY + minR);
                    if (scenePt != null) {
                        topParentPt = (brickPanel != null && brickPanel.getParent() != null)
                                ? brickPanel.getParent().sceneToLocal(scenePt)
                                : scenePt;
                    }
                }
            } catch (Exception ex) { LOGGER.log(Level.FINER, "Failed to compute topParentPt", ex); }
            Point2D topLocal = null;
            try {
                if (topParentPt != null) {
                    javafx.geometry.Point2D scenePt = (brickPanel != null && brickPanel.getParent() != null)
                            ? brickPanel.getParent().localToScene(new javafx.geometry.Point2D(topParentPt.getX(), topParentPt.getY()))
                            : new javafx.geometry.Point2D(topParentPt.getX(), topParentPt.getY());
                    topLocal = (particlePane != null) ? particlePane.sceneToLocal(scenePt) : scenePt;
                } else {
                    // fallback: compute using board coords from start
                    javafx.geometry.Point2D fallback = new javafx.geometry.Point2D(start.getxPosition() * cellWpx, (startBoardY + minR) * cellHpx);
                    topLocal = (particlePane != null) ? particlePane.sceneToLocal(fallback) : fallback;
                }
            } catch (Exception ex) {
                LOGGER.log(Level.FINER, "Failed to compute topLocal for lock effect", ex);
                topLocal = new Point2D(0,0);
            }

            for (int r = 0; r < shape.length; r++) {
                for (int c = 0; c < shape[r].length; c++) {
                    if (shape[r][c] == 0) continue;
                    int boardX = start.getxPosition() + c;
                    int boardYEnd = endBoardY + r;

                    // compute start cell local
                    Point2D cellStartLocal = null;
                    try {
                        if (bv != null) {
                            javafx.geometry.Point2D scene = bv.boardCellScenePoint(boardX, startBoardY + r);
                            if (scene != null) cellStartLocal = (particlePane != null) ? particlePane.sceneToLocal(scene) : scene;
                        }
                    } catch (Exception ex) { LOGGER.log(Level.FINER, "Failed to compute cellStartLocal", ex); }
                    if (cellStartLocal == null) {
                        javafx.geometry.Point2D cellStartParent = new javafx.geometry.Point2D(Math.round(boardX * cellWpx), Math.round((startBoardY + r) * cellHpx));
                        javafx.geometry.Point2D cellStartScene = (brickPanel != null && brickPanel.getParent() != null)
                                ? brickPanel.getParent().localToScene(cellStartParent)
                                : cellStartParent;
                        cellStartLocal = (particlePane != null) ? particlePane.sceneToLocal(cellStartScene) : cellStartScene;
                    }

                    // compute end cell local
                    Point2D cellEndLocal = null;
                    try {
                        if (bv != null) {
                            javafx.geometry.Point2D scene = bv.boardCellScenePoint(boardX, boardYEnd);
                            if (scene != null) cellEndLocal = (particlePane != null) ? particlePane.sceneToLocal(scene) : scene;
                        }
                    } catch (Exception ex) { LOGGER.log(Level.FINER, "Failed to compute cellEndLocal", ex); }
                    if (cellEndLocal == null) {
                        javafx.geometry.Point2D cellEndParent = new javafx.geometry.Point2D(Math.round(boardX * cellWpx), Math.round(boardYEnd * cellHpx));
                        javafx.geometry.Point2D cellEndScene = (brickPanel != null && brickPanel.getParent() != null)
                                ? brickPanel.getParent().localToScene(cellEndParent)
                                : cellEndParent;
                        cellEndLocal = (particlePane != null) ? particlePane.sceneToLocal(cellEndScene) : cellEndScene;
                    }

                    double x = Math.round(cellStartLocal.getX());
                    double y = Math.round(topLocal.getY());

                    Rectangle cellRect = new Rectangle(Math.round(cellWpx), Math.round(cellHpx));
                    cellRect.setArcWidth(6);
                    cellRect.setArcHeight(6);
                    // visual properties driven by CSS class for easier theming
                    cellRect.getStyleClass().add("particle-lock-rect");
                    cellRect.setOpacity(intense ? 0.95 : 0.85);
                    cellRect.setMouseTransparent(true);
                    cellRect.setLayoutX(x);
                    cellRect.setLayoutY(y);
                    cellRect.setTranslateX(0);
                    cellRect.setTranslateY(0);

                    DropShadow ds = new DropShadow(BlurType.GAUSSIAN, Color.WHITE, intense ? 14.0 : 9.0, 0.35, 0.0, 0.0);
                    ds.setSpread(intense ? 0.7 : 0.45);
                    cellRect.setEffect(ds);

                    if (particlePane != null) particlePane.getChildren().add(cellRect);

                    TranslateTransition tt = new TranslateTransition(Duration.millis(travelMs), cellRect);
                    double deltaY = Math.round(cellEndLocal.getY() - topLocal.getY());
                    tt.setByY(deltaY);
                    tt.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
                    tt.setDelay(Duration.millis(r * 18));

                    FadeTransition ft = new FadeTransition(Duration.millis(fadeMs), cellRect);
                    ft.setFromValue(cellRect.getOpacity());
                    ft.setToValue(0.0);

                    ParallelTransition pt = new ParallelTransition(tt, ft);
                    pt.setOnFinished(e -> { try { e.consume(); if (particlePane != null) particlePane.getChildren().remove(cellRect); } catch (Exception ex) { LOGGER.log(Level.FINER, "Failed to remove cellRect after lock effect", ex); } });
                    running.add(pt);
                }
            }

            javafx.application.Platform.runLater(() -> {
                for (ParallelTransition pt : running) pt.play();
            });

    } catch (Exception ex) { LOGGER.log(Level.FINER, "playLockEffect failed", ex); }
    }

    /**
     * Attempt to load the particles stylesheet into the pane's stylesheets list.
     * This is a best-effort helper and will silently return if the resource
     * cannot be located.
     */
    private static void ensureParticleStyles(Pane pane) {
        if (pane == null) return;
        if (PARTICLE_STYLES_LOADED) return;
        synchronized (ParticleHelper.class) {
            if (PARTICLE_STYLES_LOADED) return;
            try {
                java.net.URL res = ParticleHelper.class.getResource("/css/particles.css");
                if (res != null) {
                    String url = res.toExternalForm();
                    javafx.collections.ObservableList<String> ss = pane.getStylesheets();
                    if (!ss.contains(url)) ss.add(url);
                    PARTICLE_STYLES_LOADED = true;
                }
            } catch (Exception ex) {
                // loading CSS is optional; log at FINER so we don't spam by default
                LOGGER.log(Level.FINER, "Could not load particle stylesheet", ex);
            }
        }
    }
}
