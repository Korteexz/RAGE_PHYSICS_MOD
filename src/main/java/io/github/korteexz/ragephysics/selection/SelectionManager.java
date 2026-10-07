package io.github.korteexz.ragephysics.selection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.world.entity.player.Player;

public final class SelectionManager {

    // =========================
    // SELEÇÕES POR JOGADOR
    // =========================

    private static final Map<UUID, PlayerSelection> SELECTIONS =
            new HashMap<>();


    // =========================
    // PEGA A SELEÇÃO DO PRÓPRIO PLAYER
    // =========================

    public static PlayerSelection get(Player player) {

        return SELECTIONS.computeIfAbsent(
                player.getUUID(),
                uuid -> new PlayerSelection()
        );
    }


    private SelectionManager() {}
}