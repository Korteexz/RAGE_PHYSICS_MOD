// ARQUIVO:
// src/main/java/com/example/examplemod/mixin/LivingEntityMixin.java

package com.example.examplemod.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


// =========================
// ALVO DO MIXIN
// =========================

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {


    // =========================
    // MODIFICA FORÇA DO PULO
    // =========================

    @Inject(
            method = "getJumpPower",
            at = @At("RETURN"),
            cancellable = true
    )
    private void ragePhysics$doublePlayerJump(
            CallbackInfoReturnable<Float> cir
    ) {

        // Só modifica players.
        if ((Object) this instanceof Player) {

            // Resultado que o Minecraft calculou normalmente.
            float vanillaJumpPower =
                    cir.getReturnValue();

            // Substitui pelo dobro.
            cir.setReturnValue(
                    vanillaJumpPower * 2.0F
            );
        }
    }
}