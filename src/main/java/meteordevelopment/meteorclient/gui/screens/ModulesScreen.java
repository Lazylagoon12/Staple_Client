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
    }

    @Override
    public void initWidgets() {
        controller = add(new WCategoryController()).widget();
    }

    @Override
    protected void init() {
        super.init();
        controller.refresh();
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
        private final List<WWindow> categoryWindows = new ArrayList<>();
        private final List<WNavButton> categoryButtons = new ArrayList<>();
        private Cell<WWindow> favorites;
        private String selected = "Combat";

        @Override
        protected boolean propagateEvents(WWidget widget) {
            return widget.visible;
        }

        @Override
        public void init() {
            List<Module> all = new ArrayList<>();
            for (Module module : Modules.get().getAll()) {
                if (!Config.get().hiddenModules.get().contains(module)) all.add(module);
            }

            List<Module> combat = new ArrayList<>();
            List<Module> movement = new ArrayList<>();
            List<Module> render = new ArrayList<>();
            List<Module> player = new ArrayList<>();
            List<Module> world = new ArrayList<>();
            List<Module> misc = new ArrayList<>();

            for (Module module : all) {
                if (module.category.equals(Categories.Combat)) combat.add(module);
                else if (module.category.equals(Categories.Movement)) movement.add(module);
                else if (module.category.equals(Categories.Render)) render.add(module);
                else if (module.category.equals(Categories.Player)) player.add(module);
                else if (module.category.equals(Categories.World)) world.add(module);
                else misc.add(module);
            }

            addCategory("Combat", combat);
            addCategory("Movement", movement);
            addCategory("Render", render);
            addCategory("Player", player);
            addCategory("World", world);
            addCategory("Misc", misc);

            add(new WPanelAction("Top Bar", () -> Tabs.get(GuiTab.class).openScreen(theme)));
            add(new WPanelAction("Configs", () -> Tabs.get(ConfigTab.class).openScreen(theme)));
            add(new WPanelAction("Modules", () -> select("Combat")));
            add(new WPanelAction("ClickGUI", () -> Tabs.get(GuiTab.class).openScreen(theme)));
            add(new WPanelAction("Themes", () -> Tabs.get(GuiTab.class).openScreen(theme)));
            add(new WPanelAction("Settings", () -> Tabs.get(ConfigTab.class).openScreen(theme)));

            createSearch(this);
            refresh();
        }

        private void addCategory(String name, List<Module> modules) {
            WNavButton button = add(new WNavButton(name, () -> select(name))).widget();
            categoryButtons.add(button);

            WWindow window = theme.window(name);
            window.id = "staple-" + name.toLowerCase();
            window.padding = 5;
            window.spacing = 7;
            window.visible = name.equals(selected);
            window.view.scrollOnlyWhenMouseOver = true;
            window.view.hasScrollBar = true;
            window.view.spacing = 7;
            window.minWidth = 220;

            List<Module> first = new ArrayList<>();
            List<Module> second = new ArrayList<>();
            for (int i = 0; i < modules.size(); i++) {
                (i % 2 == 0 ? first : second).add(modules.get(i));
            }

            WContainer grid = new WContainer() {
                @Override
                protected void onCalculateSize() {
                    width = 0;
                    height = 0;
                    for (Cell<?> cell : cells) {
                        width += cell.widget().width;
                        height = Math.max(height, cell.widget().height);
                    }
                }

                @Override
                protected void onCalculateWidgetPositions() {
                    double gap = theme.scale(8);
                    double cardWidth = Math.max(theme.scale(150), (width - gap) / 2);
                    double leftX = x;
                    double rightX = x + cardWidth + gap;
                    for (int i = 0; i < cells.size(); i++) {
                        Cell<?> cell = cells.get(i);
                        cell.x = (i < first.size() ? leftX : rightX);
                        cell.y = y + (i < first.size() ? i : i - first.size()) * (theme.scale(58) + gap);
                        cell.width = cardWidth;
                        cell.height = cell.widget().height;
                        cell.widget().width = cardWidth;
                        cell.widget().calculateWidgetPositions();
                    }
                    height = Math.max(first.size(), second.size()) * (theme.scale(58) + gap);
                }
            };
            grid.spacing = 8;
            for (int i = 0; i < Math.max(first.size(), second.size()); i++) {
                if (i < first.size()) grid.add(theme.module(first.get(i))).expandX();
                if (i < second.size()) grid.add(theme.module(second.get(i))).expandX();
            }
            window.add(grid).expandX();
            add(window);
            categoryWindows.add(window);
        }

        private void select(String name) {
            selected = name;
            for (WWindow window : categoryWindows) window.visible = window.titleEquals(name);
            for (WNavButton button : categoryButtons) button.active = button.label.equals(name);
            invalidate();
        }

        protected void refresh() {
            if (favorites == null) {
                boolean hasFavorites = Modules.get().getAll().stream().anyMatch(module -> module.favorite);
                if (hasFavorites) {
                    WWindow window = theme.window("Favorites");
                    window.id = "favorites";
                    window.padding = 5;
                    window.spacing = 6;
                    favorites = add(window);
                    window.view.scrollOnlyWhenMouseOver = true;
                    window.view.hasScrollBar = true;
                    for (Module module : Modules.get().getAll()) if (module.favorite) window.add(theme.module(module)).expandX();
                }
            }
        }

        @Override
        protected void onCalculateWidgetPositions() {
            double pad = theme.scale(18);
            double gap = theme.scale(14);
            double screenW = getWindowWidth();
            double screenH = getWindowHeight();
            double leftW = theme.scale(164);
            double rightW = theme.scale(210);
            double centerX = pad + leftW + gap;
            double centerW = Math.max(theme.scale(300), screenW - centerX - rightW - gap - pad);
            double top = theme.scale(72);
            double bottom = screenH - theme.scale(24);
            double contentH = bottom - top;

            for (int i = 0; i < categoryButtons.size(); i++) {
                WNavButton button = categoryButtons.get(i);
                button.width = leftW;
                button.height = theme.scale(42);
                button.x = pad;
                button.y = top + i * theme.scale(49);
                button.calculateWidgetPositions();
            }

            for (WWindow window : categoryWindows) {
                window.x = centerX;
                window.y = top;
                window.width = centerW;
                window.minWidth = centerW / theme.scale(1);
                window.height = contentH;
                window.calculateSize();
                window.width = centerW;
                window.height = contentH;
                window.calculateWidgetPositions();
            }

            int actionIndex = 0;
            for (Cell<?> cell : cells) {
                if (cell.widget() instanceof WPanelAction action) {
                    action.x = screenW - pad - rightW;
                    action.y = top + theme.scale(46) + actionIndex * theme.scale(54);
                    action.width = rightW;
                    action.height = theme.scale(44);
                    action.calculateWidgetPositions();
                    actionIndex++;
                } else if (cell.widget() instanceof WWindow window && "search".equals(window.id)) {
                    window.x = centerX;
                    window.y = theme.scale(18);
                    window.width = centerW;
                    window.minWidth = centerW / theme.scale(1);
                    window.calculateSize();
                    window.calculateWidgetPositions();
                } else if (cell.widget() instanceof WWindow window && "favorites".equals(window.id)) {
                    window.x = pad;
                    window.y = screenH - theme.scale(70);
                    window.width = leftW;
                    window.minWidth = leftW / theme.scale(1);
                    window.calculateSize();
                    window.calculateWidgetPositions();
                }
            }
        }

        private class WNavButton extends WPressable {
            private final String label;
            private final Runnable action;
            private boolean active;

            private WNavButton(String label, Runnable action) {
                this.label = label;
                this.action = action;
            }

            @Override
            protected void onPressed(int button) {
                if (button == MOUSE_BUTTON_LEFT) action.run();
            }

            @Override
            protected void onRender(meteordevelopment.meteorclient.gui.renderer.GuiRenderer renderer, double mouseX, double mouseY, double delta) {
                boolean selected = active || mouseOver;
                ColorPair.draw(renderer, x, y, width, height, selected);
                renderer.text(label, x + theme.scale(16), y + (height - theme.textHeight()) / 2, theme.textColor(), true);
            }
        }

        private class WPanelAction extends WPressable {
            private final String label;
            private final Runnable action;

            private WPanelAction(String label, Runnable action) {
                this.label = label;
                this.action = action;
            }

            @Override
            protected void onPressed(int button) {
                if (button == MOUSE_BUTTON_LEFT) action.run();
            }

            @Override
            protected void onRender(meteordevelopment.meteorclient.gui.renderer.GuiRenderer renderer, double mouseX, double mouseY, double delta) {
                ColorPair.draw(renderer, x, y, width, height, mouseOver);
                renderer.text(label, x + theme.scale(15), y + (height - theme.textHeight()) / 2, theme.textColor(), true);
            }
        }
    }

    private static class ColorPair {
        private static void draw(meteordevelopment.meteorclient.gui.renderer.GuiRenderer renderer, double x, double y, double width, double height, boolean active) {
            meteordevelopment.meteorclient.utils.render.color.Color left = active
                ? new meteordevelopment.meteorclient.utils.render.color.Color(89, 36, 205, 235)
                : new meteordevelopment.meteorclient.utils.render.color.Color(35, 31, 62, 225);
            meteordevelopment.meteorclient.utils.render.color.Color right = active
                ? new meteordevelopment.meteorclient.utils.render.color.Color(111, 62, 224, 235)
                : new meteordevelopment.meteorclient.utils.render.color.Color(45, 39, 75, 225);
            double radius = Math.min(height / 2, width / 2);
            renderer.quad(x + radius, y, width - radius * 2, height, left, right);
            renderer.quad(x, y, height, height, meteordevelopment.meteorclient.gui.renderer.GuiRenderer.CIRCLE, left);
            renderer.quad(x + width - height, y, height, height, meteordevelopment.meteorclient.gui.renderer.GuiRenderer.CIRCLE, right);
        }
    }
}
