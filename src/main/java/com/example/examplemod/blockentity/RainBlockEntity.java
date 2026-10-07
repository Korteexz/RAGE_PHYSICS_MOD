package com.example.examplemod.blockentity;


import com.example.examplemod.ExampleMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.example.examplemod.registry.ModBlockEntities;
import net.neoforged.neoforge.items.ItemStackHandler;

public class RainBlockEntity extends BlockEntity {

    private final ItemStackHandler inventory = new ItemStackHandler(1) {

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(Items.GHAST_TEAR);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            updateWeather();
        }
    };

    public RainBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.RAIN_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    private void updateWeather() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean hasGhastTear =
                inventory.getStackInSlot(0).is(Items.GHAST_TEAR);

        if (hasGhastTear) {
            serverLevel.setWeatherParameters(
                    0,
                    20 * 60 * 60,
                    true,
                    false
            );
        } else {
            serverLevel.setWeatherParameters(
                    20 * 60 * 60,
                    0,
                    false,
                    false
            );
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateWeather();
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(tag, registries);

        tag.put(
                "inventory",
                inventory.serializeNBT(registries)
        );
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(tag, registries);

        if (tag.contains("inventory")) {
            inventory.deserializeNBT(
                    registries,
                    tag.getCompound("inventory")
            );
        }
    }
}
