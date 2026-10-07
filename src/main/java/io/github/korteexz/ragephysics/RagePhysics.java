package io.github.korteexz.ragephysics;

import com.mojang.logging.LogUtils;
import io.github.korteexz.ragephysics.registry.ModItems;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import org.slf4j.Logger;

@Mod(RagePhysics.MODID)
public class RagePhysics {

    public static final String MODID = "ragephysics";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RagePhysics(IEventBus modBus) {

        ModItems.register(modBus);

        LOGGER.info("Rage Physics loaded.");
    }
}