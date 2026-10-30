/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WTriangle;
import meteordevelopment.meteorclient.systems.modules.Module;

import java.util.List;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WMeteorWindow extends WWindow implements MeteorWidget {
    private List<Module> modules;
    private WLabel countLabel;

    public void setModules(List<Module> modules) {
        this.modules = List.copyOf(modules);
    }

    private String moduleCount() {
        if (modules == null) return "";
        long active = modules.stream().filter(Module::isActive).count();
        return active + "/" + modules.size();
    }
    public WMeteorWindow(WWidget icon, String title) {
        super(icon, title);
    }

    @Override
    protected WHeader header(WWidget icon) {
        return new WMeteorHeader(icon);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (expanded || animProgress > 0) {
            double bodyY = y + header.height;
            double bodyHeight = height - header.height;
            double radius = Math.min(theme().scale(5), Math.min(width / 2, bodyHeight / 2));
            Color left = new Color(70, 61, 82, 142);
            Color right = new Color(88, 76, 101, 142);
            renderer.quad(x, bodyY, width, Math.max(0, bodyHeight - radius), left, right);
            renderer.quad(x + radius, bodyY + bodyHeight - radius, width - radius * 2, radius, left, right);
            renderer.quad(x, bodyY + bodyHeight - radius * 2, radius * 2, radius * 2, GuiRenderer.CIRCLE, left);
            renderer.quad(x + width - radius * 2, bodyY + bodyHeight - radius * 2, radius * 2, radius * 2, GuiRenderer.CIRCLE, right);
            renderer.quad(x, bodyY, width, theme().scale(1), new Color(221, 213, 234, 80));
        }
    }

    private class WMeteorHeader extends WHeader {
        public WMeteorHeader(WWidget icon) {
            super(icon);
        }

        @Override
        public void init() {
            WHorizontalList row = theme().horizontalList();
            row.spacing = 1;
            add(row).expandX().widget();
            row.add(theme().label(title, true)).expandCellX().centerY().pad(5);
            if (modules != null) {
                countLabel = theme().label(moduleCount(), false);
                row.add(countLabel).right().centerY().pad(3);
            }
            WTriangle triangle = theme().triangle();
            row.add(triangle).right().centerY().pad(4);
            triangle.action = () -> setExpanded(!expanded);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            double radius = Math.min(theme().scale(5), Math.min(width / 2, height / 2));
            Color left = new Color(108, 96, 124, 224);
            Color right = new Color(77, 68, 94, 224);
            renderer.quad(x + radius, y, width - radius * 2, height, left, right);
            renderer.quad(x, y + radius, radius * 2, height - radius, left);
            renderer.quad(x + width - radius * 2, y + radius, radius * 2, height - radius, right);
            renderer.quad(x, y, radius * 2, radius * 2, GuiRenderer.CIRCLE, left);
            renderer.quad(x + width - radius * 2, y, radius * 2, radius * 2, GuiRenderer.CIRCLE, right);
            renderer.quad(x + radius, y + theme().scale(1), width - radius * 2, theme().scale(1),
                new Color(220, 212, 231, 150));
            if (countLabel != null) countLabel.set(moduleCount());
        }
    }
}