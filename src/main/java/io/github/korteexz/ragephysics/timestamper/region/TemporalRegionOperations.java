package io.github.korteexz.ragephysics.timestamper.region;

import io.github.korteexz.ragephysics.selection.PlayerSelection;
import io.github.korteexz.ragephysics.timestamper.config.TemporalMode;
import io.github.korteexz.ragephysics.timestamper.config.TemporalTarget;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Small server-side API for user intent, ownership and input validation. */
public final class TemporalRegionOperations {
    public static final int MAX_NAME_LENGTH = 48;
    private static final Set<TemporalTarget> DEFAULT_TARGETS = Set.of(
            TemporalTarget.MOBS, TemporalTarget.PROJECTILES,
            TemporalTarget.OTHER_ENTITIES, TemporalTarget.BLOCK_ENTITIES);

    private final TemporalRegionSavedData data;

    public TemporalRegionOperations(TemporalRegionSavedData data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    public static TemporalRegionOperations forPlayer(ServerPlayer player) {
        return new TemporalRegionOperations(TemporalRegionSavedData.get(player.serverLevel()));
    }

    public RegionOperationResult createFromSelection(ServerPlayer requester, PlayerSelection selection) {
        Objects.requireNonNull(requester, "requester");
        return createFromSelection(requester.getUUID(), requester.level().dimension(), selection);
    }

    public RegionOperationResult createFromSelection(UUID requester, ResourceKey<Level> currentDimension,
            PlayerSelection selection) {
        Objects.requireNonNull(requester, "requester");
        Objects.requireNonNull(currentDimension, "currentDimension");
        if (selection == null || !selection.hasPosA()) return RegionOperationResult.failure(RegionOperationStatus.NO_SELECTION);
        if (!selection.isComplete()) return RegionOperationResult.failure(RegionOperationStatus.INCOMPLETE_SELECTION);
        if (!selection.isInDimension(currentDimension)) return RegionOperationResult.failure(RegionOperationStatus.INVALID_DIMENSION);

        TemporalRegion region = new TemporalRegion(UUID.randomUUID(), requester, nextDefaultName(requester),
                currentDimension, new TemporalRegionBounds(selection.getPosA(), selection.getPosB()),
                selection.getTimeScale(), true, TemporalMode.SIMULATION_AND_VISUAL, DEFAULT_TARGETS, 0);
        return data.create(requester, region)
                ? RegionOperationResult.success(region)
                : RegionOperationResult.failure(RegionOperationStatus.OVERLAP);
    }

    public RegionOperationResult rename(UUID requester, UUID regionId, String name) {
        TemporalRegion current = ownedRegion(requester, regionId);
        if (current == null) return missingOrForeign(requester, regionId);
        if (name == null) return RegionOperationResult.failure(RegionOperationStatus.INVALID_NAME);
        String normalized = name.trim();
        if (normalized.isEmpty() || normalized.length() > MAX_NAME_LENGTH) {
            return RegionOperationResult.failure(RegionOperationStatus.INVALID_NAME);
        }
        if (normalized.equals(current.name())) return RegionOperationResult.failure(RegionOperationStatus.NO_CHANGE);
        return update(requester, copy(current, normalized, current.timeScale(), current.enabled(), current.mode(), current.targets()),
                RegionOperationStatus.OVERLAP);
    }

    public RegionOperationResult updateTimeScale(UUID requester, UUID regionId, double timeScale) {
        TemporalRegion current = ownedRegion(requester, regionId);
        if (current == null) return missingOrForeign(requester, regionId);
        if (!TemporalRegion.isValidTimeScale(timeScale)) {
            return RegionOperationResult.failure(RegionOperationStatus.INVALID_TIME_SCALE);
        }
        if (Double.compare(current.timeScale(), timeScale) == 0) {
            return RegionOperationResult.failure(RegionOperationStatus.NO_CHANGE);
        }
        return update(requester, copy(current, current.name(), timeScale, current.enabled(), current.mode(), current.targets()),
                RegionOperationStatus.OVERLAP);
    }

    public RegionOperationResult updateMode(UUID requester, UUID regionId, TemporalMode mode) {
        TemporalRegion current = ownedRegion(requester, regionId);
        if (current == null) return missingOrForeign(requester, regionId);
        if (mode == null) return RegionOperationResult.failure(RegionOperationStatus.INVALID_MODE);
        if (current.mode() == mode) return RegionOperationResult.failure(RegionOperationStatus.NO_CHANGE);
        return update(requester, copy(current, current.name(), current.timeScale(), current.enabled(), mode, current.targets()),
                RegionOperationStatus.OVERLAP);
    }

    public RegionOperationResult updateTargets(UUID requester, UUID regionId, Set<TemporalTarget> targets) {
        TemporalRegion current = ownedRegion(requester, regionId);
        if (current == null) return missingOrForeign(requester, regionId);
        if (targets == null) {
            return RegionOperationResult.failure(RegionOperationStatus.INVALID_TARGETS);
        }
        EnumSet<TemporalTarget> normalized = EnumSet.noneOf(TemporalTarget.class);
        for (TemporalTarget target : targets) {
            if (target == null) return RegionOperationResult.failure(RegionOperationStatus.INVALID_TARGETS);
            normalized.add(target);
        }
        if (current.targets().equals(normalized)) return RegionOperationResult.failure(RegionOperationStatus.NO_CHANGE);
        return update(requester, copy(current, current.name(), current.timeScale(), current.enabled(), current.mode(), normalized),
                RegionOperationStatus.OVERLAP);
    }

    public RegionOperationResult setEnabled(UUID requester, UUID regionId, boolean enabled) {
        TemporalRegion current = ownedRegion(requester, regionId);
        if (current == null) return missingOrForeign(requester, regionId);
        if (current.enabled() == enabled) return RegionOperationResult.failure(RegionOperationStatus.NO_CHANGE);
        if (!data.setEnabled(requester, regionId, enabled)) {
            return RegionOperationResult.failure(RegionOperationStatus.OVERLAP);
        }
        return RegionOperationResult.success(data.getById(regionId).orElseThrow());
    }

    public RegionOperationResult delete(UUID requester, UUID regionId) {
        TemporalRegion current = ownedRegion(requester, regionId);
        if (current == null) return missingOrForeign(requester, regionId);
        return data.remove(requester, regionId)
                ? RegionOperationResult.success(current)
                : RegionOperationResult.failure(RegionOperationStatus.NOT_OWNER);
    }

    public List<TemporalRegion> list(UUID requester) {
        return data.getByOwner(Objects.requireNonNull(requester, "requester"));
    }

    public RegionOperationResult get(UUID requester, UUID regionId) {
        TemporalRegion current = ownedRegion(requester, regionId);
        return current == null ? missingOrForeign(requester, regionId) : RegionOperationResult.success(current);
    }

    private RegionOperationResult update(UUID requester, TemporalRegion proposed, RegionOperationStatus failure) {
        if (!data.update(requester, proposed)) return RegionOperationResult.failure(failure);
        return RegionOperationResult.success(data.getById(proposed.id()).orElseThrow());
    }

    private TemporalRegion ownedRegion(UUID requester, UUID regionId) {
        if (requester == null || regionId == null) return null;
        TemporalRegion region = data.getById(regionId).orElse(null);
        return region != null && region.owner().equals(requester) ? region : null;
    }

    private RegionOperationResult missingOrForeign(UUID requester, UUID regionId) {
        TemporalRegion region = regionId == null ? null : data.getById(regionId).orElse(null);
        return RegionOperationResult.failure(region == null
                ? RegionOperationStatus.NOT_FOUND : RegionOperationStatus.NOT_OWNER);
    }

    private String nextDefaultName(UUID owner) {
        Set<String> names = new HashSet<>();
        for (TemporalRegion region : data.getByOwner(owner)) names.add(region.name());
        for (int index = 1; index < Integer.MAX_VALUE; index++) {
            String candidate = "Region " + index;
            if (!names.contains(candidate)) return candidate;
        }
        throw new IllegalStateException("No available default region name");
    }

    private static TemporalRegion copy(TemporalRegion region, String name, double timeScale, boolean enabled,
            TemporalMode mode, Set<TemporalTarget> targets) {
        return new TemporalRegion(region.id(), region.owner(), name, region.dimension(), region.bounds(),
                timeScale, enabled, mode, targets, region.revision());
    }
}
