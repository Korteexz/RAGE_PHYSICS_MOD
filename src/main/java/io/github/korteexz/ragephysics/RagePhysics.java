package io.github.korteexz.ragephysics;

import com.mojang.logging.LogUtils;
import io.github.korteexz.ragephysics.registry.ModItems;
import io.github.korteexz.ragephysics.temporal.TemporalConfig;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

import org.slf4j.Logger;

@Mod(RagePhysics.MODID)
public class RagePhysics {

    public static final String MODID = "ragephysics";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RagePhysics(IEventBus modBus, ModContainer container) {

        ModItems.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, TemporalConfig.SPEC);

        LOGGER.info("Rage Physics loaded.");
    }
}
