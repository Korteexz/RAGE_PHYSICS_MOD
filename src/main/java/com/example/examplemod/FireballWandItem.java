package com.example.examplemod;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredItem;

import static com.example.examplemod.ExampleMod.ITEMS;

public class FireballWandItem extends Item {

    public FireballWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack wand = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            Vec3 direction = player.getLookAngle();

            SmallFireball fireball = new SmallFireball(
                    level,
                    player,
                    direction
            );

            Vec3 spawnPosition = player.getEyePosition()
                    .add(direction.scale(1.0));

            fireball.setPos(
                    spawnPosition.x,
                    spawnPosition.y,
                    spawnPosition.z
            );

            level.addFreshEntity(fireball);
        }

        return InteractionResultHolder.sidedSuccess(
                wand,
                level.isClientSide()
        );
    }

}
