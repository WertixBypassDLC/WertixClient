package sweetie.nezi.client.ui.clickgui.module;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import sweetie.nezi.api.module.setting.Setting;
import sweetie.nezi.api.utils.animation.AnimationUtil;
import sweetie.nezi.client.ui.UIComponent;

@Getter
@RequiredArgsConstructor
public abstract class SettingComponent extends UIComponent {
    private final Setting<?> setting;
    private final AnimationUtil visibleAnimation = new AnimationUtil();
    @Setter private float moduleEnabled = 1f;

    public void updateHeight(float value) {
        setHeight(scaled(value));
    }
}
