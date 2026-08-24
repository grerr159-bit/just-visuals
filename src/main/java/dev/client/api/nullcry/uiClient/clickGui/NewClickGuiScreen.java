package dev.client.api.nullcry.uiClient.clickGui;

import com.google.common.eventbus.Subscribe;
import dev.client.Just;
import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.events.core.input.KeyBindEvent;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.ModuleManager;
import dev.client.api.nullcry.render.core.animations.client.Direction;
import dev.client.api.nullcry.uiClient.clickGui.newgui.GuiState;
import dev.client.api.nullcry.uiClient.clickGui.newgui.render.GuiRenderer;
import dev.client.api.nullcry.uiClient.clickGui.newgui.FriendsStorage;
import dev.client.api.nullcry.uiClient.clickGui.newgui.LocationStorage;
import dev.client.api.nullcry.uiClient.clickGui.newgui.render.SettingsRenderer;
import dev.client.api.nullcry.uiClient.clickGui.newgui.util.ScrollUtil;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

/**
 * Новый ClickGui экран
 */
public class NewClickGuiScreen extends Screen implements ClientApi {
    
    private boolean closing = false;
    
    public NewClickGuiScreen() {
        super(Text.of("Just.new_click_gui"));
        Just.getInstance().getEventBus().register(this);
    }
    
    @Override
    protected void init() {
        super.init();
        
        GuiState.reset();
        
        float scaledWidth = mc.getWindow().getScaledWidth();
        float scaledHeight = mc.getWindow().getScaledHeight();
        GuiState.setX(scaledWidth / 2f - GuiState.getWidth() / 2f);
        GuiState.setY(scaledHeight / 2f - GuiState.getHeight() / 2f);
        
        updateModules();
        
        GuiState.mainAnimation.reset();
        GuiState.mainAnimation.setDirection(Direction.FORWARDS);
        
        closing = false;
        FriendsStorage.load();
        LocationStorage.load();
    }
    
    private void updateModules() {
        ModuleManager manager = Just.getInstance().getModuleManager();
        GuiState.setModules(manager.stream()
            .filter(m -> m.getModuleCategory() == GuiState.getSelectedCategory())
            .toList());
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        GuiState.setCurrentMouseX(mouseX);
        GuiState.setCurrentMouseY(mouseY);
        
        GuiRenderer.render(context, mouseX, mouseY, delta);
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (closing && GuiState.mainAnimation.isFinished(Direction.BACKWARDS)) {
            finishClose();
        }
    }
    
    @Override
    public void close() {
        if (!closing) {
            startClosing();
            return;
        }
        finishClose();
    }
    
    private void startClosing() {
        closing = true;
        GuiState.setExit(true);
        GuiState.mainAnimation.setDirection(Direction.BACKWARDS);
        GuiState.mainAnimation.reset();
    }
    
    private void finishClose() {
        closing = false;
        GuiState.setExit(false);
        mc.setScreen(null);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Не рендерим стандартный фон
    }
    
