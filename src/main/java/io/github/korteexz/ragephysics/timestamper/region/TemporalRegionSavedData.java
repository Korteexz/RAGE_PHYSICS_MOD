package io.github.korteexz.ragephysics.timestamper.region;

import io.github.korteexz.ragephysics.selection.PlayerSelection;
import io.github.korteexz.ragephysics.timestamper.config.TemporalMode;
import io.github.korteexz.ragephysics.timestamper.config.TemporalTarget;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent, server-authoritative catalog of all Timestamper regions in a world. */
public final class TemporalRegionSavedData extends SavedData {
    public static final String FILE_ID = "ragephysics_temporal_regions";
    private static final Set<TemporalTarget> PROTOTYPE_TARGETS = EnumSet.of(
            TemporalTarget.MOBS, TemporalTarget.PROJECTILES,
            TemporalTarget.OTHER_ENTITIES, TemporalTarget.BLOCK_ENTITIES);
    private final Map<UUID, TemporalRegion> regions = new LinkedHashMap<>();

    public static Factory<TemporalRegionSavedData> factory() {
        return new Factory<>(TemporalRegionSavedData::new, TemporalRegionSavedData::load);
    }

    /** Uses the Overworld data storage as the single catalog for every dimension. */
    public static TemporalRegionSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(factory(), FILE_ID);
    }

    public Optional<TemporalRegion> getById(UUID id) {
        return Optional.ofNullable(regions.get(id));
    }

    public List<TemporalRegion> getByOwner(UUID owner) {
        return regions.values().stream().filter(region -> region.owner().equals(owner)).toList();
    }

    public List<TemporalRegion> getActiveRegions(ResourceKey<Level> dimension) {
        return regions.values().stream()
                .filter(TemporalRegion::enabled)
                .filter(region -> region.dimension().equals(dimension))
                .toList();
    }

    public List<TemporalRegion> allRegions() {
        return List.copyOf(regions.values());
    }

    public Optional<TemporalRegion> createForSelection(UUID owner, PlayerSelection selection) {
        if (!selection.isComplete()) return Optional.empty();
        UUID id = UUID.randomUUID();
        TemporalRegion region = new TemporalRegion(id, owner, "Region " + id.toString().substring(0, 8),
                selection.getDimension(), new TemporalRegionBounds(selection.getPosA(), selection.getPosB()),
                selection.getTimeScale(), true, TemporalMode.SIMULATION_AND_VISUAL, PROTOTYPE_TARGETS, 0);
        return create(owner, region) ? Optional.of(region) : Optional.empty();
    }

    public boolean create(UUID requester, TemporalRegion region) {
        if (!requester.equals(region.owner()) || regions.containsKey(region.id()) || conflicts(region, null)) return false;
        regions.put(region.id(), region);
        setDirty();
        return true;
    }

    /** Replaces all editable fields and advances revision after centralized validation. */
    public boolean update(UUID requester, TemporalRegion proposed) {
        TemporalRegion current = regions.get(proposed.id());
        if (current == null || !current.owner().equals(requester) || !proposed.owner().equals(requester)
                || conflicts(proposed, proposed.id())) return false;
        TemporalRegion updated = copyWithRevision(proposed, current.revision() + 1);
        regions.put(updated.id(), updated);
        setDirty();
        return true;
    }

    public boolean updateTimeScale(UUID requester, UUID id, double timeScale) {
        TemporalRegion current = regions.get(id);
        if (current == null || !current.owner().equals(requester) || !TemporalRegion.isValidTimeScale(timeScale)) return false;
        return update(requester, copy(current, current.bounds(), timeScale, current.enabled()));
    }

    public boolean setEnabled(UUID requester, UUID id, boolean enabled) {
        TemporalRegion current = regions.get(id);
        if (current == null || !current.owner().equals(requester)) return false;
        if (current.enabled() == enabled) return true;
        return update(requester, copy(current, current.bounds(), current.timeScale(), enabled));
    }

    public boolean remove(UUID requester, UUID id) {
        TemporalRegion current = regions.get(id);
        if (current == null || !current.owner().equals(requester)) return false;
        regions.remove(id);
        setDirty();
        return true;
    }

    /** Disabled regions do not reserve space. The edited region is ignored by id. */
    private boolean conflicts(TemporalRegion candidate, UUID ignoredId) {
        if (!candidate.enabled()) return false;
        for (TemporalRegion existing : regions.values()) {
            if ((ignoredId == null || !existing.id().equals(ignoredId))
                    && existing.enabled()
                    && existing.dimension().equals(candidate.dimension())
                    && existing.bounds().intersects(candidate.bounds())) return true;
        }
        return false;
    }

    private static TemporalRegion copy(TemporalRegion region, TemporalRegionBounds bounds, double timeScale, boolean enabled) {
        return new TemporalRegion(region.id(), region.owner(), region.name(), region.dimension(), bounds,
                timeScale, enabled, region.mode(), region.targets(), region.revision());
    }

    private static TemporalRegion copyWithRevision(TemporalRegion region, long revision) {
        return new TemporalRegion(region.id(), region.owner(), region.name(), region.dimension(), region.bounds(),
                region.timeScale(), region.enabled(), region.mode(), region.targets(), revision);
    }

    public static TemporalRegionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TemporalRegionSavedData data = new TemporalRegionSavedData();
        ListTag list = tag.getList("regions", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            TemporalRegion region;
            try {
                region = readRegion(list.getCompound(index));
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Invalid persisted temporal region at index " + index, exception);
            }
            if (data.regions.containsKey(region.id())) throw new IllegalStateException("Duplicate persisted temporal region id: " + region.id());
            if (data.conflicts(region, null)) throw new IllegalStateException("Overlapping persisted temporal region: " + region.id());
            data.regions.put(region.id(), region);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (TemporalRegion region : regions.values()) list.add(writeRegion(region));
        tag.put("regions", list);
        return tag;
    }

    private static CompoundTag writeRegion(TemporalRegion region) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", region.id());
        tag.putUUID("owner", region.owner());
        tag.putString("name", region.name());
        tag.putString("dimension", region.dimension().location().toString());
        tag.putInt("minX", region.bounds().min().getX());
        tag.putInt("minY", region.bounds().min().getY());
        tag.putInt("minZ", region.bounds().min().getZ());
        tag.putInt("maxX", region.bounds().max().getX());
        tag.putInt("maxY", region.bounds().max().getY());
        tag.putInt("maxZ", region.bounds().max().getZ());
        tag.putDouble("timeScale", region.timeScale());
        tag.putBoolean("enabled", region.enabled());
        tag.putString("mode", region.mode().name());
        tag.putLong("revision", region.revision());
        ListTag targets = new ListTag();
        for (TemporalTarget target : region.targets()) targets.add(StringTag.valueOf(target.name()));
        tag.put("targets", targets);
        return tag;
    }

    private static TemporalRegion readRegion(CompoundTag tag) {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("dimension")));
        TemporalRegionBounds bounds = new TemporalRegionBounds(
                new net.minecraft.core.BlockPos(tag.getInt("minX"), tag.getInt("minY"), tag.getInt("minZ")),
                new net.minecraft.core.BlockPos(tag.getInt("maxX"), tag.getInt("maxY"), tag.getInt("maxZ")));
        EnumSet<TemporalTarget> targets = EnumSet.noneOf(TemporalTarget.class);
        ListTag targetTags = tag.getList("targets", Tag.TAG_STRING);
        for (int index = 0; index < targetTags.size(); index++) targets.add(TemporalTarget.valueOf(targetTags.getString(index)));
        return new TemporalRegion(tag.getUUID("id"), tag.getUUID("owner"), tag.getString("name"), dimension,
                bounds, tag.getDouble("timeScale"), tag.getBoolean("enabled"),
                TemporalMode.valueOf(tag.getString("mode")), targets, tag.getLong("revision"));
    }
}
