package sweetie.nezi.client.features.modules.render;

import lombok.Getter;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.SliderSetting;

@ModuleRegister(name = "AspectRatio", category = Category.RENDER)
public class AspectRatioModule extends Module {
    @Getter
    private static final AspectRatioModule instance = new AspectRatioModule();
    public final SliderSetting ratio = new SliderSetting("Ratio").value(1.33f).range(0.5f, 3.0f).step(0.01f);

    public AspectRatioModule() {
        addSettings(ratio);
    }

    @Override
    public void onEvent() {
    }
}