package io.github.korteexz.ragephysics.timestamper;

import io.github.korteexz.ragephysics.timestamper.config.TemporalMode;
import io.github.korteexz.ragephysics.timestamper.config.TemporalTarget;
import io.github.korteexz.ragephysics.timestamper.region.TemporalRegion;
import io.github.korteexz.ragephysics.timestamper.region.TemporalRegionBounds;
import io.github.korteexz.ragephysics.timestamper.region.TemporalRegionSavedData;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Small pure-Java checks for the Timestamper domain contract. */
public final class TemporalRegionDomainChecks {
    private static int checks;
    private static void check(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }

    public static void run() {
        TemporalRegionBounds normalized = new TemporalRegionBounds(new BlockPos(9, 4, 8), new BlockPos(0, 1, 2));
        check(normalized.min().equals(new BlockPos(0, 1, 2)), "normalizes min");
        check(normalized.max().equals(new BlockPos(9, 4, 8)), "normalizes max");
        check(normalized.contains(new BlockPos(0, 1, 2)), "inclusive min");
        check(normalized.contains(new BlockPos(9, 4, 8)), "inclusive max");
        check(!normalized.contains(new BlockPos(10, 4, 8)), "outside");
        TemporalRegionBounds left = new TemporalRegionBounds(new BlockPos(0, 0, 0), new BlockPos(9, 9, 9));
        check(!left.intersects(new TemporalRegionBounds(new BlockPos(10, 0, 0), new BlockPos(19, 9, 9))), "adjacent");
        check(left.intersects(new TemporalRegionBounds(new BlockPos(9, 5, 9), new BlockPos(19, 19, 19))), "partial overlap");
        check(new TemporalRegionBounds(new BlockPos(0, 0, 0), new BlockPos(19, 19, 19)).intersects(new TemporalRegionBounds(new BlockPos(5, 5, 5), new BlockPos(9, 9, 9))), "contained");
        check(new TemporalRegionBounds(new BlockPos(0, 0, 0), new BlockPos(0, 0, 0)).intersects(new TemporalRegionBounds(new BlockPos(0, 0, 0), new BlockPos(1, 1, 1))), "single shared block");

        var dimension = Level.OVERWORLD;
        TemporalRegion region = new TemporalRegion(UUID.randomUUID(), UUID.randomUUID(), "test", dimension, normalized, 1.0, true, TemporalMode.SIMULATION_ONLY, EnumSet.of(TemporalTarget.MOBS), 0);
        check(region.affects(TemporalTarget.MOBS), "target set");
        check(TemporalRegion.isValidTimeScale(.125) && TemporalRegion.isValidTimeScale(1.0) && TemporalRegion.isValidTimeScale(8.0), "scale boundaries");
        for (double invalid : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0, .124, 8.001}) check(!TemporalRegion.isValidTimeScale(invalid), "invalid scale " + invalid);
        try {
            new TemporalRegion(UUID.randomUUID(), UUID.randomUUID(), "bad", dimension, normalized, Double.NaN, true, TemporalMode.VISUAL_ONLY, EnumSet.noneOf(TemporalTarget.class), 0);
            throw new AssertionError("invalid scale accepted");
        } catch (IllegalArgumentException expected) { checks++; }

        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        TemporalRegionSavedData data = new TemporalRegionSavedData();
        TemporalRegion first = region(owner, Level.OVERWORLD, 0, 9, true);
        check(data.create(owner, first), "create region");
        check(data.isDirty(), "create marks dirty");
        check(data.getById(first.id()).orElseThrow().id().equals(first.id()), "lookup by id");
        check(data.getByOwner(owner).size() == 1, "lookup by owner");
        check(data.create(owner, region(owner, Level.OVERWORLD, 10, 19, true)), "adjacent accepted");
        check(data.create(owner, region(owner, Level.OVERWORLD, 30, 39, true)), "separate accepted");
        check(!data.create(other, region(other, Level.OVERWORLD, 9, 15, true)), "partial overlap rejected");
        check(!data.create(other, region(other, Level.OVERWORLD, 2, 5, true)), "contained overlap rejected");
        check(!data.create(other, region(other, Level.OVERWORLD, 9, 9, true)), "shared boundary block rejected");
        check(data.create(other, region(other, Level.NETHER, 0, 9, true)), "different dimension accepted");
        TemporalRegion disabled = region(other, Level.OVERWORLD, 0, 9, false);
        check(data.create(other, disabled), "disabled overlap accepted");
        check(!data.getActiveRegions(Level.OVERWORLD).contains(disabled), "disabled region is not active");
        check(!data.setEnabled(other, disabled.id(), true), "conflicting re-enable rejected");
        TemporalRegion unchanged = copy(first, first.bounds(), first.enabled());
        check(data.update(owner, unchanged), "unchanged bounds accepted");
        check(data.getById(first.id()).orElseThrow().revision() == 1, "update advances revision");
        TemporalRegion collidingEdit = copy(first, new TemporalRegionBounds(new BlockPos(15, 0, 0), new BlockPos(25, 9, 9)), true);
        check(!data.update(owner, collidingEdit), "colliding bounds edit rejected");
        check(!data.update(other, unchanged), "foreign owner update rejected");
        check(!data.remove(other, first.id()), "foreign owner removal rejected");

        var saved = data.save(new net.minecraft.nbt.CompoundTag(), null);
        TemporalRegionSavedData loaded = TemporalRegionSavedData.load(saved, null);
        TemporalRegion restored = loaded.getById(first.id()).orElseThrow();
        check(restored.id().equals(first.id()), "persistent id");
        check(restored.owner().equals(first.owner()), "persistent owner");
        check(restored.name().equals(first.name()), "persistent name");
        check(restored.dimension().equals(first.dimension()), "persistent dimension");
        check(restored.bounds().min().equals(first.bounds().min()) && restored.bounds().max().equals(first.bounds().max()), "persistent bounds");
        check(restored.timeScale() == first.timeScale(), "persistent scale");
        check(restored.enabled() == first.enabled(), "persistent enabled");
        check(restored.mode() == first.mode(), "persistent mode");
        check(restored.targets().equals(first.targets()), "persistent targets");
        check(restored.revision() == 1, "persistent revision");
        System.out.println("PASS: " + checks + " timestamper domain checks");
    }

    private static TemporalRegion region(UUID owner, net.minecraft.resources.ResourceKey<Level> dimension,
            int minX, int maxX, boolean enabled) {
        return new TemporalRegion(UUID.randomUUID(), owner, "test-region", dimension,
                new TemporalRegionBounds(new BlockPos(minX, 0, 0), new BlockPos(maxX, 9, 9)),
                2.0, enabled, TemporalMode.SIMULATION_AND_VISUAL,
                Set.of(TemporalTarget.MOBS, TemporalTarget.PROJECTILES), 0);
    }

    private static TemporalRegion copy(TemporalRegion region, TemporalRegionBounds bounds, boolean enabled) {
        return new TemporalRegion(region.id(), region.owner(), region.name(), region.dimension(), bounds,
                region.timeScale(), enabled, region.mode(), region.targets(), region.revision());
    }
}
