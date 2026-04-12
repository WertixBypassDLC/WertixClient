package sweetie.nezi.client.ui.auction;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.setting.*;
import sweetie.nezi.api.utils.animation.AnimationUtil;
import sweetie.nezi.api.utils.animation.Easing;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.MouseUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.ScissorUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.services.RenderService;
import sweetie.nezi.client.ui.clickgui.module.SettingComponent;
import sweetie.nezi.client.ui.clickgui.module.ExpandableComponent;
import sweetie.nezi.client.ui.clickgui.module.settings.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Панель настроек модуля, рендерится прямо на экране аукциона.
 * По сути — один ModuleComponent из ClickGUI, но без категорий/панелей.
 */
public class AuctionSettingsPanel {
    private final Module module;
    private final List<SettingComponent> settings = new ArrayList<>();
    private final AnimationUtil openAnimation = new AnimationUtil();

    private float x, y, width;
    private float scroll = 0;
    private boolean open = true;
    private boolean dragging = false;
    private float dragOffsetX, dragOffsetY;

    public AuctionSettingsPanel(Module module) {
        this.module = module;
        openAnimation.setValue(1.0);

        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting bool) {
                settings.add(new BooleanComponent(bool));
            } else if (setting instanceof MultiBooleanSetting multi) {
                settings.add(new MultiBooleanComponent(multi));
            } else if (setting instanceof ModeSetting mode) {
                settings.add(new ModeComponent(mode));
            } else if (setting instanceof MultiModeSetting multiMode) {
                settings.add(new MultiModeComponent(multiMode));
            } else if (setting instanceof SliderSetting slider) {
                settings.add(new SliderComponent(slider));
            } else if (setting instanceof ColorSetting color) {
                settings.add(new ColorComponent(color));
            } else if (setting instanceof StringSetting str) {
                settings.add(new StringComponent(str));
            }
        }
    }

    public void setPosition(float x, float y, float width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    private float scaled(float value) {
        return RenderService.getInstance().scaled(value);
    }

    private float gap() {
        return scaled(2f);
    }

    private float offset() {
        return gap() * 1.5f;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        openAnimation.update();
        openAnimation.run(open ? 1.0 : 0.0, 250, Easing.EXPO_OUT);
        float anim = (float) openAnimation.getValue();
        if (anim <= 0.01) return;

        MatrixStack ms = context.getMatrices();

        float headerH = scaled(20f);
        float fontSize = scaled(7f);
        float totalContentH = 0;

        // Вычисляем общую высоту контента
        for (SettingComponent setting : settings) {
            setting.getVisibleAnimation().update();
            setting.getVisibleAnimation().run(setting.getSetting().isVisible() ? 1.0 : 0.0, 120, Easing.SINE_OUT);
            float visAnim = (float) setting.getVisibleAnimation().getValue();
            if (visAnim > 0) {
                totalContentH += (setting.getHeight() + gap()) * visAnim;
            }
        }

        float maxH = scaled(300f);
        float contentH = Math.min(totalContentH, maxH);
        float panelH = (headerH + contentH + gap() * 2) * anim;
        float round = scaled(4f);

        // Фон панели
        RenderUtil.BLUR_RECT.draw(ms, x, y, width, panelH, new Vector4f(round, round, round, round),
                UIColors.panelSoft(200));
        RenderUtil.BLUR_RECT.draw(ms, x + scaled(1f), y + scaled(1f),
                width - scaled(2f), panelH - scaled(2f),
                new Vector4f(round - scaled(0.5f), round - scaled(0.5f), round - scaled(0.5f), round - scaled(0.5f)),
                UIColors.overlay(200));

        // Заголовок
        RenderUtil.BLUR_RECT.draw(ms, x, y, width, headerH, new Vector4f(round, round, 0, 0),
                UIColors.primary(180));
        Fonts.PS_BOLD.drawCenteredText(ms, module.getName(), x + width / 2f,
                y + headerH / 2f - fontSize / 2f + scaled(0.5f), fontSize, UIColors.textColor(255));

        if (anim < 0.1) return;

        // Контент с clip
        float contentY = y + headerH;
        ScissorUtil.start(ms, x, contentY, width, contentH * anim + gap());

        float componentY = contentY + gap() - scroll;
        float govnarik = offset();

        for (SettingComponent setting : settings) {
            float visAnim = (float) setting.getVisibleAnimation().getValue();
            if (visAnim > 0) {
                setting.setX(x + govnarik);
                setting.setY(componentY);
                setting.setWidth(width - govnarik * 2f);
                setting.setAlpha(visAnim * anim);

                setting.render(context, mouseX, mouseY, delta);
                componentY += (setting.getHeight() + gap()) * visAnim;
            }
        }

        ScissorUtil.stop(ms);

        // Скроллбар
        if (totalContentH > maxH) {
            float barH = contentH * (contentH / totalContentH);
            float barY = contentY + (scroll / totalContentH) * contentH;
            RenderUtil.RECT.draw(ms, x + width - scaled(2f), barY, scaled(2f), barH, scaled(1f),
                    UIColors.primary(120));
        }
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        float headerH = scaled(20f);

        // Клик по заголовку — toggle panel / drag
        if (MouseUtil.isHovered(mouseX, mouseY, x, y, width, headerH)) {
            if (button == 1) {
                open = !open;
            } else if (button == 0) {
                dragging = true;
                dragOffsetX = (float) mouseX - x;
                dragOffsetY = (float) mouseY - y;
            }
            return;
        }

        if (!open || openAnimation.getValue() < 0.5) return;

        for (SettingComponent setting : settings) {
            if (setting.getAlpha() < 0.2) continue;
            setting.mouseClicked(mouseX, mouseY, button);
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;

        if (!open) return;
        for (SettingComponent setting : settings) {
            if (setting.getAlpha() < 0.2) continue;
            setting.mouseReleased(mouseX, mouseY, button);
        }
    }

    public void mouseDragged(double mouseX, double mouseY) {
        if (dragging) {
            x = (float) mouseX - dragOffsetX;
            y = (float) mouseY - dragOffsetY;
        }
    }

    public void mouseScrolled(double mouseX, double mouseY, double amount) {
        float maxH = scaled(300f);
        float totalContentH = 0;
        for (SettingComponent setting : settings) {
            float visAnim = (float) setting.getVisibleAnimation().getValue();
            if (visAnim > 0) {
                totalContentH += (setting.getHeight() + gap()) * visAnim;
            }
        }

        if (totalContentH > maxH) {
            scroll -= (float) amount * scaled(10f);
            scroll = Math.max(0, Math.min(scroll, totalContentH - maxH));
        }
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!open) return;
        for (SettingComponent setting : settings) {
            if (setting.getAlpha() < 0.2) continue;
            setting.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public boolean isOpen() {
        return open;
    }
}
