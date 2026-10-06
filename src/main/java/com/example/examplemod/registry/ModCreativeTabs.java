package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.core.registries.Registries;

public final class ModCreativeTabs {

    // ABAS DO INVENTÁRIO CRIATIVO
    public static final DeferredRegister<CreativeModeTab>
            CREATIVE_MODE_TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    ExampleMod.MODID
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab>
            LAB_TAB =
            CREATIVE_MODE_TABS.register(
                    "lab_tab",
                    () -> CreativeModeTab.builder()

                            .title(
                                    Component.literal(
                                            "Rage Physics Lab"
                                    )
                            )

                            .withTabsBefore(
                                    CreativeModeTabs.COMBAT
                            )

                            .icon(
                                    () -> ModItems.FIREBALL_WAND
                                            .get()
                                            .getDefaultInstance()
                            )

                            .displayItems(
                                    (parameters, output) -> {
                                        output.accept(
                                                ModItems.FIREBALL_WAND.get()
                                        );

                                        output.accept(
                                                ModItems.RAIN_BLOCK_ITEM.get()
                                        );
                                    }
                            )

                            .build()
            );

    private ModCreativeTabs() {}
}