package sweetie.nezi.client.ui.clickgui.module.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.utils.animation.AnimationUtil;
import sweetie.nezi.api.utils.animation.Easing;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.MathUtil;
import sweetie.nezi.api.utils.math.MouseUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.features.modules.render.InterfaceModule;
import sweetie.nezi.client.ui.clickgui.module.SettingComponent;

import java.awt.Color;

public class SliderComponent extends SettingComponent {
    private final SliderSetting setting;
    private boolean dragging;
    private float currentWidth;
    private float previewValue;

    private final AnimationUtil dragAnimation = new AnimationUtil();

    public SliderComponent(SliderSetting setting) {
        super(setting);
        this.setting = setting;
        this.previewValue = setting.getValue();
        updateHeight(getDefaultHeight());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateHeight(getDefaultHeight());

        dragAnimation.update();
        dragAnimation.run(dragging ? 1.0 : 0.0, 500, Easing.EXPO_OUT);

        MatrixStack matrixStack = context.getMatrices();

        float bigPenis = dragging ? previewValue : setting.getValue();
        float fontSize = fontSize();
        float shownValue = getDisplayValue(bigPenis);
        String valueText = String.format("%.2f", shownValue).replaceAll("0+$", "").replaceAll("\\.$", "");
        float valueWidth = Fonts.PS_MEDIUM.getWidth(valueText, fontSize);
        float piska = scaled(0.5f);

        int fullAlpha = (int) (getAlpha() * 255f);

        float progress = (bigPenis - setting.getMin()) / (setting.getMax() - setting.getMin()) * sliderWidth();
        currentWidth = MathHelper.clamp(MathUtil.interpolate(currentWidth, progress, 0.2f), 0f, sliderWidth());

        Fonts.PS_MEDIUM.drawText(matrixStack, setting.getName(), getX() + piska, getY() + piska, fontSize, UIColors.mutedText(fullAlpha));
        Fonts.PS_MEDIUM.drawText(matrixStack, valueText, getX() - piska + getWidth() - valueWidth, getY() + piska, fontSize, UIColors.textColor(fullAlpha));

        float sliderRound = sliderHeight() / 2f;
        float knobX = MathHelper.clamp(sliderX() + currentWidth - knobSize() / 2f, sliderX(), sliderX() + sliderWidth() - knobSize());

        Color knobColor = ColorUtil.setAlpha(ColorUtil.interpolate(UIColors.textColor(), UIColors.inactiveKnob(), 1f - (float) dragAnimation.getValue()), fullAlpha);

        float hui = (knobSize() - sliderHeight()) / 2f;

        Color color1 = UIColors.primary(fullAlpha);
        Color color2 = UIColors.secondary(fullAlpha);

        RenderUtil.BLUR_RECT.draw(matrixStack, sliderX(), sliderY(), sliderWidth(), sliderHeight(), sliderRound, UIColors.cardSecondary(Math.min(fullAlpha, 120)));
        RenderUtil.GRADIENT_RECT.draw(matrixStack, sliderX(), sliderY(), currentWidth, sliderHeight(), sliderRound, color1, color2, color1, color2);
        RenderUtil.BLUR_RECT.draw(matrixStack, knobX, sliderY() - hui, knobSize(), knobSize(), knobSize() / 2f, knobColor);

        setHeight(sliderHeight() + (sliderY() - getY()) + knobSize() / 2f);

        if (dragging) {
            float newValue = (mouseX - sliderX()) / sliderWidth();
            newValue = setting.getMin() + newValue * (setting.getMax() - setting.getMin());
            newValue = Math.round(newValue / setting.getStep()) * setting.getStep();
            previewValue = MathUtil.round(Math.max(setting.getMin(), Math.min(setting.getMax(), newValue)), setting.getStep());
        }

        if (dragging && !MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            setting.setValue(previewValue);
            dragging = false;
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        float knobAreaY = sliderY() - knobSize() / 2f;
        float knobAreaH = knobSize() + scaled(2f);
        if (button == 0 && MouseUtil.isHovered(mouseX, mouseY, sliderX(), knobAreaY, sliderWidth(), knobAreaH)) {
            dragging = true;
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            setting.setValue(previewValue);
        }
        dragging = false;
    }

    private float fontSize() { return scaled(15f) * 0.45f; }
    private float sliderWidth() { return getWidth(); }
    private float sliderHeight() { return scaled(4f); }
    private float knobSize() { return sliderHeight() * 1.85f; }
    private float sliderY() { return getY() + fontSize() + knobSize() / 2f; }
    private float sliderX() { return getX(); }
    private float getDefaultHeight() { return fontSize() + gap() + knobSize(); }

    private float getDisplayValue(float actualValue) {
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        if (setting == interfaceModule.scale) {
            return actualValue + 0.1f;
        }
        if (setting == interfaceModule.widgetScale) {
            return actualValue + 0.05f;
        }
        return actualValue;
    }

    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
}