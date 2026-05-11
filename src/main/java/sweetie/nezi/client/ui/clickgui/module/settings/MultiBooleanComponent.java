package sweetie.nezi.client.ui.clickgui.module.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.MultiBooleanSetting;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.animation.AnimationUtil;
import sweetie.nezi.api.utils.animation.Easing;
import sweetie.nezi.api.utils.math.MouseUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.ScissorUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.ui.clickgui.module.ExpandableComponent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class MultiBooleanComponent extends ExpandableComponent.ExpandableSettingComponent {
    private final MultiBooleanSetting setting;

    private final AnimationUtil settingsAnimation = new AnimationUtil();

    private final List<BooleanComponent> booleans = new ArrayList<>();

    public MultiBooleanComponent(MultiBooleanSetting setting) {
        super(setting);
        this.setting = setting;
        updateHeight(getDefaultHeight());

        for (BooleanSetting value : setting.getValue()) {
            booleans.add(new BooleanComponent(value, true));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrixStack = context.getMatrices();

        updateOpen();
        settingsAnimation.update();

        if (isOpen()) {
            getAnim().run(1.0, 300, Easing.EXPO_OUT);
            settingsAnimation.run(getValue() >= 0.9 ? 1.0 : 0.0, 300, Easing.EXPO_OUT);
        } else {
            settingsAnimation.run(0.0, 200, Easing.EXPO_OUT);
            if (settingsAnimation.getValue() <= 0.1) {
                getAnim().run(0.0, 300, Easing.EXPO_OUT);
            }
        }

        float openAnim = getValue();
        float settingsAnim = (float) settingsAnimation.getValue();

        float fontSize = getDefaultHeight() * scaled(0.40f);
        float scd = scaled(getDefaultHeight());
        int fullAlpha = (int) (getAlpha() * 255f);
        int selectedCount = setting.getList().size();
        int totalCount = setting.getValue().size();
        String countText = selectedCount + "/" + totalCount;
        float countWidth = Fonts.PS_MEDIUM.getWidth(countText, fontSize);
        float arrowSize = scaled(5.0f);
        String arrow = settingsAnim > 0.5f ? "⌃" : "⌄";
        float arrowWidth = Fonts.PS_BOLD.getWidth(arrow, arrowSize);
        float headerRound = getWidth() * 0.04f;
        float badgeWidth = countWidth + arrowWidth + scaled(8f);

        Fonts.PS_MEDIUM.drawWrap(matrixStack, setting.getName(), getX() + scaled(2f), getY() + scd / 2f - fontSize / 2f, getWidth() - offset() * 3f - badgeWidth, fontSize, UIColors.textColor(fullAlpha), scaled(16f), Duration.ofMillis(3000), Duration.ofMillis(500));
        float badgeX = getX() + getWidth() - badgeWidth - scaled(2f);
        Fonts.PS_MEDIUM.drawText(matrixStack, countText, badgeX + scaled(1.5f), getY() + scd / 2f - fontSize / 2f, fontSize, UIColors.mutedText(fullAlpha));
        Fonts.PS_BOLD.drawText(matrixStack, arrow, badgeX + badgeWidth - scaled(3.4f) - arrowWidth, getY() + scd / 2f - arrowSize / 2f - scaled(0.7f), arrowSize, UIColors.textColor((int) (fullAlpha * (0.75f + settingsAnim * 0.25f))));

        if (openAnim > 0.0) {
            float currentY = getY() + scd + scaled(1.4f) - scaled(6f) * (1f - settingsAnim);
            float visibleHeight = 0.0f;

            for (BooleanComponent component : booleans) {
                AnimationUtil anim = component.getVisibleAnimation();
                anim.update();
                anim.run(component.getSetting().isVisible() ? 1.0 : 0.0, 120, Easing.SINE_OUT);
                float itemAnim = (float) anim.getValue();
                if (itemAnim <= 0.0f) {
                    continue;
                }
                component.setX(getX() + offset());
                component.setY(currentY);
                component.setWidth(getWidth() - offset() * 2f);
                currentY += (component.getHeight() + scaled(2.0f)) * itemAnim;
                visibleHeight = currentY - (getY() + scd);
            }

            setHeight(scd + visibleHeight * settingsAnim + scaled(1.2f));

            if (settingsAnim > 0.0) {
                ScissorUtil.start(matrixStack, getX(), getY(), getWidth(), getHeight());
                for (BooleanComponent component : booleans) {
                    component.setAlpha((float) (component.getVisibleAnimation().getValue() * getAlpha() * settingsAnim));
                    component.render(context, mouseX, mouseY, delta);
                }
                ScissorUtil.stop(matrixStack);
            }
        } else {
            updateHeight(getDefaultHeight());
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), scaled(getDefaultHeight()))) {
            toggleOpen();
            return;
        }

        if (isNotOver()) return;

        for (BooleanComponent aBoolean : booleans) {
            if (aBoolean.getVisibleAnimation().getValue() < 0.8) continue;
            aBoolean.mouseClicked(mouseX, mouseY, button);
        }
    }

    private float getDefaultHeight() {
        return 16f;
    }

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}