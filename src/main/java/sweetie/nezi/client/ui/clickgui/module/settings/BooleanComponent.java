package sweetie.nezi.client.ui.clickgui.module.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.animation.AnimationUtil;
import sweetie.nezi.api.utils.animation.Easing;
import sweetie.nezi.api.utils.math.MouseUtil;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.ui.clickgui.module.SettingComponent;

import java.awt.Color;
import java.time.Duration;

public class BooleanComponent extends SettingComponent {
    private final BooleanSetting setting;

    private final AnimationUtil toggleAnimation = new AnimationUtil();
    private final boolean inMenu;
    private Color color;

    public BooleanComponent(BooleanSetting setting) {
        this(setting, false);
    }

    public BooleanComponent(BooleanSetting setting, boolean inMenu) {
        super(setting);
        this.setting = setting;
        updateHeight(15f);
        toggleAnimation.setValue(setting.getValue() ? 1.0 : 0.0);
        this.inMenu = inMenu;
        this.color = inMenu ? UIColors.widgetBlur() : UIColors.backgroundBlur();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateHeight(15f);

        this.color = inMenu ? UIColors.panelSoft() : UIColors.cardSecondary();

        toggleAnimation.update();
        toggleAnimation.run(setting.getValue() ? 1.0 : 0.0, 100, Easing.SINE_OUT);

        MatrixStack matrixStack = context.getMatrices();
        float fontSize = getHeight() * 0.40f;
        int fullAlpha = (int) (getAlpha() * 255f);

        float anim = (float) toggleAnimation.getValue();

        float checkHeight = getHeight() * 0.7f;
        float checkWidth = checkHeight * 1.95f;
        float checkX = getX() + getWidth() - checkWidth;
        float checkY = getY() + getHeight() / 2f - checkHeight / 2f;
        float checkRound = checkHeight / 2f;

        float knobSize = checkHeight - scaled(3f);
        float knobInset = (checkHeight - knobSize) / 2f;
        float knobY = checkY + knobInset;
        float knobX = checkX + knobInset + ((checkWidth - knobSize - knobInset * 2f) * anim);
        float knobRound = knobSize / 2f;

        Color labelColor = ColorUtil.interpolate(UIColors.textColor(fullAlpha), UIColors.mutedText(fullAlpha), anim);
        Fonts.PS_MEDIUM.drawWrap(matrixStack, setting.getName(), getX(), getY() + getHeight() / 2f - fontSize / 2f, getWidth() - checkWidth - scaled(6f), fontSize, labelColor, scaled(16f), Duration.ofMillis(3000), Duration.ofMillis(500));

        Color inactiveTrack = ColorUtil.setAlpha(color, fullAlpha);
        Color activeTrack = ColorUtil.interpolate(new Color(255, 255, 255, fullAlpha), UIColors.cardSecondary(fullAlpha), 0.24f);
        Color trackColor = ColorUtil.interpolate(activeTrack, inactiveTrack, anim);
        Color knobColor = ColorUtil.interpolate(UIColors.textColor(fullAlpha), UIColors.inactiveKnob(fullAlpha), anim);

        RenderUtil.BLUR_RECT.draw(matrixStack, checkX, checkY, checkWidth, checkHeight, checkRound, trackColor);
        RenderUtil.BLUR_RECT.draw(matrixStack, knobX, knobY, knobSize, knobSize, knobRound, knobColor);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            setting.toggle();
        }
    }

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}