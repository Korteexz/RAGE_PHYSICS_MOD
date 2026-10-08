package io.github.korteexz.ragephysics.timestamper;

import io.github.korteexz.ragephysics.timestamper.config.TemporalMode;
import io.github.korteexz.ragephysics.timestamper.config.TemporalTarget;
import io.github.korteexz.ragephysics.timestamper.region.TemporalRegion;
import io.github.korteexz.ragephysics.timestamper.region.TemporalRegionBounds;
import io.github.korteexz.ragephysics.timestamper.region.TemporalRegionSavedData;
import io.github.korteexz.ragephysics.timestamper.region.TemporalRegionOperations;
import io.github.korteexz.ragephysics.timestamper.region.RegionOperationStatus;
import io.github.korteexz.ragephysics.selection.PlayerSelection;
import io.github.korteexz.ragephysics.temporal.TemporalRegionResolver;
import io.github.korteexz.ragephysics.network.RegionManagementPayloads;
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
        check(data.getById(first.id()).orElseThrow().revision() == 0, "unchanged update keeps revision");
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
        check(restored.revision() == 0, "persistent revision");
        operationChecks();
        System.out.println("PASS: " + checks + " timestamper domain checks");
    }

    private static void operationChecks() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        TemporalRegionSavedData data = new TemporalRegionSavedData();
        TemporalRegionOperations operations = new TemporalRegionOperations(data);

        check(operations.createFromSelection(owner, Level.OVERWORLD, new PlayerSelection()).status()
                == RegionOperationStatus.NO_SELECTION, "empty selection rejected");
        PlayerSelection incomplete = new PlayerSelection();
        incomplete.select(Level.OVERWORLD, new BlockPos(0, 0, 0));
        check(operations.createFromSelection(owner, Level.OVERWORLD, incomplete).status()
                == RegionOperationStatus.INCOMPLETE_SELECTION, "incomplete selection rejected");
        PlayerSelection wrongDimension = selection(100, 109, 1.0);
        check(operations.createFromSelection(owner, Level.NETHER, wrongDimension).status()
                == RegionOperationStatus.INVALID_DIMENSION, "selection dimension validated");

        PlayerSelection firstSelection = selection(0, 9, 2.0);
        var createdResult = operations.createFromSelection(owner, Level.OVERWORLD, firstSelection);
        check(createdResult.succeeded(), "complete selection creates");
        TemporalRegion created = createdResult.region().orElseThrow();
        check(created.id() != null, "new uuid assigned");
        check(created.owner().equals(owner), "requester becomes owner");
        check(created.name().equals("Region 1"), "readable deterministic name");

        check(operations.createFromSelection(other, Level.OVERWORLD, selection(5, 12, 1.0)).status()
                == RegionOperationStatus.OVERLAP, "create overlap rejected");
        check(operations.createFromSelection(owner, Level.OVERWORLD, selection(10, 19, 1.0)).succeeded(),
                "adjacent create accepted");
        check(data.getByOwner(owner).size() == 2, "second region preserves first");

        UUID id = created.id();
        long revision = created.revision();
        var renamed = operations.rename(owner, id, "  Lab  ");
        check(renamed.succeeded() && renamed.region().orElseThrow().name().equals("Lab"), "rename trims");
        check(renamed.region().orElseThrow().id().equals(id), "rename preserves uuid");
        check(operations.rename(owner, id, "   ").status() == RegionOperationStatus.INVALID_NAME,
                "empty rename rejected");
        check(operations.rename(owner, id, "x".repeat(TemporalRegionOperations.MAX_NAME_LENGTH + 1)).status()
                == RegionOperationStatus.INVALID_NAME, "long rename rejected");
        long renamedRevision = renamed.region().orElseThrow().revision();
        check(renamedRevision == revision + 1, "rename increments revision");
        check(operations.rename(owner, id, "Lab").status() == RegionOperationStatus.NO_CHANGE,
                "same rename is no change");
        check(data.getById(id).orElseThrow().revision() == renamedRevision, "no-change rename keeps revision");
        check(operations.rename(other, id, "Stolen").status() == RegionOperationStatus.NOT_OWNER,
                "foreign rename rejected");

        for (double valid : new double[] {0.125, 1.0, 8.0}) {
            var result = operations.updateTimeScale(owner, id, valid);
            check(result.succeeded(), "valid scale " + valid);
            check(result.region().orElseThrow().id().equals(id), "scale preserves uuid " + valid);
        }
        long scaleRevision = data.getById(id).orElseThrow().revision();
        check(operations.updateTimeScale(owner, id, 8.0).status() == RegionOperationStatus.NO_CHANGE,
                "same scale is no change");
        check(data.getById(id).orElseThrow().revision() == scaleRevision, "no-change scale keeps revision");
        for (double invalid : new double[] {Double.NaN, Double.POSITIVE_INFINITY, 0.124, 8.001}) {
            check(operations.updateTimeScale(owner, id, invalid).status() == RegionOperationStatus.INVALID_TIME_SCALE,
                    "invalid operation scale " + invalid);
        }
        check(operations.updateTimeScale(other, id, 2.0).status() == RegionOperationStatus.NOT_OWNER,
                "foreign scale update rejected");

        check(operations.updateMode(owner, id, null).status() == RegionOperationStatus.INVALID_MODE,
                "null mode rejected");
        check(operations.updateMode(owner, id, TemporalMode.SIMULATION_ONLY).succeeded(), "simulation mode update");
        TemporalRegion simulationOnly = data.getById(id).orElseThrow();
        check(TemporalRegionResolver.allowsSimulation(simulationOnly, TemporalTarget.MOBS, true),
                "simulation-only participates");
        check(operations.updateMode(owner, id, TemporalMode.SIMULATION_AND_VISUAL).succeeded(),
                "combined mode update");
        check(TemporalRegionResolver.allowsSimulation(data.getById(id).orElseThrow(), TemporalTarget.MOBS, true),
                "combined mode participates");
        check(operations.updateMode(owner, id, TemporalMode.VISUAL_ONLY).succeeded(), "visual-only mode update");
        check(!TemporalRegionResolver.allowsSimulation(data.getById(id).orElseThrow(), TemporalTarget.MOBS, true),
                "visual-only skips simulation");
        check(operations.updateMode(owner, id, TemporalMode.SIMULATION_ONLY).succeeded(), "restore simulation mode");

        check(operations.updateTargets(owner, id, Set.of(TemporalTarget.PROJECTILES)).succeeded(),
                "projectile-only targets");
        TemporalRegion projectileOnly = data.getById(id).orElseThrow();
        check(TemporalRegionResolver.allowsSimulation(projectileOnly, TemporalTarget.PROJECTILES, true),
                "projectile target allowed");
        check(!TemporalRegionResolver.allowsSimulation(projectileOnly, TemporalTarget.MOBS, true),
                "mob target excluded");
        check(operations.updateTargets(owner, id, Set.of(TemporalTarget.MOBS)).succeeded(), "mob-only targets");
        TemporalRegion mobOnly = data.getById(id).orElseThrow();
        check(TemporalRegionResolver.allowsSimulation(mobOnly, TemporalTarget.MOBS, true), "mob target allowed");
        check(!TemporalRegionResolver.allowsSimulation(mobOnly, TemporalTarget.PROJECTILES, true),
                "projectile target excluded");
        check(operations.updateTargets(owner, id, Set.of(TemporalTarget.BLOCK_ENTITIES)).succeeded(),
                "block-entity-only targets");
        check(TemporalRegionResolver.allowsSimulation(data.getById(id).orElseThrow(), TemporalTarget.BLOCK_ENTITIES, true),
                "block entity target allowed");
        check(!TemporalRegionResolver.allowsSimulation(data.getById(id).orElseThrow(), TemporalTarget.BLOCK_ENTITIES, false),
                "global config cannot be overridden by region");
        check(!TemporalRegionResolver.allowsSimulation(data.getById(id).orElseThrow(), TemporalTarget.PLAYER, true),
                "unimplemented target remains inactive");
        check(operations.updateTargets(owner, id, null).status() == RegionOperationStatus.INVALID_TARGETS,
                "null target set rejected");
        long targetRevision = data.getById(id).orElseThrow().revision();
        check(operations.updateTargets(owner, id, Set.of(TemporalTarget.BLOCK_ENTITIES)).status()
                == RegionOperationStatus.NO_CHANGE, "identical targets are no change");
        check(data.getById(id).orElseThrow().revision() == targetRevision,
                "identical targets keep revision");
        check(TemporalTarget.MOBS != TemporalTarget.OTHER_ENTITIES
                && TemporalTarget.PROJECTILES != TemporalTarget.OTHER_ENTITIES,
                "entity target categories are exclusive enum values");

        check(operations.setEnabled(owner, id, false).succeeded(), "disable succeeds");
        check(!data.getActiveRegions(Level.OVERWORLD).stream().anyMatch(region -> region.id().equals(id)),
                "disabled removed from active resolver input");
        check(operations.setEnabled(owner, id, true).succeeded(), "valid enable succeeds");
        TemporalRegion disabledOverlap = region(other, Level.OVERWORLD, 0, 9, false);
        check(data.create(other, disabledOverlap), "disabled overlap prepared");
        long disabledRevision = disabledOverlap.revision();
        check(operations.setEnabled(other, disabledOverlap.id(), true).status() == RegionOperationStatus.OVERLAP,
                "conflicting enable rejected");
        check(data.getById(disabledOverlap.id()).orElseThrow().revision() == disabledRevision,
                "failed enable keeps revision");

        check(operations.get(owner, id).succeeded(), "owner details lookup");
        check(operations.get(other, id).status() == RegionOperationStatus.NOT_OWNER, "foreign details rejected");
        check(operations.list(owner).size() == 2, "owner list contains all owner regions");
        check(operations.list(owner).stream().noneMatch(region -> region.owner().equals(other)),
                "owner list excludes foreign regions");
        RegionManagementPayloads.RegionSummary summary = RegionManagementPayloads.RegionSummary.from(
                data.getById(id).orElseThrow());
        check(summary.id().equals(id) && summary.name().equals(data.getById(id).orElseThrow().name())
                && summary.targets().equals(data.getById(id).orElseThrow().targets()), "summary matches stored data");

        check(operations.delete(other, id).status() == RegionOperationStatus.NOT_OWNER, "foreign delete rejected");
        check(operations.delete(owner, id).succeeded(), "owner delete succeeds");
        check(data.getById(id).isEmpty(), "deleted region absent from lookup");
        check(data.getByOwner(owner).stream().noneMatch(region -> region.id().equals(id)),
                "deleted region absent from preview/list source");

        UUID persistedId = operations.list(owner).getFirst().id();
        operations.rename(owner, persistedId, "Persistent");
        operations.updateMode(owner, persistedId, TemporalMode.SIMULATION_AND_VISUAL);
        operations.updateTargets(owner, persistedId, Set.of(TemporalTarget.MOBS, TemporalTarget.PROJECTILES));
        operations.setEnabled(owner, persistedId, false);
        TemporalRegion beforeSave = data.getById(persistedId).orElseThrow();
        TemporalRegionSavedData reloaded = TemporalRegionSavedData.load(
                data.save(new net.minecraft.nbt.CompoundTag(), null), null);
        TemporalRegion afterSave = reloaded.getById(persistedId).orElseThrow();
        check(afterSave.name().equals("Persistent"), "rename persists");
        check(afterSave.mode() == TemporalMode.SIMULATION_AND_VISUAL, "mode persists");
        check(afterSave.targets().equals(Set.of(TemporalTarget.MOBS, TemporalTarget.PROJECTILES)), "targets persist");
        check(!afterSave.enabled(), "enabled persists");
        check(afterSave.revision() == beforeSave.revision(), "revision persists");
    }

    private static PlayerSelection selection(int minX, int maxX, double scale) {
        PlayerSelection selection = new PlayerSelection();
        selection.select(Level.OVERWORLD, new BlockPos(minX, 0, 0));
        selection.select(Level.OVERWORLD, new BlockPos(maxX, 9, 9));
        check(selection.updateTimeScale(selection.getRevision(), scale), "prepare selection scale");
        return selection;
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
