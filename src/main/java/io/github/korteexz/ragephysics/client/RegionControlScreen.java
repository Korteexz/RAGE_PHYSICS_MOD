package io.github.korteexz.ragephysics.client;

import io.github.korteexz.ragephysics.network.RegionControlPayloads;
import io.github.korteexz.ragephysics.selection.TimeScaleVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RegionControlScreen extends Screen {
    private static final int PANEL_HEIGHT = 212;
    private final RegionControlPayloads.Open selection;
    private final ClientLevel levelAtOpen;
    private double timeScale;
    private double lastSentScale;
    private int sendTicks;
    private int left;
    private int top;
    private int panelWidth;

    public RegionControlScreen(RegionControlPayloads.Open selection) {
        super(Component.translatable("screen.ragephysics.region_control"));
        this.selection = selection;
        this.timeScale = selection.timeScale();
        this.lastSentScale = timeScale;
        this.levelAtOpen = Minecraft.getInstance().level;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(304, width - 16);
        left = (width - panelWidth) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        TimeScaleSlider slider = addRenderableWidget(new TimeScaleSlider(left + 18, top + 105,
                panelWidth - 36, timeScale, value -> timeScale = value));
        int buttonWidth = (panelWidth - 42) / 2;
        addRenderableWidget(Button.builder(Component.translatable("screen.ragephysics.reset"),
                button -> slider.resetToNormal()).bounds(left + 18, top + 176, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(left + 24 + buttonWidth, top + 176, buttonWidth, 20).build());
    }

    @Override
    public void tick() {
        if (minecraft.player == null || !minecraft.player.isAlive() || minecraft.level != levelAtOpen) {
            onClose();
            return;
        }
        // Coalesce o arraste: no máximo dez atualizações por segundo, sem depender do FPS.
        if (++sendTicks >= 2) {
            sendTicks = 0;
            sendPendingScale();
        }
    }

    private void sendPendingScale() {
        if (timeScale != lastSentScale && minecraft.getConnection() != null && minecraft.player != null
                && minecraft.player.isAlive() && minecraft.level == levelAtOpen) {
            PacketDistributor.sendToServer(new RegionControlPayloads.Update(selection.revision(), timeScale));
            lastSentScale = timeScale;
        }
    }

    @Override
    public void removed() {
        // Escape/Concluído também enviam o último movimento ainda pendente.
        sendPendingScale();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Fundo simples, sem o blur do Screen padrão.
        graphics.fill(0, 0, width, height, 0x70000000);
        graphics.fill(left, top, left + panelWidth, top + PANEL_HEIGHT, 0xFF101014);
        graphics.fill(left + 1, top + 1, left + panelWidth - 1, top + PANEL_HEIGHT - 1, 0xFFE0E0E0);
        graphics.fill(left + 3, top + 3, left + panelWidth - 1, top + PANEL_HEIGHT - 1, 0xFF55555C);
        graphics.fill(left + 3, top + 3, left + panelWidth - 3, top + PANEL_HEIGHT - 3, 0xFF303036);
        graphics.fill(left + 8, top + 28, left + panelWidth - 8, top + 29, TimeScaleVisuals.color(timeScale));
        graphics.fill(left + 12, top + 35, left + panelWidth - 12, top + 66, 0xFF202025);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int center = left + panelWidth / 2;
        graphics.drawCenteredString(font, title, center, top + 12, 0xFFFFFFFF);
        graphics.drawString(font, "A: " + selection.posA().toShortString(), left + 18, top + 40, 0xFFCCCCCC);
        graphics.drawString(font, "B: " + selection.posB().toShortString(), left + 18, top + 52, 0xFFCCCCCC);
        graphics.drawCenteredString(font, Component.translatable("screen.ragephysics.time_scale", TimeScaleSlider.format(timeScale)),
                center, top + 79, TimeScaleVisuals.color(timeScale));
        String[] ticks = {"0.125x", "0.25x", "0.5x", "1x", "2x", "4x", "8x"};
        int travel = panelWidth - 44;
        for (int i = 0; i < ticks.length; i++) {
            int tickX = left + 22 + (int) Math.round(travel * i / 6.0);
            graphics.drawCenteredString(font, ticks[i], tickX, top + 132,
                    TimeScaleVisuals.color(TimeScaleVisuals.fromSlider(i / 6.0)));
        }
        graphics.drawCenteredString(font, Component.translatable("screen.ragephysics.visual_only"),
                center, top + 154, 0xFFBBBBBB);
    }
}
