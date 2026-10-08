package io.github.korteexz.ragephysics.timestamper.region;

import io.github.korteexz.ragephysics.timestamper.config.TemporalMode;
import io.github.korteexz.ragephysics.timestamper.config.TemporalTarget;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Immutable domain description of one Timestamper region. */
public final class TemporalRegion {
    public static final double MIN_TIME_SCALE = 0.125;
    public static final double MAX_TIME_SCALE = 8.0;

    private final UUID id;
    private final UUID owner;
    private final String name;
    private final ResourceKey<Level> dimension;
    private final TemporalRegionBounds bounds;
    private final double timeScale;
    private final boolean enabled;
    private final TemporalMode mode;
    private final Set<TemporalTarget> targets;
    private final long revision;

    public TemporalRegion(UUID id, UUID owner, String name, ResourceKey<Level> dimension,
            TemporalRegionBounds bounds, double timeScale, boolean enabled, TemporalMode mode,
            Set<TemporalTarget> targets, long revision) {
        this.id = Objects.requireNonNull(id, "id");
        this.owner = Objects.requireNonNull(owner, "owner");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Region name must not be blank");
        this.name = name;
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        if (!isValidTimeScale(timeScale)) throw new IllegalArgumentException("Invalid temporal region scale: " + timeScale);
        this.timeScale = timeScale;
        this.enabled = enabled;
        this.mode = Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(targets, "targets");
        EnumSet<TemporalTarget> copiedTargets = EnumSet.noneOf(TemporalTarget.class);
        copiedTargets.addAll(targets);
        this.targets = Collections.unmodifiableSet(copiedTargets);
        if (revision < 0) throw new IllegalArgumentException("Revision must not be negative");
        this.revision = revision;
    }

    public static boolean isValidTimeScale(double timeScale) {
        return Double.isFinite(timeScale) && timeScale >= MIN_TIME_SCALE && timeScale <= MAX_TIME_SCALE;
    }

    public UUID id() { return id; }
    public UUID owner() { return owner; }
    public String name() { return name; }
    public ResourceKey<Level> dimension() { return dimension; }
    public TemporalRegionBounds bounds() { return bounds; }
    public double timeScale() { return timeScale; }
    public boolean enabled() { return enabled; }
    public TemporalMode mode() { return mode; }
    public Set<TemporalTarget> targets() { return targets; }
    public long revision() { return revision; }

    public boolean affects(TemporalTarget target) { return targets.contains(Objects.requireNonNull(target, "target")); }
}
