package sweetie.nezi.client.ui.clickgui.module.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import sweetie.nezi.api.module.setting.BindSetting;
import sweetie.nezi.api.system.backend.KeyStorage;
import sweetie.nezi.api.utils.animation.AnimationUtil;
import sweetie.nezi.api.utils.animation.Easing;
import sweetie.nezi.api.utils.color.UIColors;
import java.awt.Color;
import sweetie.nezi.api.utils.math.MouseUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.ScissorUtil;
import sweetie.nezi.api.utils.render.fonts.Font;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.ui.clickgui.module.SettingComponent;

public class BindComponent extends SettingComponent {
    private final BindSetting setting;

    private final AnimationUtil animation = new AnimationUtil();

    private boolean bind;

    public BindComponent(BindSetting setting) {
        super(setting);
        this.setting = setting;
        updateHeight(getDefaultHeight());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateHeight(getDefaultHeight());

        MatrixStack matrixStack = context.getMatrices();

        animation.update();
        animation.run(bind ? 1.0 : 0.0, 500, Easing.EXPO_OUT);

        float fontSize = getHeight() * 0.45f;
        float halfY = getY() + getHeight() / 2f;
        int fullAlpha = (int) (getAlpha() * 255f);

        Font mediumFont = Fonts.PS_MEDIUM;

        float anim = (float) animation.getValue();
        float reverseAnim = (float) (1.0 - animation.getValue());

        float valueSize = fontSize * 0.9f;
        String noneText = "None";
        String valueText = setting.getValue() == -999 ? noneText : KeyStorage.getBind(setting.getValue());
        String bindingText = "...";
        float valueWidth = mediumFont.getWidth(valueText, valueSize);
        float bindingWidth = mediumFont.getWidth(bindingText, valueSize);

        float totalWidth = (valueWidth * reverseAnim) + (bindingWidth * anim);

        float valueY = halfY - valueSize / 2f;

        float bindX = getX() + getWidth() - totalWidth - offset() * 2f;
        float bindY = valueY - offset();
        float bindWidth = totalWidth + offset() * 2f;
        float bindHeight = valueSize + offset() * 2f;
        float bindRound = bindHeight * 0.24f;
        mediumFont.drawText(matrixStack, setting.getName(), getX() + scaled(2f), halfY - fontSize / 2f, fontSize, UIColors.textColor(fullAlpha));
        Color badgeBg = bind ? UIColors.primary(Math.min(fullAlpha, 140)) : UIColors.panelSoft(Math.min(fullAlpha, 130));
        Color badgeStroke = bind ? UIColors.primary(Math.min(fullAlpha, 180)) : UIColors.stroke(Math.min(fullAlpha, 100));
        RenderUtil.BLUR_RECT.draw(matrixStack, bindX, bindY, bindWidth, bindHeight, bindRound, badgeBg);
        RenderUtil.RECT.draw(matrixStack, bindX, bindY, bindWidth, bindHeight, bindRound, badgeStroke);

        ScissorUtil.start(matrixStack, bindX, bindY, bindWidth, bindHeight);

        if (reverseAnim > 0)
            mediumFont.drawText(matrixStack, valueText, bindX + offset(), valueY, valueSize, UIColors.textColor(getAlphaFrom(reverseAnim)));

        if (anim > 0)
            mediumFont.drawText(matrixStack, bindingText, bindX + offset(), valueY, valueSize, UIColors.textColor(getAlphaFrom(anim)));

        ScissorUtil.stop(matrixStack);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bind) {
            setting.setValue(keyCode == GLFW.GLFW_KEY_DELETE ? -999 : keyCode);
            bind = false;
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (bind && button != 1 && button != 0) {
            setting.setValue(-100 + button);
            bind = false;
            return;
        }

        if (MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            bind = !bind;
        }
    }

    private int getAlphaFrom(float anim) {
        return (int) (anim * getAlpha() * 255f);
    }

    private float getDefaultHeight() {
        return 15;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {

    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {

    }

    public boolean isBinding() {
        return bind;
    }
}