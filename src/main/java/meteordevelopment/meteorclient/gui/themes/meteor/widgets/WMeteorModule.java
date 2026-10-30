/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Mth;

import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WMeteorModule extends WPressable implements MeteorWidget {
    private final Module module;
    private final String title;
    private double titleWidth;
    private double animationProgress1;

    public WMeteorModule(Module module, String title) {
        this.module = module;
        this.title = title;
        this.tooltip = module.description;
        animationProgress1 = module.isActive() ? 1 : 0;
    }

    @Override
    public double pad() {
        return theme.scale(7);
    }

    @Override
    protected void onCalculateSize() {
        if (titleWidth == 0) titleWidth = theme.textWidth(title);
        width = Math.max(theme.scale(170), titleWidth + theme.scale(52));
        height = theme.scale(52);
    }

    @Override
    protected void onPressed(int button) {
        if (button == MOUSE_BUTTON_LEFT) module.toggle();
        else if (button == MOUSE_BUTTON_RIGHT) mc.gui.setScreen(theme.moduleScreen(module));
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        MeteorGuiTheme theme = theme();
        boolean active = module.isActive();
        animationProgress1 = Mth.clamp(animationProgress1 + delta * 8 * ((active || mouseOver) ? 1 : -1), 0, 1);

        Color backgroundLeft = new Color(27, 27, 55, 235);
        Color backgroundRight = new Color(39, 34, 72, 235);
        double radius = Math.min(height / 2, width / 2);
        renderer.quad(x + radius, y, width - radius * 2, height, backgroundLeft, backgroundRight);
        renderer.quad(x, y, height, height, GuiRenderer.CIRCLE, backgroundLeft);
        renderer.quad(x + width - height, y, height, height, GuiRenderer.CIRCLE, backgroundRight);

        if (animationProgress1 > 0) {
            double alpha = animationProgress1;
            Color activeLeft = new Color(94, 38, 205, (int) (235 * alpha));
            Color activeRight = new Color(117, 67, 225, (int) (235 * alpha));
            renderer.quad(x + radius, y, width - radius * 2, height, activeLeft, activeRight);
            renderer.quad(x, y, height, height, GuiRenderer.CIRCLE, activeLeft);
            renderer.quad(x + width - height, y, height, height, GuiRenderer.CIRCLE, activeRight);
        }

        double textX = x + theme.scale(10);
        renderer.text(title, textX, y + theme.scale(7), theme.textColor.get(), true);
        String description = module.description == null ? "" : module.description;
        if (!description.isEmpty()) {
            if (description.length() > 34) description = description.substring(0, 32) + "...";
            renderer.text(description, textX, y + theme.scale(29), theme.textSecondaryColor.get(), false);
        }

        double toggleW = theme.scale(34);
        double toggleH = theme.scale(17);
        double toggleX = x + width - toggleW - theme.scale(10);
        double toggleY = y + (height - toggleH) / 2;
        Color toggle = active ? new Color(110, 51, 220, 255) : new Color(72, 66, 105, 255);
        renderer.quad(toggleX + toggleH / 2, toggleY, toggleW - toggleH, toggleH, toggle, toggle);
        renderer.quad(toggleX, toggleY, toggleH, toggleH, GuiRenderer.CIRCLE, toggle);
        renderer.quad(toggleX + toggleW - toggleH, toggleY, toggleH, toggleH, GuiRenderer.CIRCLE, toggle);
        double knob = theme.scale(11);
        double knobX = active ? toggleX + toggleW - knob - theme.scale(3) : toggleX + theme.scale(3);
        renderer.quad(knobX, toggleY + (toggleH - knob) / 2, knob, knob, GuiRenderer.CIRCLE,
            new Color(232, 225, 255, 255));
    }
}
