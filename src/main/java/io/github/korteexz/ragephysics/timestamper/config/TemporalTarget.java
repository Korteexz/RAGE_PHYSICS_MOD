package io.github.korteexz.ragephysics.timestamper.config;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

/** Authoritative temporal target categories for regions and the current simulation. */
public enum TemporalTarget {
    PLAYER("players", false),
    MOBS("living_entities", true),
    PROJECTILES("projectiles", true),
    FALLING_BLOCKS("falling_blocks", false),
    OTHER_ENTITIES("other_entities", true),
    BLOCK_ENTITIES("block_entities", true),
    RANDOM_BLOCK_TICKS("random_block_ticks", false),
    SCHEDULED_BLOCK_TICKS("scheduled_block_ticks", false),
    FLUIDS("fluids", false),
    REDSTONE("redstone", false);

    private final String configKey;
    private final boolean implemented;

    TemporalTarget(String configKey, boolean implemented) {
        this.configKey = configKey;
        this.implemented = implemented;
    }

    /** Retains existing server-config keys while the domain names become authoritative. */
    public String configKey() {
        return configKey;
    }

    public boolean implemented() {
        return implemented;
    }

    /**
     * Exclusive current entity classification. Falling blocks remain OTHER_ENTITIES
     * until their dedicated category receives a separate simulation path.
     */
    public static TemporalTarget classify(Entity entity) {
        if (entity instanceof Player) return PLAYER;
        if (entity instanceof Projectile) return PROJECTILES;
        if (entity instanceof LivingEntity) return MOBS;
        return OTHER_ENTITIES;
    }
}
