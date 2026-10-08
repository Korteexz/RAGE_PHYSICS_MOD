package io.github.korteexz.ragephysics.network;

import io.github.korteexz.ragephysics.RagePhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

/** Os três pequenos payloads da interface. Nenhum aceita UUID do cliente. */
public final class RegionControlPayloads {
    public record Request() implements CustomPacketPayload {
        public static final Request INSTANCE = new Request();
        public static final Type<Request> TYPE = new Type<>(id("request_region_control"));
        public static final StreamCodec<FriendlyByteBuf, Request> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<Request> type() { return TYPE; }
    }

    public record Open(UUID regionId, ResourceLocation dimension, BlockPos posA, BlockPos posB,
            long revision, double timeScale) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(id("open_region_control"));
        public static final StreamCodec<FriendlyByteBuf, Open> CODEC = StreamCodec.of(
                (buf, value) -> {
                    buf.writeUUID(value.regionId);
                    buf.writeResourceLocation(value.dimension);
                    buf.writeBlockPos(value.posA);
                    buf.writeBlockPos(value.posB);
                    buf.writeVarLong(value.revision);
                    buf.writeDouble(value.timeScale);
                },
                buf -> new Open(buf.readUUID(), buf.readResourceLocation(), buf.readBlockPos(),
                        buf.readBlockPos(), buf.readVarLong(), buf.readDouble()));

        @Override
        public Type<Open> type() { return TYPE; }
    }

    public record Update(UUID regionId, double timeScale) implements CustomPacketPayload {
        public static final Type<Update> TYPE = new Type<>(id("update_region_time_scale"));
        public static final StreamCodec<FriendlyByteBuf, Update> CODEC = StreamCodec.of(
                (buf, value) -> { buf.writeUUID(value.regionId); buf.writeDouble(value.timeScale); },
                buf -> new Update(buf.readUUID(), buf.readDouble()));

        @Override
        public Type<Update> type() { return TYPE; }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RagePhysics.MODID, path);
    }

    private RegionControlPayloads() {}
}
