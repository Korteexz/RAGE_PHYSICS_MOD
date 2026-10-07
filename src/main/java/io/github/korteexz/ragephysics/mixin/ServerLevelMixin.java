package io.github.korteexz.ragephysics.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.korteexz.ragephysics.temporal.TemporalEntityTicker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
    @WrapMethod(method = "tickNonPassenger")
    private void ragephysics$localEntityTime(Entity entity, Operation<Void> original) {
        if (!TemporalEntityTicker.hasTemporalWork(entity)) {
            original.call(entity);
            return;
        }
        TemporalEntityTicker.tick(entity, () -> original.call(entity));
    }
}
