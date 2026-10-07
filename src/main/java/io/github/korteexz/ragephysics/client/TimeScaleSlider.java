package io.github.korteexz.ragephysics.client;

import io.github.korteexz.ragephysics.selection.TimeScaleVisuals;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/** Interação/narração vanilla com uma trilha pixelada azul → branco → vermelho. */
public final class TimeScaleSlider extends AbstractSliderButton {
    private final DoubleConsumer onChange;

    public TimeScaleSlider(int x, int y, int width, double scale, DoubleConsumer onChange) {
        super(x, y, width, 20, Component.empty(), TimeScaleVisuals.toSlider(scale));
        this.onChange = onChange;
        updateMessage();
    }

    public void resetToNormal() {
        value = 0.5;
        applyValue();
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.translatable("screen.ragephysics.time_scale", format(TimeScaleVisuals.fromSlider(value))));
    }

    @Override
    protected void applyValue() {
        // Um pixel ao redor do centro facilita selecionar exatamente 1x com o mouse.
        if (Math.abs(value - 0.5) <= 0.5 / (width - 8)) {
            value = 0.5;
        }
        onChange.accept(TimeScaleVisuals.fromSlider(value));
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int travel = width - 8;
        graphics.fill(x, y, x + width, y + height, isFocused() || isHovered ? 0xFFFFFFFF : 0xFF101014);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF25252A);
        for (int pixel = 0; pixel <= travel; pixel++) {
            int color = TimeScaleVisuals.color(TimeScaleVisuals.fromSlider((double) pixel / travel));
            graphics.fill(x + 4 + pixel, y + 6, x + 5 + pixel, y + 14, color);
        }
        for (int tick = 0; tick <= 6; tick++) {
            int tickX = x + 4 + (int) Math.round(travel * tick / 6.0);
            graphics.fill(tickX, y + 15, tickX + 1, y + 18, 0xFFB0B0B0);
        }
        int center = x + width / 2;
        graphics.fill(center, y + 2, center + 1, y + height - 2, 0xFFFFFFFF);
        int handle = x + (int) Math.round(value * travel);
        graphics.blitSprite(getHandleSprite(), handle, y, 8, height);
        graphics.fill(handle + 3, y + 6, handle + 5, y + 14,
                TimeScaleVisuals.color(TimeScaleVisuals.fromSlider(value)));
    }

    public static String format(double scale) {
        return String.format(Locale.ROOT, "%.2fx", scale);
    }
}
