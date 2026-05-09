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

        // --- Цвет фона под худ (база 23,23,34) ---
        // Idle (выкл): чуть темнее, без радужности
        Color idle1 = new Color(20, 20, 28, Math.min(fullAlpha, 200));
        Color idle2 = new Color(16, 16, 22, Math.min(fullAlpha, 195));

        int colorSeed = module.getName().hashCode();

        // Active: худ-база слегка смешана с темой (не радужный)
        Color hudBase = new Color(28, 28, 40, Math.min(fullAlpha, 215));
        Color activeBg1 = ColorUtil.interpolate(hudBase, UIColors.themeFlow(colorSeed, Math.min(fullAlpha, 150)), 0.30f);
        Color activeBg2 = ColorUtil.interpolate(hudBase, UIColors.themeFlowAlt(colorSeed, Math.min(fullAlpha, 130)), 0.26f);

        float enabled = (float) enableAnimation.getValue();

        String bindText = (bind ? "Binding: " : "Bind: ") + KeyStorage.getBind(module.getBind());
        String defaultText = module.getName();

        float fontSize = getDefaultHeight() * 0.39f + scaled((float) hoverAnimation.getValue() * 0.22f);

        float openAnim = getAnim();
        Vector4f round = getMainRound(openAnim);

        float nameAnim = 1f - (float) bindAnimation.getValue();
        float bindAnim = (float) bindAnimation.getValue();

        int bindAlpha1 = (int) (nameAnim * getAlpha() * 255f);
        int bindAlpha2 = (int) (bindAnim * getAlpha() * 255f);

        Color rectColor1 = ColorUtil.interpolate(activeBg1, idle1, enabled);
        Color rectColor2 = ColorUtil.interpolate(activeBg2, idle2, enabled);

        Color inactiveTextColor = new Color(170, 170, 180, bindAlpha1);
        Color textColor1 = ColorUtil.interpolate(new Color(255, 255, 255, bindAlpha1), inactiveTextColor, enabled);
        Color textColor2 = ColorUtil.interpolate(new Color(188, 188, 198, bindAlpha2), new Color(255, 255, 255, bindAlpha2), 0.52f);
        boolean huesos = bindAnim > 0;

        if (openAnim > 0.0) moduleSetting(context, mouseX, mouseY, delta);

        // Рендер фона модуля
        RenderUtil.BLUR_RECT.draw(matrixStack, getX(), getY(), getWidth(), getDefaultHeight(), round,
                rectColor1, rectColor1, rectColor2, rectColor2);

        RenderUtil.BLUR_RECT.draw(matrixStack, getX() + scaled(1f), getY() + scaled(1f), getWidth() - scaled(2f), getDefaultHeight() - scaled(2f),
                new Vector4f(Math.max(0f, round.x - scaled(0.5f)), Math.max(0f, round.y - scaled(0.5f)), Math.max(0f, round.z - scaled(0.5f)), Math.max(0f, round.w - scaled(0.5f))),
                UIColors.overlay(Math.min(fullAlpha, 58)));

        RenderUtil.RECT.draw(matrixStack, getX(), getY(), getWidth(), getDefaultHeight(), round, UIColors.stroke(fullAlpha));

        if (huesos) ScissorUtil.start(matrixStack, getX(), getY(), getWidth(), getDefaultHeight());
        if (nameAnim > 0) Fonts.PS_BOLD.drawCenteredText(matrixStack, defaultText, getX() + (getWidth() / 2f - offset() * openAnim) * nameAnim, getY() + getDefaultHeight() / 2f - fontSize / 2f + scaled(0.5f), fontSize, textColor1);
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

        // Выкл модуль → серая панель настроек (без радужного фона), но настройки полностью читаемы
        boolean modOn = module.isEnabled();
        int openAlpha = (int) (getAlpha() * openAnim * 255f);
        if (modOn) {
            RenderUtil.BLUR_RECT.draw(matrixStack, getX(), getY() + getDefaultHeight(), getWidth(), getHeight() - getDefaultHeight(), round,
                    UIColors.panelSoft(openAlpha));
        } else {
            // Серый худ-цвет (под HUD-карты)
            int a = (int) (openAlpha * 0.92f);
            Color graySoft = new Color(20, 20, 28, Math.min(255, a));
            RenderUtil.BLUR_RECT.draw(matrixStack, getX(), getY() + getDefaultHeight(), getWidth(), getHeight() - getDefaultHeight(), round, graySoft);
        }
        RenderUtil.BLUR_RECT.draw(matrixStack, getX() + scaled(1f), getY() + getDefaultHeight(), getWidth() - scaled(2f), getHeight() - getDefaultHeight() - scaled(1f), new Vector4f(0f, 0f, Math.max(0f, settingsBottomRound - scaled(0.5f)), Math.max(0f, settingsBottomRound - scaled(0.5f))), UIColors.overlay(openAlpha));
        RenderUtil.RECT.draw(matrixStack, getX(), getY() + getDefaultHeight(), getWidth(), getHeight() - getDefaultHeight(), round, UIColors.stroke(openAlpha));

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

                setting.render(context, mouseX, mouseY, delta);
                componentY += ((setting.getHeight() + gap()) * visibleAnim) * openAnim;
            }
        }
    }

    public float getDefaultHeight() {
        return scaled(18f);
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