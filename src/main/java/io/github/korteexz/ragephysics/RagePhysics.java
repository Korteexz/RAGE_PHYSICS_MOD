package io.github.korteexz.ragephysics;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.neoforged.fml.common.Mod;

@Mod(RagePhysics.MODID)
public class RagePhysics {

    // =========================
    // IDENTIDADE
    // =========================

    public static final String MODID = "RagePhysics";
    public static final Logger LOGGER = LogUtils.getLogger();


    // =========================
    // INICIALIZAÇÃO
    // =========================

    public RagePhysics() {
        LOGGER.info("Rage Physics loaded.");
    }
}