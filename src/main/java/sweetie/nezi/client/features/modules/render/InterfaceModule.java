package sweetie.nezi.client.features.modules.render;

import lombok.Getter;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.ModeSetting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.module.setting.MultiBooleanSetting;
import sweetie.nezi.api.system.language.ClientLanguage;
import sweetie.nezi.api.system.language.LanguageManager;
import sweetie.nezi.client.services.RenderService;
import sweetie.nezi.client.ui.widget.WidgetManager;

@ModuleRegister(name = "HUD", category = Category.RENDER)
public class InterfaceModule extends Module {
    @Getter private static final InterfaceModule instance = new InterfaceModule();

    private static final float GLASSY = 0.68f;
    private static final int PASSES = 14;
    private static final float OFFSET = 12f;

    public final MultiBooleanSetting widgets = new MultiBooleanSetting("Виджеты");
    public final ModeSetting language = new ModeSetting("Language")
            .values("Russian", "English")
            .value("Russian")
            .onAction(this::syncLanguage);
    public final SliderSetting scale = new SliderSetting("Масштаб").value(0.85f).range(0.6f, 1.5f).step(0.05f).onAction(() -> RenderService.getInstance().updateScale());
    public final SliderSetting widgetScale = new SliderSetting("Размер HUD").value(0.9f).range(0.5f, 2.0f).step(0.05f);
    public final BooleanSetting hudParticles = new BooleanSetting("Частицы HUD").value(true);
    public static float getScale() { return getInstance().scale.getValue(); }
    public static float getWidgetScale() { return getInstance().widgetScale.getValue(); }
    public static float getGlassy() { return GLASSY; }
    public static int getPasses() { return PASSES; }
    public static float getOffset() { return OFFSET; }
    public static boolean isHudParticlesEnabled() { return getInstance().hudParticles.getValue(); }

    public InterfaceModule() {
        syncLanguage();
    }

    public void init() {
        widgets.value(WidgetManager.getInstance().getWidgets().stream()
                .map(widget -> {
                    BooleanSetting setting = new BooleanSetting(widget.getName()).value(widget.isEnabled());
                    setting.onAction(() -> widget.setEnabled(setting.getValue()));
                    return setting;
                })
                .toList());

        addSettings(widgets, language, scale, widgetScale, hudParticles);
    }

    @Override
    public void onEvent() {

    }

    private void syncLanguage() {
        LanguageManager.getInstance().setCurrentLanguage(ClientLanguage.fromModeName(language.getValue()));
    }
}
