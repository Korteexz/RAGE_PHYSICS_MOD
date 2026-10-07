package io.github.korteexz.ragephysics.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.korteexz.ragephysics.temporal.TemporalBlockEntityTicker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Level.class)
abstract class LevelMixin {
    @WrapOperation(method = "tickBlockEntities", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"))
    private void ragephysics$localBlockEntityTime(TickingBlockEntity ticker, Operation<Void> original) {
        if ((Object) this instanceof ServerLevel level) {
            TemporalBlockEntityTicker.tick(level, ticker, () -> original.call(ticker));
        } else {
            original.call(ticker);
        }
    }
}
