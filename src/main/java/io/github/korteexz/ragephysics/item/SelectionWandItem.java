package io.github.korteexz.ragephysics.item;

import io.github.korteexz.ragephysics.selection.PlayerSelection;
import io.github.korteexz.ragephysics.selection.SelectionManager;
import io.github.korteexz.ragephysics.timestamper.region.RegionOperationStatus;
import io.github.korteexz.ragephysics.timestamper.region.TemporalRegionOperations;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class SelectionWandItem extends Item {

    // =========================
    // CONSTRUTOR
    // =========================

    public SelectionWandItem(Properties properties) {
        super(properties);
    }


    // =========================
    // CLIQUE EM BLOCO
    // =========================

    @Override
    public InteractionResult useOn(UseOnContext context) {

        Player player = context.getPlayer();
        Level level = context.getLevel();

        if (player == null) {
            return InteractionResult.PASS;
        }


        // A seleção verdadeira fica no servidor.
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }


        // Qual bloco foi clicado?
        BlockPos clickedPos =
                context.getClickedPos();


        // Qual seleção pertence a ESTE jogador?
        PlayerSelection selection =
                SelectionManager.get(player);


        // Decide se este clique será A ou B.
        boolean selectingA =
                !selection.hasPosA()
                        || !selection.isInDimension(level.dimension())
                        || selection.isComplete();


        // Faz a seleção.
        selection.select(level.dimension(), clickedPos);
        RegionOperationStatus createStatus = RegionOperationStatus.SUCCESS;
        if (selection.isComplete()) {
            // Compatibilidade temporária até a LVL 3C: completar A+B ainda cria
            // imediatamente, mas toda regra passa pela API server-authoritative.
            var result = TemporalRegionOperations.forPlayer((net.minecraft.server.level.ServerPlayer) player)
                    .createFromSelection((net.minecraft.server.level.ServerPlayer) player, selection);
            result.region().ifPresent(created -> selection.bindRegion(created.id()));
            createStatus = result.status();
        }


        // Feedback visual provisório via chat.
        if (selectingA) {

            player.displayClientMessage(
                    Component.literal(
                            "[RAGE PHYSICS] A = "
                                    + clickedPos.toShortString()
                    ),
                    false
            );

        } else {

            player.displayClientMessage(
                    Component.literal(
                            "[RAGE PHYSICS] B = "
                                    + clickedPos.toShortString()
                    ),
                    false
            );

            player.displayClientMessage(Component.literal(createStatus == RegionOperationStatus.SUCCESS
                    ? "[RAGE PHYSICS] Região selecionada."
                    : "[RAGE PHYSICS] Região sobrepõe outra região e não foi criada."), false);
        }


        return InteractionResult.SUCCESS;
    }
}
