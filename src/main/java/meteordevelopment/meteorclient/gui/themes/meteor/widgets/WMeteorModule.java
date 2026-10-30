/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
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

    private double animationProgress2;

    public WMeteorModule(Module module, String title) {
        this.module = module;
        this.title = title;
        this.tooltip = module.description;

        if (module.isActive()) {
            animationProgress1 = 1;
            animationProgress2 = 1;
        } else {
            animationProgress1 = 0;
            animationProgress2 = 0;
        }
    }

    @Override
    public double pad() {
        return theme.scale(3);
    }

    @Override
    protected void onCalculateSize() {
        double pad = pad();

        if (titleWidth == 0) titleWidth = theme.textWidth(title);

        width = Math.max(theme.scale(116), pad + titleWidth + pad);
        height = theme.scale(20);
    }

    @Override
    protected void onPressed(int button) {
        if (button == MOUSE_BUTTON_LEFT) module.toggle();
        else if (button == MOUSE_BUTTON_RIGHT) mc.gui.setScreen(theme.moduleScreen(module));
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        MeteorGuiTheme theme = theme();
        double pad = pad();
        boolean highlighted = module.isActive() || mouseOver;
        animationProgress1 += delta * 7 * (highlighted ? 1 : -1);
        animationProgress1 = Mth.clamp(animationProgress1, 0, 1);

        if (animationProgress1 > 0) {
            double alpha = animationProgress1;
            Color left = new Color(205, 198, 216, (int) (205 * alpha));
            Color right = new Color(116, 103, 137, (int) (220 * alpha));
            double radius = Math.min(height, width) / 2;
            double middleWidth = Math.max(0, width - height);
            renderer.quad(x + radius, y, middleWidth, height, left, right);
            renderer.quad(x, y, height, height, GuiRenderer.CIRCLE, left);
            renderer.quad(x + width - height, y, height, height, GuiRenderer.CIRCLE, right);
            renderer.quad(x + radius, y + theme.scale(1), middleWidth, theme.scale(1),
                new Color(245, 239, 250, (int) (125 * alpha)));
        }

        double textX = this.x + pad + theme.scale(2);
        // Keep module names left-aligned to match the compact reference layout.
        renderer.text(title, textX, y + (height - theme.textHeight()) / 2, theme.textColor.get(), false);
    }
}
