package io.github.korteexz.ragephysics.timestamper.config;

/** Mutually exclusive future target categories for a temporal region. */
public enum TemporalTarget {
    PLAYER,
    MOBS,
    PROJECTILES,
    FALLING_BLOCKS,
    OTHER_ENTITIES,
    BLOCK_ENTITIES,
    RANDOM_BLOCK_TICKS,
    SCHEDULED_BLOCK_TICKS,
    FLUIDS,
    REDSTONE
}
