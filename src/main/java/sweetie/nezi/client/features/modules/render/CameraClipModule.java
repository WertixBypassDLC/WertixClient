package sweetie.nezi.client.features.modules.render;

import lombok.Getter;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.SliderSetting;

@ModuleRegister(name = "Camera Clip", category = Category.RENDER)
public class CameraClipModule extends Module {
    @Getter private static final CameraClipModule instance = new CameraClipModule();

    public final SliderSetting thirdPersonDistance = new SliderSetting("Third person distance").value(4f).range(1f, 10f).step(0.1f);
    public final BooleanSetting firstPersonBypass = new BooleanSetting("First person bypass").value(true);
    public final SliderSetting firstPersonDistance = new SliderSetting("First person distance").value(0.25f).range(0.05f, 1.5f).step(0.05f)
            .setVisible(firstPersonBypass::getValue);

    public CameraClipModule() {
        addSettings(thirdPersonDistance, firstPersonBypass, firstPersonDistance);
    }

    @Override
    public void onEvent() {
    }
}
