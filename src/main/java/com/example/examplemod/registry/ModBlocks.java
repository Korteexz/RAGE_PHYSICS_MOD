package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.block.RainBlock;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    // =========================
    // REGISTRY DE BLOCOS
    // =========================

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(
                    ExampleMod.MODID
            );


    // =========================
    // RAIN BLOCK
    // =========================

    public static final DeferredBlock<RainBlock> RAIN_BLOCK =
            BLOCKS.registerBlock(
                    "rain_block",
                    RainBlock::new,
                    BlockBehaviour.Properties.ofFullCopy(
                            Blocks.NETHERITE_BLOCK
                    )
            );


    private ModBlocks() {}
}