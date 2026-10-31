/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import com.mojang.datafixers.util.Pair;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.tabs.builtin.ConfigTab;
import meteordevelopment.meteorclient.gui.tabs.builtin.GuiTab;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.apache.commons.lang3.SystemUtils;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.mojang.blaze3d.platform.InputConstants.*;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

public class ModulesScreen extends TabScreen {
    private WCategoryController controller;
    private WWindow searchWindow;
    private WTextBox searchTextBox;

    public ModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());
        // Always play the entrance animation when the ClickGUI is opened.
        animProgress = 0;
    }

    @Override
    public void renderCustom(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        double progress = Math.max(0, Math.min(1, animProgress));
        // Smooth ease-out pop: scale from 94% to full size around the screen center.
        double eased = 1 - Math.pow(1 - progress, 3);
        float scale = (float) (0.94 + 0.06 * eased);
        float centerX = getWindowWidth() / 2.0f;
        float centerY = getWindowHeight() / 2.0f;

        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX * (1 - scale), centerY * (1 - scale));
        graphics.pose().scale(scale, scale);
        super.renderCustom(graphics, mouseX, mouseY, delta);
        graphics.pose().popMatrix();
    }

    @Override
    public void initWidgets() {
        controller = add(new WCategoryController()).widget();
    }

    @Override
    protected void init() {
        super.init();
    }

    protected void createSearchW(WContainer w, String text) {
        if (text.isEmpty()) return;

        List<Pair<Module, String>> modules = Modules.get().searchTitles(text);
        if (!modules.isEmpty()) {
            WSection section = w.add(theme.section("Modules")).expandX().widget();
            for (Pair<Module, String> p : modules) {
                if (section.cells.size() >= Config.get().moduleSearchCount.get()) break;
                section.add(theme.module(p.getFirst(), p.getSecond())).expandX();
            }
        }

        Set<Module> settings = Modules.get().searchSettingTitles(text);
        if (!settings.isEmpty()) {
            WSection section = w.add(theme.section("Settings")).expandX().widget();
            for (Module module : settings) {
                if (section.cells.size() >= Config.get().moduleSearchCount.get()) break;
                section.add(theme.module(module)).expandX();
            }
        }
    }

    protected WWindow createSearch(WContainer parent) {
        WWindow window = theme.window("Search");
        window.id = "search";
        searchWindow = window;
        window.visible = false;
        parent.add(window);
        window.view.scrollOnlyWhenMouseOver = true;
        window.view.hasScrollBar = true;

        WVerticalList results = theme.verticalList();
        WTextBox text = window.add(theme.textBox("")).minWidth(180).expandX().widget();
        text.setFocused(true);
        searchTextBox = text;
        text.action = () -> {
            results.clear();
            createSearchW(results, text.get());
        };
        window.add(results).expandX();
        return window;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent input) {
        if (locked) return false;
        boolean ctrl = SystemUtils.IS_OS_MAC
            ? Input.modsMatch(input.modifiers(), MOD_SUPER)
            : Input.modsMatch(input.modifiers(), MOD_CONTROL);

        if (ctrl && input.key() == KEY_F) {
            if (searchWindow != null) {
                searchWindow.visible = true;
                searchWindow.setExpanded(true);
                controller.invalidate();
            }
            if (searchTextBox != null) {
                searchTextBox.setFocused(true);
                searchTextBox.setCursorMax();
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean toClipboard() {
        return NbtUtils.toClipboard(Modules.get());
    }

    @Override
    public boolean fromClipboard() {
        return NbtUtils.fromClipboard(Modules.get());
    }

    @Override
    public void reload() {
    }

    private class WCategoryController extends WContainer {
        private final List<WNavButton> navButtons = new ArrayList<>();
        private final List<WPanelAction> actions = new ArrayList<>();
        private final List<Module> allModules = new ArrayList<>();
        private final WMainPanel panel;
        private final WModuleGrid moduleGrid;
        private WTextBox searchBox;
        private String selected = "Combat";

        WCategoryController() {
            panel = new WMainPanel();
            moduleGrid = new WModuleGrid();
        }

        @Override
        public void init() {
            for (Module module : Modules.get().getAll()) {
                if (!Config.get().hiddenModules.get().contains(module)) allModules.add(module);
            }

            add(panel);
            panel.add(theme.label("STAPLE CLIENT", true)).widget();
            searchBox = panel.add(theme.textBox("Search modules...")).widget();
            searchBox.action = () -> select(selected);
            for (String category : List.of("Combat", "Movement", "Render", "Player", "World", "Misc")) {
                WNavButton button = panel.add(new WNavButton(category, () -> select(category))).widget();
                navButtons.add(button);
            }
            panel.add(moduleGrid);
            addAction("Configs", () -> Tabs.get(ConfigTab.class).openScreen(theme));
            addAction("Modules", () -> select("Combat"));
            addAction("ClickGUI", () -> select(selected));
            addAction("Themes", () -> Tabs.get(GuiTab.class).openScreen(theme));
            addAction("Settings", () -> Tabs.get(ConfigTab.class).openScreen(theme));
            select("Combat");
        }

        private void addAction(String label, Runnable action) {
            actions.add(panel.add(new WPanelAction(label, action)).widget());
        }

        private void select(String category) {
            selected = category;
            for (WNavButton button : navButtons) button.active = button.label.equals(category);
            moduleGrid.clear();
            for (Module module : allModules) {
                String moduleCategory = module.category.name;
                boolean matches = category.equals("Misc")
                    ? !List.of("Combat", "Movement", "Render", "Player", "World").contains(moduleCategory)
                    : moduleCategory.equalsIgnoreCase(category);
                String query = searchBox == null ? "" : searchBox.get().trim().toLowerCase();
                boolean matchesSearch = query.isEmpty() || module.title.toLowerCase().contains(query) || module.name.toLowerCase().contains(query);
                if (matches && matchesSearch) moduleGrid.addModule(theme.module(module));
            }
            invalidate();
        }

        @Override
        protected void onCalculateSize() {
            width = getWindowWidth();
            height = getWindowHeight();
        }

        @Override
        protected void onCalculateWidgetPositions() {
            double screenW = getWindowWidth();
            double screenH = getWindowHeight();
            double panelW = Math.min(theme.scale(1120), screenW - theme.scale(48));
            double panelH = Math.min(theme.scale(720), screenH - theme.scale(48));
            panel.x = (screenW - panelW) / 2;
            panel.y = (screenH - panelH) / 2;
            panel.width = panelW;
            panel.height = panelH;
            panel.calculateSize();
            panel.calculateWidgetPositions();
        }

        private class WMainPanel extends WContainer {
            @Override
            protected void onRender(meteordevelopment.meteorclient.gui.renderer.GuiRenderer renderer, double mouseX, double mouseY, double delta) {
                double radius = theme.scale(18);
                double x0 = x, y0 = y, w = width, h = height;
                meteordevelopment.meteorclient.utils.render.color.Color base = new meteordevelopment.meteorclient.utils.render.color.Color(29, 25, 45, 242);
                meteordevelopment.meteorclient.utils.render.color.Color edge = new meteordevelopment.meteorclient.utils.render.color.Color(53, 43, 78, 242);
                renderer.quad(x0 + radius, y0, w - radius * 2, h, base, edge);
                renderer.quad(x0, y0 + radius, radius * 2, h - radius * 2, base);
                renderer.quad(x0 + w - radius * 2, y0 + radius, radius * 2, h - radius * 2, edge);
                renderer.quad(x0, y0, radius * 2, radius * 2, meteordevelopment.meteorclient.gui.renderer.GuiRenderer.CIRCLE, base);
                renderer.quad(x0 + w - radius * 2, y0, radius * 2, radius * 2, meteordevelopment.meteorclient.gui.renderer.GuiRenderer.CIRCLE, edge);
                renderer.quad(x0, y0 + h - radius * 2, radius * 2, radius * 2, meteordevelopment.meteorclient.gui.renderer.GuiRenderer.CIRCLE, base);
                renderer.quad(x0 + w - radius * 2, y0 + h - radius * 2, radius * 2, radius * 2, meteordevelopment.meteorclient.gui.renderer.GuiRenderer.CIRCLE, edge);
                renderer.quad(x0 + theme.scale(174), y0 + theme.scale(20), theme.scale(1), h - theme.scale(40), new meteordevelopment.meteorclient.utils.render.color.Color(120, 105, 151, 100));
                renderer.quad(x0 + w - theme.scale(205), y0 + theme.scale(20), theme.scale(1), h - theme.scale(40), new meteordevelopment.meteorclient.utils.render.color.Color(120, 105, 151, 100));
            }

            @Override
            protected void onCalculateSize() {
                width = Math.min(theme.scale(1120), getWindowWidth() - theme.scale(48));
                height = Math.min(theme.scale(720), getWindowHeight() - theme.scale(48));
            }

            @Override
            protected void onCalculateWidgetPositions() {
                double pad = theme.scale(24);
                double gap = theme.scale(14);
                double leftW = theme.scale(150);
                double rightW = theme.scale(180);
                for (int i = 0; i < cells.size(); i++) {
                    Cell<?> cell = cells.get(i);
                    WWidget widget = cell.widget();
                    if (widget instanceof WNavButton) {
                        int index = navButtons.indexOf(widget);
                        widget.x = x + pad;
                        widget.y = y + theme.scale(92) + index * theme.scale(49);
                        widget.width = leftW;
                        widget.height = theme.scale(40);
                    } else if (widget == searchBox) {
                        widget.x = x + pad + leftW + gap;
                        widget.y = y + theme.scale(26);
                        widget.width = width - pad * 2 - leftW - rightW - gap * 2;
                        widget.height = theme.scale(30);
                    } else if (widget == moduleGrid) {
                        widget.x = x + pad + leftW + gap;
                        widget.y = y + theme.scale(72);
                        widget.width = width - pad * 2 - leftW - rightW - gap * 2;
                        widget.height = height - theme.scale(96);
                        moduleGrid.maxHeight = widget.height;
                    } else if (widget instanceof WPanelAction) {
                        int index = actions.indexOf(widget);
                        widget.x = x + width - pad - rightW;
                        widget.y = y + theme.scale(92) + index * theme.scale(52);
                        widget.width = rightW;
                        widget.height = theme.scale(40);
                    } else {
                        widget.x = x + pad;
                        widget.y = y + theme.scale(28);
                        widget.width = theme.scale(150);
                        widget.height = theme.scale(24);
                    }
                    widget.calculateSize();
                    if (widget == searchBox) {
                        widget.width = width - pad * 2 - leftW - rightW - gap * 2;
                        widget.height = theme.scale(30);
                    }
                    widget.calculateWidgetPositions();
                }
            }
        }

        private class WModuleGrid extends WView {
            private WModuleRow currentRow;

            WModuleGrid() {
                spacing = 10;
                scrollOnlyWhenMouseOver = true;
                hasScrollBar = true;
            }

            void addModule(WWidget moduleCard) {
                if (currentRow == null || currentRow.cells.size() == 2) {
                    currentRow = new WModuleRow();
                    add(currentRow).expandX();
                }
                currentRow.add(moduleCard).expandX();
                invalidate();
            }

            @Override
            public void clear() {
                super.clear();
                currentRow = null;
            }

            private class WModuleRow extends WContainer {
                @Override
                protected void onCalculateSize() {
                    width = 0;
                    height = theme.scale(52);
                }

                @Override
                protected void onCalculateWidgetPositions() {
                    double gap = theme.scale(10);
                    double cardW = Math.max(0, (width - gap) / 2);
                    for (int i = 0; i < cells.size(); i++) {
                        Cell<?> cell = cells.get(i);
                        cell.x = x + i * (cardW + gap);
                        cell.y = y;
                        cell.width = cardW;
                        cell.height = height;
                        cell.widget().calculateSize();
                        cell.widget().x = cell.x;
                        cell.widget().y = cell.y;
                        cell.widget().width = cardW;
                        cell.widget().height = height;
                        cell.widget().calculateWidgetPositions();
                    }
                }
            }

            @Override
            protected void onCalculateWidgetPositions() {
                super.onCalculateWidgetPositions();
                for (Cell<?> cell : cells) cell.widget().calculateWidgetPositions();
            }
        }

        private class WNavButton extends WPressable {
            private final String label;
            private final Runnable action;
            private boolean active;

            WNavButton(String label, Runnable action) { this.label = label; this.action = action; }

            @Override protected void onPressed(int button) { if (button == MOUSE_BUTTON_LEFT) action.run(); }

            @Override
            protected void onRender(meteordevelopment.meteorclient.gui.renderer.GuiRenderer renderer, double mouseX, double mouseY, double delta) {
                drawPill(renderer, x, y, width, height, active || mouseOver);
                renderer.text(label, x + theme.scale(14), y + (height - theme.textHeight()) / 2, theme.textColor(), true);
            }
        }

        private class WPanelAction extends WPressable {
            private final String label;
            private final Runnable action;

            WPanelAction(String label, Runnable action) { this.label = label; this.action = action; }

            @Override protected void onPressed(int button) { if (button == MOUSE_BUTTON_LEFT) action.run(); }

            @Override
            protected void onRender(meteordevelopment.meteorclient.gui.renderer.GuiRenderer renderer, double mouseX, double mouseY, double delta) {
                drawPill(renderer, x, y, width, height, mouseOver);
                renderer.text(label, x + theme.scale(14), y + (height - theme.textHeight()) / 2, theme.textColor(), true);
            }
        }

        private void drawPill(meteordevelopment.meteorclient.gui.renderer.GuiRenderer renderer, double x, double y, double width, double height, boolean active) {
            meteordevelopment.meteorclient.utils.render.color.Color left = active
                ? new meteordevelopment.meteorclient.utils.render.color.Color(91, 39, 194, 245)
                : new meteordevelopment.meteorclient.utils.render.color.Color(43, 37, 62, 235);
            meteordevelopment.meteorclient.utils.render.color.Color right = active
                ? new meteordevelopment.meteorclient.utils.render.color.Color(119, 69, 211, 245)
                : new meteordevelopment.meteorclient.utils.render.color.Color(57, 48, 78, 235);
            double r = height / 2;
            renderer.quad(x + r, y, width - r * 2, height, left, right);
            renderer.quad(x, y, height, height, meteordevelopment.meteorclient.gui.renderer.GuiRenderer.CIRCLE, left);
            renderer.quad(x + width - height, y, height, height, meteordevelopment.meteorclient.gui.renderer.GuiRenderer.CIRCLE, right);
        }
    }

}
