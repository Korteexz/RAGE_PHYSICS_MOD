package io.github.korteexz.ragephysics.item;

import io.github.korteexz.ragephysics.selection.PlayerSelection;
import io.github.korteexz.ragephysics.selection.SelectionManager;

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
                        || selection.isComplete();


        // Faz a seleção.
        selection.select(clickedPos);


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

            player.displayClientMessage(
                    Component.literal(
                            "[RAGE PHYSICS] Região selecionada."
                    ),
                    false
            );
        }


        return InteractionResult.SUCCESS;
    }
}