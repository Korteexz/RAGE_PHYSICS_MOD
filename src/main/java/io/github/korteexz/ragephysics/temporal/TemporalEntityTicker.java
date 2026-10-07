package io.github.korteexz.ragephysics.temporal;

import io.github.korteexz.ragephysics.network.TemporalEntityPayload;
import io.github.korteexz.ragephysics.selection.PlayerSelection;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TemporalEntityTicker {
    // Chaves fracas + remoção explícita no lifecycle. O valor nunca referencia a entidade.
    private static final Map<Entity, State> STATES = new WeakHashMap<>();

    private static PlayerSelection region(Entity entity) {
        if (entity instanceof Player || entity.isPassenger() || entity.isVehicle() || entity.isRemoved()) return null;
        return TemporalRegionResolver.resolve(entity.level().dimension(), entity.blockPosition(), TemporalTarget.classify(entity));
    }

    public static double scale(Entity entity) {
        PlayerSelection selection = region(entity);
        return selection == null ? 1.0 : selection.getTimeScale();
    }

    public static boolean hasTemporalWork(Entity entity) {
        return STATES.containsKey(entity) || region(entity) != null;
    }

    /** Executa o dispatcher original, nunca chama Entity.tick de dentro de um evento. */
    public static void tick(Entity entity, Runnable vanillaDispatcher) {
        State state = STATES.get(entity);
        if (state != null && state.running) return; // Reentrada do mesmo sujeito, inclusive via outro mod.
        PlayerSelection selection = region(entity);
        if (selection == null) {
            if (STATES.remove(entity) != null && !entity.isRemoved()) send(entity, 1.0);
            vanillaDispatcher.run();
            return;
        }
        boolean entered = state == null;
        if (entered) {
            state = new State();
            STATES.put(entity, state);
        }
        double scale = selection.getTimeScale();
        long revision = selection.getRevision();
        int steps = state.budget.advance(selection, revision, scale);
        boolean changed = state.lastScale != scale;
        state.running = true;
        try {
            for (int step = 0; step < steps; step++) {
                vanillaDispatcher.run();
                PlayerSelection next = region(entity);
                // Se cruzou uma fronteira ou mudou de lifecycle, descarta o resto do budget deste tick.
                if (next != selection || selection.getRevision() != revision || selection.getTimeScale() != scale) {
                    state.budget.reset();
                    break;
                }
            }
        } finally {
            state.running = false;
        }
        if (entity.isRemoved()) {
            STATES.remove(entity);
            return;
        }
        double nextScale = scale(entity);
        if (entered || changed || steps > 0 || nextScale != scale) send(entity, nextScale);
        state.lastScale = nextScale;
        if (nextScale == 1.0) STATES.remove(entity);
    }

    private static void send(Entity entity, double scale) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, TemporalEntityPayload.of(entity, scale));
    }

    public static void remove(Entity entity) { STATES.remove(entity); }
    public static void clear(Level level) { STATES.keySet().removeIf(entity -> entity.level() == level); }
    public static void clear() { STATES.clear(); }

    private static final class State {
        final TemporalTickBudget budget = new TemporalTickBudget();
        boolean running;
        double lastScale = 1.0;
    }

    private TemporalEntityTicker() {}
}
