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
        animationProgress1 += delta * 5 * ((module.isActive() || mouseOver) ? 1 : -1);
        animationProgress1 = Mth.clamp(animationProgress1, 0, 1);
        animationProgress2 += delta * 7 * (module.isActive() ? 1 : -1);
        animationProgress2 = Mth.clamp(animationProgress2, 0, 1);
        Color base = new Color(48, 43, 57, 112);
        Color hover = new Color(178, 168, 190, 105);
        renderer.quad(x + height / 2, y, width - height, height, base, mouseOver ? hover : base);
        renderer.quad(x, y, height, height, GuiRenderer.CIRCLE, base);
        renderer.quad(x + width - height, y, height, height, GuiRenderer.CIRCLE, mouseOver ? hover : base);
        renderer.quad(x, y, width, theme.scale(1), new Color(225, 218, 235, 95));
        if (animationProgress1 > 0) {
            Color activeLeft = new Color(194, 184, 207, (int) (105 * animationProgress1));
            Color activeRight = new Color(127, 113, 145, (int) (135 * animationProgress1));
            double activeWidth = width * animationProgress1;
            renderer.quad(x + height / 2, y, Math.max(0, activeWidth - height), height, activeLeft, activeRight);
            renderer.quad(x, y, height, height, GuiRenderer.CIRCLE, activeLeft);
            if (activeWidth >= height) renderer.quad(x + activeWidth - height, y, height, height, GuiRenderer.CIRCLE, activeRight);
        }
        if (animationProgress2 > 0) {
            renderer.quad(x, y + height - theme.scale(2), width * animationProgress2, theme.scale(2),
                new Color(238, 231, 247, (int) (220 * animationProgress2)));
        }
        double textX = this.x + pad + theme.scale(2);
        renderer.text(title, textX, y + (height - theme.textHeight()) / 2, theme.textColor.get(), false);
    }
}
