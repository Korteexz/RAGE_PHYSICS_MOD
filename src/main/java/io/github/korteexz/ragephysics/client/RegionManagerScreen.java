package io.github.korteexz.ragephysics.client;

import io.github.korteexz.ragephysics.network.RegionManagementPayloads;
import io.github.korteexz.ragephysics.network.RegionManagementPayloads.RegionSummary;
import io.github.korteexz.ragephysics.selection.TimeScaleVisuals;
import io.github.korteexz.ragephysics.timestamper.config.TemporalMode;
import io.github.korteexz.ragephysics.timestamper.config.TemporalTarget;
import io.github.korteexz.ragephysics.timestamper.region.RegionOperationStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-authoritative manager for all persistent regions owned by the local player. */
public final class RegionManagerScreen extends Screen {
    private static final int ROW_HEIGHT = 34;
    private static final int MIN_LEFT_WIDTH = 104;
    private static final Set<TemporalTarget> EDITABLE_TARGETS = EnumSet.of(
            TemporalTarget.MOBS, TemporalTarget.PROJECTILES,
            TemporalTarget.OTHER_ENTITIES, TemporalTarget.BLOCK_ENTITIES);

    private List<RegionSummary> regions = List.of();
    private RegionManagementPayloads.DraftSummary draft;
    private RegionSummary selected;
    private UUID selectedRegionId;
    private Component feedback = Component.empty();
    private int feedbackColor = 0xFFB8B8B8;
    private int scrollRows;
    private boolean confirmDelete;
    private double pendingScale;
    private double lastSentScale;
    private int sendTicks;

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int leftWidth;
    private int dividerX;
    private int listTop;
    private int listBottom;
    private EditBox nameBox;

    public RegionManagerScreen(RegionManagementPayloads.ListResponse initial) {
        super(Component.translatable("screen.ragephysics.region_manager"));
        applyList(initial);
    }

    @Override
    protected void init() {
        panelWidth = Math.min(620, Math.max(300, width - 16));
        panelHeight = Math.min(360, Math.max(220, height - 16));
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;
        leftWidth = Math.max(MIN_LEFT_WIDTH, Math.min(190, panelWidth / 3));
        dividerX = panelLeft + leftWidth;
        listTop = panelTop + 31;
        listBottom = panelTop + panelHeight - 76;
        buildWidgets();
    }

    private void buildWidgets() {
        clearWidgets();
        int footerY = panelTop + panelHeight - 24;
        int margin = 8;

        Button create = addRenderableWidget(Button.builder(Component.translatable("screen.ragephysics.create_region"),
                button -> PacketDistributor.sendToServer(RegionManagementPayloads.CreateFromSelection.INSTANCE))
                .bounds(panelLeft + margin, footerY, leftWidth - margin * 2, 18).build());
        create.active = draft != null;

        int rightX = dividerX + 10;
        int rightWidth = panelLeft + panelWidth - rightX - 9;
        if (selected == null) {
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                    .bounds(rightX + Math.max(0, rightWidth - 78), footerY, Math.min(78, rightWidth), 18).build());
            return;
        }

        nameBox = addRenderableWidget(new EditBox(font, rightX, panelTop + 31,
                Math.max(40, rightWidth - 60), 18, Component.translatable("screen.ragephysics.name")));
        nameBox.setMaxLength(48);
        nameBox.setValue(selected.name());
        addRenderableWidget(Button.builder(Component.translatable("screen.ragephysics.apply"), button -> applyName())
                .bounds(rightX + rightWidth - 56, panelTop + 31, 56, 18).build());

        pendingScale = selected.timeScale();
        lastSentScale = selected.timeScale();
        addRenderableWidget(new TimeScaleSlider(rightX, panelTop + 53, rightWidth, pendingScale,
                value -> pendingScale = value));

        int pairGap = 4;
        int pairWidth = (rightWidth - pairGap) / 2;
        addRenderableWidget(Button.builder(modeLabel(selected.mode()), button -> cycleMode())
                .bounds(rightX, panelTop + 98, pairWidth, 18).build());
        addRenderableWidget(Button.builder(enabledLabel(selected.enabled()), button -> toggleEnabled())
                .bounds(rightX + pairWidth + pairGap, panelTop + 98, rightWidth - pairWidth - pairGap, 18).build());

