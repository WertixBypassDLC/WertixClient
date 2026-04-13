package sweetie.nezi.client.features.modules.render;

import lombok.Getter;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.module.setting.RunSetting;

@ModuleRegister(name = "View Model", category = Category.RENDER)
public class ViewModelModule extends Module {
    @Getter private static final ViewModelModule instance = new ViewModelModule();

    public final SliderSetting rightX = new SliderSetting("Правая X").value(0f).range(-2f, 2f).step(0.1f);
    public final SliderSetting rightY = new SliderSetting("Правая Y").value(0f).range(-2f, 2f).step(0.1f);
    public final SliderSetting rightZ = new SliderSetting("Правая Z").value(0f).range(-2f, 2f).step(0.1f);
    public final SliderSetting leftX = new SliderSetting("Левая X").value(0f).range(-2f, 2f).step(0.1f);
    public final SliderSetting leftY = new SliderSetting("Левая Y").value(0f).range(-2f, 2f).step(0.1f);
    public final SliderSetting leftZ = new SliderSetting("Левая Z").value(0f).range(-2f, 2f).step(0.1f);

    private final RunSetting reset = new RunSetting("Сброс позиции").value(this::resetPos);

    public ViewModelModule() {
        addSettings(rightX, rightY, rightZ, leftX, leftY, leftZ, reset);
    }

    @Override
    public void onEvent() {

    }

    private void resetPos() {
        getSettings().forEach(setting -> {
            if (setting instanceof SliderSetting f) {
                f.setValue(0f);
            }
        });
    }
}
