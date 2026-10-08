package io.github.korteexz.ragephysics.network;

import io.github.korteexz.ragephysics.RagePhysics;
import io.github.korteexz.ragephysics.timestamper.config.TemporalMode;
import io.github.korteexz.ragephysics.timestamper.config.TemporalTarget;
import io.github.korteexz.ragephysics.timestamper.region.RegionOperationStatus;
import io.github.korteexz.ragephysics.timestamper.region.TemporalRegion;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Typed wire contract for the future Region Manager. Owner identity is never client-provided. */
public final class RegionManagementPayloads {
    public record RegionSummary(UUID id, String name, ResourceLocation dimension, BlockPos min, BlockPos max,
            double timeScale, boolean enabled, TemporalMode mode, Set<TemporalTarget> targets, long revision) {
        public RegionSummary {
            targets = Set.copyOf(targets);
        }

        public static RegionSummary from(TemporalRegion region) {
            return new RegionSummary(region.id(), region.name(), region.dimension().location(),
                    region.bounds().min(), region.bounds().max(), region.timeScale(), region.enabled(),
                    region.mode(), region.targets(), region.revision());
        }
    }

    public record RequestList() implements CustomPacketPayload {
        public static final RequestList INSTANCE = new RequestList();
        public static final Type<RequestList> TYPE = new Type<>(id("request_region_list"));
        public static final StreamCodec<FriendlyByteBuf, RequestList> CODEC = StreamCodec.unit(INSTANCE);
        @Override public Type<RequestList> type() { return TYPE; }
    }

    public record RequestDetails(UUID regionId) implements CustomPacketPayload {
        public static final Type<RequestDetails> TYPE = new Type<>(id("request_region_details"));
        public static final StreamCodec<FriendlyByteBuf, RequestDetails> CODEC = uuidCodec(RequestDetails::new, RequestDetails::regionId);
        @Override public Type<RequestDetails> type() { return TYPE; }
    }

    public record CreateFromSelection() implements CustomPacketPayload {
        public static final CreateFromSelection INSTANCE = new CreateFromSelection();
        public static final Type<CreateFromSelection> TYPE = new Type<>(id("create_region_from_selection"));
        public static final StreamCodec<FriendlyByteBuf, CreateFromSelection> CODEC = StreamCodec.unit(INSTANCE);
        @Override public Type<CreateFromSelection> type() { return TYPE; }
    }

    public record Rename(UUID regionId, String name) implements CustomPacketPayload {
        public static final Type<Rename> TYPE = new Type<>(id("rename_region"));
        public static final StreamCodec<FriendlyByteBuf, Rename> CODEC = StreamCodec.of(
                (buf, value) -> { buf.writeUUID(value.regionId); buf.writeUtf(value.name, 256); },
                buf -> new Rename(buf.readUUID(), buf.readUtf(256)));
        @Override public Type<Rename> type() { return TYPE; }
    }

    public record UpdateTimeScale(UUID regionId, double timeScale) implements CustomPacketPayload {
        public static final Type<UpdateTimeScale> TYPE = new Type<>(id("manage_region_time_scale"));
        public static final StreamCodec<FriendlyByteBuf, UpdateTimeScale> CODEC = StreamCodec.of(
                (buf, value) -> { buf.writeUUID(value.regionId); buf.writeDouble(value.timeScale); },
                buf -> new UpdateTimeScale(buf.readUUID(), buf.readDouble()));
        @Override public Type<UpdateTimeScale> type() { return TYPE; }
    }

    public record UpdateMode(UUID regionId, TemporalMode mode) implements CustomPacketPayload {
        public static final Type<UpdateMode> TYPE = new Type<>(id("update_region_mode"));
        public static final StreamCodec<FriendlyByteBuf, UpdateMode> CODEC = StreamCodec.of(
                (buf, value) -> { buf.writeUUID(value.regionId); buf.writeEnum(value.mode); },
                buf -> new UpdateMode(buf.readUUID(), buf.readEnum(TemporalMode.class)));
        @Override public Type<UpdateMode> type() { return TYPE; }
    }

    public record UpdateTargets(UUID regionId, Set<TemporalTarget> targets) implements CustomPacketPayload {
        public UpdateTargets { targets = Set.copyOf(targets); }
        public static final Type<UpdateTargets> TYPE = new Type<>(id("update_region_targets"));
        public static final StreamCodec<FriendlyByteBuf, UpdateTargets> CODEC = StreamCodec.of(
                (buf, value) -> { buf.writeUUID(value.regionId); buf.writeVarInt(targetMask(value.targets)); },
                buf -> new UpdateTargets(buf.readUUID(), RegionManagementPayloads.targets(buf.readVarInt())));
        @Override public Type<UpdateTargets> type() { return TYPE; }
    }

