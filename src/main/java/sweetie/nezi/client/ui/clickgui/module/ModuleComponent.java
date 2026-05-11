package sweetie.nezi.client.ui.clickgui.module;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.setting.*;
import sweetie.nezi.api.system.backend.KeyStorage;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.animation.AnimationUtil;
import sweetie.nezi.api.utils.animation.Easing;
import sweetie.nezi.api.utils.math.MouseUtil;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.math.TimerUtil;
import sweetie.nezi.api.utils.other.SearchUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.ScissorUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.ui.clickgui.module.settings.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ModuleComponent extends ExpandableComponent {
    private final List<SettingComponent> settings = new ArrayList<>();
    private final Module module;
    @Setter private float round;
    @Setter private boolean last;
    @Setter private boolean topRounded;
    @Setter private boolean bottomRounded;
    @Setter private int index;

    private boolean bind;

    private final TimerUtil shakeTimer = new TimerUtil();

    private final AnimationUtil enableAnimation = new AnimationUtil();
    private final AnimationUtil bindAnimation = new AnimationUtil();
    private final AnimationUtil hoverAnimation = new AnimationUtil();

    public ModuleComponent(Module module) {
        this.module = module;

        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting bool) {
                settings.add(new BooleanComponent(bool));
            }
            if (setting instanceof MultiBooleanSetting multi) {
                settings.add(new MultiBooleanComponent(multi));
            }
            if (setting instanceof ModeSetting mode) {
                settings.add(new ModeComponent(mode));
            }
            if (setting instanceof SliderSetting slider) {
                settings.add(new SliderComponent(slider));
            }
            if (setting instanceof ColorSetting color) {
                settings.add(new ColorComponent(color));
            }
            if (setting instanceof RunSetting DoniKuni) {
                settings.add(new ButtonComponent(DoniKuni));
            }
            if (setting instanceof BindSetting sex) {
                settings.add(new BindComponent(sex));
            }
            if (setting instanceof StringSetting str) {
                settings.add(new StringComponent(str));
            }
            if (setting instanceof MultiModeSetting multiMode) {
                settings.add(new MultiModeComponent(multiMode));
            }
        }

        enableAnimation.setValue(module.isEnabled() ? 1.0 : 0.0);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrixStack = context.getMatrices();

        updateOpen();
        hoverAnimation.update();
        enableAnimation.update();
        bindAnimation.update();

        bindAnimation.run(bind ? 1.0 : 0.0, 400, Easing.EXPO_OUT);
        hoverAnimation.run(MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getDefaultHeight()) ? 1.0 : 0.0, 300, Easing.QUINT_OUT);
        enableAnimation.run(module.isEnabled() ? 1.0 : 0.0, 200, Easing.EXPO_OUT);

        int fullAlpha = (int) (getAlpha() * 255f);

        int colorSeed = module.getName().hashCode();

        float enabled = (float) enableAnimation.getValue();
        float themedState = 1f - enabled;

        String bindText = (bind ? "Binding: " : "Bind: ") + KeyStorage.getBind(module.getBind());
        String defaultText = module.getName();

        float fontSize = getDefaultHeight() * 0.39f + scaled((float) hoverAnimation.getValue() * 0.22f);

        float openAnim = getAnim();
        Vector4f round = getMainRound(openAnim);

        float nameAnim = 1f - (float) bindAnimation.getValue();
        float bindAnim = (float) bindAnimation.getValue();

        int bindAlpha1 = (int) (nameAnim * getAlpha() * 255f);
        int bindAlpha2 = (int) (bindAnim * getAlpha() * 255f);

        Color idleBg = new Color(24, 24, 32, Math.min(fullAlpha, 188));
        Color activeBg = ColorUtil.interpolate(UIColors.card(Math.min(fullAlpha, 238)), UIColors.themeFlow(colorSeed, Math.min(fullAlpha, 202)), 0.22f);
        Color rectColor = ColorUtil.interpolate(idleBg, activeBg, themedState);
        Color accentColor = ColorUtil.interpolate(new Color(72, 72, 84, Math.min(fullAlpha, 104)), ColorUtil.interpolate(UIColors.primary(Math.min(fullAlpha, 255)), UIColors.textColor(Math.min(fullAlpha, 226)), 0.16f), themedState);
        Color glowColor = ColorUtil.interpolate(new Color(8, 8, 12, Math.min(fullAlpha, 10)), UIColors.themeFlowAlt(colorSeed, Math.min(fullAlpha, 104)), themedState);

        Color inactiveTextColor = new Color(198, 198, 208, bindAlpha1);
        Color themedTextColor = new Color(255, 255, 255, bindAlpha1);
        Color textColor1 = ColorUtil.interpolate(inactiveTextColor, themedTextColor, themedState);
        Color textColor2 = ColorUtil.interpolate(new Color(202, 202, 214, bindAlpha2), new Color(255, 255, 255, bindAlpha2), themedState * 0.72f);
        boolean huesos = bindAnim > 0;

        if (openAnim > 0.0) moduleSetting(context, mouseX, mouseY, delta);

        RenderUtil.BLUR_RECT.draw(matrixStack, getX(), getY(), getWidth(), getDefaultHeight(), round, glowColor, glowColor, glowColor, glowColor, 0.05f);
        RenderUtil.RECT.draw(matrixStack, getX() - scaled(0.35f), getY() - scaled(0.35f), getWidth() + scaled(0.7f), getDefaultHeight() + scaled(0.7f), round, ColorUtil.setAlpha(accentColor, (int) (fullAlpha * (0.34f + themedState * 0.26f))));
        RenderUtil.BLUR_RECT.draw(matrixStack, getX(), getY(), getWidth(), getDefaultHeight(), round, rectColor, rectColor, rectColor, rectColor, 0.04f);
        RenderUtil.RECT.draw(matrixStack, getX(), getY(), getWidth(), getDefaultHeight(), round, rectColor);
        RenderUtil.RECT.draw(matrixStack, getX(), getY(), getWidth(), getDefaultHeight(), round, UIColors.stroke(Math.min(fullAlpha, 130)));

        float badgeWidth = scaled(17.5f);
        float badgeHeight = scaled(8.0f);
        float badgeX = getX() + getWidth() - badgeWidth - scaled(5.2f);
        float badgeY = getY() + getDefaultHeight() / 2f - badgeHeight / 2f;
        Color badgeColor = ColorUtil.interpolate(new Color(38, 38, 48, Math.min(fullAlpha, 160)), ColorUtil.interpolate(UIColors.primary(Math.min(fullAlpha, 214)), UIColors.card(Math.min(fullAlpha, 228)), 0.20f), themedState);
        Color badgeText = ColorUtil.interpolate(new Color(222, 222, 228, fullAlpha), new Color(255, 255, 255, fullAlpha), themedState);
        RenderUtil.BLUR_RECT.draw(matrixStack, badgeX, badgeY, badgeWidth, badgeHeight, badgeHeight / 2f, badgeColor);
        Fonts.PS_BOLD.drawCenteredText(matrixStack, module.isEnabled() ? "ON" : "OFF", badgeX + badgeWidth / 2f, badgeY + badgeHeight / 2f - scaled(2.15f), scaled(4.45f), badgeText);

        if (huesos) ScissorUtil.start(matrixStack, getX(), getY(), getWidth(), getDefaultHeight());
        if (nameAnim > 0) Fonts.PS_BOLD.drawText(matrixStack, defaultText, getX() + scaled(7f), getY() + getDefaultHeight() / 2f - fontSize / 2f + scaled(0.5f), fontSize, textColor1);
        if (bindAnim > 0) Fonts.PS_BOLD.drawCenteredText(matrixStack, bindText, getX() + (getWidth() / 2f) + getWidth() * nameAnim, getY() + getDefaultHeight() / 2f - fontSize / 2f + scaled(0.5f), fontSize, textColor2);
        if (huesos) ScissorUtil.stop(matrixStack);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bind) {
            boolean deleteButton = keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE;
            module.setBind(deleteButton ? -999 : keyCode);
            bind = false;
        }

        if (isNotOver()) return;

        for (SettingComponent setting : settings) {
            // Клавиши работают даже у выключенных модулей, если настройка видима
            if (setting.getVisibleAnimation().getValue() < 0.9) continue;
            setting.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        boolean hoveredToDefault = MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getDefaultHeight());

        if (bind && button != 1 && button != 2 && button != 0) {
            module.setBind(-100 + button);
            bind = false;
            return;
        }

        if (hoveredToDefault) {
            switch (button) {
                case 0 -> module.toggle();
                case 1 -> {
                    if (!settings.isEmpty()) {
                        toggleOpen();
                    }
                    if (!isOpen()) {
                        for (SettingComponent setting : settings) {
                            if (setting instanceof ExpandableSettingComponent e) {
                                e.setOpen(false);
                            }
                        }
                    }
                }
                case 2 -> bind = !bind;
            }
            return;
        }

        if (isNotOver()) return;

        for (SettingComponent setting : settings) {
            if (setting.getVisibleAnimation().getValue() < 0.9) continue;
            setting.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (isNotOver()) return;

        for (SettingComponent setting : settings) {
            if (setting.getVisibleAnimation().getValue() < 0.9) continue;
            setting.mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    }

    private void moduleSetting(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrixStack = context.getMatrices();

        float openAnim = getAnim();
        float reverseAnim = 1f - openAnim;
        float animPad = gap() * reverseAnim;

        Vector4f round = getSettingsRound(openAnim);
        float settingsBottomRound = round.z;

        float enabled = (float) enableAnimation.getValue();
        int openAlpha = (int) (getAlpha() * openAnim * 255f);
        Color offBg = new Color(18, 18, 24, Math.min(openAlpha, 210));
        Color onBg = ColorUtil.interpolate(UIColors.panelSoft(Math.min(openAlpha, 220)), UIColors.primary(Math.min(openAlpha, 18)), 0.12f);
        Color settingsBg = ColorUtil.interpolate(offBg, onBg, enabled);
        RenderUtil.BLUR_RECT.draw(matrixStack, getX(), getY() + getDefaultHeight(), getWidth(), getHeight() - getDefaultHeight(), round,
                settingsBg, settingsBg, settingsBg, settingsBg, 0.045f);
        Color innerOverlay = ColorUtil.interpolate(UIColors.overlay(Math.min(openAlpha, 36)), UIColors.primary(Math.min(openAlpha, 16)), enabled * 0.35f);
        RenderUtil.RECT.draw(matrixStack, getX() + scaled(0.65f), getY() + getDefaultHeight(), getWidth() - scaled(1.3f), getHeight() - getDefaultHeight() - scaled(0.65f), new Vector4f(0f, 0f, Math.max(0f, settingsBottomRound - scaled(0.45f)), Math.max(0f, settingsBottomRound - scaled(0.45f))), innerOverlay);
        Color borderColor = ColorUtil.interpolate(UIColors.stroke(Math.min(openAlpha, 100)), UIColors.primary(Math.min(openAlpha, 80)), enabled * 0.28f);
        RenderUtil.RECT.draw(matrixStack, getX(), getY() + getDefaultHeight(), getWidth(), getHeight() - getDefaultHeight(), round, borderColor);

        float govnarik = offset() * (1f + 0.7f * reverseAnim);

        float doni = gap() - animPad;
        float componentY = getY() + (getDefaultHeight() * openAnim) + doni + scaled(1f);

        for (SettingComponent setting : settings) {
            setting.getVisibleAnimation().update();
            setting.getVisibleAnimation().run(setting.getSetting().isVisible() ? 1.0 : 0.0, 120, Easing.SINE_OUT);
            float visibleAnim = (float) setting.getVisibleAnimation().getValue();
            if (setting.getVisibleAnimation().getValue() > 0.0) {
                setting.setX(getX() + govnarik);
                setting.setY(componentY);
                setting.setWidth(getWidth() - govnarik * 2f);
                setting.setAlpha(visibleAnim * openAnim * getAlpha());
                setting.setModuleEnabled(enabled);

                setting.render(context, mouseX, mouseY, delta);
                componentY += ((setting.getHeight() + gap()) * visibleAnim) * openAnim;
            }
        }
    }

    public float getDefaultHeight() {
        return scaled(16.5f);
    }

    public boolean matchesSearch(String query) {
        return SearchUtil.matches(module.getName(), query);
    }

    public boolean isCapturingInput() {
        if (bind) return true;

        for (SettingComponent setting : settings) {
            if (setting instanceof BindComponent bindComponent && bindComponent.isBinding()) return true;
            if (setting instanceof StringComponent stringComponent && stringComponent.isTyping()) return true;
        }

        return false;
    }

    private Vector4f getMainRound(float openAnim) {
        float radius = getRound() * 2f;
        float top = topRounded ? radius : 0f;
        float bottom = bottomRounded && openAnim <= 0.02f ? radius : 0f;
        return new Vector4f(top, top, bottom, bottom);
    }

    private Vector4f getSettingsRound(float openAnim) {
        float bottom = bottomRounded ? getRound() * 2f * openAnim : 0f;
        return new Vector4f(0f, 0f, bottom, bottom);
    }
}