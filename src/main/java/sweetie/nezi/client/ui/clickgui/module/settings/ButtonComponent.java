package sweetie.nezi.client.ui.clickgui.module.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import sweetie.nezi.api.module.setting.RunSetting;
import sweetie.nezi.api.utils.animation.AnimationUtil;
import sweetie.nezi.api.utils.animation.Easing;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.MouseUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.ui.clickgui.module.SettingComponent;

import java.awt.Color;
import java.time.Duration;

public class ButtonComponent extends SettingComponent {
    private final RunSetting setting;

    private final AnimationUtil hoverAnimation = new AnimationUtil();

    public ButtonComponent(RunSetting setting) {
        super(setting);
        this.setting = setting;
        updateHeight(getDefaultHeight());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateHeight(getDefaultHeight());

        MatrixStack matrixStack = context.getMatrices();
        int fullAlpha = (int) (getAlpha() * 255f);

        hoverAnimation.update();
        hoverAnimation.run(hovered(mouseX, mouseY) ? 1.0 : 0.0, 500, Easing.EXPO_OUT);

        Color buttonColor1 = ColorUtil.interpolate(UIColors.widgetBlur(Math.min(fullAlpha, 212)), UIColors.cardSecondary(Math.min(fullAlpha, 196)), hoverAnimation.getValue());
        Color buttonColor2 = ColorUtil.interpolate(UIColors.backgroundBlur(Math.min(fullAlpha, 202)), UIColors.card(Math.min(fullAlpha, 184)), hoverAnimation.getValue());

        float fontSize = getHeight() * 0.39f + scaled((float) hoverAnimation.getValue() * 0.18f);
        float round = getWidth() * 0.04f;
        RenderUtil.BLUR_RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeight(), new Vector4f(round), buttonColor1, buttonColor2, buttonColor1, buttonColor2);
        RenderUtil.RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeight(), round, UIColors.stroke(Math.min(fullAlpha, 116)));
        Fonts.PS_MEDIUM.drawWrap(
                matrixStack,
                setting.getName(),
                getX() + scaled(4f),
                getY() + getHeight() / 2f - fontSize / 2f,
                getWidth() - scaled(8f),
                fontSize,
                ColorUtil.setAlpha(UIColors.textColor(), fullAlpha),
                scaled(14f),
                Duration.ofMillis(2600),
                Duration.ofMillis(350)
        );
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (hovered(mouseX, mouseY)) {
            if (setting.getValue() != null) {
                setting.getValue().run();
            }
        }
    }

    private boolean hovered(double mouseX, double mouseY) {
        return MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), scaled(getDefaultHeight()));
    }

    private float getDefaultHeight() {
        return 15f;
    }

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}