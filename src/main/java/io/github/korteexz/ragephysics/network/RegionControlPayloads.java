package io.github.korteexz.ragephysics.network;

import io.github.korteexz.ragephysics.RagePhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Os três pequenos payloads da interface. Nenhum aceita UUID do cliente. */
public final class RegionControlPayloads {
    public record Request() implements CustomPacketPayload {
        public static final Request INSTANCE = new Request();
        public static final Type<Request> TYPE = new Type<>(id("request_region_control"));
        public static final StreamCodec<FriendlyByteBuf, Request> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<Request> type() { return TYPE; }
    }

    public record Open(ResourceLocation dimension, BlockPos posA, BlockPos posB, long revision, double timeScale) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(id("open_region_control"));
        public static final StreamCodec<FriendlyByteBuf, Open> CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Open::dimension,
                BlockPos.STREAM_CODEC, Open::posA,
                BlockPos.STREAM_CODEC, Open::posB,
                ByteBufCodecs.VAR_LONG, Open::revision,
                ByteBufCodecs.DOUBLE, Open::timeScale, Open::new);

        @Override
        public Type<Open> type() { return TYPE; }
    }

    public record Update(long revision, double timeScale) implements CustomPacketPayload {
        public static final Type<Update> TYPE = new Type<>(id("update_region_time_scale"));
        public static final StreamCodec<FriendlyByteBuf, Update> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Update::revision,
                ByteBufCodecs.DOUBLE, Update::timeScale, Update::new);

        @Override
        public Type<Update> type() { return TYPE; }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RagePhysics.MODID, path);
    }

    private RegionControlPayloads() {}
}
