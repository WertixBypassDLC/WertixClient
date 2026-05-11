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

        float fontSize = getHeight() * 0.39f + scaled((float) hoverAnimation.getValue() * 0.18f);
        Color textColor = ColorUtil.interpolate(UIColors.mutedText(fullAlpha), UIColors.textColor(fullAlpha), hoverAnimation.getValue());
        Fonts.PS_MEDIUM.drawWrap(
                matrixStack,
                setting.getName(),
                getX() + scaled(2f),
                getY() + getHeight() / 2f - fontSize / 2f,
                getWidth() - scaled(14f),
                fontSize,
                textColor,
                scaled(14f),
                Duration.ofMillis(2600),
                Duration.ofMillis(350)
        );
        Fonts.PS_BOLD.drawText(matrixStack, "\u2192", getX() + getWidth() - scaled(8f), getY() + getHeight() / 2f - scaled(2.6f), scaled(5.0f), UIColors.textColor((int) (fullAlpha * (0.72f + hoverAnimation.getValue() * 0.28f))));
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