package io.github.korteexz.ragephysics.network;

import io.github.korteexz.ragephysics.RagePhysics;
import io.github.korteexz.ragephysics.selection.PlayerSelection;
import io.github.korteexz.ragephysics.selection.SelectionManager;
import io.github.korteexz.ragephysics.timestamper.region.TemporalRegion;
import io.github.korteexz.ragephysics.timestamper.region.RegionOperationResult;
import io.github.korteexz.ragephysics.timestamper.region.RegionOperationStatus;
import io.github.korteexz.ragephysics.timestamper.region.TemporalRegionOperations;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
                                IPayloadHandler<TemporalEntityPayload> entityHandler,
                                IPayloadHandler<RegionManagementPayloads.ListResponse> listHandler,
                                IPayloadHandler<RegionManagementPayloads.DetailsResponse> detailsHandler,
                                IPayloadHandler<RegionManagementPayloads.OperationResponse> operationHandler) {
        var registrar = event.registrar("4");
        // O registrar executa os handlers na thread principal do respectivo lado lógico.
        registrar.playToServer(RegionControlPayloads.Request.TYPE, RegionControlPayloads.Request.CODEC,
                RegionControlNetworking::request);
        registrar.playToClient(RegionControlPayloads.Open.TYPE, RegionControlPayloads.Open.CODEC, openHandler);
        registrar.playToClient(TemporalEntityPayload.TYPE, TemporalEntityPayload.CODEC, entityHandler);
        registrar.playToServer(RegionControlPayloads.Update.TYPE, RegionControlPayloads.Update.CODEC,
                RegionControlNetworking::update);
        registrar.playToServer(RegionManagementPayloads.RequestList.TYPE, RegionManagementPayloads.RequestList.CODEC,
                RegionControlNetworking::list);
        registrar.playToServer(RegionManagementPayloads.RequestDetails.TYPE, RegionManagementPayloads.RequestDetails.CODEC,
                RegionControlNetworking::details);
        registrar.playToServer(RegionManagementPayloads.CreateFromSelection.TYPE, RegionManagementPayloads.CreateFromSelection.CODEC,
                RegionControlNetworking::create);
        registrar.playToServer(RegionManagementPayloads.Rename.TYPE, RegionManagementPayloads.Rename.CODEC,
                RegionControlNetworking::rename);
        registrar.playToServer(RegionManagementPayloads.UpdateTimeScale.TYPE, RegionManagementPayloads.UpdateTimeScale.CODEC,
                RegionControlNetworking::updateTimeScale);
        registrar.playToServer(RegionManagementPayloads.UpdateMode.TYPE, RegionManagementPayloads.UpdateMode.CODEC,
                RegionControlNetworking::updateMode);
        registrar.playToServer(RegionManagementPayloads.UpdateTargets.TYPE, RegionManagementPayloads.UpdateTargets.CODEC,
                RegionControlNetworking::updateTargets);
        registrar.playToServer(RegionManagementPayloads.SetEnabled.TYPE, RegionManagementPayloads.SetEnabled.CODEC,
                RegionControlNetworking::setEnabled);
        registrar.playToServer(RegionManagementPayloads.Delete.TYPE, RegionManagementPayloads.Delete.CODEC,
                RegionControlNetworking::delete);
        registrar.playToClient(RegionManagementPayloads.ListResponse.TYPE, RegionManagementPayloads.ListResponse.CODEC,
                listHandler);
        registrar.playToClient(RegionManagementPayloads.DetailsResponse.TYPE, RegionManagementPayloads.DetailsResponse.CODEC,
                detailsHandler);
        registrar.playToClient(RegionManagementPayloads.OperationResponse.TYPE, RegionManagementPayloads.OperationResponse.CODEC,
                operationHandler);
    }

    private static void request(RegionControlPayloads.Request payload, IPayloadContext context) {
        PlayerSelection selection = SelectionManager.get(context.player());
        if (!selection.isComplete() || !selection.isInDimension(context.player().level().dimension())) {
            context.player().displayClientMessage(Component.translatable("message.ragephysics.incomplete_selection"), true);
            return;
        }
        ServerPlayer player = serverPlayer(context);
        if (player == null) return;
        TemporalRegion region = selection.getRegionId() == null ? null
                : TemporalRegionOperations.forPlayer(player).get(player.getUUID(), selection.getRegionId())
                        .region().orElse(null);
        if (region == null) {
            context.player().displayClientMessage(Component.translatable("message.ragephysics.selection_changed"), true);
            return;
        }
        context.reply(new RegionControlPayloads.Open(region.id(), selection.getDimension().location(),
                selection.getPosA(), selection.getPosB(), region.revision(), region.timeScale()));
    }

    private static void update(RegionControlPayloads.Update payload, IPayloadContext context) {
        ServerPlayer player = serverPlayer(context);
        if (player == null) return;
        RegionOperationResult result = TemporalRegionOperations.forPlayer(player)
                .updateTimeScale(player.getUUID(), payload.regionId(), payload.timeScale());
        if (!result.succeeded() && result.status() != RegionOperationStatus.NO_CHANGE) {
            context.player().displayClientMessage(Component.translatable("message.ragephysics.selection_changed"), true);
        }
    }

    private static void list(RegionManagementPayloads.RequestList payload, IPayloadContext context) {
        ServerPlayer player = serverPlayer(context);
        if (player == null) return;
        var summaries = TemporalRegionOperations.forPlayer(player).list(player.getUUID()).stream()
                .map(RegionManagementPayloads.RegionSummary::from).toList();
        PlayerSelection selection = SelectionManager.get(player);
        RegionManagementPayloads.DraftSummary draft = selection.isComplete()
                ? new RegionManagementPayloads.DraftSummary(selection.getDimension().location(),
                        selection.getPosA(), selection.getPosB())
                : null;
        context.reply(new RegionManagementPayloads.ListResponse(summaries, draft));
    }

    private static void details(RegionManagementPayloads.RequestDetails payload, IPayloadContext context) {
        ServerPlayer player = serverPlayer(context);
        if (player == null) return;
        RegionOperationResult result = TemporalRegionOperations.forPlayer(player)
                .get(player.getUUID(), payload.regionId());
        context.reply(new RegionManagementPayloads.DetailsResponse(wireStatus(result),
                result.region().map(RegionManagementPayloads.RegionSummary::from).orElse(null)));
    }

    private static void create(RegionManagementPayloads.CreateFromSelection payload, IPayloadContext context) {
        ServerPlayer player = serverPlayer(context);
        if (player == null) return;
        RegionOperationResult result = TemporalRegionOperations.forPlayer(player)
                .createFromSelection(player, SelectionManager.get(player));
        if (result.succeeded()) SelectionManager.get(player).clear();
        reply(context, RegionManagementPayloads.OperationType.CREATE, result);
    }

    private static void rename(RegionManagementPayloads.Rename payload, IPayloadContext context) {
        ServerPlayer player = serverPlayer(context);
        if (player != null) reply(context, RegionManagementPayloads.OperationType.RENAME, TemporalRegionOperations.forPlayer(player)
                .rename(player.getUUID(), payload.regionId(), payload.name()));
    }

    private static void updateTimeScale(RegionManagementPayloads.UpdateTimeScale payload, IPayloadContext context) {
        ServerPlayer player = serverPlayer(context);
        if (player != null) reply(context, RegionManagementPayloads.OperationType.TIME_SCALE, TemporalRegionOperations.forPlayer(player)
                .updateTimeScale(player.getUUID(), payload.regionId(), payload.timeScale()));
    }

    private static void updateMode(RegionManagementPayloads.UpdateMode payload, IPayloadContext context) {
        ServerPlayer player = serverPlayer(context);
        if (player != null) reply(context, RegionManagementPayloads.OperationType.MODE, TemporalRegionOperations.forPlayer(player)
                .updateMode(player.getUUID(), payload.regionId(), payload.mode()));
    }

    private static void updateTargets(RegionManagementPayloads.UpdateTargets payload, IPayloadContext context) {
        ServerPlayer player = serverPlayer(context);
        if (player != null) reply(context, RegionManagementPayloads.OperationType.TARGETS, TemporalRegionOperations.forPlayer(player)
                .updateTargets(player.getUUID(), payload.regionId(), payload.targets()));
    }

    private static void setEnabled(RegionManagementPayloads.SetEnabled payload, IPayloadContext context) {
        ServerPlayer player = serverPlayer(context);
        if (player != null) reply(context, RegionManagementPayloads.OperationType.ENABLED, TemporalRegionOperations.forPlayer(player)
                .setEnabled(player.getUUID(), payload.regionId(), payload.enabled()));
    }

    private static void delete(RegionManagementPayloads.Delete payload, IPayloadContext context) {
        ServerPlayer player = serverPlayer(context);
        if (player != null) reply(context, RegionManagementPayloads.OperationType.DELETE, TemporalRegionOperations.forPlayer(player)
                .delete(player.getUUID(), payload.regionId()));
    }

    private static void reply(IPayloadContext context, RegionManagementPayloads.OperationType operation,
            RegionOperationResult result) {
        context.reply(new RegionManagementPayloads.OperationResponse(operation, wireStatus(result),
                result.region().map(RegionManagementPayloads.RegionSummary::from).orElse(null)));
    }

    private static RegionOperationStatus wireStatus(RegionOperationResult result) {
        return result.status() == RegionOperationStatus.NOT_OWNER
                ? RegionOperationStatus.NOT_FOUND
                : result.status();
    }

    private static ServerPlayer serverPlayer(IPayloadContext context) {
        return context.player() instanceof ServerPlayer player ? player : null;
    }

    @EventBusSubscriber(modid = RagePhysics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.DEDICATED_SERVER)
    public static final class ServerRegistration {
        @SubscribeEvent
        public static void registerPayloads(RegisterPayloadHandlersEvent event) {
            // Registra o mesmo protocolo; payloads clientbound não são recebidos neste lado físico.
            register(event, (payload, context) -> {}, (payload, context) -> {},
                    (payload, context) -> {}, (payload, context) -> {}, (payload, context) -> {});
        }
    }

    private RegionControlNetworking() {}
}
