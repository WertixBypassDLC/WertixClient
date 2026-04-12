package sweetie.nezi.client.ui.clickgui.module.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import sweetie.nezi.api.module.setting.StringSetting;
import sweetie.nezi.api.system.language.LanguageManager;
import sweetie.nezi.api.utils.animation.AnimationUtil;
import sweetie.nezi.api.utils.animation.Easing;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.MouseUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.ScissorUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.ui.clickgui.module.SettingComponent;

import java.awt.*;

public class StringComponent extends SettingComponent {
    private final StringSetting setting;
    private boolean typing;
    private String currentString;

    private final AnimationUtil focusAnimation = new AnimationUtil();

    public StringComponent(StringSetting setting) {
        super(setting);
        this.setting = setting;
        this.currentString = setting.getText();
        updateHeight(getDefaultHeight());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateHeight(getDefaultHeight());

        focusAnimation.update();
        focusAnimation.run(typing ? 1.0 : 0.0, 300, Easing.EXPO_OUT);

        MatrixStack matrixStack = context.getMatrices();

        float fontSize = fontSize();
        int fullAlpha = (int) (getAlpha() * 255f);
        float labelOffset = scaled(0.5f);

        Fonts.PS_MEDIUM.drawText(matrixStack, LanguageManager.getInstance().getClickGuiText(setting.getName()), getX() + labelOffset, getY() + labelOffset, fontSize, UIColors.textColor(fullAlpha));

        float boxHeight = scaled(12f);
        float boxY = getY() + fontSize + scaled(3f);
        float boxX = getX();
        float boxWidth = getWidth();
        float round = boxHeight * 0.25f;

        Color idleColor = UIColors.backgroundBlur(fullAlpha);
        Color focusColor = new Color(UIColors.primary().getRed(), UIColors.primary().getGreen(), UIColors.primary().getBlue(), Math.min(255, fullAlpha));
        Color boxColor = ColorUtil.interpolate(idleColor, focusColor, focusAnimation.getValue() * 0.3f);

        RenderUtil.BLUR_RECT.draw(matrixStack, boxX, boxY, boxWidth, boxHeight, new Vector4f(round), boxColor);

        String displayString = currentString;
        if (typing && (System.currentTimeMillis() % 1000 > 500)) {
            displayString += "_";
        } else if (currentString.isEmpty() && !typing) {
            displayString = "Type here...";
        }

        Color textColor = currentString.isEmpty() && !typing ? UIColors.inactiveTextColor(fullAlpha) : UIColors.textColor(fullAlpha);

        ScissorUtil.start(matrixStack, boxX + scaled(2f), boxY, boxWidth - scaled(4f), boxHeight);
        Fonts.PS_MEDIUM.drawText(matrixStack, displayString, boxX + scaled(4f), boxY + boxHeight / 2f - fontSize / 2f, fontSize, textColor);
        ScissorUtil.stop(matrixStack);

        setHeight((boxY + boxHeight) - getY() + scaled(4f));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        float boxHeight = scaled(12f);
        float boxY = getY() + fontSize() + scaled(3f);

        boolean hovered = MouseUtil.isHovered(mouseX, mouseY, getX(), boxY, getWidth(), boxHeight);

        if (button == 0) {
            if (hovered) {
                typing = true;
            } else if (typing) {
                typing = false;
                setting.setValue(currentString);
            }
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!typing) {
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!currentString.isEmpty()) {
                currentString = currentString.substring(0, currentString.length() - 1);
            }
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            typing = false;
            setting.setValue(currentString);
            return;
        }

        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;

        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            char letter = (char) keyCode;
            appendChar(shift ? letter : Character.toLowerCase(letter));
            return;
        }

        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            appendChar((char) keyCode);
            return;
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE -> appendChar(' ');
            case GLFW.GLFW_KEY_MINUS -> appendChar(shift ? '_' : '-');
            case GLFW.GLFW_KEY_COMMA -> appendChar(shift ? '<' : ',');
            case GLFW.GLFW_KEY_PERIOD -> appendChar(shift ? '>' : '.');
            case GLFW.GLFW_KEY_SEMICOLON -> appendChar(shift ? ':' : ';');
            case GLFW.GLFW_KEY_SLASH -> appendChar(shift ? '?' : '/');
            case GLFW.GLFW_KEY_EQUAL -> appendChar(shift ? '+' : '=');
        }
    }

    private void appendChar(char c) {
        if (currentString.length() < 128) {
            currentString += c;
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {}

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}

    private float fontSize() {
        return scaled(15f) * 0.45f;
    }

    private float getDefaultHeight() {
        return fontSize() + scaled(16f);
    }

    public boolean isTyping() {
        return typing;
    }
}
