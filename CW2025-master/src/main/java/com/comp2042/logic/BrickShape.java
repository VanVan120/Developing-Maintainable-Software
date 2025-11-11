package com.comp2042.logic;

import javafx.scene.paint.Color;

/**
 * Enum of brick identity and their display color.
 */
public enum BrickShape {
    I(Color.CYAN),
    J(Color.BLUE),
    L(Color.ORANGE),
    O(Color.YELLOW),
    S(Color.GREEN),
    T(Color.PURPLE),
    Z(Color.RED);

    private final Color color;

    BrickShape(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }
}
