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
        updateHeight(13f);

        toggleAnimation.update();
        toggleAnimation.run(setting.getValue() ? 1.0 : 0.0, 170, Easing.EXPO_OUT);

        MatrixStack matrixStack = context.getMatrices();
        float fontSize = getHeight() * 0.44f;
        int fullAlpha = (int) (getAlpha() * 255f);

        float anim = (float) toggleAnimation.getValue();

        float checkSize = scaled(7f);
        float checkX = getX() + scaled(2.5f);
        float checkY = getY() + getHeight() / 2f - checkSize / 2f;
        float checkRound = scaled(1.5f);

        float me = getModuleEnabled();
        Color activeColor = ColorUtil.interpolate(new Color(130, 130, 140, Math.min(fullAlpha, 210)), UIColors.primary(Math.min(fullAlpha, 210)), me);
        Color checkBg = ColorUtil.interpolate(UIColors.panelSoft(Math.min(fullAlpha, 130)), activeColor, anim);
        Color activeStroke = ColorUtil.interpolate(new Color(110, 110, 120, Math.min(fullAlpha, 200)), UIColors.primary(Math.min(fullAlpha, 200)), me);
        Color checkStroke = ColorUtil.interpolate(UIColors.stroke(Math.min(fullAlpha, 140)), activeStroke, anim);

        RenderUtil.BLUR_RECT.draw(matrixStack, checkX, checkY, checkSize, checkSize, checkRound, checkBg);
        RenderUtil.RECT.draw(matrixStack, checkX, checkY, checkSize, checkSize, checkRound, checkStroke);

        if (anim > 0.1f) {
            float checkmarkSize = checkSize * 0.72f;
            Fonts.PS_BOLD.drawCenteredText(matrixStack, "\u2713", checkX + checkSize / 2f, checkY + checkSize / 2f - checkmarkSize / 2f, checkmarkSize, UIColors.textColor((int) (fullAlpha * anim)));
        }

        Color labelColor = ColorUtil.interpolate(UIColors.mutedText(fullAlpha), UIColors.textColor(fullAlpha), anim);
        Fonts.PS_MEDIUM.drawWrap(matrixStack, setting.getName(), checkX + checkSize + scaled(3.5f), getY() + getHeight() / 2f - fontSize / 2f, getWidth() - checkSize - scaled(12f), fontSize, labelColor, scaled(16f), Duration.ofMillis(3000), Duration.ofMillis(500));
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