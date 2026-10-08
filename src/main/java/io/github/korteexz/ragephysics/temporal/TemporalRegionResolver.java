package io.github.korteexz.ragephysics.temporal;

import io.github.korteexz.ragephysics.timestamper.region.TemporalRegion;
import io.github.korteexz.ragephysics.timestamper.region.TemporalRegionSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class TemporalRegionResolver {
    /** Uma região vencedora, nunca produto de escalas. 1x não mascara regiões ativas. */
    public static TemporalRegion resolve(ServerLevel level, BlockPos pos, TemporalTarget target) {
        if (!TemporalConfig.enabled(target)) return null;
        TemporalRegion resolved = null;
        for (TemporalRegion region : TemporalRegionSavedData.get(level).getActiveRegions(level.dimension())) {
            if (region.timeScale() != 1.0 && region.bounds().contains(pos)) {
                if (resolved != null) throw new IllegalStateException("Overlapping enabled temporal regions");
                resolved = region;
            }
        }
        return resolved;
    }

    private TemporalRegionResolver() {}
}
