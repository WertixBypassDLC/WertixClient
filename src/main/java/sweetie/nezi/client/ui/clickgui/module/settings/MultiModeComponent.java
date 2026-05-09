package sweetie.nezi.client.ui.clickgui.module.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import sweetie.nezi.api.module.setting.MultiModeSetting;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.animation.AnimationUtil;
import sweetie.nezi.api.utils.animation.Easing;
import sweetie.nezi.api.utils.math.MouseUtil;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.ScissorUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.ui.clickgui.module.ExpandableComponent;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiModeComponent extends ExpandableComponent.ExpandableSettingComponent {
    private final MultiModeSetting setting;

    private final List<Bound> bounds = new ArrayList<>();
    private final Map<String, AnimationUtil> modeAnimations = new HashMap<>();

    public MultiModeComponent(MultiModeSetting setting) {
        super(setting);
        this.setting = setting;
        updateHeight(getDefaultHeight());

        for (String mode : setting.getAllModes()) {
            modeAnimations.put(mode, new AnimationUtil());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrixStack = context.getMatrices();

        updateOpen();

        float fontSize = getDefaultHeight() * scaled(0.45f);
        float scd = scaled(getDefaultHeight());
        float zavoz = offset();
        float anim = getValue();

        String name = setting.getName();
        int selectedCount = setting.getSelectedCount();
        String countText = selectedCount > 0 ? selectedCount + "/" + setting.getAllModes().size() : "0";
        float countWidth = Fonts.PS_MEDIUM.getWidth(countText, fontSize);
        int fullAlpha = (int) (getAlpha() * 255f);
        float arrowSize = scaled(5.0f);
        String arrow = anim > 0.5f ? "⌃" : "⌄";
        float arrowWidth = Fonts.PS_BOLD.getWidth(arrow, arrowSize);
        float headerRound = getWidth() * 0.04f;

        RenderUtil.BLUR_RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeight(), headerRound, UIColors.cardSecondary(Math.min(fullAlpha, 196)));
        RenderUtil.RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeight(), headerRound, UIColors.stroke(Math.min(fullAlpha, 124)));
        Fonts.PS_BOLD.drawWrap(matrixStack, name, getX() + zavoz, getY() + scd / 2f - fontSize / 2f, getWidth() - zavoz * 3f - countWidth - arrowWidth, fontSize, UIColors.textColor(fullAlpha), scaled(16f), Duration.ofMillis(3000), Duration.ofMillis(500));

        ScissorUtil.start(matrixStack, getX(), getY(), getWidth(), getHeight());
        Fonts.PS_MEDIUM.drawText(matrixStack, countText, getX() + getWidth() - zavoz - countWidth - arrowWidth - scaled(4f), getY() + scd / 2f - fontSize / 2f, fontSize, UIColors.mutedText(fullAlpha));
        Fonts.PS_BOLD.drawText(matrixStack, arrow, getX() + getWidth() - zavoz - arrowWidth, getY() + scd / 2f - arrowSize / 2f - scaled(0.7f), arrowSize, UIColors.textColor((int) (fullAlpha * (0.75f + 0.25f * anim))));

        if (anim > 0.0) {
            bounds.clear();
            float rowHeight = scaled(12.8f);
            float rowGap = scaled(2.2f);
            float currentY = getY() + scd + scaled(1.3f) - scaled(6f) * (1f - anim);
            int listAlpha = (int) (getAlpha() * anim * 255f);

            for (String mode : setting.getAllModes()) {
                AnimationUtil modeAnim = modeAnimations.get(mode);

                modeAnim.update();
                modeAnim.run(setting.isSelected(mode) ? 1.0 : 0.0, 220, Easing.EXPO_OUT);

                float rowX = getX() + zavoz;
                float rowWidth = getWidth() - zavoz * 2f;
                bounds.add(new Bound(rowX, currentY, rowWidth, rowHeight, mode));

                float selected = (float) modeAnim.getValue();
                Color rowColor = ColorUtil.interpolate(UIColors.panelSoft(Math.min(listAlpha, 190)), ColorUtil.interpolate(UIColors.primary(Math.min(listAlpha, 220)), UIColors.card(Math.min(listAlpha, 230)), 0.20f), selected);
                Color rowStroke = ColorUtil.interpolate(UIColors.stroke(Math.min(listAlpha, 118)), UIColors.primary(Math.min(listAlpha, 198)), selected * 0.55f);
                Color textColor = ColorUtil.interpolate(UIColors.mutedText(listAlpha), UIColors.textColor(listAlpha), selected);
                float boxSize = rowHeight * 0.58f;
                float boxX = rowX + scaled(4f);
                float boxY = currentY + rowHeight / 2f - boxSize / 2f;
                RenderUtil.BLUR_RECT.draw(matrixStack, rowX, currentY, rowWidth, rowHeight, rowHeight * 0.24f, rowColor);
                RenderUtil.RECT.draw(matrixStack, rowX, currentY, rowWidth, rowHeight, rowHeight * 0.2f, rowStroke);
                RenderUtil.BLUR_RECT.draw(matrixStack, boxX, boxY, boxSize, boxSize, boxSize * 0.24f, ColorUtil.interpolate(UIColors.panelSoft(Math.min(listAlpha, 210)), UIColors.primary(Math.min(listAlpha, 216)), selected));
                RenderUtil.RECT.draw(matrixStack, boxX, boxY, boxSize, boxSize, boxSize * 0.24f, ColorUtil.interpolate(UIColors.stroke(Math.min(listAlpha, 120)), UIColors.primary(Math.min(listAlpha, 198)), selected * 0.6f));
                if (selected > 0.02f) {
                    Fonts.PS_BOLD.drawCenteredText(matrixStack, "✓", boxX + boxSize / 2f, boxY + boxSize / 2f - scaled(2.2f), scaled(4.75f), ColorUtil.setAlpha(UIColors.textColor(listAlpha), (int) (listAlpha * selected)));
                }
                Fonts.PS_MEDIUM.drawText(matrixStack, mode, rowX + scaled(11f), currentY + rowHeight / 2f - fontSize / 2f, fontSize, textColor);
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
                setting.toggle(bound.value);
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