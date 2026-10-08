package io.github.korteexz.ragephysics.temporal;

import io.github.korteexz.ragephysics.timestamper.region.TemporalRegion;
import io.github.korteexz.ragephysics.timestamper.region.TemporalRegionSavedData;
import io.github.korteexz.ragephysics.timestamper.config.TemporalMode;
import io.github.korteexz.ragephysics.timestamper.config.TemporalTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class TemporalRegionResolver {
    /** Retorna no máximo uma região válida; overlap enabled é rejeitado pelo armazenamento. */
    public static TemporalRegion resolve(ServerLevel level, BlockPos pos, TemporalTarget target) {
        if (!TemporalConfig.enabled(target)) return null;
        TemporalRegion resolved = null;
        for (TemporalRegion region : TemporalRegionSavedData.get(level).getActiveRegions(level.dimension())) {
            if (allowsSimulation(region, target, true) && region.bounds().contains(pos)) {
                if (resolved != null) throw new IllegalStateException("Overlapping enabled temporal regions");
                resolved = region;
            }
        }
        return resolved;
    }

    /** Pure policy hook shared with domain checks: global config AND region target. */
    public static boolean allowsSimulation(TemporalRegion region, TemporalTarget target, boolean globallyEnabled) {
        return globallyEnabled
                && target.implemented()
                && region.enabled()
                && region.mode() != TemporalMode.VISUAL_ONLY
                && region.affects(target)
                && region.timeScale() != 1.0;
    }

    private TemporalRegionResolver() {}
}
