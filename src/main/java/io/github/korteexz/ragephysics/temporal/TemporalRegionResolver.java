package io.github.korteexz.ragephysics.temporal;

import io.github.korteexz.ragephysics.selection.PlayerSelection;
import io.github.korteexz.ragephysics.selection.SelectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class TemporalRegionResolver {
    /** Uma região vencedora, nunca produto de escalas. 1x não mascara regiões ativas. */
    public static PlayerSelection resolve(ResourceKey<Level> dimension, BlockPos pos, TemporalTarget target) {
        if (!TemporalConfig.enabled(target)) return null;
        for (PlayerSelection selection : SelectionManager.orderedSelections()) {
            if (selection.getTimeScale() != 1.0 && selection.contains(dimension, pos)) {
                return selection;
            }
        }
        return null;
    }

    private TemporalRegionResolver() {}
}