        TemporalTarget[] targets = TemporalTarget.values();
        int targetGap = 4;
        int targetWidth = (rightWidth - targetGap) / 2;
        int footerYRelative = panelHeight - 24;
        int targetRowHeight = Math.max(10, Math.min(14, (footerYRelative - 134) / 5 - 1));
        int targetRowSpacing = targetRowHeight + 1;
        for (int index = 0; index < targets.length; index++) {
            TemporalTarget target = targets[index];
            int column = index % 2;
            int row = index / 2;
            Button targetButton = Button.builder(targetLabel(target), button -> toggleTarget(target))
                    .bounds(rightX + column * (targetWidth + targetGap), panelTop + 132 + row * targetRowSpacing,
                            column == 0 ? targetWidth : rightWidth - targetWidth - targetGap, targetRowHeight)
                    .build();
            targetButton.active = EDITABLE_TARGETS.contains(target);
            if (!targetButton.active) {
                targetButton.setTooltip(Tooltip.create(Component.translatable("screen.ragephysics.unavailable")));
            }
            addRenderableWidget(targetButton);
        }

        if (confirmDelete) {
            int third = Math.max(42, (rightWidth - 8) / 3);
            addRenderableWidget(Button.builder(Component.translatable("screen.ragephysics.confirm_delete"),
                    button -> deleteSelected()).bounds(rightX, footerY, third, 18).build());
            addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> {
                confirmDelete = false;
                buildWidgets();
            }).bounds(rightX + third + 4, footerY, third, 18).build());
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                    .bounds(rightX + (third + 4) * 2, footerY,
                            Math.max(36, rightWidth - (third + 4) * 2), 18).build());
        } else {
            int buttonWidth = Math.max(58, (rightWidth - 4) / 2);
            addRenderableWidget(Button.builder(Component.translatable("screen.ragephysics.delete"), button -> {
                confirmDelete = true;
                buildWidgets();
            }).bounds(rightX, footerY, buttonWidth, 18).build());
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                    .bounds(rightX + buttonWidth + 4, footerY,
                            Math.max(40, rightWidth - buttonWidth - 4), 18).build());
        }
    }

    @Override
    public void tick() {
        if (minecraft.player == null || !minecraft.player.isAlive()) {
            onClose();
            return;
        }
        if (++sendTicks >= 2) {
            sendTicks = 0;
            sendPendingScale();
        }
    }

    private void applyName() {
        if (selected != null && nameBox != null) {
            PacketDistributor.sendToServer(new RegionManagementPayloads.Rename(selected.id(), nameBox.getValue()));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == 257 || keyCode == 335) && nameBox != null && nameBox.isFocused()) {
            applyName();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void cycleMode() {
        if (selected == null) return;
        TemporalMode[] values = TemporalMode.values();
        TemporalMode next = values[(selected.mode().ordinal() + 1) % values.length];
        PacketDistributor.sendToServer(new RegionManagementPayloads.UpdateMode(selected.id(), next));
    }

    private void toggleEnabled() {
        if (selected != null) {
            PacketDistributor.sendToServer(new RegionManagementPayloads.SetEnabled(selected.id(), !selected.enabled()));
        }
    }

    private void toggleTarget(TemporalTarget target) {
        if (selected == null || !EDITABLE_TARGETS.contains(target)) return;
        EnumSet<TemporalTarget> targets = EnumSet.noneOf(TemporalTarget.class);
        targets.addAll(selected.targets());
        if (!targets.remove(target)) targets.add(target);
        PacketDistributor.sendToServer(new RegionManagementPayloads.UpdateTargets(selected.id(), targets));
    }

    private void deleteSelected() {
        if (selected != null) PacketDistributor.sendToServer(new RegionManagementPayloads.Delete(selected.id()));
    }

    private void sendPendingScale() {
        if (selected != null && Double.compare(pendingScale, lastSentScale) != 0) {
            PacketDistributor.sendToServer(new RegionManagementPayloads.UpdateTimeScale(selected.id(), pendingScale));
            lastSentScale = pendingScale;
        }
    }

    @Override
    public void removed() {
        sendPendingScale();
    }

    public void receiveList(RegionManagementPayloads.ListResponse payload) {
        applyList(payload);
        if (minecraft != null) buildWidgets();
        if (selectedRegionId != null) {
            PacketDistributor.sendToServer(new RegionManagementPayloads.RequestDetails(selectedRegionId));
        }
    }

    public void receiveDetails(RegionManagementPayloads.DetailsResponse payload) {
        if (payload.status() == RegionOperationStatus.SUCCESS && payload.region() != null
                && payload.region().id().equals(selectedRegionId)) {
            selected = payload.region();
            buildWidgets();
        } else if (payload.status() != RegionOperationStatus.SUCCESS) {
            feedback(payload.status());
            RegionManagerClientState.requestOpen();
        }
    }

    public void receiveOperation(RegionManagementPayloads.OperationResponse payload) {
        feedback(payload.status());
        if (payload.status() == RegionOperationStatus.SUCCESS || payload.status() == RegionOperationStatus.NO_CHANGE) {
            if (payload.operation() == RegionManagementPayloads.OperationType.DELETE) {
                if (payload.region() == null || payload.region().id().equals(selectedRegionId)) {
                    selectedRegionId = null;
                    selected = null;
                }
                confirmDelete = false;
            } else if (payload.operation() == RegionManagementPayloads.OperationType.CREATE
                    && payload.region() != null) {
                selectedRegionId = payload.region().id();
                selected = payload.region();
            } else if (payload.region() != null) {
                regions = replaceSummary(regions, payload.region());
                if (payload.region().id().equals(selectedRegionId)) selected = payload.region();
            }
        }
        if (payload.operation() == RegionManagementPayloads.OperationType.TIME_SCALE
                && payload.status() == RegionOperationStatus.SUCCESS && payload.region() != null) {
            regions = replaceSummary(regions, payload.region());
            return;
        }
        RegionManagerClientState.requestOpen();
    }

    private void applyList(RegionManagementPayloads.ListResponse payload) {
        ResourceLocation currentDimension = Minecraft.getInstance().level == null ? null
                : Minecraft.getInstance().level.dimension().location();
        List<RegionSummary> sorted = new ArrayList<>(payload.regions());
        sorted.sort(Comparator
                .comparing((RegionSummary region) -> currentDimension == null || !region.dimension().equals(currentDimension))
                .thenComparing(RegionSummary::name, String.CASE_INSENSITIVE_ORDER));
        regions = List.copyOf(sorted);
        draft = payload.draft();
        selected = selectedRegionId == null ? null : find(selectedRegionId);
        if (selected == null && !regions.isEmpty()) {
            selected = regions.getFirst();
            selectedRegionId = selected.id();
        }
        clampScroll();
    }

    private static List<RegionSummary> replaceSummary(List<RegionSummary> source, RegionSummary replacement) {
        List<RegionSummary> result = new ArrayList<>(source.size());
        for (RegionSummary region : source) result.add(region.id().equals(replacement.id()) ? replacement : region);
        return List.copyOf(result);
    }

    private RegionSummary find(UUID id) {
        for (RegionSummary region : regions) if (region.id().equals(id)) return region;
        return null;
    }

    private void select(RegionSummary region) {
        sendPendingScale();
        selectedRegionId = region.id();
        selected = region;
        confirmDelete = false;
        feedback = Component.empty();
        buildWidgets();
        PacketDistributor.sendToServer(new RegionManagementPayloads.RequestDetails(region.id()));
    }

    private void feedback(RegionOperationStatus status) {
        feedback = Component.translatable("screen.ragephysics.status." + status.name().toLowerCase(Locale.ROOT));
        feedbackColor = status == RegionOperationStatus.SUCCESS || status == RegionOperationStatus.NO_CHANGE
                ? 0xFF80D080 : 0xFFFF7777;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= panelLeft + 4 && mouseX < dividerX - 3
                && mouseY >= listTop && mouseY < listBottom) {
            int index = scrollRows + ((int) mouseY - listTop) / ROW_HEIGHT;
            if (index >= 0 && index < regions.size()) {
                select(regions.get(index));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= panelLeft + 4 && mouseX < dividerX - 3 && mouseY >= listTop && mouseY < listBottom) {
            scrollRows -= (int) Math.signum(scrollY);
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void clampScroll() {
        int visible = Math.max(1, (listBottom - listTop) / ROW_HEIGHT);
        scrollRows = Math.max(0, Math.min(scrollRows, Math.max(0, regions.size() - visible)));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x70000000);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xFF101014);
        graphics.fill(panelLeft + 1, panelTop + 1, panelLeft + panelWidth - 1, panelTop + panelHeight - 1, 0xFF77777F);
        graphics.fill(panelLeft + 3, panelTop + 3, panelLeft + panelWidth - 3, panelTop + panelHeight - 3, 0xFF29292F);
        graphics.fill(dividerX, panelTop + 25, dividerX + 1, panelTop + panelHeight - 5, 0xFF77777F);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        renderLabels(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, panelLeft + 9, panelTop + 9, 0xFFFFFFFF);
        graphics.drawString(font, Component.translatable("screen.ragephysics.regions"), panelLeft + 8, panelTop + 20, 0xFFB8B8B8);
        renderRegionList(graphics, mouseX, mouseY);
        renderDraft(graphics);

        int rightX = dividerX + 10;
        int rightWidth = panelLeft + panelWidth - rightX - 9;
        if (selected == null) {
            String empty = font.plainSubstrByWidth(
                    Component.translatable("screen.ragephysics.no_regions").getString(), rightWidth);
            String hint = font.plainSubstrByWidth(
                    Component.translatable("screen.ragephysics.no_regions_hint").getString(), rightWidth);
            graphics.drawCenteredString(font, empty, rightX + rightWidth / 2, panelTop + 62, 0xFFFFFFFF);
            graphics.drawCenteredString(font, hint, rightX + rightWidth / 2, panelTop + 78, 0xFFAAAAAA);
        } else {
            graphics.drawString(font, Component.translatable("screen.ragephysics.selected_region"), rightX, panelTop + 20, 0xFFFFFFFF);
            graphics.drawString(font, Component.translatable("screen.ragephysics.region_dimension", dimensionName(selected.dimension())),
                    rightX, panelTop + 75, 0xFFCCCCCC);
            int sizeX = selected.max().getX() - selected.min().getX() + 1;
            int sizeY = selected.max().getY() - selected.min().getY() + 1;
            int sizeZ = selected.max().getZ() - selected.min().getZ() + 1;
            String bounds = selected.min().toShortString() + " → " + selected.max().toShortString()
                    + "  (" + sizeX + "×" + sizeY + "×" + sizeZ + ")";
            graphics.drawString(font, font.plainSubstrByWidth(bounds, rightWidth), rightX, panelTop + 85, 0xFF999999);
            Component targetsLabel = Component.translatable("screen.ragephysics.targets");
            graphics.drawString(font, targetsLabel, rightX, panelTop + 120, 0xFFCCCCCC);
            if (selected.mode() == TemporalMode.VISUAL_ONLY) {
                graphics.drawString(font, font.plainSubstrByWidth(
                        Component.translatable("screen.ragephysics.visual_only_warning").getString(),
                        Math.max(10, rightWidth - font.width(targetsLabel) - 8)),
                        rightX + font.width(targetsLabel) + 8, panelTop + 120, 0xFFFFB060);
            }
        }
        if (!feedback.getString().isEmpty()) {
            String message = font.plainSubstrByWidth(feedback.getString(), Math.max(20, rightWidth / 2));
            graphics.drawString(font, message, panelLeft + panelWidth - 9 - font.width(message),
                    panelTop + 20, feedbackColor);
        }
    }

    private void renderRegionList(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.enableScissor(panelLeft + 4, listTop, dividerX - 3, listBottom);
        int visible = Math.max(1, (listBottom - listTop) / ROW_HEIGHT + 1);
        for (int offset = 0; offset < visible; offset++) {
            int index = scrollRows + offset;
            if (index >= regions.size()) break;
            RegionSummary region = regions.get(index);
            int y = listTop + offset * ROW_HEIGHT;
            boolean chosen = region.id().equals(selectedRegionId);
            boolean hovered = mouseX >= panelLeft + 5 && mouseX < dividerX - 4
                    && mouseY >= y && mouseY < y + ROW_HEIGHT - 2;
            graphics.fill(panelLeft + 5, y, dividerX - 4, y + ROW_HEIGHT - 2,
                    chosen ? 0xFF455A70 : hovered ? 0xFF3A3A42 : 0xFF202026);
            int textWidth = dividerX - panelLeft - 16;
            graphics.drawString(font, font.plainSubstrByWidth(region.name(), textWidth), panelLeft + 9, y + 5,
                    region.enabled() ? 0xFFFFFFFF : 0xFF999999);
            String state = Component.translatable(region.enabled()
                    ? "screen.ragephysics.on" : "screen.ragephysics.off").getString();
            String detail = TimeScaleSlider.format(region.timeScale()) + " • " + dimensionName(region.dimension()).getString()
                    + " • " + state;
            graphics.drawString(font, font.plainSubstrByWidth(detail, textWidth), panelLeft + 9, y + 18,
                    region.enabled() ? TimeScaleVisuals.color(region.timeScale()) : 0xFF777777);
        }
        graphics.disableScissor();
    }

    private void renderDraft(GuiGraphics graphics) {
        int y = panelTop + panelHeight - 68;
        graphics.drawString(font, Component.translatable("screen.ragephysics.current_draft"), panelLeft + 8, y, 0xFFFFFFFF);
        if (draft == null) {
            String noDraft = font.plainSubstrByWidth(
                    Component.translatable("screen.ragephysics.no_draft").getString(), leftWidth - 16);
            graphics.drawString(font, noDraft, panelLeft + 8, y + 12, 0xFF888888);
            return;
        }
        int sizeX = Math.abs(draft.posB().getX() - draft.posA().getX()) + 1;
        int sizeY = Math.abs(draft.posB().getY() - draft.posA().getY()) + 1;
        int sizeZ = Math.abs(draft.posB().getZ() - draft.posA().getZ()) + 1;
        int width = leftWidth - 16;
        graphics.drawString(font, font.plainSubstrByWidth("A " + draft.posA().toShortString(), width), panelLeft + 8, y + 12, 0xFFAAAAAA);
        graphics.drawString(font, font.plainSubstrByWidth("B " + draft.posB().toShortString(), width), panelLeft + 8, y + 22, 0xFFAAAAAA);
        graphics.drawString(font, sizeX + "×" + sizeY + "×" + sizeZ, panelLeft + 8, y + 32, 0xFFAAAAAA);
    }

    private Component modeLabel(TemporalMode mode) {
        return Component.translatable("screen.ragephysics.mode." + mode.name().toLowerCase(Locale.ROOT));
    }

    private Component enabledLabel(boolean enabled) {
        return Component.translatable(enabled ? "screen.ragephysics.enabled" : "screen.ragephysics.disabled");
    }

    private Component targetLabel(TemporalTarget target) {
        String key = "screen.ragephysics.target." + target.name().toLowerCase(Locale.ROOT);
        String state = selected.targets().contains(target) ? "☒ " : "☐ ";
        if (!EDITABLE_TARGETS.contains(target)) state = "— ";
        return Component.literal(state).append(Component.translatable(key));
    }

    private static Component dimensionName(ResourceLocation dimension) {
        if (dimension.equals(ResourceLocation.withDefaultNamespace("overworld"))) {
            return Component.translatable("screen.ragephysics.dimension.overworld");
        }
        if (dimension.equals(ResourceLocation.withDefaultNamespace("the_nether"))) {
            return Component.translatable("screen.ragephysics.dimension.nether");
        }
        if (dimension.equals(ResourceLocation.withDefaultNamespace("the_end"))) {
            return Component.translatable("screen.ragephysics.dimension.end");
        }
        return Component.literal(dimension.toString());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
