package io.github.korteexz.ragephysics.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.korteexz.ragephysics.client.TemporalEntityPresentation;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
    // Mantém o codec de deltas vanilla atualizado, mas evita snaps (AbstractArrow.lerpTo é imediato).
    @WrapOperation(method = {"handleMoveEntity", "handleTeleportEntity"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;lerpTo(DDDFFI)V"))
    private void ragephysics$useAuthoritativeSnapshot(Entity entity, double x, double y, double z,
                                                     float yaw, float pitch, int steps, Operation<Void> original) {
        if (!TemporalEntityPresentation.manages(entity)) original.call(entity, x, y, z, yaw, pitch, steps);
    }
}
