package io.github.korteexz.ragephysics.temporal;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

public enum TemporalTarget {
    PLAYERS(false),
    LIVING_ENTITIES(true),
    PROJECTILES(true),
    OTHER_ENTITIES(true),
    BLOCK_ENTITIES(true),
    SCHEDULED_BLOCK_TICKS(false),
    RANDOM_BLOCK_TICKS(false),
    FLUIDS(false);

    private final boolean implemented;

    TemporalTarget(boolean implemented) {
        this.implemented = implemented;
    }

    public boolean implemented() {
        return implemented;
    }

    /** Classificação exclusiva: nunca aplica Entity e LivingEntity separadamente. */
    public static TemporalTarget classify(Entity entity) {
        if (entity instanceof Player) return PLAYERS;
        if (entity instanceof Projectile) return PROJECTILES;
        if (entity instanceof LivingEntity) return LIVING_ENTITIES;
        return OTHER_ENTITIES;
    }
}
