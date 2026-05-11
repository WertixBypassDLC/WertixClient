package sweetie.nezi.client.ui.theme;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.other.WindowResizeEvent;
import sweetie.nezi.api.system.configs.ThemeManager;
import sweetie.nezi.api.utils.animation.AnimationUtil;
import sweetie.nezi.api.utils.animation.Easing;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.MouseUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.ScissorUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.ui.UIComponent;
import sweetie.nezi.client.ui.clickgui.module.settings.ColorComponent;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ThemeEditor extends UIComponent {
    @Getter private static final ThemeEditor instance = new ThemeEditor();

    private final List<ThemeSelectable> themeSelectables = new ArrayList<>();
    private final List<ThemeBound> themeBounds = new ArrayList<>();
    private final Theme defaultTheme = new Theme("Wertix");

    private Theme currentTheme = defaultTheme;
    private Theme pendingTheme = null;
    private boolean pendingTransitionStarted = false;
    private Theme renamingTheme;
    private String renameText = "";

    public boolean hasPendingTransition() { return pendingTheme != null; }
    public Theme getPendingTheme() { return pendingTheme; }
    public boolean shouldStartPendingTransition() { return pendingTheme != null && !pendingTransitionStarted; }
    public void markPendingTransitionStarted() {
        if (pendingTheme != null) {
            pendingTransitionStarted = true;
        }
    }
    public Theme beginPendingPreview() {
        if (pendingTheme == null) {
            return currentTheme;
        }

        Theme previousTheme = currentTheme;
        currentTheme = pendingTheme;
        return previousTheme;
    }

    public void restorePreviewTheme(Theme previousTheme) {
        if (previousTheme != null) {
            currentTheme = previousTheme;
        }
    }

    public void applyPendingTheme() {
        if (pendingTheme != null) {
            currentTheme = pendingTheme;
            ThemeManager.getInstance().saveLastSelected(currentTheme);
            pendingTheme = null;
            pendingTransitionStarted = false;
        }
    }

    @Setter private boolean open;
    @Setter private float anim;

    private float openProgress;
    private float scroll;
    private float gridContentHeight;
    private float gridViewportHeight;
    private final AnimationUtil scrollAnimation = new AnimationUtil();
    private float settingsScroll;
    private float settingsContentHeight;
    private float settingsViewportHeight;
    private final AnimationUtil settingsScrollAnimation = new AnimationUtil();

    private ClickRegion closeRegion = new ClickRegion(-1f, -1f, 0f, 0f);
    private ClickRegion createRegion = new ClickRegion(-1f, -1f, 0f, 0f);
    private ClickRegion settingsRegion = new ClickRegion(-1f, -1f, 0f, 0f);
    private ClickRegion gridRegion = new ClickRegion(-1f, -1f, 0f, 0f);

    private final int defaultThemeCount = 13;

    private record ThemeBound(float x, float y, float width, float height, Theme.ElementColor elementColor) { }
    private record ClickRegion(float x, float y, float width, float height) { }

    public ThemeEditor() {
        setWidth(scaled(274f));
        setHeight(scaled(188f));

        WindowResizeEvent.getInstance().subscribe(new Listener<>(-1, event -> {
            setWidth(scaled(274f));
            setHeight(scaled(188f));
        }));
    }

    public void init() {
        refresh();
    }

    public void save(boolean last) {
        if (!last) {
            ThemeManager.getInstance().saveAll();
        } else if (currentTheme != null) {
            ThemeManager.getInstance().saveLastSelected(currentTheme);
        }
    }

    public void load() {
        refresh();
    }

    public boolean isInside(double mouseX, double mouseY) {
        if (!open || openProgress <= 0.02f) {
            return false;
        }

        boolean insideMain = MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight());
        boolean insideSettings = MouseUtil.isHovered(mouseX, mouseY, settingsRegion.x, settingsRegion.y, settingsRegion.width, settingsRegion.height);
        return insideMain || insideSettings;
    }

    public boolean isCustomTheme(Theme theme) {
        if (theme == null) {
            return false;
        }

        for (int i = 0; i < themeSelectables.size(); i++) {
            if (themeSelectables.get(i).getTheme() == theme) {
                return i >= defaultThemeCount;
            }
        }
        return false;
    }

    public boolean isRenaming(Theme theme) {
        return renamingTheme == theme;
    }

    public String getThemeDisplayName(Theme theme) {
        if (theme == renamingTheme) {
            String cursor = System.currentTimeMillis() % 1000L > 450L ? "_" : "";
            return renameText + cursor;
        }
        return theme.getName();
    }

    private void refresh() {
        String preferredName = currentTheme != null ? currentTheme.getName() : defaultTheme.getName();
        ThemeManager.getInstance().refresh();

        Theme restored = ThemeManager.getInstance().loadLastSelected();
        if (restored != null) {
            currentTheme = restored;
        } else {
            currentTheme = findTheme(preferredName);
        }

        if (currentTheme == null && !themeSelectables.isEmpty()) {
            currentTheme = themeSelectables.getFirst().getTheme();
        }
        if (currentTheme == null) {
            currentTheme = defaultTheme;
        }

        renamingTheme = null;
        renameText = "";
        scroll = 0f;
        settingsScroll = 0f;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        openProgress += ((open ? 1.0f : 0.0f) - openProgress) * 0.24f;
        openProgress = MathHelper.clamp(openProgress, 0.0f, 1.0f);
        if (openProgress <= 0.02f) {
            return;
        }

        scrollAnimation.update();
        scrollAnimation.run(scroll, 240, Easing.QUINT_OUT);
        settingsScrollAnimation.update();
        settingsScrollAnimation.run(settingsScroll, 220, Easing.QUINT_OUT);

        themeBounds.clear();
        createRegion = new ClickRegion(-1f, -1f, 0f, 0f);
        settingsRegion = new ClickRegion(-1f, -1f, 0f, 0f);

        MatrixStack matrixStack = context.getMatrices();
        int alpha = Math.max(5, (int) (255f * openProgress * anim));
        float contentProgress = MathHelper.clamp((openProgress - 0.12f) / 0.88f, 0f, 1f);
        int contentAlpha = Math.max(0, (int) (alpha * contentProgress));
        float contentOffset = scaled(2.3f) * (1f - contentProgress);

        float pad = scaled(8f);
        float gap = scaled(6f);
        float headerH = scaled(17f);
        float tileH = scaled(42f);
        float tileW = (getWidth() - pad * 2f - gap * 2f) / 3f;

        List<Object> entries = new ArrayList<>(themeSelectables);
        entries.add(CreateEntry.INSTANCE);

        int rows = Math.max(1, (int) Math.ceil(entries.size() / 3f));
        gridContentHeight = rows * tileH + Math.max(0, rows - 1) * gap;
        gridViewportHeight = scaled(146f);
        setHeight(headerH + pad + gridViewportHeight + pad);

        float minScroll = Math.min(0f, gridViewportHeight - gridContentHeight);
        scroll = MathHelper.clamp(scroll, minScroll, 0f);

        drawGlassSurface(matrixStack, getX(), getY(), getWidth(), getHeight(), scaled(7f),
                UIColors.card(Math.min(alpha, 120)), alpha, false);

        float titleSize = scaled(6.6f);
        Fonts.PS_BOLD.drawText(matrixStack, "Themes",
                getX() + pad,
                getY() + headerH / 2f - titleSize / 2f + scaled(0.2f) + contentOffset,
                titleSize, UIColors.textColor(contentAlpha));

        float closeSize = scaled(12f);
        float closeX = getX() + getWidth() - pad - closeSize;
        float closeY = getY() + headerH / 2f - closeSize / 2f;
        closeRegion = new ClickRegion(closeX, closeY, closeSize, closeSize);
        drawGlassSurface(matrixStack, closeX, closeY, closeSize, closeSize, scaled(4f),
                UIColors.cardSecondary(Math.min(alpha, 132)), alpha, true);
        Fonts.getICONS().drawCenteredText(matrixStack, "\u00D7",
                closeX + closeSize / 2f,
                closeY + closeSize / 2f - scaled(4.2f) + contentOffset * 0.2f,
                scaled(8.4f), UIColors.textColor(contentAlpha), 0.1f);

        float startY = getY() + headerH + pad;
        gridRegion = new ClickRegion(getX() + pad, startY, getWidth() - pad * 2f, gridViewportHeight);

        ScissorUtil.start(matrixStack, gridRegion.x, gridRegion.y, gridRegion.width, gridRegion.height);
        for (int i = 0; i < entries.size(); i++) {
            float tileX = getX() + pad + (i % 3) * (tileW + gap);
            float tileY = startY + (i / 3) * (tileH + gap) + (float) scrollAnimation.getValue();

            Object entry = entries.get(i);
            if (entry instanceof ThemeSelectable selectable) {
                selectable.setAlpha(openProgress * anim);
                selectable.setX(tileX);
                selectable.setY(tileY);
                selectable.setWidth(tileW);
                selectable.setHeight(tileH);
                selectable.render(context, mouseX, mouseY, delta);
            } else {
                renderCreateTile(matrixStack, tileX, tileY, tileW, tileH, alpha, contentAlpha, contentOffset);
                createRegion = new ClickRegion(tileX, tileY, tileW, tileH);
            }
        }
        ScissorUtil.stop(matrixStack);
        themeSelectables.removeIf(ThemeSelectable::isRemovalFinished);

        if (gridContentHeight > gridViewportHeight + scaled(1f)) {
            float trackHeight = Math.max(scaled(24f), gridViewportHeight * (gridViewportHeight / gridContentHeight));
            float scrollRange = Math.max(1f, gridContentHeight - gridViewportHeight);
            float progress = MathHelper.clamp(-((float) scrollAnimation.getValue()) / scrollRange, 0f, 1f);
            float trackY = startY + (gridViewportHeight - trackHeight) * progress;
            float trackX = getX() + getWidth() - scaled(3f);
            RenderUtil.RECT.draw(matrixStack, trackX, trackY, scaled(1.2f), trackHeight, scaled(0.6f), UIColors.stroke(Math.max(18, alpha / 2)));
        }

        renderSettingsPanel(context, mouseX, mouseY, delta, alpha, gap);
    }

    private void renderCreateTile(MatrixStack matrixStack, float x, float y, float width, float height, int alpha, int contentAlpha, float contentOffset) {
        float round = scaled(5f);
        float plusSize = scaled(10f);
        float textSize = scaled(5.4f);

        drawGlassSurface(matrixStack, x, y, width, height, round,
                UIColors.cardSecondary(Math.min(alpha, 116)), alpha, true);
        Fonts.PS_BOLD.drawCenteredText(matrixStack, "+", x + width / 2f, y + scaled(8f) + contentOffset * 0.2f, plusSize, UIColors.primary(contentAlpha));
        Fonts.PS_MEDIUM.drawCenteredText(matrixStack, "Новая тема", x + width / 2f, y + height - scaled(12f) + contentOffset, textSize, UIColors.textColor(contentAlpha));
    }

    private void renderSettingsPanel(DrawContext context, int mouseX, int mouseY, float delta, int alpha, float gap) {
        if (currentTheme == null) {
            return;
        }

        MatrixStack matrixStack = context.getMatrices();
        float contentProgress = MathHelper.clamp((openProgress - 0.12f) / 0.88f, 0f, 1f);
        int contentAlpha = Math.max(0, (int) (alpha * contentProgress));
        float panelWidth = scaled(138f);
        float panelHeight = scaled(176f);
        float panelX = getX() + getWidth() + gap * 1.5f + (1f - openProgress) * scaled(18f);
        float panelY = getY() + getHeight() / 2f - panelHeight / 2f;
        float pad = scaled(7f);
        float titleSize = scaled(6.2f);
        float rowGap = scaled(4f);
        float rowHeight = scaled(14f);

        float contentY = panelY + pad + titleSize + scaled(6f);
        float rowAreaHeight = panelHeight - (contentY - panelY) - pad;
        float totalHeight = pad + titleSize + scaled(6f);

        for (Theme.ElementColor elementColor : currentTheme.getElementColors()) {
            ColorComponent colorComponent = elementColor.getColorComponent();
            colorComponent.updateOpen();

            float height = rowHeight;
            float colorAnim = colorComponent.getValue();
            if (colorAnim > 0.0f) {
                height += colorComponent.getHeight() + rowGap;
            }

            totalHeight += height + rowGap;
        }
        totalHeight += pad - rowGap;
        settingsContentHeight = totalHeight - (contentY - panelY) - pad;
        settingsViewportHeight = Math.max(scaled(32f), rowAreaHeight);
        float minSettingsScroll = Math.min(0f, settingsViewportHeight - settingsContentHeight);
        settingsScroll = MathHelper.clamp(settingsScroll, minSettingsScroll, 0f);

        settingsRegion = new ClickRegion(panelX, panelY, panelWidth, panelHeight);

        drawGlassSurface(matrixStack, panelX, panelY, panelWidth, panelHeight, scaled(7f),
                UIColors.card(Math.min(alpha, 120)), alpha, false);

        Fonts.PS_BOLD.drawText(matrixStack, currentTheme.getName(),
                panelX + pad,
                panelY + pad + scaled(1.5f) * (1f - contentProgress),
                titleSize, UIColors.textColor(contentAlpha));

        float rowY = contentY + (float) settingsScrollAnimation.getValue();
        ScissorUtil.start(matrixStack, panelX + scaled(1f), contentY, panelWidth - scaled(2f), settingsViewportHeight);
        for (Theme.ElementColor elementColor : currentTheme.getElementColors()) {
            ColorComponent colorComponent = elementColor.getColorComponent();
            float colorAnim = colorComponent.getValue();
            float rowWidth = panelWidth - pad * 2f;
            float previewSize = scaled(9f);
            float previewX = panelX + panelWidth - pad - previewSize;
            float previewY = rowY + rowHeight / 2f - previewSize / 2f;

            drawGlassSurface(matrixStack, panelX + pad, rowY, rowWidth, rowHeight, scaled(4f),
                    UIColors.cardSecondary(Math.min(alpha, 112)), alpha, true);
            Fonts.PS_MEDIUM.drawText(matrixStack, translateElementName(elementColor.getName()),
                    panelX + pad + scaled(4f),
                    rowY + rowHeight / 2f - scaled(5.1f),
                    scaled(5.4f), UIColors.textColor(alpha));
            RenderUtil.RECT.draw(matrixStack, previewX, previewY, previewSize, previewSize, scaled(2.5f), elementColor.getColor());

            float height = rowHeight;
            if (colorAnim > 0.0f) {
                colorComponent.setX(panelX + pad);
                colorComponent.setY(rowY + rowHeight + scaled(2f));
                colorComponent.setWidth(rowWidth);
                colorComponent.setAlpha(openProgress * anim);
                colorComponent.render(context, mouseX, mouseY, delta);
                height += colorComponent.getHeight() + rowGap;
            }

            themeBounds.add(new ThemeBound(panelX + pad, rowY, rowWidth, height, elementColor));
            rowY += height + rowGap;
        }
        ScissorUtil.stop(matrixStack);

        if (settingsContentHeight > settingsViewportHeight + scaled(1f)) {
            float trackHeight = Math.max(scaled(22f), settingsViewportHeight * (settingsViewportHeight / settingsContentHeight));
            float scrollRange = Math.max(1f, settingsContentHeight - settingsViewportHeight);
            float progress = MathHelper.clamp(-((float) settingsScrollAnimation.getValue()) / scrollRange, 0f, 1f);
            float trackY = contentY + (settingsViewportHeight - trackHeight) * progress;
            float trackX = panelX + panelWidth - scaled(3f);
            RenderUtil.RECT.draw(matrixStack, trackX, trackY, scaled(1.2f), trackHeight, scaled(0.6f), UIColors.stroke(Math.max(18, alpha / 2)));
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!open) {
            return;
        }

        if (renamingTheme != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                cancelRename();
                return;
            }

            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !renameText.isEmpty()) {
                renameText = renameText.substring(0, renameText.length() - 1);
                return;
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                commitRename();
                return;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            open = false;
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!open) {
            return;
        }

        if (MouseUtil.isHovered(mouseX, mouseY, closeRegion.x, closeRegion.y, closeRegion.width, closeRegion.height)) {
            cancelRename();
            open = false;
            return;
        }

        if (MouseUtil.isHovered(mouseX, mouseY, createRegion.x, createRegion.y, createRegion.width, createRegion.height) && button == 0) {
            commitRename();
            createTheme();
            return;
        }

        for (ThemeBound themeBound : themeBounds) {
            ColorComponent colorComponent = themeBound.elementColor.getColorComponent();
            if (MouseUtil.isHovered(mouseX, mouseY, themeBound.x, themeBound.y, themeBound.width, scaled(14f))) {
                closeOtherPickers(themeBound.elementColor);
                colorComponent.toggleOpen();
                return;
            }

            if (MouseUtil.isHovered(mouseX, mouseY, themeBound.x, themeBound.y, themeBound.width, themeBound.height)) {
                colorComponent.mouseClicked(mouseX, mouseY, button);
                return;
            }
        }

        Theme hoveredTheme = null;
        ThemeSelectable hoveredSelectable = null;
        for (ThemeSelectable selectable : themeSelectables) {
            if (MouseUtil.isHovered(mouseX, mouseY, selectable.getX(), selectable.getY(), selectable.getWidth(), selectable.getHeight())) {
                hoveredTheme = selectable.getTheme();
                hoveredSelectable = selectable;
                break;
            }
        }

        if (hoveredSelectable != null) {
            if (hoveredSelectable.isDeleteHovered(mouseX, mouseY) && button == 0 && isCustomTheme(hoveredTheme)) {
                deleteTheme(hoveredTheme);
                return;
            }

            if (button == 1 && isCustomTheme(hoveredTheme)) {
                startRename(hoveredTheme);
                return;
            }

            if (button == 0) {
                if (renamingTheme != null && renamingTheme != hoveredTheme) {
                    commitRename();
                }
                if (currentTheme != hoveredTheme) {
                    pendingTheme = hoveredTheme;
                    pendingTransitionStarted = false;
                }
                return;
            }
        }

        if (renamingTheme != null && button == 0 && !MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            commitRename();
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (!open) {
            return;
        }

        for (ThemeBound themeBound : themeBounds) {
            themeBound.elementColor.getColorComponent().mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!open) {
            return;
        }

        if (MouseUtil.isHovered(mouseX, mouseY, settingsRegion.x, settingsRegion.y, settingsRegion.width, settingsRegion.height)) {
            float minSettingsScroll = Math.min(0f, settingsViewportHeight - settingsContentHeight);
            settingsScroll = MathHelper.clamp(settingsScroll + (float) (verticalAmount * scaled(18f)), minSettingsScroll, 0f);
            return;
        }

        if (!MouseUtil.isHovered(mouseX, mouseY, gridRegion.x, gridRegion.y, gridRegion.width, gridRegion.height)) {
            return;
        }

        float minScroll = Math.min(0f, gridViewportHeight - gridContentHeight);
        scroll = MathHelper.clamp(scroll + (float) (verticalAmount * scaled(18f)), minScroll, 0f);
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!open || renamingTheme == null || Character.isISOControl(chr)) {
            return false;
        }

        if (renameText.length() >= 18) {
            return true;
        }

        renameText += chr;
        return true;
    }

    private void closeOtherPickers(Theme.ElementColor except) {
        for (Theme.ElementColor elementColor : currentTheme.getElementColors()) {
            if (elementColor != except) {
                elementColor.getColorComponent().setOpen(false);
            }
        }
    }

    private void createTheme() {
        int index = 1;
        String name;
        do {
            name = "Тема " + index++;
        } while (findTheme(name) != null);

        Theme source = currentTheme != null ? currentTheme : defaultTheme;
        Theme newTheme = new Theme(name);
        newTheme.getElementColors().clear();
        for (Theme.ElementColor elementColor : source.getElementColors()) {
            newTheme.getElementColors().add(new Theme.ElementColor(elementColor.getName(), elementColor.getColor()));
        }

        themeSelectables.add(new ThemeSelectable(newTheme, false));
        currentTheme = newTheme;
        ThemeManager.getInstance().saveAll();
    }

    private void startRename(Theme theme) {
        if (!isCustomTheme(theme)) {
            return;
        }

        renamingTheme = theme;
        renameText = theme.getName();
    }

    private void cancelRename() {
        renamingTheme = null;
        renameText = "";
    }

    private void commitRename() {
        if (renamingTheme == null) {
            return;
        }

        String trimmed = ThemeManager.getInstance().safeFileName(renameText.trim());
        if (!trimmed.isEmpty()) {
            boolean exists = themeSelectables.stream()
                    .map(ThemeSelectable::getTheme)
                    .anyMatch(theme -> theme != renamingTheme && theme.getName().equalsIgnoreCase(trimmed));
            if (!exists) {
                renamingTheme.setName(trimmed);
                ThemeManager.getInstance().saveAll();
            }
        }

        cancelRename();
    }

    private void deleteTheme(Theme theme) {
        if (!isCustomTheme(theme)) {
            return;
        }

        for (ThemeSelectable selectable : themeSelectables) {
            if (selectable.getTheme() == theme) {
                selectable.startRemoving();
            }
        }
        if (currentTheme == theme) {
            currentTheme = themeSelectables.stream()
                    .map(ThemeSelectable::getTheme)
                    .filter(nextTheme -> nextTheme != theme)
                    .findFirst()
                    .orElse(defaultTheme);
        }
        cancelRename();
        ThemeManager.getInstance().saveAll();
    }

    private Theme findTheme(String name) {
        if (name == null) {
            return null;
        }

        return themeSelectables.stream()
                .map(ThemeSelectable::getTheme)
                .filter(theme -> theme.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private enum CreateEntry {
        INSTANCE
    }

    private void drawGlassSurface(MatrixStack matrixStack, float x, float y, float width, float height, float round,
                                  java.awt.Color surface, int alpha, boolean compact) {
        int blurAlpha = Math.max(0, Math.min(255, compact ? (int) (alpha * 0.62f) : (int) (alpha * 0.68f)));
        int overlayAlpha = Math.max(0, Math.min(255, compact ? (int) (alpha * 0.07f) : (int) (alpha * 0.09f)));
        int strokeAlpha = Math.max(0, Math.min(255, compact ? (int) (alpha * 0.15f) : (int) (alpha * 0.18f)));

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, width, height, round, UIColors.blur(blurAlpha), 0.045f);
        RenderUtil.RECT.draw(matrixStack, x, y, width, height, round, surface);
        RenderUtil.RECT.draw(matrixStack, x, y, width, height, round, UIColors.overlay(overlayAlpha));
        RenderUtil.RECT.draw(matrixStack, x, y, width, height, round, UIColors.stroke(strokeAlpha));
    }

    private String translateElementName(String name) {
        return name;
    }
}
