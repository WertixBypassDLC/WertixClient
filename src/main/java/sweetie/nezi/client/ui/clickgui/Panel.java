package sweetie.nezi.client.ui.clickgui;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.other.WindowResizeEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleManager;
import sweetie.nezi.api.utils.animation.AnimationUtil;
import sweetie.nezi.api.utils.animation.Easing;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.MouseUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.ScissorUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.ui.UIComponent;
import sweetie.nezi.client.ui.clickgui.module.ModuleComponent;
import sweetie.nezi.client.ui.clickgui.module.SettingComponent;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Panel extends UIComponent {
    private final Category category;
    private final List<ModuleComponent> moduleComponents = new ArrayList<>();

    @Setter private int categoryIndex;

    private double scroll = 0f;
    private final AnimationUtil scrollAnimation = new AnimationUtil();
    private float contentHeight;
    private float prevX;
    private float tiltAngle;
    private float dragJitterX;
    private float dragJitterY;
    private float releasePulse;
    private boolean dragging;
    private boolean wasDragging;

    public Panel(Category category) {
        this.category = category;

        for (Module module : ModuleManager.getInstance().getModules()) {
            if (module.getCategory() == category && !"Click GUI".equalsIgnoreCase(module.getName())) {
                ModuleComponent moduleComponent = new ModuleComponent(module);
                moduleComponent.setRound(getRound() * 2f);
                moduleComponents.add(moduleComponent);
            }
        }

        moduleComponents.sort((first, second) -> first.getModule().getName().compareToIgnoreCase(second.getModule().getName()));

        if (!moduleComponents.isEmpty()) {
            moduleComponents.getLast().setLast(true);
        }

        int index = categoryIndex;
        for (ModuleComponent module : moduleComponents) {
            module.setIndex(index);
            index += 45;
        }

        WindowResizeEvent.getInstance().subscribe(new Listener<>(-1, event -> scroll = 0f));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateThings();
        renderThings(context, mouseX, mouseY, delta);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        moduleComponents.forEach(module -> module.keyPressed(keyCode, scanCode, modifiers));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!isInsideBody(mouseX, mouseY)) {
            return;
        }

        for (ModuleComponent module : moduleComponents) {
            if (!module.matchesSearch(ScreenClickGUI.getInstance().getSearchQuery())) {
                continue;
            }
            if (!MouseUtil.isHovered(mouseX, mouseY, module.getX(), module.getY(), module.getWidth(), module.getHeight())) {
                continue;
            }
            module.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (!isInsideBody(mouseX, mouseY)) {
            return;
        }

        for (ModuleComponent module : moduleComponents) {
            if (!module.matchesSearch(ScreenClickGUI.getInstance().getSearchQuery())) {
                continue;
            }
            if (!MouseUtil.isHovered(mouseX, mouseY, module.getX(), module.getY(), module.getWidth(), module.getHeight())) {
                continue;
            }
            module.mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isInsideBody(mouseX, mouseY)) {
            return;
        }

        float contentAreaHeight = getBodyContentHeight();
        float minScroll = Math.min(0f, contentAreaHeight - contentHeight);
        scroll = MathHelper.clamp((float) scroll + (float) (verticalAmount * scaled(16f)), minScroll, 0f);
    }

    private void updateThings() {
        scrollAnimation.update();
        scrollAnimation.run(scroll, 300, Easing.QUINT_OUT);
        updateDragEffects();

        if (getWidth() <= 0f) {
            setWidth(scaled(106f));
        }
        if (getHeight() <= 0f) {
            setHeight(scaled(252f));
        }

        moduleComponents.forEach(module -> module.setRound(getRound() * 2f));
    }

    private void updateDragEffects() {
        float dx = getX() - prevX;
        prevX = getX();

        if (!dragging && wasDragging) {
            releasePulse = 1.0f;
        }
        wasDragging = dragging;

        if (!dragging) {
            tiltAngle += (0f - tiltAngle) * 0.24f;
            dragJitterX += (0f - dragJitterX) * 0.25f;
            dragJitterY += (0f - dragJitterY) * 0.25f;
        } else {
            float filteredDx = Math.abs(dx) < 0.25f ? 0f : dx;
            float targetTilt = Math.max(-14f, Math.min(14f, filteredDx * 1.8f));
            tiltAngle += (targetTilt - tiltAngle) * 0.20f;
            float time = (System.currentTimeMillis() % 100000L) / 1000f;
            dragJitterX += (((float) Math.sin(time * 18f + category.ordinal() * 0.9f) * scaled(0.9f)) - dragJitterX) * 0.32f;
            dragJitterY += (((float) Math.cos(time * 13f + category.ordinal() * 0.7f) * scaled(0.55f)) - dragJitterY) * 0.32f;
        }

        if (Math.abs(tiltAngle) < 0.01f) tiltAngle = 0f;
        if (Math.abs(dragJitterX) < 0.01f) dragJitterX = 0f;
        if (Math.abs(dragJitterY) < 0.01f) dragJitterY = 0f;
        releasePulse = Math.max(0f, releasePulse + (0f - releasePulse) * 0.13f);
        if (releasePulse < 0.01f) releasePulse = 0f;
    }

    private void renderThings(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrixStack = context.getMatrices();
        int fullAlpha = (int) (getAlpha() * 255f);

        float headerHeight = getHeaderHeight();
        float headerGap = getHeaderGap();
        float bodyY = getY() + headerHeight + headerGap;
        float bodyHeight = getHeight() - headerHeight - headerGap;
        float bodyPad = scaled(7f);
        float contentTop = bodyY + bodyPad;
        float contentAreaHeight = Math.max(0f, bodyHeight - bodyPad * 2f);

        calcModules(bodyPad, contentTop, contentAreaHeight);

        float minScroll = Math.min(0f, contentAreaHeight - contentHeight);
        scroll = MathHelper.clamp((float) scroll, minScroll, 0f);

        float headerInset = scaled(1f);
        float iconBox = headerHeight;
        float headerCardY = getY();
        float iconX = getX();
        float iconY = getY();
        float titleSize = scaled(5.9f);
        float iconSize = scaled(8.1f);
        float headerRound = Math.max(scaled(4.8f), getRound() - scaled(0.7f));
        float headerContentProgress = MathHelper.clamp((getAlpha() - 0.16f) / 0.84f, 0f, 1f);
        int headerContentAlpha = Math.max(0, (int) (fullAlpha * headerContentProgress));
        float headerContentOffset = scaled(2f) * (1f - headerContentProgress);

        drawGlassBlock(matrixStack, getX(), headerCardY, getWidth(), headerHeight, headerRound,
                UIColors.card(Math.min(fullAlpha, 122)), fullAlpha, false);

        drawGlassBlock(matrixStack, iconX, iconY, iconBox, iconBox, scaled(5f),
                UIColors.panel(Math.min(fullAlpha, 130)), fullAlpha, true);

        String icon = getCategoryLetter();
        float iconWidth = Fonts.PS_BOLD.getWidth(icon, iconSize);
        Fonts.PS_BOLD.drawText(matrixStack, icon,
                iconX + iconBox / 2f - iconWidth / 2f,
                iconY + iconBox / 2f - iconSize / 2f + scaled(0.2f) + headerContentOffset * 0.25f,
                iconSize, UIColors.themeFlow(category.ordinal() * 77, headerContentAlpha));
        Fonts.PS_BOLD.drawText(matrixStack, category.name(),
                getX() + iconBox + scaled(6f),
                iconY + iconBox / 2f - titleSize / 2f + scaled(0.2f) + headerContentOffset,
                titleSize,
                UIColors.textColor(headerContentAlpha));

        drawGlassBlock(matrixStack, getX(), bodyY, getWidth(), bodyHeight, getRound(),
                UIColors.panelSoft(Math.min(fullAlpha, 114)), fullAlpha, false);

        ScissorUtil.start(matrixStack, getX(), bodyY, getWidth(), bodyHeight);
        for (ModuleComponent module : moduleComponents) {
            if (!module.matchesSearch(ScreenClickGUI.getInstance().getSearchQuery())) {
                continue;
            }

            module.setAlpha(getAlpha());
            module.render(context, mouseX, mouseY, delta);
        }
        ScissorUtil.stop(matrixStack);
        RenderUtil.RECT.draw(matrixStack, getX(), bodyY, getWidth(), bodyHeight, getRound(), UIColors.stroke(Math.min(255, (int) (fullAlpha * 0.22f))));
    }

    private void calcModules(float bodyPad, float contentTop, float contentAreaHeight) {
        String query = ScreenClickGUI.getInstance().getSearchQuery();
        float moduleGap = scaled(2f);
        float moduleY = (float) scrollAnimation.getValue();
        float measuredY = 0f;
        float moduleWidth = getWidth() - bodyPad * 2f;
        float clipBottom = contentTop + contentAreaHeight;
        ModuleComponent firstMatching = null;
        ModuleComponent lastMatching = null;

        for (ModuleComponent module : moduleComponents) {
            module.setTopRounded(false);
            module.setBottomRounded(false);
            if (!module.matchesSearch(query)) {
                module.setHeight(0f);
                continue;
            }

            if (firstMatching == null) {
                firstMatching = module;
            }
            lastMatching = module;

            float openAnim = module.getAnim();
            float moduleHeight = module.getDefaultHeight();
            if (openAnim > 0f) {
                float settingOffset = 0f;
                for (SettingComponent setting : module.getSettings()) {
                    float visibleAnim = (float) setting.getVisibleAnimation().getValue();
                    if (visibleAnim > 0.0f) {
                        settingOffset += (setting.getHeight() + gap()) * visibleAnim;
                    }
                }
                moduleHeight += (settingOffset + gap()) * openAnim;
            }

            module.setHeight(moduleHeight);
            module.setWidth(moduleWidth);
            module.setRound(getRound() / 2f);
            module.setX(getX() + bodyPad);
            module.setY(contentTop + moduleY);

            moduleY += moduleHeight + moduleGap;
            measuredY += moduleHeight + moduleGap;
        }

        if (firstMatching != null) {
            firstMatching.setTopRounded(true);
            lastMatching.setBottomRounded(true);
        }

        contentHeight = Math.max(0f, measuredY - moduleGap);
    }

    public float getHeaderHeight() {
        return scaled(17f);
    }

    public float getHeaderGap() {
        return scaled(6f);
    }

    public float getRound() {
        return scaled(7f);
    }

    public float getBodyContentHeight() {
        return getHeight() - getHeaderHeight() - getHeaderGap() - scaled(14f);
    }

    public boolean isInsideHeader(double mouseX, double mouseY) {
        return MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeaderHeight());
    }

    private boolean isInsideBody(double mouseX, double mouseY) {
        float bodyY = getY() + getHeaderHeight() + getHeaderGap();
        return MouseUtil.isHovered(mouseX, mouseY, getX(), bodyY, getWidth(), getHeight() - getHeaderHeight() - getHeaderGap());
    }

    private String getCategoryLetter() {
        return switch (category) {
            case COMBAT -> "C";
            case MOVEMENT -> "M";
            case RENDER -> "V";
            case PLAYER -> "P";
            case OTHER -> "O";
        };
    }

    public boolean capturesInput() {
        for (ModuleComponent moduleComponent : moduleComponents) {
            if (moduleComponent.isCapturingInput()) {
                return true;
            }
        }
        return false;
    }

    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    public void renderReleasePulse(MatrixStack ms) {
        if (releasePulse <= 0.01f) {
            return;
        }

        float expansion = scaled(3f) + scaled(11f) * (1f - releasePulse);
        int alpha = Math.max(0, Math.min(255, (int) (releasePulse * 125f * getAlpha())));
        RenderUtil.STROKE(ms,
                getX() - expansion,
                getY() - expansion,
                getWidth() + expansion * 2f,
                getHeight() + expansion * 2f,
                getRound() + expansion * 0.7f,
                alpha);
    }

    private void drawGlassBlock(MatrixStack matrixStack, float x, float y, float width, float height, float round,
                                java.awt.Color surface, int alpha, boolean compact) {
        int blurAlpha = Math.max(0, Math.min(255, compact ? (int) (alpha * 0.62f) : (int) (alpha * 0.68f)));

        java.awt.Color hudBg = new java.awt.Color(23, 23, 34, (int) (78 * (alpha / 255f)));
        java.awt.Color hudStroke = new java.awt.Color(65, 65, 65, (int) (138 * (alpha / 255f)));

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, width, height, round, UIColors.blur(blurAlpha), 0.045f);
        RenderUtil.RECT.draw(matrixStack, x - scaled(0.7f), y - scaled(0.7f),
                width + scaled(1.4f), height + scaled(1.4f), round + scaled(0.7f), hudStroke);
        RenderUtil.RECT.draw(matrixStack, x, y, width, height, round, hudBg);
    }
}