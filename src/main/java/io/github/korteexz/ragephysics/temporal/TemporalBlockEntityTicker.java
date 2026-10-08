package io.github.korteexz.ragephysics.temporal;

import io.github.korteexz.ragephysics.timestamper.region.TemporalRegion;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TickingBlockEntity;

public final class TemporalBlockEntityTicker {
    private static final Map<BlockEntity, TemporalTickBudget> BUDGETS = new WeakHashMap<>();

    public static void tick(ServerLevel level, TickingBlockEntity ticker, Runnable vanillaTicker) {
        TemporalRegion region = TemporalRegionResolver.resolve(level, ticker.getPos(), TemporalTarget.BLOCK_ENTITIES);
        BlockEntity entity = level.getBlockEntity(ticker.getPos());
        if (region == null || entity == null) {
            if (entity != null) BUDGETS.remove(entity);
            vanillaTicker.run();
            return;
        }
        TemporalTickBudget budget = BUDGETS.computeIfAbsent(entity, ignored -> new TemporalTickBudget());
        long revision = region.revision();
        double scale = region.timeScale();
        int steps = budget.advance(region.id(), revision, scale);
        for (int step = 0; step < steps; step++) {
            // O ticker original ainda valida estado, chunk, profiling e tratamento de erros.
            vanillaTicker.run();
            if (entity.isRemoved() || ticker.isRemoved() || level.getBlockEntity(ticker.getPos()) != entity
                    || TemporalRegionResolver.resolve(level, ticker.getPos(), TemporalTarget.BLOCK_ENTITIES) != region
                    || region.revision() != revision || region.timeScale() != scale) {
                BUDGETS.remove(entity);
                break;
            }
        }
    }

    public static void clear(Level level) { BUDGETS.keySet().removeIf(entity -> entity.getLevel() == level); }
    public static void clear() { BUDGETS.clear(); }

    private TemporalBlockEntityTicker() {}
}
