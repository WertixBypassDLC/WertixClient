package sweetie.nezi.client.features.modules.other;

import lombok.Getter;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.ModeSetting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.utils.other.SoundUtil;

@ModuleRegister(name = "Toggle Sounds", category = Category.OTHER)
public class ToggleSoundsModule extends Module {

    @Getter
    private static final ToggleSoundsModule instance = new ToggleSoundsModule();

    private final ModeSetting sound = new ModeSetting("Sound")
            .value("Blop")
            .values("Blop", "Celestial");

    public final SliderSetting volume =
            new SliderSetting("Volume").value(60f).range(1f, 100f).step(1f);

    public ToggleSoundsModule() {
        addSettings(sound, volume);
    }

    public static void playToggle(boolean state) {
        if (!instance.isEnabled()) return;

        SoundUtil.playSound(
                switch (instance.sound.getValue()) {
                    case "Celestial" ->
                            state ? SoundUtil.ENABLE_CEL_EVENT : SoundUtil.DISABLE_CEL_EVENT;
                    case "Blop" ->
                            state ? SoundUtil.ENABLE_BLOP_EVENT : SoundUtil.DISABLE_BLOP_EVENT;
                    default -> null;
                }
        );
    }

    @Override
    public void onEvent() {
        // nothing
    }
}
