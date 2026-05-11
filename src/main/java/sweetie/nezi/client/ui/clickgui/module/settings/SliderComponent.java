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

        int fullAlpha = (int) (getAlpha() * 255f);

        float progress = (bigPenis - setting.getMin()) / (setting.getMax() - setting.getMin());
        progress = MathHelper.clamp(progress, 0f, 1f);
        float targetWidth = progress * getWidth();
        currentWidth = MathUtil.interpolate(currentWidth, targetWidth, 0.2f);

        Fonts.PS_MEDIUM.drawText(matrixStack, setting.getName(), getX() + scaled(2f), getY() + scaled(1.8f), fontSize, UIColors.textColor(fullAlpha));

        Color valueColor = ColorUtil.interpolate(UIColors.mutedText(fullAlpha), UIColors.textColor(fullAlpha), (float) dragAnimation.getValue());
        Fonts.PS_MEDIUM.drawText(matrixStack, valueText, getX() + getWidth() - valueWidth - scaled(2f), getY() + scaled(1.8f), fontSize, valueColor);

        float lineY = getY() + scaled(9.8f);
        float lineH = scaled(1.6f);
        float lineRound = lineH / 2f;
        RenderUtil.BLUR_RECT.draw(matrixStack, getX(), lineY, getWidth(), lineH, lineRound, UIColors.panelSoft(Math.min(fullAlpha, 120)));

        float me = getModuleEnabled();
        Color color1 = ColorUtil.interpolate(new Color(120, 120, 130, fullAlpha), UIColors.primary(fullAlpha), me);
        Color color2 = ColorUtil.interpolate(new Color(90, 90, 100, fullAlpha), UIColors.secondary(fullAlpha), me);
        if (currentWidth > lineRound * 2f) {
            RenderUtil.GRADIENT_RECT.draw(matrixStack, getX(), lineY, currentWidth, lineH, lineRound, color1, color2, color1, color2);
        }

        if (dragging) {
            float newValue = (mouseX - getX()) / getWidth();
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
        if (button == 0 && MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
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
    private float getDefaultHeight() { return 13f; }

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