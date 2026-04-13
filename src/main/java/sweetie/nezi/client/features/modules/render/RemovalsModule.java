package sweetie.nezi.client.features.modules.render;

import lombok.Getter;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.MultiBooleanSetting;

import java.util.Arrays;

@ModuleRegister(name = "Removals", category = Category.RENDER)
public class RemovalsModule extends Module {
    @Getter private static final RemovalsModule instance = new RemovalsModule();

    private final String[] elements = {
            "Огонь", "Тряска камеры", "Текстуры в стене", "Вода",
            "Скорборд", "Эффект свечения", "Плохие эффекты", "Босс бар"
    };

    private final MultiBooleanSetting remove = new MultiBooleanSetting("Удалить").value(
            Arrays.stream(elements)
                    .map(name -> new BooleanSetting(name).value(false))
                    .toArray(BooleanSetting[]::new)
    );

    public RemovalsModule() {
        addSettings(remove);
    }

    public boolean isFireOverlay()   { return isEnabled() && remove.isEnabled("Огонь"); }
    public boolean isHurtCamera()    { return isEnabled() && remove.isEnabled("Тряска камеры"); }
    public boolean isInwallOverlay() { return isEnabled() && remove.isEnabled("Текстуры в стене"); }
    public boolean isWaterOverlay()  { return isEnabled() && remove.isEnabled("Вода"); }
    public boolean isScoreboard()    { return isEnabled() && remove.isEnabled("Скорборд"); }
    public boolean isGlowEffect()    { return isEnabled() && remove.isEnabled("Эффект свечения"); }
    public boolean isBadEffects()    { return isEnabled() && remove.isEnabled("Плохие эффекты"); }
    public boolean isBossBar()       { return isEnabled() && remove.isEnabled("Босс бар"); }

    @Override
    public void onEvent() {

    }
}