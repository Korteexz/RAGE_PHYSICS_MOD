package io.github.korteexz.ragephysics.event;

import io.github.korteexz.ragephysics.RagePhysics;
import io.github.korteexz.ragephysics.item.SelectionWandItem;
import io.github.korteexz.ragephysics.selection.PlayerSelection;
import io.github.korteexz.ragephysics.selection.SelectionManager;
import io.github.korteexz.ragephysics.selection.TimeScaleVisuals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = RagePhysics.MODID)
public final class SelectionPreviewHandler {

    // Distância entre uma "estrela" e outra.
    private static final double STEP = 0.5;


    // =========================
    // PREVIEW DA SELEÇÃO
    // =========================

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {

        // Só queremos trabalhar com o player real do servidor.
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Não precisa redesenhar 20 vezes por segundo.
        // 4 ticks = 5 atualizações por segundo.
        if (player.tickCount % 4 != 0) {
            return;
        }

        // Só mostra a região enquanto segura a Wand.
        if (!(player.getMainHandItem().getItem()
                instanceof SelectionWandItem)) {
            return;
        }

        PlayerSelection selection =
                SelectionManager.get(player);

        // Sem A + B, ainda não existe caixa completa.
        if (!selection.isComplete()) {
            return;
        }

        BlockPos a = selection.getPosA();
        BlockPos b = selection.getPosB();

        ServerLevel level = player.serverLevel();

        drawBox(
                level,
                player,
                a,
                b,
                new DustParticleOptions(Vec3.fromRGB24(TimeScaleVisuals.color(selection.getTimeScale())).toVector3f(), 0.65F)
        );
    }


    // =========================
    // DESENHA A CAIXA
    // =========================

    private static void drawBox(
            ServerLevel level,
            ServerPlayer player,
            BlockPos a,
            BlockPos b,
            DustParticleOptions options
    ) {

        double minX = Math.min(a.getX(), b.getX());
        double minY = Math.min(a.getY(), b.getY());
        double minZ = Math.min(a.getZ(), b.getZ());

        double maxX = Math.max(a.getX(), b.getX()) + 1.0;
        double maxY = Math.max(a.getY(), b.getY()) + 1.0;
        double maxZ = Math.max(a.getZ(), b.getZ()) + 1.0;


        // =========================
        // ARESTAS NO EIXO X
        // =========================

        for (double x = minX; x <= maxX; x += STEP) {

            particle(level, player, options, x, minY, minZ);
            particle(level, player, options, x, maxY, minZ);

            particle(level, player, options, x, minY, maxZ);
            particle(level, player, options, x, maxY, maxZ);
        }


        // =========================
        // ARESTAS NO EIXO Y
        // =========================

        for (double y = minY; y <= maxY; y += STEP) {

            particle(level, player, options, minX, y, minZ);
            particle(level, player, options, maxX, y, minZ);

            particle(level, player, options, minX, y, maxZ);
            particle(level, player, options, maxX, y, maxZ);
        }


        // =========================
        // ARESTAS NO EIXO Z
        // =========================

        for (double z = minZ; z <= maxZ; z += STEP) {

            particle(level, player, options, minX, minY, z);
            particle(level, player, options, maxX, minY, z);

            particle(level, player, options, minX, maxY, z);
            particle(level, player, options, maxX, maxY, z);
        }
    }


    // =========================
    // UMA ESTRELA
    // =========================

    private static void particle(
            ServerLevel level,
            ServerPlayer player,
            DustParticleOptions options,
            double x,
            double y,
            double z
    ) {

        level.sendParticles(
                player,
                options,
                false,
                x,
                y,
                z,
                1,
                0,
                0,
                0,
                0
        );
    }


    private SelectionPreviewHandler() {}
}
