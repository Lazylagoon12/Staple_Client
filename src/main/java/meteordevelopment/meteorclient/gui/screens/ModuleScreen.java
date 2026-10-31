/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.ActiveModulesChangedEvent;
import meteordevelopment.meteorclient.events.meteor.ModuleBindChangedEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WKeybind;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.NonNull;
import net.minecraft.client.input.KeyEvent;

import java.util.Optional;

import static com.mojang.blaze3d.platform.InputConstants.*;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

public class ModuleScreen extends WidgetScreen {
    private final Module module;
    private WView settingsView;
    private WKeybind keybind;
    private WCheckbox active;

    public ModuleScreen(GuiTheme theme, Module module) {
        super(theme, module.title);
        this.module = module;
        animProgress = 0;
    }

    @Override
    public void renderCustom(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        double progress = Math.max(0, Math.min(1, animProgress));
        double eased = 1 - Math.pow(1 - progress, 3);
        float scale = (float) (0.94 + 0.06 * eased);
        float cx = getWindowWidth() / 2.0f, cy = getWindowHeight() / 2.0f;
        graphics.pose().pushMatrix();
        graphics.pose().translate(cx * (1 - scale), cy * (1 - scale));
        graphics.pose().scale(scale, scale);
        super.renderCustom(graphics, mouseX, mouseY, delta);
        graphics.pose().popMatrix();
    }

    @Override
    public void initWidgets() {
        WSettingsPanel panel = add(new WSettingsPanel()).widget();
        panel.add(new WCategorySidebar());
        panel.add(theme.label("STAPLE CLIENT", true));
        panel.add(theme.label(module.title, true));
        if (module.description != null && !module.description.isBlank()) panel.add(theme.label(module.description));
        if (module.addon != null && module.addon != MeteorClient.ADDON) {
            panel.add(theme.label("From: " + module.addon.name).color(module.addon.color));
        }

        settingsView = panel.add(new WSettingsView()).expandX().widget();
        settingsView.scrollOnlyWhenMouseOver = true;
        settingsView.hasScrollBar = true;
        if (!module.settings.groups.isEmpty()) settingsView.add(theme.settings(module.settings)).expandX();

        WWidget custom = module.getWidget(theme);
        if (custom != null) settingsView.add(custom).expandX();

        panel.add(theme.label("BIND & MODULE", true));
        WContainer bindRow = panel.add(theme.horizontalList()).expandX().widget();
        bindRow.add(theme.label("Bind: "));
        keybind = bindRow.add(theme.keybind(module.keybind)).expandX().widget();
        keybind.actionOnSet = () -> Modules.get().setModuleToBind(module);
        WButton reset = bindRow.add(theme.button(GuiRenderer.RESET)).widget();
        reset.action = keybind::resetBind;
        reset.tooltip = "Reset";

        WContainer flags = panel.add(theme.horizontalList()).expandX().widget();
        flags.add(theme.label("Toggle on bind release"));
        WCheckbox release = flags.add(theme.checkbox(module.toggleOnBindRelease)).widget();
        release.action = () -> module.toggleOnBindRelease = release.checked;
        flags.add(theme.label("Chat feedback"));
        WCheckbox feedback = flags.add(theme.checkbox(module.chatFeedback)).widget();
        feedback.action = () -> module.chatFeedback = feedback.checked;

        WContainer footer = panel.add(theme.horizontalList()).expandX().widget();
        footer.add(theme.label("Enabled"));
        active = footer.add(theme.checkbox(module.isActive())).widget();
        active.action = () -> { if (module.isActive() != active.checked) module.toggle(); };
        WButton copy = footer.add(theme.button(GuiRenderer.COPY)).right().widget();
        copy.action = this::toClipboard;
        copy.tooltip = "Copy config";
        WButton paste = footer.add(theme.button(GuiRenderer.PASTE)).widget();
        paste.action = this::fromClipboard;
        paste.tooltip = "Paste config";
        WButton close = footer.add(theme.button("Back")).widget();
        close.action = this::onClose;
    }

    @Override
    public void tick() {
        super.tick();
        module.settings.tick(settingsView, theme);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !Modules.get().isBinding();
    }

    @EventHandler
    private void onModuleBindChanged(ModuleBindChangedEvent event) {
        if (keybind != null) keybind.reset();
    }

    @EventHandler
    private void onActiveModulesChanged(ActiveModulesChangedEvent event) {
        if (active != null) active.checked = module.isActive();
    }

