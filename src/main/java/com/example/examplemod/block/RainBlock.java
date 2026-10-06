package com.example.examplemod.block;

import com.example.examplemod.blockentity.RainBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

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
}