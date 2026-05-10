package sweetie.nezi.client.ui.clickgui.module.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import sweetie.nezi.api.module.setting.ModeSetting;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.animation.AnimationUtil;
import sweetie.nezi.api.utils.animation.Easing;
import sweetie.nezi.api.utils.math.MouseUtil;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.ScissorUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.ui.clickgui.module.ExpandableComponent;

import java.awt.Color;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModeComponent extends ExpandableComponent.ExpandableSettingComponent {
    private final ModeSetting setting;

    private final List<Bound> bounds = new ArrayList<>();
    private final Map<String, AnimationUtil> modeAnimations = new HashMap<>();

    public ModeComponent(ModeSetting setting) {
        super(setting);
        this.setting = setting;
        updateHeight(getDefaultHeight());

        for (String mode : setting.getModes()) {
            modeAnimations.put(mode, new AnimationUtil());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrixStack = context.getMatrices();

        updateOpen();

        float fontSize = getDefaultHeight() * scaled(0.40f);
        float scd = scaled(getDefaultHeight());
        float zavoz = offset();
        float anim = getValue();

        String valueText = setting.getValue();
        String name = setting.getName();
        float valueWidth = Fonts.PS_MEDIUM.getWidth(valueText, fontSize);
        int fullAlpha = (int) (getAlpha() * 255f);
        float arrowSize = scaled(5.0f);
        String arrow = anim > 0.5f ? "⌃" : "⌄";
        float arrowWidth = Fonts.PS_BOLD.getWidth(arrow, arrowSize);
        float headerRound = getWidth() * 0.04f;
        float badgeWidth = valueWidth + arrowWidth + scaled(8f);

        Fonts.PS_BOLD.drawWrap(matrixStack, name, getX() + scaled(2f), getY() + scd / 2f - fontSize / 2f, getWidth() - zavoz * 3f - badgeWidth, fontSize, UIColors.textColor(fullAlpha), scaled(16f), Duration.ofMillis(3000), Duration.ofMillis(500));

        ScissorUtil.start(matrixStack, getX(), getY(), getWidth(), getHeight());
        float badgeX = getX() + getWidth() - badgeWidth - scaled(3.4f);
        float badgeY = getY() + scd / 2f - scaled(4.5f);
        float badgeHeight = scaled(9.0f);
        RenderUtil.BLUR_RECT.draw(matrixStack, badgeX, badgeY, badgeWidth, badgeHeight, badgeHeight * 0.32f, UIColors.panelSoft(Math.min(fullAlpha, 194)));
        RenderUtil.RECT.draw(matrixStack, badgeX, badgeY, badgeWidth, badgeHeight, badgeHeight * 0.32f, UIColors.stroke(Math.min(fullAlpha, 112)));
        Fonts.PS_MEDIUM.drawText(matrixStack, valueText, badgeX + scaled(3.3f), getY() + scd / 2f - fontSize / 2f, fontSize, UIColors.mutedText(fullAlpha));
        Fonts.PS_BOLD.drawText(matrixStack, arrow, badgeX + badgeWidth - scaled(3.4f) - arrowWidth, getY() + scd / 2f - arrowSize / 2f - scaled(0.7f), arrowSize, UIColors.textColor((int) (fullAlpha * (0.75f + 0.25f * anim))));

        if (anim > 0.0) {
            bounds.clear();
            float chipHeight = scaled(10.8f);
            float chipGapX = scaled(3.0f);
            float chipGapY = scaled(2.4f);
            float currentX = getX() + zavoz;
            float currentY = getY() + scd + scaled(1.6f) - scaled(6f) * (1f - anim);
            float maxX = getX() + getWidth() - zavoz;
            int listAlpha = (int) (getAlpha() * anim * 255f);

            for (String mode : setting.getModes()) {
                AnimationUtil modeAnim = modeAnimations.get(mode);

                modeAnim.update();
                modeAnim.run(setting.is(mode) ? 1.0 : 0.0, 220, Easing.EXPO_OUT);

                float textWidth = Fonts.PS_MEDIUM.getWidth(mode, fontSize);
                float chipWidth = Math.max(scaled(18f), textWidth + scaled(10f));
                if (currentX + chipWidth > maxX) {
                    currentX = getX() + zavoz;
                    currentY += chipHeight + chipGapY;
                }

                bounds.add(new Bound(currentX, currentY, chipWidth, chipHeight, mode));

                float selected = (float) modeAnim.getValue();
                Color rowColor = ColorUtil.interpolate(UIColors.panelSoft(Math.min(listAlpha, 190)), ColorUtil.interpolate(UIColors.primary(Math.min(listAlpha, 214)), UIColors.card(Math.min(listAlpha, 228)), 0.18f), selected);
                Color rowStroke = ColorUtil.interpolate(UIColors.stroke(Math.min(listAlpha, 118)), UIColors.primary(Math.min(listAlpha, 196)), 0.20f + selected * 0.48f);
                Color textColor = ColorUtil.interpolate(UIColors.mutedText(listAlpha), UIColors.textColor(listAlpha), selected);
                RenderUtil.BLUR_RECT.draw(matrixStack, currentX, currentY, chipWidth, chipHeight, chipHeight * 0.42f, rowColor);
                RenderUtil.RECT.draw(matrixStack, currentX, currentY, chipWidth, chipHeight, chipHeight * 0.42f, rowStroke);
                Fonts.PS_MEDIUM.drawCenteredText(matrixStack, mode, currentX + chipWidth / 2f, currentY + chipHeight / 2f - fontSize / 2f, fontSize, textColor);
                currentX += chipWidth + chipGapX;
            }

            float listHeight = currentY + chipHeight - (getY() + scd);
            setHeight(scd + listHeight * anim + scaled(1.6f));
        } else {
            updateHeight(getDefaultHeight());
        }

        ScissorUtil.stop(matrixStack);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), scaled(getDefaultHeight()))) {
            toggleOpen();
            return;
        }

        if (isNotOver()) return;
        for (Bound bound : bounds) {
            if (MouseUtil.isHovered(mouseX, mouseY, bound.x, bound.y, bound.width, bound.height)) {
                setting.setValue(bound.value);
            }
        }
    }

    private float getDefaultHeight() {
        return 15f;
    }

    private record Bound(float x, float y, float width, float height, String value) {}

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}