package io.github.korteexz.ragephysics.timestamper.region;

import java.util.Objects;
import net.minecraft.core.BlockPos;

/** Inclusive, normalized block volume used by a Timestamper region. */
public final class TemporalRegionBounds {
    private final BlockPos min;
    private final BlockPos max;

    public TemporalRegionBounds(BlockPos first, BlockPos second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        this.min = new BlockPos(Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ()));
        this.max = new BlockPos(Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()), Math.max(first.getZ(), second.getZ()));
    }

    public BlockPos min() { return min; }
    public BlockPos max() { return max; }
    public BlockPos getMin() { return min; }
    public BlockPos getMax() { return max; }

    public boolean contains(BlockPos pos) {
        Objects.requireNonNull(pos, "pos");
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    /** True when both volumes share at least one block. */
    public boolean intersects(TemporalRegionBounds other) {
        Objects.requireNonNull(other, "other");
        return min.getX() <= other.max.getX() && max.getX() >= other.min.getX()
                && min.getY() <= other.max.getY() && max.getY() >= other.min.getY()
                && min.getZ() <= other.max.getZ() && max.getZ() >= other.min.getZ();
    }

    public static boolean intersects(TemporalRegionBounds first, TemporalRegionBounds second) {
        return Objects.requireNonNull(first, "first").intersects(second);
    }
}
