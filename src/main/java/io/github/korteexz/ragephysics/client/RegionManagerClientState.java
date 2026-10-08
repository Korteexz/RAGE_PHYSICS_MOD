package io.github.korteexz.ragephysics.client;

import io.github.korteexz.ragephysics.network.RegionManagementPayloads;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

/** Small presentation cache. All authoritative data still comes from the server. */
public final class RegionManagerClientState {
    public static void requestOpen() {
        PacketDistributor.sendToServer(RegionManagementPayloads.RequestList.INSTANCE);
    }

    public static void receive(RegionManagementPayloads.ListResponse payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof RegionManagerScreen screen) {
            screen.receiveList(payload);
        } else if (minecraft.player != null && minecraft.level != null && minecraft.screen == null) {
            minecraft.setScreen(new RegionManagerScreen(payload));
        }
    }

    public static void receive(RegionManagementPayloads.DetailsResponse payload) {
        if (Minecraft.getInstance().screen instanceof RegionManagerScreen screen) {
            screen.receiveDetails(payload);
        }
    }

    public static void receive(RegionManagementPayloads.OperationResponse payload) {
        if (Minecraft.getInstance().screen instanceof RegionManagerScreen screen) {
            screen.receiveOperation(payload);
        }
    }

    private RegionManagerClientState() {}
}
