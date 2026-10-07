package io.github.korteexz.ragephysics.event;

import io.github.korteexz.ragephysics.RagePhysics;
import io.github.korteexz.ragephysics.network.TemporalEntityPayload;
import io.github.korteexz.ragephysics.selection.SelectionManager;
import io.github.korteexz.ragephysics.temporal.TemporalBlockEntityTicker;
import io.github.korteexz.ragephysics.temporal.TemporalEntityTicker;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = RagePhysics.MODID)
public final class TemporalLifecycleHandler {
    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            double scale = TemporalEntityTicker.scale(event.getTarget());
            if (scale != 1.0) PacketDistributor.sendToPlayer(player, TemporalEntityPayload.of(event.getTarget(), scale));
        }
    }

    @SubscribeEvent
    public static void leaving(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) TemporalEntityTicker.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!event.getEntity().level().isClientSide()) SelectionManager.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
            TemporalEntityTicker.clear(level);
            TemporalBlockEntityTicker.clear(level);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        SelectionManager.clear();
        TemporalEntityTicker.clear();
        TemporalBlockEntityTicker.clear();
    }

    private TemporalLifecycleHandler() {}
}
