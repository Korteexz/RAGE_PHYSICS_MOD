package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.FireballWandItem;

import net.minecraft.world.item.BlockItem;

import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    // =========================
    // REGISTRY DE ITENS
    // =========================

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(
                    ExampleMod.MODID
            );


    // =========================
    // FIREBALL WAND
    // =========================

    public static final DeferredItem<FireballWandItem> FIREBALL_WAND =
            ITEMS.registerItem(
                    "fireball_wand",
                    FireballWandItem::new
            );


    // =========================
    // RAIN BLOCK ITEM
    // =========================

    public static final DeferredItem<BlockItem> RAIN_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(
                    "rain_block",
                    ModBlocks.RAIN_BLOCK
            );


    private ModItems() {}
}