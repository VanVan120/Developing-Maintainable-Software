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
        /**
         * Flash a rectangular row highlight at the specified local coordinates
         * inside the given particle pane. The flash is created and animated on
         * the JavaFX thread and removed after the animation completes.
         *
         * @param particlePane pane to place the flash into
         * @param leftXLocal   local X coordinate for the top-left of the flash
         * @param topYLocal    local Y coordinate for the top-left of the flash
         * @param width        width of the flash rectangle in pixels
         * @param height       height of the flash rectangle in pixels
         */
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
        /**
         * Spawn decorative particles for each cleared cell described by
         * {@code clearRow}. Particles are added to {@code particlePane} and
         * removed when their animations complete.
         *
         * @param particlePane  pane to host particle nodes
         * @param clearRow      the ClearRow result describing cleared rows
         * @param displayMatrix the grid rectangles used for color lookup
         * @param bv            optional BoardView used for precise positioning
         * @param baseOffsetX   fallback X offset when BoardView is unavailable
         * @param baseOffsetY   fallback Y offset when BoardView is unavailable
         * @param cellW         cell width in pixels
         * @param cellH         cell height in pixels
         * @param gameScene     optional scene used to compute fall distances
         */
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
                        javafx.scene.paint.Paint fill = resolveCellFill(bv, displayMatrix, r, c);
                        if (fill == null) continue;
                        if (fill == javafx.scene.paint.Color.TRANSPARENT) continue;
                        javafx.scene.paint.Color color = (fill instanceof javafx.scene.paint.Color) ? (javafx.scene.paint.Color) fill : null;

                        int particles = 4 + (int)(Math.random() * 6); // 4..9
                        for (int p = 0; p < particles; p++) {
                            double pw = Math.max(3.0, Math.round(cellW / 3.0));
                            double ph = Math.max(3.0, Math.round(cellH / 3.0));
                            Rectangle sq = createParticleSquare(color, pw, ph);

                            Point2D local = computeParticleStartLocal(particlePane, bv, c, r, baseOffsetX, rowTopY, cellW, cellH, pw, ph);
                            sq.setTranslateX(local.getX());
                            sq.setTranslateY(local.getY());
                            particlePane.getChildren().add(sq);

                            double sceneHeight = 800.0;
                            try { if (gameScene != null) sceneHeight = gameScene.getHeight(); } catch (Exception ex) { LOGGER.log(Level.FINER, "Failed to get gameScene height", ex); }
                            double fallBy = sceneHeight - local.getY() + 80 + Math.random() * 120;
                            double durationMs = 2000 + Math.random() * 1500; // 2000..3500ms

                            playParticleTransitions(particlePane, sq, fallBy, durationMs);
                        }
                    } catch (Exception ex) { LOGGER.log(Level.FINER, "spawnRowClearParticles particle creation failed", ex); }
                }
            }
        } catch (Exception ex) { LOGGER.log(Level.FINER, "spawnRowClearParticles failed", ex); }
    }

    // Resolve the paint for a board cell, preferring BoardView.getCellFill when available
    private static javafx.scene.paint.Paint resolveCellFill(BoardView bv, Rectangle[][] displayMatrix, int r, int c) {
        javafx.scene.paint.Paint fill = null;
        if (bv != null) {
            try { fill = bv.getCellFill(r, c); } catch (Exception ignored) {}
        } else {
            Rectangle boardCell = null;
            if (displayMatrix != null && displayMatrix.length > r && r >= 0) boardCell = displayMatrix[r][c];
            fill = (boardCell != null) ? boardCell.getFill() : null;
        }
        return fill;
    }

    private static Rectangle createParticleSquare(javafx.scene.paint.Color color, double pw, double ph) {
        Rectangle sq = new Rectangle(pw, ph);
        sq.setArcWidth(2);
        sq.setArcHeight(2);
        if (color != null) {
            sq.setFill(color);
        } else {
            sq.getStyleClass().add("particle-square-default");
        }
        sq.setMouseTransparent(true);
        return sq;
    }

    private static Point2D computeParticleStartLocal(Pane particlePane, BoardView bv, int c, int r, double baseOffsetX, double rowTopY, double cellW, double cellH, double pw, double ph) {
        Point2D local = null;
        if (bv != null) {
            try {
                Point2D sceneCell = bv.boardCellScenePoint(c, r);
                if (sceneCell != null) {
                    double jitterX = (Math.random() - 0.5) * (bv.getCellWidth() * 0.4);
                    double jitterY = (Math.random() - 0.5) * (bv.getCellHeight() * 0.4);
                    Point2D jitteredScene = new Point2D(sceneCell.getX() + bv.getCellWidth() * 0.5 + jitterX, sceneCell.getY() + bv.getCellHeight() * 0.5 + jitterY);
                    local = (particlePane != null) ? particlePane.sceneToLocal(jitteredScene) : jitteredScene;
                }
            } catch (Exception ignored) {}
        }
        if (local == null) {
            double cellX = Math.round(baseOffsetX + c * cellW);
            double cellY = rowTopY;
            double jitterX = (Math.random() - 0.5) * (cellW * 0.4);
            double jitterY = (Math.random() - 0.5) * (cellH * 0.4);
            double startX = Math.round(cellX + cellW * 0.5 + jitterX - pw * 0.5);
            double startY = Math.round(cellY + cellH * 0.5 + jitterY - ph * 0.5);
            javafx.geometry.Point2D parentPt = new javafx.geometry.Point2D(startX, startY);
            javafx.geometry.Point2D scenePt = parentPt;
            local = (particlePane != null) ? particlePane.sceneToLocal(scenePt) : scenePt;
        }
        return local;
    }

    private static void playParticleTransitions(Pane particlePane, Rectangle sq, double fallBy, double durationMs) {
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

    public static void flashRow(Pane particlePane, double baseOffsetX, double topY, double width, double height) {
        /**
         * Flash a solid white rectangle at the given coordinates inside
         * {@code particlePane}. This is a simpler variant used when BoardView
         * positioning is already available.
         */
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
        /**
         * Spawn a burst of short-lived circular particles centered around the
         * given coordinates. Colors are sampled from {@link BoardView#mapCodeToPaint}
         * when a {@code brickShape} is provided; otherwise a default CSS style
         * is applied.
         *
         * @param particlePane pane to add particle nodes to
         * @param centerX      center X coordinate in local coordinates
         * @param centerY      center Y coordinate in local coordinates
         * @param brickShape   optional shape matrix used to sample colors
         */
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
        /**
         * Play the lock animation that visually moves small rounded rectangles
         * from the falling piece area down into their landed positions. This
         * is a best-effort visual effect and will not modify model state.
         *
         * @param particlePane pane to host particles
         * @param start        starting ViewData snapshot (pre-lock)
         * @param end          ending ViewData snapshot (post-lock)
         * @param intense      when true use a shorter, more intense animation
         * @param brickPanel   optional pane containing the piece nodes used for positioning
         * @param bv           optional BoardView for precise cell scene points
         * @param cellWpx      cell width in pixels
         * @param cellHpx      cell height in pixels
         */
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

            int minR = computeMinRow(shape);

            Point2D topParentPt = computeTopParentPt(bv, brickPanel, start.getxPosition(), startBoardY + minR);
            Point2D topLocal = computeTopLocal(particlePane, brickPanel, topParentPt, start, minR, cellWpx, cellHpx);

            for (int r = 0; r < shape.length; r++) {
                for (int c = 0; c < shape[r].length; c++) {
                    if (shape[r][c] == 0) continue;
                    int boardX = start.getxPosition() + c;
                    int boardYEnd = endBoardY + r;

                    Point2D cellStartLocal = computeCellLocal(particlePane, bv, brickPanel, boardX, startBoardY + r, cellWpx, cellHpx);
                    Point2D cellEndLocal = computeCellLocal(particlePane, bv, brickPanel, boardX, boardYEnd, cellWpx, cellHpx);

                    double x = Math.round(cellStartLocal.getX());
                    double y = Math.round(topLocal.getY());

                    Rectangle cellRect = createLockCellRect(cellWpx, cellHpx, intense);
                    cellRect.setLayoutX(x);
                    cellRect.setLayoutY(y);

                    DropShadow ds = createDropShadow(intense);
                    cellRect.setEffect(ds);

                    if (particlePane != null) particlePane.getChildren().add(cellRect);

                    double deltaY = Math.round(cellEndLocal.getY() - topLocal.getY());
                    ParallelTransition pt = createLockTransition(cellRect, travelMs, fadeMs, deltaY, r);
                    running.add(pt);
                }
            }

            javafx.application.Platform.runLater(() -> {
                for (ParallelTransition pt : running) pt.play();
            });
        } catch (Exception ex) { LOGGER.log(Level.FINER, "playLockEffect failed", ex); }
    }

    private static int computeMinRow(int[][] shape) {
        int minR = Integer.MAX_VALUE;
        for (int rr = 0; rr < shape.length; rr++) {
            for (int cc = 0; cc < shape[rr].length; cc++) if (shape[rr][cc] != 0) { if (rr < minR) minR = rr; }
        }
        if (minR == Integer.MAX_VALUE) minR = 0;
        return minR;
    }

    private static Point2D computeTopParentPt(BoardView bv, Pane brickPanel, int boardX, int boardY) {
        Point2D topParentPt = null;
        try {
            if (bv != null) {
                Point2D scenePt = bv.boardCellScenePoint(boardX, boardY);
                if (scenePt != null) {
                    topParentPt = (brickPanel != null && brickPanel.getParent() != null) ? brickPanel.getParent().sceneToLocal(scenePt) : scenePt;
                }
            }
        } catch (Exception ex) { LOGGER.log(Level.FINER, "Failed to compute topParentPt", ex); }
        return topParentPt;
    }

    private static Point2D computeTopLocal(Pane particlePane, Pane brickPanel, Point2D topParentPt, ViewData start, int minR, double cellWpx, double cellHpx) {
        Point2D topLocal = null;
        try {
            if (topParentPt != null) {
                javafx.geometry.Point2D scenePt = (brickPanel != null && brickPanel.getParent() != null)
                        ? brickPanel.getParent().localToScene(new javafx.geometry.Point2D(topParentPt.getX(), topParentPt.getY()))
                        : new javafx.geometry.Point2D(topParentPt.getX(), topParentPt.getY());
                topLocal = (particlePane != null) ? particlePane.sceneToLocal(scenePt) : scenePt;
            } else {
                javafx.geometry.Point2D fallback = new javafx.geometry.Point2D(start.getxPosition() * cellWpx, (start.getyPosition() - 2 + minR) * cellHpx);
                topLocal = (particlePane != null) ? particlePane.sceneToLocal(fallback) : fallback;
            }
        } catch (Exception ex) {
            LOGGER.log(Level.FINER, "Failed to compute topLocal for lock effect", ex);
            topLocal = new Point2D(0,0);
        }
        return topLocal;
    }

    private static Point2D computeCellLocal(Pane particlePane, BoardView bv, Pane brickPanel, int boardX, int boardY, double cellWpx, double cellHpx) {
        Point2D local = null;
        try {
            if (bv != null) {
                javafx.geometry.Point2D scene = bv.boardCellScenePoint(boardX, boardY);
                if (scene != null) local = (particlePane != null) ? particlePane.sceneToLocal(scene) : scene;
            }
        } catch (Exception ex) { LOGGER.log(Level.FINER, "Failed to compute cell local", ex); }
        if (local == null) {
            javafx.geometry.Point2D cellParent = new javafx.geometry.Point2D(Math.round(boardX * cellWpx), Math.round(boardY * cellHpx));
            javafx.geometry.Point2D cellScene = (brickPanel != null && brickPanel.getParent() != null) ? brickPanel.getParent().localToScene(cellParent) : cellParent;
            local = (particlePane != null) ? particlePane.sceneToLocal(cellScene) : cellScene;
        }
        return local;
    }

    private static Rectangle createLockCellRect(double cellWpx, double cellHpx, boolean intense) {
        Rectangle cellRect = new Rectangle(Math.round(cellWpx), Math.round(cellHpx));
        cellRect.setArcWidth(6);
        cellRect.setArcHeight(6);
        cellRect.getStyleClass().add("particle-lock-rect");
        cellRect.setOpacity(intense ? 0.95 : 0.85);
        cellRect.setMouseTransparent(true);
        cellRect.setTranslateX(0);
        cellRect.setTranslateY(0);
        return cellRect;
    }

    private static DropShadow createDropShadow(boolean intense) {
        DropShadow ds = new DropShadow(BlurType.GAUSSIAN, Color.WHITE, intense ? 14.0 : 9.0, 0.35, 0.0, 0.0);
        ds.setSpread(intense ? 0.7 : 0.45);
        return ds;
    }

    private static ParallelTransition createLockTransition(Rectangle cellRect, double travelMs, double fadeMs, double deltaY, int rowIndex) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(travelMs), cellRect);
        tt.setByY(deltaY);
        tt.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        tt.setDelay(Duration.millis(rowIndex * 18));

        FadeTransition ft = new FadeTransition(Duration.millis(fadeMs), cellRect);
        ft.setFromValue(cellRect.getOpacity());
        ft.setToValue(0.0);

        ParallelTransition pt = new ParallelTransition(tt, ft);
        pt.setOnFinished(e -> {
            try {
                e.consume();
                javafx.scene.Parent parent = cellRect.getParent();
                if (parent instanceof Pane) {
                    ((Pane) parent).getChildren().remove(cellRect);
                }
            } catch (Exception ex) {
                LOGGER.log(Level.FINER, "Failed to remove cellRect after lock effect", ex);
            }
        });
        return pt;
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
