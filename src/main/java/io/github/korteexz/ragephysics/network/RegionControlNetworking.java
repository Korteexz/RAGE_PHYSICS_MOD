package io.github.korteexz.ragephysics.network;

import io.github.korteexz.ragephysics.RagePhysics;
import io.github.korteexz.ragephysics.selection.PlayerSelection;
import io.github.korteexz.ragephysics.selection.SelectionManager;
import io.github.korteexz.ragephysics.timestamper.region.TemporalRegion;
import io.github.korteexz.ragephysics.timestamper.region.TemporalRegionSavedData;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public final class RegionControlNetworking {
    /** Cada lado físico fornece seu handler de abertura, sem carregar classes client no servidor. */
    public static void register(RegisterPayloadHandlersEvent event,
                                IPayloadHandler<RegionControlPayloads.Open> openHandler,
                                IPayloadHandler<TemporalEntityPayload> entityHandler) {
        var registrar = event.registrar("2");
        // O registrar executa os handlers na thread principal do respectivo lado lógico.
        registrar.playToServer(RegionControlPayloads.Request.TYPE, RegionControlPayloads.Request.CODEC,
                RegionControlNetworking::request);
        registrar.playToClient(RegionControlPayloads.Open.TYPE, RegionControlPayloads.Open.CODEC, openHandler);
        registrar.playToClient(TemporalEntityPayload.TYPE, TemporalEntityPayload.CODEC, entityHandler);
        registrar.playToServer(RegionControlPayloads.Update.TYPE, RegionControlPayloads.Update.CODEC,
                RegionControlNetworking::update);
    }

    private static void request(RegionControlPayloads.Request payload, IPayloadContext context) {
        PlayerSelection selection = SelectionManager.get(context.player());
        if (!selection.isComplete() || !selection.isInDimension(context.player().level().dimension())) {
            context.player().displayClientMessage(Component.translatable("message.ragephysics.incomplete_selection"), true);
            return;
        }
        TemporalRegion region = selection.getRegionId() == null ? null
                : TemporalRegionSavedData.get((net.minecraft.server.level.ServerLevel) context.player().level())
                        .getById(selection.getRegionId()).orElse(null);
        if (region == null || !region.owner().equals(context.player().getUUID())) {
            context.player().displayClientMessage(Component.translatable("message.ragephysics.selection_changed"), true);
            return;
        }
        context.reply(new RegionControlPayloads.Open(selection.getDimension().location(), selection.getPosA(), selection.getPosB(),
                selection.getRevision(), region.timeScale()));
    }

    private static void update(RegionControlPayloads.Update payload, IPayloadContext context) {
        // A identidade vem da conexão: o cliente nunca escolhe a seleção de outro jogador.
        PlayerSelection selection = SelectionManager.get(context.player());
        TemporalRegionSavedData data = TemporalRegionSavedData.get((net.minecraft.server.level.ServerLevel) context.player().level());
        TemporalRegion region = selection.getRegionId() == null ? null : data.getById(selection.getRegionId()).orElse(null);
        if (!selection.isInDimension(context.player().level().dimension()) || region == null
                || !region.owner().equals(context.player().getUUID())
                || !selection.updateTimeScale(payload.revision(), payload.timeScale())
                || !data.updateTimeScale(context.player().getUUID(), region.id(), payload.timeScale())) {
            context.player().displayClientMessage(Component.translatable("message.ragephysics.selection_changed"), true);
        }
    }

    @EventBusSubscriber(modid = RagePhysics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.DEDICATED_SERVER)
    public static final class ServerRegistration {
        @SubscribeEvent
        public static void registerPayloads(RegisterPayloadHandlersEvent event) {
            // Registra o mesmo protocolo; payloads clientbound não são recebidos neste lado físico.
            register(event, (payload, context) -> {}, (payload, context) -> {});
        }
    }

    private RegionControlNetworking() {}
}
