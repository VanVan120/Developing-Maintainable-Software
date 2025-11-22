package com.comp2042.controller.mainMenu;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class JfxInitializer {
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    public static void init() throws Exception {
        if (initialized.get()) return;
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            // create JFXPanel to init toolkit
            new JFXPanel();
            Platform.runLater(latch::countDown);
        });
        if (!latch.await(5, TimeUnit.SECONDS)) throw new RuntimeException("Failed to initialize JavaFX toolkit");
        initialized.set(true);
    }
}
