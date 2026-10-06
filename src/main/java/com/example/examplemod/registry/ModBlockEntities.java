package com.example.examplemod.registry;

import java.util.function.Supplier;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.blockentity.RainBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    // TIPOS DE BLOCK ENTITY DO MOD
    public static final DeferredRegister<BlockEntityType<?>>
            BLOCK_ENTITY_TYPES =
            DeferredRegister.create(
                    Registries.BLOCK_ENTITY_TYPE,
                    ExampleMod.MODID
            );

    // TIPO DA BLOCK ENTITY DO RAIN BLOCK
    public static final Supplier<BlockEntityType<RainBlockEntity>>
            RAIN_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "rain_block",
                    () -> BlockEntityType.Builder.of(
                            RainBlockEntity::new,
                            ModBlocks.RAIN_BLOCK.get()
                    ).build(null)
            );

    private ModBlockEntities() {}
}