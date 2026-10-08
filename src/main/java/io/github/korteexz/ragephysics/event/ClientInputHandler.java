package io.github.korteexz.ragephysics.event;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.korteexz.ragephysics.RagePhysics;
import io.github.korteexz.ragephysics.client.RegionManagerClientState;
import io.github.korteexz.ragephysics.client.TemporalEntityPresentation;
import io.github.korteexz.ragephysics.network.RegionControlNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = RagePhysics.MODID, value = Dist.CLIENT)
public final class ClientInputHandler {
    private static final KeyMapping OPEN_REGION = new KeyMapping("key.ragephysics.region_control",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.ragephysics");

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_REGION.consumeClick()) {
            if (minecraft.player != null && minecraft.level != null && minecraft.screen == null) {
                RegionManagerClientState.requestOpen();
            }
        }
    }

    @EventBusSubscriber(modid = RagePhysics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_REGION);
        }

        @SubscribeEvent
        public static void registerPayloads(RegisterPayloadHandlersEvent event) {
            RegionControlNetworking.register(event, (payload, context) -> {},
                    (payload, context) -> TemporalEntityPresentation.receive(payload),
                    (payload, context) -> RegionManagerClientState.receive(payload),
                    (payload, context) -> RegionManagerClientState.receive(payload),
                    (payload, context) -> RegionManagerClientState.receive(payload));
        }
    }

    private ClientInputHandler() {}
}
