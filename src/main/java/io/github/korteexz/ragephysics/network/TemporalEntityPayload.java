package io.github.korteexz.ragephysics.network;

import io.github.korteexz.ragephysics.RagePhysics;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Snapshot autoritativo do sujeito, enviado a todos que o acompanham, dentro ou fora da região. */
public record TemporalEntityPayload(ResourceLocation dimension, int entityId, UUID entityUuid,
                                    double scale, Vec3 position, Vec3 velocity, float yaw, float pitch)
        implements CustomPacketPayload {
    public static final Type<TemporalEntityPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RagePhysics.MODID, "temporal_entity"));
    public static final StreamCodec<FriendlyByteBuf, TemporalEntityPayload> CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeResourceLocation(value.dimension);
                buf.writeVarInt(value.entityId);
                buf.writeUUID(value.entityUuid);
                buf.writeDouble(value.scale);
                writeVector(buf, value.position);
                writeVector(buf, value.velocity);
                buf.writeFloat(value.yaw);
                buf.writeFloat(value.pitch);
            },
            buf -> new TemporalEntityPayload(buf.readResourceLocation(), buf.readVarInt(), buf.readUUID(),
                    buf.readDouble(), readVector(buf), readVector(buf), buf.readFloat(), buf.readFloat()));

    public static TemporalEntityPayload of(Entity entity, double scale) {
        return new TemporalEntityPayload(entity.level().dimension().location(), entity.getId(), entity.getUUID(),
                scale, entity.position(), entity.getDeltaMovement(), entity.getYRot(), entity.getXRot());
    }

    private static void writeVector(FriendlyByteBuf buf, Vec3 vector) {
        buf.writeDouble(vector.x);
        buf.writeDouble(vector.y);
        buf.writeDouble(vector.z);
    }

    private static Vec3 readVector(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    @Override
    public Type<TemporalEntityPayload> type() { return TYPE; }
}
