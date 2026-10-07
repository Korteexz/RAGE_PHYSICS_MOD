package io.github.korteexz.ragephysics.client;

import io.github.korteexz.ragephysics.RagePhysics;
import io.github.korteexz.ragephysics.network.TemporalEntityPayload;
import io.github.korteexz.ragephysics.temporal.TemporalTickBudget;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/** Apresentação de snapshots do servidor; não consulta posição da câmera nem config local. */
@EventBusSubscriber(modid = RagePhysics.MODID, value = Dist.CLIENT)
public final class TemporalEntityPresentation {
    private static final Map<Entity, State> STATES = new WeakHashMap<>();

    public static void receive(TemporalEntityPayload payload) {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().equals(payload.dimension()) || !TemporalTickBudget.isValid(payload.scale())) return;
        Entity entity = level.getEntity(payload.entityId());
        if (entity == null || !entity.getUUID().equals(payload.entityUuid()) || entity instanceof Player) return;
        if (payload.scale() == 1.0) {
            if (STATES.remove(entity) != null) {
                entity.setPos(payload.position());
                entity.setYRot(payload.yaw());
                entity.setXRot(payload.pitch());
                entity.setDeltaMovement(payload.velocity());
                entity.setOldPosAndRot();
            }
            return;
        }
        State state = STATES.computeIfAbsent(entity, ignored -> new State());
        state.scale = payload.scale();
        state.target = payload.position();
        state.yaw = payload.yaw();
        state.pitch = payload.pitch();
        state.remaining = Math.max(1, (int) Math.ceil(1.0 / payload.scale()));
        entity.setDeltaMovement(payload.velocity());
        // Teleportes grandes não são animados atravessando paredes/chunks.
        if (entity.position().distanceToSqr(state.target) > 4096.0) {
            entity.setPos(state.target);
            entity.setOldPosAndRot();
        }
    }

    public static boolean manages(Entity entity) {
        return !(entity instanceof Player) && !entity.isPassenger() && !entity.isVehicle() && STATES.containsKey(entity);
    }

    public static void tick(Entity entity, Runnable vanillaDispatcher) {
        State state = STATES.get(entity);
        if (state == null || !manages(entity)) {
            STATES.remove(entity);
            vanillaDispatcher.run();
            return;
        }
        if (state.running) return;
        Vec3 previous = entity.position();
        float previousYaw = entity.getYRot();
        float previousPitch = entity.getXRot();
        int steps = state.budget.advance(state, 0, state.scale);
        state.running = true;
        try {
            // Projéteis são apresentados por snapshots: não prevê colisões numa posição interpolada.
            if (entity instanceof Projectile) {
                entity.tickCount += steps;
            } else {
                // Mantém animações locais no ritmo do sujeito; a posição final vem do servidor.
                for (int step = 0; step < steps && !entity.isRemoved(); step++) vanillaDispatcher.run();
            }
        } finally {
            state.running = false;
        }
        if (entity.isRemoved()) return;
        double alpha = state.remaining > 0 ? 1.0 / state.remaining : 1.0;
        entity.setPos(previous.lerp(state.target, alpha));
        entity.setYRot(Mth.rotLerp((float) alpha, previousYaw, state.yaw));
        entity.setXRot(Mth.lerp((float) alpha, previousPitch, state.pitch));
        entity.xo = entity.xOld = previous.x;
        entity.yo = entity.yOld = previous.y;
        entity.zo = entity.zOld = previous.z;
        entity.yRotO = previousYaw;
        entity.xRotO = previousPitch;
        if (state.remaining > 0) state.remaining--;
    }

    @SubscribeEvent
    public static void leaving(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) STATES.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) STATES.clear();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { STATES.clear(); }

    private static final class State {
        final TemporalTickBudget budget = new TemporalTickBudget();
        Vec3 target = Vec3.ZERO;
        double scale;
        float yaw;
        float pitch;
        int remaining;
        boolean running;
    }

    private TemporalEntityPresentation() {}
}
