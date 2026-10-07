package io.github.korteexz.ragephysics.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.korteexz.ragephysics.client.TemporalEntityPresentation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {
    @WrapMethod(method = "tickNonPassenger")
    private void ragephysics$presentLocalTime(Entity entity, Operation<Void> original) {
        if (TemporalEntityPresentation.manages(entity)) {
            TemporalEntityPresentation.tick(entity, () -> original.call(entity));
        } else {
            original.call(entity);
        }
    }
}
