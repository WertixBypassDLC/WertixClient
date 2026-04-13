package sweetie.nezi.api.utils.auction;

import lombok.AllArgsConstructor;
import sweetie.nezi.api.module.setting.ModeSetting;

@AllArgsConstructor
public enum ParseModeChoice implements ModeSetting.NamedChoice {
    FUN_TIME("Fun Time");

    private final String name;

    @Override
    public String getName() {
        return name;
    }
}