    @Subscribe
    public void keyListener(KeyBindEvent event) {
        if (Objects.isNull(mc.currentScreen) && event.getKey() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            mc.setScreen(this);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) return super.mouseClicked(mouseX, mouseY, button);

        if (handleCategoryClick(mouseX, mouseY, button)) {
            return true;
        }

        if (GuiState.getSelectedCategory() == ModuleCategory.Friends) {
            if (handleFriendsClick(mouseX, mouseY, button)) {
                return true;
            }
        }

        if (GuiState.getSelectedCategory() == ModuleCategory.Location) {
            if (handleLocationClick(mouseX, mouseY, button)) {
                return true;
            }
        }

        if (handleModuleAndSettingsClick(mouseX, mouseY, button)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private boolean handleCategoryClick(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        
        float x = GuiState.getX();
        float y = GuiState.getY() + 33.7f;
        float width = GuiState.getWidth() * 0.28f;
        float categoryHeight = 21.325f;
        
        float downY = 0f;
        for (ModuleCategory category : ModuleCategory.values()) {
            float catY = y + 9.665f + downY;
            
            if (isHovered(mouseX, mouseY, x, catY, width, categoryHeight)) {
                GuiState.setSelectedCategory(category);
                updateModules();
                return true;
            }
            
            downY += 24f;
        }
        
        return false;
    }
    
    private boolean handleModuleAndSettingsClick(double mouseX, double mouseY, int button) {
        float leftWidth = GuiState.getWidth() * 0.28f;
        float x = GuiState.getX() + leftWidth + 0.5f;
        float y = GuiState.getY() + 34.025f;
        float width = GuiState.getWidth() - leftWidth - 0.5f;
        float height = GuiState.getHeight() - 34.025f;
        float moduleWidth = (width - 21f) / 2f;
        float moduleHeight = 21.325f;
        
        // Проверяем, что клик внутри области модулей
        if (!isHovered(mouseX, mouseY, x, y, width, height)) {
            return false;
        }
        
        // Учитываем скролл
        float scrollOffset = GuiState.scrollUtil.getScroll();
        float clipY = y + 5f;
        float clipHeight = height - 10f;
        
        float downYColumn1 = -scrollOffset;
        float downYColumn2 = -scrollOffset;
        int index = 1;
        
        for (Module module : GuiState.getModules()) {
            boolean isColumn1 = (index % 2 == 1);
            float moduleX = isColumn1 ? x + 7.15f : x + width / 2f + 3f;
            float moduleY = y + 9.34f + (isColumn1 ? downYColumn1 : downYColumn2);
            
            // Вычисляем полную высоту модуля
            float totalModuleHeight = moduleHeight;
            
            // Проверяем, что модуль виден в области клипа
            if (moduleY + moduleHeight >= clipY && moduleY <= clipY + clipHeight) {
                // Сначала проверяем клики по настройкам (если они открыты)
                if (GuiState.openSettingsModules.contains(module) && !module.getSettings().isEmpty()) {
                    float settingsY = moduleY + moduleHeight + 2f;
                    
                    for (var setting : module.getSettings()) {
                        if (setting.getShown().get()) {
                            float settingHeight = SettingsRenderer.calculateSettingHeight(setting, moduleWidth - 6f);
                            
                            // Проверяем, что настройка видна
                            if (settingsY + settingHeight >= clipY && settingsY <= clipY + clipHeight) {
                                // Проверяем клик по настройке
                                if (isHovered(mouseX, mouseY, moduleX + 3f, settingsY, moduleWidth - 6f, settingHeight)) {
                                    if (SettingsRenderer.handleClick(
                                        setting, mouseX, mouseY, button, moduleX + 3f, settingsY, moduleWidth - 6f, settingHeight)) {
                                        return true;
                                    }
                                    
                                    if (SettingsRenderer.startDrag(
                                        setting, mouseX, mouseY, button, moduleX + 3f, settingsY, moduleWidth - 6f, settingHeight)) {
                                        return true;
                                    }
                                }
                            }
                            
                            totalModuleHeight += settingHeight + 2f;
                            settingsY += settingHeight + 2f;
                        }
                    }
                }
                
                // Теперь проверяем клик по заголовку модуля
                if (isHovered(mouseX, mouseY, moduleX, moduleY, moduleWidth, moduleHeight)) {
                    if (button == 0) {
                        // ЛКМ - переключение модуля
                        module.toggle();
                        return true;
                    } else if (button == 1) {
                        // ПКМ - открытие/закрытие настроек
                        if (!module.getSettings().isEmpty()) {
                            if (GuiState.openSettingsModules.contains(module)) {
                                GuiState.openSettingsModules.remove(module);
                            } else {
                                GuiState.openSettingsModules.add(module);
                            }
                        }
                        return true;
                    }
                }
            }
            
            if (isColumn1) {
                downYColumn1 += totalModuleHeight + 9f;
            } else {
                downYColumn2 += totalModuleHeight + 9f;
            }
            
            index++;
        }
        
        return false;
    }
    
    private boolean isHovered(double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (closing) return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        
        // Обработка перетаскивания слайдеров
        for (Module module : GuiState.getModules()) {
            if (GuiState.openSettingsModules.contains(module)) {
                for (var setting : module.getSettings()) {
                    if (setting instanceof dev.client.api.nullcry.modules.settings.Slider slider && slider.getDragging()) {
                        float leftWidth2 = GuiState.getWidth() * 0.28f;
                        float x = GuiState.getX() + leftWidth2 + 0.5f;
                        float y = GuiState.getY() + 34.025f;
                        float width = GuiState.getWidth() - leftWidth2 - 0.5f;
                        float height = GuiState.getHeight() - 34.025f;
                        float moduleWidth = (width - 21f) / 2f;
                        
                        // Находим позицию настройки (упрощенно)
                        dev.client.api.nullcry.uiClient.clickGui.newgui.render.SettingsRenderer.handleDrag(
                            setting, mouseX, mouseY, x, y, moduleWidth, 20f);
                        return true;
                    }
                }
            }
        }
        
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (closing) return super.mouseReleased(mouseX, mouseY, button);
        
        // Останавливаем перетаскивание всех слайдеров
        for (Module module : GuiState.getModules()) {
            if (GuiState.openSettingsModules.contains(module)) {
                for (var setting : module.getSettings()) {
                    dev.client.api.nullcry.uiClient.clickGui.newgui.render.SettingsRenderer.stopDrag(setting);
                }
            }
        }
        
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (closing) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        
        // Проверяем, что мышь над панелью модулей
        float leftWidth3 = GuiState.getWidth() * 0.28f;
        float x = GuiState.getX() + leftWidth3 + 0.5f;
        float y = GuiState.getY() + 34.025f;
        float width = GuiState.getWidth() - leftWidth3 - 0.5f;
        float height = GuiState.getHeight() - 34.025f;
        
        if (isHovered(mouseX, mouseY, x, y, width, height)) {
            GuiState.scrollUtil.addScroll(verticalAmount);
        }
        
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (closing) return super.keyPressed(keyCode, scanCode, modifiers);

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        if (GuiRenderer.friendInputFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!GuiRenderer.friendInput.isEmpty()) {
                    GuiRenderer.friendInput = GuiRenderer.friendInput.substring(0, GuiRenderer.friendInput.length() - 1);
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER && !GuiRenderer.friendInput.isEmpty()) {
                FriendsStorage.addFriend(GuiRenderer.friendInput);
                GuiRenderer.friendInput = "";
                return true;
            }
        }

        if (GuiRenderer.locationInputFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!GuiRenderer.locationInput.isEmpty()) {
                    GuiRenderer.locationInput = GuiRenderer.locationInput.substring(0, GuiRenderer.locationInput.length() - 1);
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                unfocusAllLocation();
                GuiRenderer.locationXFocused = true;
                return true;
            }
        }

        if (GuiRenderer.locationXFocused || GuiRenderer.locationYFocused || GuiRenderer.locationZFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (GuiRenderer.locationXFocused && !GuiRenderer.locationXInput.isEmpty()) {
                    GuiRenderer.locationXInput = GuiRenderer.locationXInput.substring(0, GuiRenderer.locationXInput.length() - 1);
                } else if (GuiRenderer.locationYFocused && !GuiRenderer.locationYInput.isEmpty()) {
                    GuiRenderer.locationYInput = GuiRenderer.locationYInput.substring(0, GuiRenderer.locationYInput.length() - 1);
                } else if (GuiRenderer.locationZFocused && !GuiRenderer.locationZInput.isEmpty()) {
                    GuiRenderer.locationZInput = GuiRenderer.locationZInput.substring(0, GuiRenderer.locationZInput.length() - 1);
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                if (GuiRenderer.locationXFocused) {
                    unfocusAllLocation();
                    GuiRenderer.locationYFocused = true;
                } else if (GuiRenderer.locationYFocused) {
                    unfocusAllLocation();
                    GuiRenderer.locationZFocused = true;
                } else {
                    unfocusAllLocation();
                    GuiRenderer.locationInputFocused = true;
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                if (!GuiRenderer.locationInput.isEmpty()) {
                    int wx = GuiRenderer.locationXInput.isEmpty() ? 0 : parseInt(GuiRenderer.locationXInput);
                    int wy = GuiRenderer.locationYInput.isEmpty() ? 0 : parseInt(GuiRenderer.locationYInput);
                    int wz = GuiRenderer.locationZInput.isEmpty() ? 0 : parseInt(GuiRenderer.locationZInput);
                    LocationStorage.addWaypoint(GuiRenderer.locationInput, wx, wy, wz, 0xFF00FF00, 0);
                    GuiRenderer.locationInput = "";
                    GuiRenderer.locationXInput = "";
                    GuiRenderer.locationYInput = "";
                    GuiRenderer.locationZInput = "";
                    unfocusAllLocation();
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                unfocusAllLocation();
                return true;
            }
        }

        if (GuiRenderer.bindingWaypointIndex >= 0) {
            var waypoints = LocationStorage.getWaypoints();
            if (GuiRenderer.bindingWaypointIndex < waypoints.size()) {
                LocationStorage.Waypoint wp = waypoints.get(GuiRenderer.bindingWaypointIndex);
                if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    wp.keyBind = 0;
                } else {
                    wp.keyBind = keyCode;
                }
                LocationStorage.save();
                GuiRenderer.bindingWaypointIndex = -1;
                return true;
            }
            GuiRenderer.bindingWaypointIndex = -1;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            for (var module : Just.getInstance().getModuleManager().getModules()) {
                for (var setting : module.getSettings()) {
                    if (setting instanceof dev.client.api.nullcry.modules.settings.KeyBind kb && Boolean.TRUE.equals(kb.getSelected())) {
                        kb.setSelected(false);
                        return true;
                    }
                }
            }
        }

        for (var module : Just.getInstance().getModuleManager().getModules()) {
            for (var setting : module.getSettings()) {
                if (setting instanceof dev.client.api.nullcry.modules.settings.KeyBind kb && Boolean.TRUE.equals(kb.getSelected())) {
                    if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                        kb.set(0);
                    } else {
                        kb.set(keyCode);
                    }
                    kb.setSelected(false);
                    return true;
                }
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (closing) return super.charTyped(chr, modifiers);

        if (GuiRenderer.friendInputFocused) {
            if (GuiRenderer.friendInput.length() < 16 && chr >= 32 && chr < 127) {
                GuiRenderer.friendInput += chr;
            }
            return true;
        }

        if (GuiRenderer.locationInputFocused) {
            if (GuiRenderer.locationInput.length() < 16 && chr >= 32 && chr < 127) {
                GuiRenderer.locationInput += chr;
            }
            return true;
        }

        if (GuiRenderer.locationXFocused || GuiRenderer.locationYFocused || GuiRenderer.locationZFocused) {
            if (chr >= '0' && chr <= '9' || chr == '-') {
                String target = GuiRenderer.locationXFocused ? GuiRenderer.locationXInput :
                        GuiRenderer.locationYFocused ? GuiRenderer.locationYInput : GuiRenderer.locationZInput;
                if (target.length() < 8) {
                    if (GuiRenderer.locationXFocused) GuiRenderer.locationXInput += chr;
                    else if (GuiRenderer.locationYFocused) GuiRenderer.locationYInput += chr;
                    else GuiRenderer.locationZInput += chr;
                }
            }
            return true;
        }

        return super.charTyped(chr, modifiers);
    }

    private boolean handleFriendsClick(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        float guiX = GuiState.getX();
        float guiY = GuiState.getY();
        float leftWidth = GuiState.getWidth() * 0.28f;
        float panelX = guiX + leftWidth + 0.5f;
        float panelY = guiY + 34.025f;
        float panelWidth = GuiState.getWidth() - leftWidth - 0.5f;

        float pad = 10f;
        float inputX = panelX + pad;
        float inputY = panelY + pad;
        float inputW = panelWidth - pad * 2 - 30f;
        float inputH = 18f;

        if (mouseX >= inputX && mouseX <= inputX + inputW && mouseY >= inputY && mouseY <= inputY + inputH) {
            GuiRenderer.friendInputFocused = true;
            return true;
        }

        float btnX = inputX + inputW + 5f;
        float btnW = 25f;
        if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= inputY && mouseY <= inputY + inputH) {
            if (!GuiRenderer.friendInput.isEmpty()) {
                FriendsStorage.addFriend(GuiRenderer.friendInput);
                GuiRenderer.friendInput = "";
            }
            return true;
        }

        float listY = inputY + inputH + 8f;
        var friends = FriendsStorage.getFriends();
        float itemH = 22f;
        float itemPad = 2f;

        for (int i = 0; i < friends.size(); i++) {
            float itemY = listY + i * (itemH + itemPad);
            float removeX = panelX + pad + panelWidth - pad * 2 - 22f;
            float removeY = itemY + (itemH - 14f) / 2f;

            if (mouseX >= removeX && mouseX <= removeX + 14f && mouseY >= removeY && mouseY <= removeY + 14f) {
                FriendsStorage.removeFriend(friends.get(i));
                return true;
            }
        }

        GuiRenderer.friendInputFocused = false;
        return false;
    }

    private boolean handleLocationClick(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        float guiX = GuiState.getX();
        float guiY = GuiState.getY();
        float leftWidth = GuiState.getWidth() * 0.28f;
        float panelX = guiX + leftWidth + 0.5f;
        float panelY = guiY + 34.025f;
        float panelWidth = GuiState.getWidth() - leftWidth - 0.5f;

        float pad = 10f;
        float inputY = panelY + pad;
        float inputH = 14f;
        float gap = 3f;
        float nameW = panelWidth - pad * 2;
        float nameX = panelX + pad;

        if (mouseX >= nameX && mouseX <= nameX + nameW && mouseY >= inputY && mouseY <= inputY + inputH) {
            unfocusAllLocation();
            GuiRenderer.locationInputFocused = true;
            return true;
        }

        float coordY = inputY + inputH + gap;
        float coordH = 14f;
        float coordW = (nameW - gap * 2) / 3f;

        for (int i = 0; i < 3; i++) {
            float cx = nameX + i * (coordW + gap);
            if (mouseX >= cx && mouseX <= cx + coordW && mouseY >= coordY && mouseY <= coordY + coordH) {
                unfocusAllLocation();
                if (i == 0) GuiRenderer.locationXFocused = true;
                else if (i == 1) GuiRenderer.locationYFocused = true;
                else GuiRenderer.locationZFocused = true;
                return true;
            }
        }

        float btnY = coordY + coordH + gap;
        float btnH = 16f;
        if (mouseX >= nameX && mouseX <= nameX + nameW && mouseY >= btnY && mouseY <= btnY + btnH) {
            if (!GuiRenderer.locationInput.isEmpty()) {
                int wx = GuiRenderer.locationXInput.isEmpty() ? 0 : parseInt(GuiRenderer.locationXInput);
                int wy = GuiRenderer.locationYInput.isEmpty() ? 0 : parseInt(GuiRenderer.locationYInput);
                int wz = GuiRenderer.locationZInput.isEmpty() ? 0 : parseInt(GuiRenderer.locationZInput);
                LocationStorage.addWaypoint(GuiRenderer.locationInput, wx, wy, wz, 0xFF00FF00, 0);
                GuiRenderer.locationInput = "";
                GuiRenderer.locationXInput = "";
                GuiRenderer.locationYInput = "";
                GuiRenderer.locationZInput = "";
            }
            return true;
        }

        float listY = btnY + btnH + 8f;
        float listW = panelWidth - pad * 2;
        var waypoints = LocationStorage.getWaypoints();
        float itemH = 22f;
        float itemPad = 2f;

        for (int i = 0; i < waypoints.size(); i++) {
            float itemY = listY + i * (itemH + itemPad);

            float statusX = panelX + pad + listW - 36f;
            float statusY = itemY + (itemH - 10f) / 2f;
            if (mouseX >= statusX && mouseX <= statusX + 22f && mouseY >= statusY && mouseY <= statusY + 10f) {
                LocationStorage.toggleWaypoint(waypoints.get(i).name);
                return true;
            }

            float kbX = statusX - 28f;
            float kbY = statusY;
            if (mouseX >= kbX && mouseX <= kbX + 24f && mouseY >= kbY && mouseY <= kbY + 10f) {
                GuiRenderer.bindingWaypointIndex = i;
                return true;
            }

            float removeX = panelX + pad + listW - 14f;
            float removeY = itemY + (itemH - 14f) / 2f;
            if (mouseX >= removeX && mouseX <= removeX + 14f && mouseY >= removeY && mouseY <= removeY + 14f) {
                LocationStorage.removeWaypoint(waypoints.get(i).name);
                return true;
            }
        }

        unfocusAllLocation();
        return false;
    }

    private void unfocusAllLocation() {
        GuiRenderer.locationInputFocused = false;
        GuiRenderer.locationXFocused = false;
        GuiRenderer.locationYFocused = false;
        GuiRenderer.locationZFocused = false;
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }
}