    public record SetEnabled(UUID regionId, boolean enabled) implements CustomPacketPayload {
        public static final Type<SetEnabled> TYPE = new Type<>(id("set_region_enabled"));
        public static final StreamCodec<FriendlyByteBuf, SetEnabled> CODEC = StreamCodec.of(
                (buf, value) -> { buf.writeUUID(value.regionId); buf.writeBoolean(value.enabled); },
                buf -> new SetEnabled(buf.readUUID(), buf.readBoolean()));
        @Override public Type<SetEnabled> type() { return TYPE; }
    }

    public record Delete(UUID regionId) implements CustomPacketPayload {
        public static final Type<Delete> TYPE = new Type<>(id("delete_region"));
        public static final StreamCodec<FriendlyByteBuf, Delete> CODEC = uuidCodec(Delete::new, Delete::regionId);
        @Override public Type<Delete> type() { return TYPE; }
    }

    public record ListResponse(List<RegionSummary> regions) implements CustomPacketPayload {
        public ListResponse { regions = List.copyOf(regions); }
        public static final Type<ListResponse> TYPE = new Type<>(id("region_list"));
        public static final StreamCodec<FriendlyByteBuf, ListResponse> CODEC = StreamCodec.of(
                (buf, value) -> {
                    buf.writeVarInt(value.regions.size());
                    for (RegionSummary region : value.regions) writeSummary(buf, region);
                },
                buf -> {
                    int size = buf.readVarInt();
                    if (size < 0 || size > 4096) throw new IllegalArgumentException("Invalid region list size: " + size);
                    List<RegionSummary> regions = new ArrayList<>(size);
                    for (int index = 0; index < size; index++) regions.add(readSummary(buf));
                    return new ListResponse(regions);
                });
        @Override public Type<ListResponse> type() { return TYPE; }
    }

    public record DetailsResponse(RegionOperationStatus status, RegionSummary region) implements CustomPacketPayload {
        public static final Type<DetailsResponse> TYPE = new Type<>(id("region_details"));
        public static final StreamCodec<FriendlyByteBuf, DetailsResponse> CODEC = StreamCodec.of(
                (buf, value) -> {
                    buf.writeEnum(value.status);
                    buf.writeBoolean(value.region != null);
                    if (value.region != null) writeSummary(buf, value.region);
                },
                buf -> new DetailsResponse(buf.readEnum(RegionOperationStatus.class),
                        buf.readBoolean() ? readSummary(buf) : null));
        @Override public Type<DetailsResponse> type() { return TYPE; }
    }

    public record OperationResponse(RegionOperationStatus status, RegionSummary region) implements CustomPacketPayload {
        public static final Type<OperationResponse> TYPE = new Type<>(id("region_operation_result"));
        public static final StreamCodec<FriendlyByteBuf, OperationResponse> CODEC = StreamCodec.of(
                (buf, value) -> {
                    buf.writeEnum(value.status);
                    buf.writeBoolean(value.region != null);
                    if (value.region != null) writeSummary(buf, value.region);
                },
                buf -> new OperationResponse(buf.readEnum(RegionOperationStatus.class),
                        buf.readBoolean() ? readSummary(buf) : null));
        @Override public Type<OperationResponse> type() { return TYPE; }
    }

    private static void writeSummary(FriendlyByteBuf buf, RegionSummary value) {
        buf.writeUUID(value.id);
        buf.writeUtf(value.name, 256);
        buf.writeResourceLocation(value.dimension);
        buf.writeBlockPos(value.min);
        buf.writeBlockPos(value.max);
        buf.writeDouble(value.timeScale);
        buf.writeBoolean(value.enabled);
        buf.writeEnum(value.mode);
        buf.writeVarInt(targetMask(value.targets));
        buf.writeVarLong(value.revision);
    }

    private static RegionSummary readSummary(FriendlyByteBuf buf) {
        return new RegionSummary(buf.readUUID(), buf.readUtf(256), buf.readResourceLocation(),
                buf.readBlockPos(), buf.readBlockPos(), buf.readDouble(), buf.readBoolean(),
                buf.readEnum(TemporalMode.class), targets(buf.readVarInt()), buf.readVarLong());
    }

    private static int targetMask(Set<TemporalTarget> targets) {
        int mask = 0;
        for (TemporalTarget target : targets) mask |= 1 << target.ordinal();
        return mask;
    }

    private static Set<TemporalTarget> targets(int mask) {
        EnumSet<TemporalTarget> targets = EnumSet.noneOf(TemporalTarget.class);
        for (TemporalTarget target : TemporalTarget.values()) {
            if ((mask & (1 << target.ordinal())) != 0) targets.add(target);
        }
        return targets;
    }

    private static <T extends CustomPacketPayload> StreamCodec<FriendlyByteBuf, T> uuidCodec(
            java.util.function.Function<UUID, T> constructor,
            java.util.function.Function<T, UUID> getter) {
        return StreamCodec.of((buf, value) -> buf.writeUUID(getter.apply(value)),
                buf -> constructor.apply(buf.readUUID()));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RagePhysics.MODID, path);
    }

    private RegionManagementPayloads() {}
}
