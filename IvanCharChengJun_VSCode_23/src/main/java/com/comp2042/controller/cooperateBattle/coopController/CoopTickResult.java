package com.comp2042.controller.cooperateBattle.coopController;

import com.comp2042.model.ClearRow;
import com.comp2042.model.DownData;

/**
 * Result of a single cooperative game tick.
 *
 * <p>Contains whether either side merged, per-player down results and any
 * cleared-row information. Instances are small immutable value objects.</p>
 */
public final class CoopTickResult {
    private final boolean merged;
    private final DownData leftData;
    private final DownData rightData;
    private final ClearRow clearRow;

    public CoopTickResult(boolean merged, DownData leftData, DownData rightData, ClearRow clearRow) {
        this.merged = merged;
        this.leftData = leftData;
        this.rightData = rightData;
        this.clearRow = clearRow;
    }

    public boolean isMerged() { return merged; }

    public DownData getLeftData() { return leftData; }

    public DownData getRightData() { return rightData; }

    public ClearRow getClearRow() { return clearRow; }

    /**
     * {@inheritDoc}
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoopTickResult that = (CoopTickResult) o;
        return merged == that.merged &&
                java.util.Objects.equals(leftData, that.leftData) &&
                java.util.Objects.equals(rightData, that.rightData) &&
                java.util.Objects.equals(clearRow, that.clearRow);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(merged, leftData, rightData, clearRow);
    }

    @Override
    public String toString() {
        return "CoopTickResult{" +
                "merged=" + merged +
                ", leftData=" + leftData +
                ", rightData=" + rightData +
                ", clearRow=" + clearRow +
                '}';
    }
}
