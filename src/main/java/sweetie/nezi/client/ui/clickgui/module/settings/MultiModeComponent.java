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
        float nameWidth = Fonts.PS_BOLD.getWidth(name, fontSize);

        float nameX = (getWidth() / 2f - nameWidth / 2f) * anim + zavoz * (1f - anim);
        int fullAlpha = (int) (getAlpha() * 255f);

        RenderUtil.BLUR_RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeight(), getWidth() * 0.04f, UIColors.card(Math.min(fullAlpha, 132)));
        Fonts.PS_BOLD.drawWrap(matrixStack, name, getX() + nameX, getY() + scd / 2f - fontSize / 2f, getWidth() - zavoz * 2f - countWidth * (1f - anim), fontSize, UIColors.textColor(fullAlpha), scaled(16f), Duration.ofMillis(3000), Duration.ofMillis(500));

        ScissorUtil.start(matrixStack, getX(), getY(), getWidth(), getHeight());
        Fonts.PS_MEDIUM.drawText(matrixStack, countText, getX() + getWidth() - zavoz - countWidth * (1f - anim), getY() + scd / 2f - fontSize / 2f, fontSize, UIColors.textColor((int) ((1f - anim) * getAlpha() * 255f)));

        if (anim > 0.0) {
            float bY = -scaled(2f) * (1f - anim);
            bounds.clear();
            float defX = getX() + zavoz;
            float currentX = defX;
            float currentY = getY() + scd + bY;
            float tileSize = fontSize * 0.9f;
            float tileHeight = tileSize * 1.8f;
            float tilePadding = gap();

            fullAlpha = (int) (getAlpha() * anim * 255f);

            RenderUtil.OTHER.scaleStart(matrixStack, getX() + getWidth() / 2f, getY() + getDefaultHeight() + getHeight() / 2f - bY, 0.95f + (0.05f * anim));

            for (String mode : setting.getAllModes()) {
                AnimationUtil modeAnim = modeAnimations.get(mode);

                modeAnim.update();
                modeAnim.run(setting.isSelected(mode) ? 1.0 : 0.0, 500, Easing.EXPO_OUT);

                float textWidth = Fonts.PS_MEDIUM.getWidth(mode, tileSize);
                float tileWidth = textWidth + tileSize;

                if (currentX + tileWidth + tilePadding > getX() + getWidth()) {
                    currentX = defX;
                    currentY += tileHeight + tilePadding;
                }

                bounds.add(new Bound(currentX, currentY, tileWidth, tileHeight, mode));

                float selected = (float) modeAnim.getValue();
                Color inactiveColor = UIColors.card(Math.min(fullAlpha, 176));
                Color activeColor = ColorUtil.interpolate(new Color(255, 255, 255, Math.min(fullAlpha, 146)), UIColors.cardSecondary(Math.min(fullAlpha, 220)), 0.22f);
                Color rectColor = ColorUtil.setAlpha(ColorUtil.interpolate(activeColor, inactiveColor, selected), fullAlpha);
                Color textColor = ColorUtil.interpolate(UIColors.textColor(fullAlpha), UIColors.mutedText(fullAlpha), selected);

                RenderUtil.BLUR_RECT.draw(matrixStack, currentX, currentY, tileWidth, tileHeight, tileHeight * 0.2f, rectColor);
                RenderUtil.RECT.draw(matrixStack, currentX, currentY, tileWidth, tileHeight, tileHeight * 0.2f,
                        selected > 0.02f ? UIColors.stroke((int) (fullAlpha * (0.6f + selected * 0.4f))) : UIColors.stroke((int) (fullAlpha * 0.45f)));
                Fonts.PS_MEDIUM.drawCenteredText(matrixStack, mode, currentX + tileWidth / 2f, currentY + tileHeight / 2f - tileSize / 2f, tileSize, textColor);

                currentX += tileWidth + tilePadding;
            }

            RenderUtil.OTHER.scaleStop(matrixStack);

            float total = (currentY - getY() + tileHeight) * anim;
            float impotentMan = Math.max(total, scd + tileHeight * anim);
            float jopa = gap() * (anim * 2f);
            setHeight(impotentMan + jopa);
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