    @Override
    public boolean toClipboard() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", module.name);
        CompoundTag settingsTag = module.settings.toTag();
        if (!settingsTag.isEmpty()) tag.put("settings", settingsTag);
        return NbtUtils.toClipboard(tag);
    }

    @Override
    public boolean fromClipboard() {
        CompoundTag tag = NbtUtils.fromClipboard();
        if (tag == null || !tag.getStringOr("name", "").equals(module.name)) return false;
        Optional<CompoundTag> settings = tag.getCompound("settings");
        if (settings.isPresent()) module.settings.fromTag(settings.get());
        else module.settings.reset();
        reload();
        return true;
    }

    private class WSettingsPanel extends WContainer {
        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            double r = theme.scale(18);
            Color base = new Color(29, 25, 45, 245);
            Color edge = new Color(53, 43, 78, 245);
            renderer.quad(x + r, y, width - r * 2, height, base, edge);
            renderer.quad(x, y + r, r * 2, height - r * 2, base);
            renderer.quad(x + width - r * 2, y + r, r * 2, height - r * 2, edge);
            renderer.quad(x, y, r * 2, r * 2, GuiRenderer.CIRCLE, base);
            renderer.quad(x + width - r * 2, y, r * 2, r * 2, GuiRenderer.CIRCLE, edge);
            renderer.quad(x, y + height - r * 2, r * 2, r * 2, GuiRenderer.CIRCLE, base);
            renderer.quad(x + width - r * 2, y + height - r * 2, r * 2, r * 2, GuiRenderer.CIRCLE, edge);

            // Staple's signature purple accent and subtle header divider.
            double accentH = theme.scale(5);
            Color accentLeft = new Color(111, 54, 220, 255);
            Color accentRight = new Color(164, 105, 245, 255);
            renderer.quad(x + r, y, width - r * 2, accentH, accentLeft, accentRight);
            renderer.quad(x, y, r * 2, accentH, GuiRenderer.CIRCLE, accentLeft);
            renderer.quad(x + width - r * 2, y, r * 2, accentH, GuiRenderer.CIRCLE, accentRight);
            renderer.quad(x + theme.scale(20), y + theme.scale(48), width - theme.scale(40), 1,
                new Color(102, 78, 140, 150), new Color(102, 78, 140, 35));
        }

        @Override
        protected void onCalculateSize() {
            width = Math.min(theme.scale(900), getWindowWidth() - theme.scale(48));
            height = Math.min(theme.scale(680), getWindowHeight() - theme.scale(48));
        }

        @Override
        protected void onCalculateWidgetPositions() {
            x = (getWindowWidth() - width) / 2;
            y = (getWindowHeight() - height) / 2;
            double pad = theme.scale(28);
            double sidebarW = theme.scale(142);
            double contentX = x + pad + sidebarW + theme.scale(22);
            double contentW = width - pad * 2 - sidebarW - theme.scale(22);
            double cy = y + pad;
            for (int i = 0; i < cells.size(); i++) {
                WWidget w = cells.get(i).widget();
                if (w instanceof WCategorySidebar) {
                    w.x = x + pad;
                    w.y = y + pad;
                    w.width = sidebarW;
                    w.height = height - pad * 2;
                } else {
                    w.x = contentX;
                    w.y = cy;
                    w.width = contentW;
                    if (w == settingsView) {
                        w.height = Math.max(theme.scale(120), height - theme.scale(250));
                    } else if (w instanceof meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList) {
                        w.height = theme.scale(34);
                    } else {
                        w.height = theme.scale(24);
                    }
                }
                w.calculateSize();
                if (w instanceof WCategorySidebar) {
                    w.width = sidebarW;
                    w.height = height - pad * 2;
                } else if (w == settingsView) {
                    w.height = Math.max(theme.scale(120), height - theme.scale(250));
                }
                w.calculateWidgetPositions();
                if (!(w instanceof WCategorySidebar)) cy += w.height + theme.scale(10);
            }
        }
    }

    private class WCategorySidebar extends WWidget {
        private final java.util.List<String> categories = java.util.List.of("Combat", "Player", "Movement", "Visuals", "Misc", "Exploits", "Client");

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            double rowH = theme.scale(40);
            double gap = theme.scale(9);
            double yy = y + theme.scale(92);
            for (String category : categories) {
                boolean selected = module.category.name.equalsIgnoreCase(category);
                Color left = selected ? new Color(105, 43, 205, 245) : new Color(43, 34, 61, 220);
                Color right = selected ? new Color(145, 75, 225, 245) : new Color(55, 43, 75, 220);
                double r = rowH / 2;
                renderer.quad(x + r, yy, width - r * 2, rowH, left, right);
                renderer.quad(x, yy, rowH, rowH, GuiRenderer.CIRCLE, left);
                renderer.quad(x + width - rowH, yy, rowH, rowH, GuiRenderer.CIRCLE, right);
                renderer.text(category, x + theme.scale(13), yy + (rowH - theme.textHeight()) / 2, theme.textColor(), true);
                yy += rowH + gap;
            }
        }
    }

    private static class WSettingsView extends WView {
        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            double r = theme.scale(12);
            Color left = new Color(21, 19, 34, 220);
            Color right = new Color(31, 25, 48, 220);
            renderer.quad(x + r, y, width - r * 2, height, left, right);
            renderer.quad(x, y + r, r * 2, height - r * 2, left);
            renderer.quad(x + width - r * 2, y + r, r * 2, height - r * 2, right);
            renderer.quad(x, y, r * 2, r * 2, GuiRenderer.CIRCLE, left);
            renderer.quad(x + width - r * 2, y, r * 2, r * 2, GuiRenderer.CIRCLE, right);
            renderer.quad(x, y + height - r * 2, r * 2, r * 2, GuiRenderer.CIRCLE, left);
            renderer.quad(x + width - r * 2, y + height - r * 2, r * 2, r * 2, GuiRenderer.CIRCLE, right);
        }
    }
}
