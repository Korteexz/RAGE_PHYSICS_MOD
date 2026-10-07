package io.github.korteexz.ragephysics.registry;

import io.github.korteexz.ragephysics.RagePhysics;
import io.github.korteexz.ragephysics.item.SelectionWandItem;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(RagePhysics.MODID);

    public static final DeferredItem<SelectionWandItem> SELECTION_WAND =
            ITEMS.registerItem(
                    "selection_wand",
                    SelectionWandItem::new
            );

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private ModItems() {}
}