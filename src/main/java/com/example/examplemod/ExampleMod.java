package com.example.examplemod;

import com.example.examplemod.registry.ModBlockEntities;
import com.example.examplemod.registry.ModBlocks;
import com.example.examplemod.registry.ModCreativeTabs;
import com.example.examplemod.registry.ModItems;
import com.example.examplemod.registry.ModItems;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@Mod(ExampleMod.MODID)
public class ExampleMod {

    // =========================
    // IDENTIDADE
    // =========================

    public static final String MODID = "examplemod";
    public static final Logger LOGGER = LogUtils.getLogger();


    // =========================
    // INICIALIZAÇÃO DO MOD
    // =========================

    public ExampleMod(
            IEventBus modEventBus,
            ModContainer modContainer
    ) {

        // Registra conteúdo do mod
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        // Registra integrações/capabilities
        modEventBus.addListener(this::registerCapabilities);

        // Registra configurações
        modContainer.registerConfig(
                ModConfig.Type.COMMON,
                Config.SPEC
        );
    }


    // =========================
    // CAPABILITIES
    // =========================

    private void registerCapabilities(
            RegisterCapabilitiesEvent event
    ) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.RAIN_BLOCK_ENTITY.get(),
                (rainBlockEntity, side) ->
                        rainBlockEntity.getInventory()
        );
    }
}