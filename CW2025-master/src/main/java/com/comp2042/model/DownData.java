package com.comp2042.model;

/**
 * Result of a "down" operation for a player: may contain cleared-row
 * information and the ViewData describing how the view should be updated.
 *
 * Instances are immutable holders.
 */
public final class DownData {
    private final ClearRow clearRow;
    private final ViewData viewData;

    public DownData(ClearRow clearRow, ViewData viewData) {
        this.clearRow = clearRow;
        this.viewData = viewData;
    }

    public ClearRow getClearRow() {
        return clearRow;
    }

    public ViewData getViewData() {
        return viewData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DownData downData = (DownData) o;
        return java.util.Objects.equals(clearRow, downData.clearRow) &&
                java.util.Objects.equals(viewData, downData.viewData);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(clearRow, viewData);
    }

    @Override
    public String toString() {
        return "DownData{" +
                "clearRow=" + clearRow +
                ", viewData=" + viewData +
                '}';
    }
}
