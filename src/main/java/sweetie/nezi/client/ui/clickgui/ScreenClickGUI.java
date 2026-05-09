package sweetie.nezi.client.ui.clickgui;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleManager;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.animation.AnimationUtil;
import sweetie.nezi.api.utils.animation.Easing;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.MouseUtil;
import sweetie.nezi.api.utils.other.SearchUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.services.RenderService;
import sweetie.nezi.client.ui.theme.Theme;
import sweetie.nezi.client.ui.theme.ThemeEditor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ScreenClickGUI extends Screen implements QuickImports {
    @Getter private static final ScreenClickGUI instance = new ScreenClickGUI();
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private boolean open;
    private final AnimationUtil openAnimation = new AnimationUtil();

    private final List<Panel> panels = new ArrayList<>();
    private final ThemeEditor themeEditor = ThemeEditor.getInstance();
    private String searchQuery = "";
    private boolean searchTyping;
    private long openedAt;

    private float guiX;
    private float guiY;
    private float guiWidth;
    private float guiHeight;
    private float panelWidth;
    private float panelHeight;
    private float columnGap;
    private float sidePad;
    private Panel draggingPanel;
    private float panelDragOffsetX;
    private final Map<Panel, Float> animatedPanelX = new HashMap<>();

    public ScreenClickGUI() {
        super(Text.of(""));

        for (int i = 0; i < Category.values().length; i++) {
            Category category = Category.values()[i];
            Panel panel = new Panel(category);
            panel.setCategoryIndex(i * 45);
            panels.add(panel);
        }
    }

    @Override
    public void close() {
        themeEditor.save(false);
        open = false;
        if (draggingPanel != null) {
            draggingPanel.setDragging(false);
        }
        draggingPanel = null;
        animatedPanelX.clear();
        super.close();
    }

    @Override
    protected void init() {
        themeEditor.init();
        open = true;
        openedAt = System.currentTimeMillis();
        searchTyping = false;
        super.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        sweetie.nezi.api.utils.render.display.ThemeTransitionRender transitionRender = sweetie.nezi.api.utils.render.display.ThemeTransitionRender.getInstance();

        if (transitionRender.isTransitioning()) {
            transitionRender.updateProgress();
        }

        if (ThemeEditor.getInstance().shouldStartPendingTransition() && !transitionRender.isTransitioning()) {
            transitionRender.startTransition(ThemeEditor.getInstance().getPendingTheme());
            ThemeEditor.getInstance().markPendingTransitionStarted();
            if (transitionRender.getFbo() != null) {
                transitionRender.getFbo().beginWrite(true);
                renderCore(context, mouseX, mouseY, delta);
                mc.getFramebuffer().beginWrite(true);
            }
            ThemeEditor.getInstance().applyPendingTheme();
        }

        renderCore(context, mouseX, mouseY, delta);

        if (transitionRender.isTransitioning()) {
            transitionRender.drawMaskedTransition(context.getMatrices(), mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
        }
    }

    private void renderCore(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        openAnimation.update();
        openAnimation.run(open ? 1.0 : 0.0, 460, Easing.EXPO_OUT);

        float openAnim = (float) openAnimation.getValue();
        if (!open && openAnim < 0.03f) {
            close();
            return;
        }

        float windowWidth = mc.getWindow().getScaledWidth();
        float windowHeight = mc.getWindow().getScaledHeight();

        columnGap = scaled(4f);
        sidePad = scaled(10f);
        panelWidth = scaled(106f);
        panelHeight = scaled(252f);
        guiWidth = panelWidth * panels.size() + columnGap * (panels.size() - 1) + sidePad * 2f;
        guiHeight = panelHeight;

        guiX = MathHelper.clamp(windowWidth / 2f - guiWidth / 2f, scaled(12f), Math.max(scaled(12f), windowWidth - guiWidth - scaled(12f)));
        float baseGuiY = windowHeight / 2f - guiHeight / 2f;
        float pullOffset = (1f - openAnim) * scaled(46f);
        guiY = MathHelper.clamp(baseGuiY + pullOffset, scaled(18f), Math.max(scaled(18f), windowHeight - guiHeight - scaled(18f)));

        renderBackdrop(context, windowWidth, windowHeight, openAnim);
        renderControls(context, guiX, guiY, guiWidth, openAnim, windowWidth, windowHeight);

        float draggedPanelX = 0f;
        if (draggingPanel != null) {
            float targetDragX = MathHelper.clamp((float) mouseX - panelDragOffsetX,
                    guiX + sidePad,
                    guiX + guiWidth - sidePad - panelWidth);
            float currentDragX = animatedPanelX.getOrDefault(draggingPanel, targetDragX);
            draggedPanelX = animatePanelX(draggingPanel, currentDragX, targetDragX, 0.34f);

            int currentIndex = panels.indexOf(draggingPanel);
            int targetIndex = closestPanelIndex(targetDragX + panelWidth / 2f);
            if (currentIndex >= 0 && targetIndex >= 0 && currentIndex != targetIndex) {
                panels.remove(draggingPanel);
                panels.add(targetIndex, draggingPanel);
            }
        }

        float panelX = guiX + sidePad;
        for (Panel panel : panels) {
            if (panel == draggingPanel) {
                panelX += panelWidth + columnGap;
                continue;
            }

            panel.setAlpha(openAnim);
            panel.setWidth(panelWidth);
            panel.setHeight(panelHeight);
            float currentX = animatedPanelX.getOrDefault(panel, panelX);
            panel.setX(animatePanelX(panel, currentX, panelX, 0.24f));
            panel.setY(guiY);
            renderPanel(context, panel, mouseX, mouseY, delta);
            panelX += panelWidth + columnGap;
        }

        if (draggingPanel != null) {
            draggingPanel.setAlpha(openAnim);
            draggingPanel.setWidth(panelWidth);
            draggingPanel.setHeight(panelHeight);
            draggingPanel.setX(draggedPanelX);
            draggingPanel.setY(guiY);
            renderPanel(context, draggingPanel, mouseX, mouseY, delta);
        }

        float themeProgress = themeEditor.getOpenProgress();
        if (themeProgress > 0.01f) {
            int blurAlpha = (int) (themeProgress * openAnim * 255f);
            RenderUtil.BLUR_RECT.draw(context.getMatrices(), 0f, 0f, windowWidth, windowHeight, 0f, UIColors.widgetBlur(Math.min(180, blurAlpha)), 0.10f * themeProgress);
            RenderUtil.RECT.draw(context.getMatrices(), 0f, 0f, windowWidth, windowHeight, 0f, UIColors.overlay(Math.min(150, (int) (blurAlpha * 0.65f))));
        }

        if (themeEditor.isOpen()) {
            themeEditor.setAnim(openAnim);
            themeEditor.setWidth(panelWidth * 2.5f + columnGap * 2f);
            themeEditor.setX(guiX + guiWidth / 2f - themeEditor.getWidth() / 2f);
            themeEditor.setY(guiY + scaled(8f));
        }
        themeEditor.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchTyping) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                searchTyping = false;
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_TAB) {
                String suggestion = getSearchSuggestion();
                if (suggestion != null) {
                    searchQuery = suggestion;
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            if (System.currentTimeMillis() - openedAt < 180L) {
                return true;
            }

            if (themeEditor.isOpen()) {
                themeEditor.setOpen(false);
                return true;
            }

            open = false;
            mc.mouse.lockCursor();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            open = false;
            themeEditor.setOpen(false);
            mc.mouse.lockCursor();
            return true;
        }

        if (themeEditor.isOpen()) {
            themeEditor.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }

        for (Panel panel : panels) {
            panel.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (MouseUtil.isHovered(mouseX, mouseY, themeButtonX(), themeButtonY(), themeButtonWidth(), controlHeight())) {
                themeEditor.setOpen(!themeEditor.isOpen());
                return true;
            }

            if (MouseUtil.isHovered(mouseX, mouseY, searchFieldX(), searchFieldY(), searchFieldWidth(), controlHeight())) {
                searchTyping = true;
                return true;
            }

            searchTyping = false;
        }

        if (themeEditor.isOpen()) {
            themeEditor.mouseClicked(mouseX, mouseY, button);
            if (themeEditor.isInside(mouseX, mouseY)) {
                return true;
            }
        }

        if (button == 0) {
            for (Panel panel : panels) {
                if (panel.isInsideHeader(mouseX, mouseY)) {
                    draggingPanel = panel;
                    panel.setDragging(true);
                    panelDragOffsetX = (float) mouseX - panel.getX();
                    return true;
                }
            }
        }

        for (Panel panel : panels) {
            panel.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingPanel != null) {
            draggingPanel.setDragging(false);
            draggingPanel = null;
            return true;
        }

        if (themeEditor.isOpen()) {
            themeEditor.mouseReleased(mouseX, mouseY, button);
            if (themeEditor.isInside(mouseX, mouseY)) {
                return true;
            }
        }

        for (Panel panel : panels) {
            panel.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (themeEditor.isOpen() && themeEditor.isInside(mouseX, mouseY)) {
            themeEditor.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            return true;
        }

        for (Panel panel : panels) {
            panel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchTyping && searchQuery.length() < 22 && !Character.isISOControl(chr)) {
            searchQuery += chr;
            return true;
        }

        if (themeEditor.isOpen()) {
            return themeEditor.charTyped(chr, modifiers);
        }
        return false;
    }

    private void renderBackdrop(DrawContext context, float windowWidth, float windowHeight, float alpha) {
        int fullAlpha = (int) (alpha * 255f);
        // Один проход blur вместо двух — FPS буст
        RenderUtil.BLUR_RECT.draw(context.getMatrices(), 0f, 0f, windowWidth, windowHeight, 0f, UIColors.blur(Math.min(210, (int) (fullAlpha * 0.85f))), 0.05f);
    }

    private void renderControls(DrawContext context, float guiX, float guiY, float guiWidth, float alpha, float windowWidth, float windowHeight) {
        int fullAlpha = (int) (alpha * 255f);
        float contentProgress = MathHelper.clamp((alpha - 0.12f) / 0.88f, 0f, 1f);
        int contentAlpha = Math.max(0, (int) (fullAlpha * contentProgress));
        float contentOffset = scaled(2f) * (1f - contentProgress);
        String suggestion = getSearchSuggestion();

        drawControl(context, themeButtonX(), themeButtonY(), themeButtonWidth(), controlHeight(), "Themes", themeEditor.isOpen(), fullAlpha, contentAlpha, contentOffset);

        String searchText = searchQuery.isBlank()
                ? "search..."
                : searchQuery
                + (searchTyping && System.currentTimeMillis() % 1000 > 450 ? "_" : "")
                + (suggestion == null ? "" : " -> " + suggestion);
        drawControl(context, searchFieldX(), searchFieldY(), searchFieldWidth(), controlHeight(), searchText, searchTyping || !searchQuery.isBlank(), fullAlpha, contentAlpha, contentOffset);

        String time = LocalTime.now().format(CLOCK_FORMAT);
        float timeSize = scaled(6.1f);
        float pad = scaled(5f);
        float clockW = Fonts.PS_MEDIUM.getWidth(time, timeSize) + pad * 2f;
        float clockH = scaled(12f);
        float clockX = windowWidth / 2f - clockW / 2f;
        float clockY = windowHeight - clockH - scaled(10f);

        drawGlassSurface(context.getMatrices(), clockX, clockY, clockW, clockH, scaled(4f),
                UIColors.card(Math.min(124, fullAlpha)), fullAlpha, true);
        Fonts.PS_MEDIUM.drawText(context.getMatrices(), time,
                clockX + pad,
                clockY + clockH / 2f - timeSize / 2f + scaled(0.2f) + contentOffset,
                timeSize, UIColors.textColor(contentAlpha));
    }

    private void drawControl(DrawContext context, float x, float y, float width, float height, String text, boolean active, int alpha, int contentAlpha, float contentOffset) {
        float radius = scaled(4.5f);
        drawGlassSurface(context.getMatrices(), x, y, width, height, radius,
                active ? UIColors.cardSecondary(Math.min(132, alpha)) : UIColors.card(Math.min(118, alpha)),
                alpha, true);

        float textSize = scaled(5.8f);
        float textWidth = Fonts.PS_MEDIUM.getWidth(text, textSize);
        float textX = x + width / 2f - textWidth / 2f;
        float textY = y + height / 2f - textSize / 2f + scaled(0.25f) + contentOffset;
        Fonts.PS_MEDIUM.drawText(
                context.getMatrices(),
                text,
                textX,
                textY,
                textSize,
                active ? UIColors.textColor(contentAlpha) : UIColors.mutedText(contentAlpha)
        );
    }

    private void drawGlassSurface(MatrixStack ms, float x, float y, float width, float height, float round,
                                  java.awt.Color surface, int alpha, boolean compact) {
        int blurAlpha = Math.max(0, Math.min(255, compact ? (int) (alpha * 0.62f) : (int) (alpha * 0.68f)));

        java.awt.Color hudBg = new java.awt.Color(23, 23, 34, (int) (78 * (alpha / 255f)));
        java.awt.Color hudStroke = new java.awt.Color(65, 65, 65, (int) (138 * (alpha / 255f)));

        RenderUtil.BLUR_RECT.draw(ms, x, y, width, height, round, UIColors.blur(blurAlpha), 0.045f);
        RenderUtil.RECT.draw(ms, x - scaled(0.7f), y - scaled(0.7f),
                width + scaled(1.4f), height + scaled(1.4f), round + scaled(0.7f), hudStroke);
        RenderUtil.RECT.draw(ms, x, y, width, height, round, hudBg);
    }

    private float themeButtonY() {
        return guiY - controlHeight() * 2f - scaled(8f);
    }

    private float searchFieldY() {
        return guiY - controlHeight() - scaled(4f);
    }

    private float themeButtonWidth() {
        return scaled(48f);
    }

    private float controlHeight() {
        return scaled(11.4f);
    }

    private float searchFieldWidth() {
        return scaled(118f);
    }

    private float controlsCenterX() {
        return guiX + guiWidth / 2f;
    }

    private float themeButtonX() {
        return controlsCenterX() - themeButtonWidth() / 2f;
    }

    private float searchFieldX() {
        return controlsCenterX() - searchFieldWidth() / 2f;
    }

    private int closestPanelIndex(float mouseCenterX) {
        int closestIndex = -1;
        float closestDistance = Float.MAX_VALUE;
        float slotX = guiX + sidePad;

        for (int i = 0; i < panels.size(); i++) {
            float centerX = slotX + panelWidth / 2f;
            float distance = Math.abs(mouseCenterX - centerX);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestIndex = i;
            }
            slotX += panelWidth + columnGap;
        }

        return closestIndex;
    }

    private float animatePanelX(Panel panel, float currentX, float targetX, float speed) {
        float nextX = currentX + (targetX - currentX) * speed;
        if (Math.abs(nextX - targetX) < 0.35f) {
            nextX = targetX;
        }
        animatedPanelX.put(panel, nextX);
        return nextX;
    }

    private void renderPanel(DrawContext context, Panel panel, int mouseX, int mouseY, float delta) {
        MatrixStack ms = context.getMatrices();
        float cx = panel.getX() + panel.getWidth() / 2f;
        float cy = panel.getY() + panel.getHeight() / 2f;

        ms.push();
        ms.translate(cx, cy, 0f);
        if (Math.abs(panel.getDragJitterX()) > 0.01f || Math.abs(panel.getDragJitterY()) > 0.01f) {
            ms.translate(panel.getDragJitterX(), panel.getDragJitterY(), 0f);
        }
        if (Math.abs(panel.getTiltAngle()) > 0.01f) {
            ms.multiply(new Quaternionf().rotateZ((float) Math.toRadians(panel.getTiltAngle())));
        }
        ms.translate(-cx, -cy, 0f);
        panel.renderReleasePulse(ms);
        panel.render(context, mouseX, mouseY, delta);
        ms.pop();
    }

    private float scaled(float value) {
        return RenderService.getInstance().scaled(value);
    }

    public boolean blocksModuleBinds() {
        if (searchTyping || themeEditor.isOpen()) {
            return true;
        }

        for (Panel panel : panels) {
            if (panel.capturesInput()) {
                return true;
            }
        }

        return false;
    }

    private String getSearchSuggestion() {
        if (searchQuery == null || searchQuery.isBlank()) {
            return null;
        }

        List<String> moduleNames = new ArrayList<>();
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (!"Click GUI".equalsIgnoreCase(module.getName())) {
                moduleNames.add(module.getName());
            }
        }

        String suggestion = SearchUtil.findBestMatch(moduleNames, searchQuery);
        if (suggestion == null) {
            return null;
        }

        return SearchUtil.normalize(suggestion).equals(SearchUtil.normalize(searchQuery)) ? null : suggestion;
    }

    @Override
    public void blur() {}

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void applyBlur() {}
}
