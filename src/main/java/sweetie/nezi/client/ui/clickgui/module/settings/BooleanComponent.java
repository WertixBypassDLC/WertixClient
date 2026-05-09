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
        toggleAnimation.run(setting.getValue() ? 1.0 : 0.0, 170, Easing.EXPO_OUT);

        MatrixStack matrixStack = context.getMatrices();
        float fontSize = getHeight() * 0.40f;
        int fullAlpha = (int) (getAlpha() * 255f);

        float anim = (float) toggleAnimation.getValue();

        float rowRound = getHeight() * 0.28f;
        float checkSize = getHeight() * 0.72f;
        float checkX = getX() + getWidth() - checkSize;
        float checkY = getY() + getHeight() / 2f - checkSize / 2f;
        float checkRound = checkSize * 0.24f;

        Color labelColor = ColorUtil.interpolate(UIColors.mutedText(fullAlpha), UIColors.textColor(fullAlpha), anim);
        Color rowColor = ColorUtil.interpolate(ColorUtil.setAlpha(color, Math.min(fullAlpha, inMenu ? 208 : 218)), ColorUtil.interpolate(UIColors.card(Math.min(fullAlpha, 232)), UIColors.primary(Math.min(fullAlpha, 196)), 0.14f), anim * 0.72f);
        Color rowStroke = ColorUtil.interpolate(UIColors.stroke(Math.min(fullAlpha, 118)), UIColors.primary(Math.min(fullAlpha, 180)), anim * 0.42f);
        RenderUtil.BLUR_RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeight(), rowRound, rowColor);
        RenderUtil.RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeight(), rowRound, rowStroke);
        Fonts.PS_MEDIUM.drawWrap(matrixStack, setting.getName(), getX() + scaled(4.2f), getY() + getHeight() / 2f - fontSize / 2f, getWidth() - checkSize - scaled(9f), fontSize, labelColor, scaled(16f), Duration.ofMillis(3000), Duration.ofMillis(500));

        Color boxColor = ColorUtil.interpolate(UIColors.panelSoft(Math.min(fullAlpha, 212)), ColorUtil.interpolate(UIColors.primary(Math.min(fullAlpha, 232)), UIColors.card(Math.min(fullAlpha, 218)), 0.24f), anim);
        Color boxStroke = ColorUtil.interpolate(UIColors.stroke(Math.min(fullAlpha, 126)), UIColors.primary(Math.min(fullAlpha, 206)), anim * 0.68f);
        Color checkColor = ColorUtil.interpolate(UIColors.inactiveTextColor((int) (fullAlpha * 0.45f)), UIColors.textColor(fullAlpha), anim);

        RenderUtil.BLUR_RECT.draw(matrixStack, checkX, checkY, checkSize, checkSize, checkRound, boxColor);
        RenderUtil.RECT.draw(matrixStack, checkX, checkY, checkSize, checkSize, checkRound, boxStroke);
        if (anim > 0.02f) {
            Fonts.PS_BOLD.drawCenteredText(matrixStack, "✓", checkX + checkSize / 2f, checkY + checkSize / 2f - scaled(2.55f), scaled(5.15f), ColorUtil.setAlpha(checkColor, (int) (fullAlpha * anim)));
        }
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