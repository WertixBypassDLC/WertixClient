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

        RenderUtil.BLUR_RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeight(), headerRound, UIColors.cardSecondary(Math.min(fullAlpha, 196)));
        RenderUtil.RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeight(), headerRound, UIColors.stroke(Math.min(fullAlpha, 124)));
        Fonts.PS_BOLD.drawWrap(matrixStack, name, getX() + zavoz, getY() + scd / 2f - fontSize / 2f, getWidth() - zavoz * 3f - valueWidth - arrowWidth, fontSize, UIColors.textColor(fullAlpha), scaled(16f), Duration.ofMillis(3000), Duration.ofMillis(500));

        ScissorUtil.start(matrixStack, getX(), getY(), getWidth(), getHeight());
        Fonts.PS_MEDIUM.drawText(matrixStack, valueText, getX() + getWidth() - zavoz - valueWidth - arrowWidth - scaled(4f), getY() + scd / 2f - fontSize / 2f, fontSize, UIColors.mutedText(fullAlpha));
        Fonts.PS_BOLD.drawText(matrixStack, arrow, getX() + getWidth() - zavoz - arrowWidth, getY() + scd / 2f - arrowSize / 2f - scaled(0.7f), arrowSize, UIColors.textColor((int) (fullAlpha * (0.75f + 0.25f * anim))));

        if (anim > 0.0) {
            bounds.clear();
            float rowHeight = scaled(12.8f);
            float rowGap = scaled(2.2f);
            float currentY = getY() + scd + scaled(1.3f) - scaled(6f) * (1f - anim);
            int listAlpha = (int) (getAlpha() * anim * 255f);

            for (String mode : setting.getModes()) {
                AnimationUtil modeAnim = modeAnimations.get(mode);

                modeAnim.update();
                modeAnim.run(setting.is(mode) ? 1.0 : 0.0, 220, Easing.EXPO_OUT);

                float rowX = getX() + zavoz;
                float rowWidth = getWidth() - zavoz * 2f;
                bounds.add(new Bound(rowX, currentY, rowWidth, rowHeight, mode));

                float selected = (float) modeAnim.getValue();
                Color rowColor = ColorUtil.interpolate(UIColors.panelSoft(Math.min(listAlpha, 190)), ColorUtil.interpolate(UIColors.primary(Math.min(listAlpha, 220)), UIColors.card(Math.min(listAlpha, 230)), 0.20f), selected);
                Color rowStroke = ColorUtil.interpolate(UIColors.stroke(Math.min(listAlpha, 118)), UIColors.primary(Math.min(listAlpha, 198)), selected * 0.55f);
                Color textColor = ColorUtil.interpolate(UIColors.mutedText(listAlpha), UIColors.textColor(listAlpha), selected);
                String prefix = selected > 0.5f ? "✓" : "•";
                float prefixSize = scaled(4.8f);
                RenderUtil.BLUR_RECT.draw(matrixStack, rowX, currentY, rowWidth, rowHeight, rowHeight * 0.24f, rowColor);
                RenderUtil.RECT.draw(matrixStack, rowX, currentY, rowWidth, rowHeight, rowHeight * 0.2f, rowStroke);
                Fonts.PS_BOLD.drawText(matrixStack, prefix, rowX + scaled(4f), currentY + rowHeight / 2f - prefixSize / 2f - scaled(0.75f), prefixSize, textColor);
                Fonts.PS_MEDIUM.drawText(matrixStack, mode, rowX + scaled(10f), currentY + rowHeight / 2f - fontSize / 2f, fontSize, textColor);
                currentY += rowHeight + rowGap;
            }

            float listHeight = currentY - (getY() + scd);
            setHeight(scd + listHeight * anim + scaled(1.2f));
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