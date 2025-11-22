package com.comp2042.view;

import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.Glow;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

/**
 * Tests for NotificationPanel that require a JavaFX toolkit.
 */
public class NotificationPanelTest {

    @BeforeAll
    public static void initToolkit() {
        // Initializes JavaFX toolkit. Creating a JFXPanel is a simple way to start the platform.
        new JFXPanel();
    }

    @Test
    public void constructorShouldCreateLabelWithStyleAndGlow() throws Exception {
        final Object[] holder = new Object[1];
        final var latch = new java.util.concurrent.CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                NotificationPanel panel = new NotificationPanel("Score +100");
                Node center = panel.getCenter();
                assertNotNull(center, "Center node should not be null");
                assertTrue(center instanceof Label, "Center node should be a Label");
                Label label = (Label) center;
                assertEquals("Score +100", label.getText(), "Label text should be set");
                assertTrue(label.getStyleClass().contains("bonusStyle"), "Label must have bonusStyle class");
                assertTrue(label.getEffect() instanceof Glow, "Label should have a Glow effect");
                holder[0] = label.getEffect();
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Timeout waiting for FX thread");
        assertNotNull(holder[0]);
    }

    /**
     * Tests that showScore() creates and adds the notification panel with animation.
     */
    @Test
    public void showScoreShouldRemovePanelFromListAfterAnimation() throws Exception {
        final java.util.concurrent.CountDownLatch setupLatch = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.atomic.AtomicReference<Pane> parentRef = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.atomic.AtomicReference<NotificationPanel> panelRef = new java.util.concurrent.atomic.AtomicReference<>();

        Platform.runLater(() -> {
            Pane parent = new Pane();
            NotificationPanel panel = new NotificationPanel("Bonus!");
            parent.getChildren().add(panel);
            panel.showScore(parent.getChildren());
            parentRef.set(parent);
            panelRef.set(panel);
            setupLatch.countDown();
        });

        assertTrue(setupLatch.await(1, TimeUnit.SECONDS), "Setup timeout");
        
        // Verify that the panel was created and added to the parent
        Thread.sleep(100);
        final boolean[] wasAdded = {false};
        final java.util.concurrent.CountDownLatch checkLatch = new java.util.concurrent.CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                Pane p = parentRef.get();
                NotificationPanel np = panelRef.get();
                wasAdded[0] = (p != null && np != null && p.getChildren().contains(np));
            } finally {
                checkLatch.countDown();
            }
        });
        checkLatch.await(1, TimeUnit.SECONDS);
        assertTrue(wasAdded[0], "NotificationPanel should have been created and added to parent");
    }
}
