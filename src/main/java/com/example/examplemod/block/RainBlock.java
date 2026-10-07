package com.example.examplemod.block;

import com.example.examplemod.blockentity.RainBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class RainBlock extends Block implements EntityBlock {

    // DESCRIÇÃO DE COMO ESTE TIPO DE BLOCO É SERIALIZADO
    public static final MapCodec<RainBlock> CODEC =
            simpleCodec(RainBlock::new);

    // CONSTRUÇÃO DO BLOCO
    public RainBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    // CRIA A BLOCK ENTITY DE CADA RAIN BLOCK COLOCADO
    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new RainBlockEntity(pos, state);
    }
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {

        // Pega a BlockEntity que existe NESTA posição.
        if (!(level.getBlockEntity(pos)
                instanceof RainBlockEntity rainBlockEntity)) {

            return ItemInteractionResult
                    .PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // SHIFT + clique = tenta retirar o item.
        if (player.isShiftKeyDown()) {

            if (!level.isClientSide()) {

                ItemStack extracted =
                        rainBlockEntity
                                .getInventory()
                                .extractItem(
                                        0,
                                        1,
                                        false
                                );

                if (!extracted.isEmpty()) {

                    if (!player.addItem(extracted)) {
                        player.drop(extracted, false);
                    }
                }
            }

            return ItemInteractionResult
                    .sidedSuccess(level.isClientSide());
        }

        // Clique segurando Ghast Tear = tenta inserir.
        if (stack.is(Items.GHAST_TEAR)) {

            if (!level.isClientSide()) {

                ItemStack oneTear = stack.copy();
                oneTear.setCount(1);

                ItemStack remainder =
                        rainBlockEntity
                                .getInventory()
                                .insertItem(
                                        0,
                                        oneTear,
                                        false
                                );

                // Entrou no slot?
                if (remainder.isEmpty()
                        && !player.getAbilities().instabuild) {

                    stack.shrink(1);
                }
            }

            return ItemInteractionResult
                    .sidedSuccess(level.isClientSide());
        }

        return ItemInteractionResult
                .PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        // Só executa se o bloco realmente deixou de ser RainBlock
        if (!state.is(newState.getBlock())) {

            if (level.getBlockEntity(pos) instanceof RainBlockEntity rainBlockEntity) {

                ItemStack stack =
                        rainBlockEntity
                                .getInventory()
                                .extractItem(
                                        0,
                                        1,
                                        false
                                );

                // Dropa a lágrima no mundo
                if (!stack.isEmpty()) {
                    popResource(
                            level,
                            pos,
                            stack
                    );
                }
            }
        }

        super.onRemove(
                state,
                level,
                pos,
                newState,
                movedByPiston
        );
    }
}